package com.hady.saas.business.service;

import com.hady.saas.application.dto.CobrancaAdminDTO;
import com.hady.saas.application.dto.InadimplenteDTO;
import com.hady.saas.application.dto.PontoSerieDTO;
import com.hady.saas.application.dto.ResumoFinanceiroDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FinanceiroMasterService {

    private final JdbcTemplate jdbcTemplate;

    public FinanceiroMasterService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── Resumo financeiro completo ───────────────────────────────────
    @Transactional(readOnly = true)
    public ResumoFinanceiroDTO gerarResumo(LocalDate inicio, LocalDate fim) {
        ResumoFinanceiroDTO dto = new ResumoFinanceiroDTO();

        LocalDate dataInicio = inicio != null ? inicio : LocalDate.now().withDayOfMonth(1);
        LocalDate dataFim = fim != null ? fim : LocalDate.now();

        BigDecimal receitaPeriodo = queryBigDecimal("""
                SELECT COALESCE(SUM(valor), 0) FROM cobrancas_plano
                WHERE status = 'AUTORIZADO'
                  AND criado_em::date BETWEEN ? AND ?
                """, dataInicio, dataFim);
        dto.setReceitaTotalPeriodo(receitaPeriodo);

        BigDecimal mrr = queryBigDecimal("""
                SELECT COALESCE(SUM(valor), 0) FROM cobrancas_plano
                WHERE tipo_cobranca = 'RECORRENTE'
                  AND status = 'AUTORIZADO'
                  AND periodo_inicio <= CURRENT_DATE
                  AND periodo_fim >= CURRENT_DATE
                """);
        dto.setMrrAtual(mrr);

        Long totalInadimplentes = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM estabelecimentos
                WHERE plano_ativo = false
                  AND plano_valido_ate IS NOT NULL
                  AND plano_valido_ate < CURRENT_DATE
                  AND NOT (trial_ativo = true AND trial_fim >= CURRENT_DATE)
                """, Long.class);
        dto.setTotalInadimplentes(totalInadimplentes != null ? totalInadimplentes : 0);

        // Valor em atraso: soma do valor do plano atual de cada inadimplente,
        // usando a última cobrança recorrente registrada como referência de valor.
        BigDecimal valorAtraso = queryBigDecimal("""
                SELECT COALESCE(SUM(cp.valor), 0)
                FROM estabelecimentos e
                JOIN LATERAL (
                    SELECT valor FROM cobrancas_plano
                    WHERE estabelecimento_id = e.id
                    ORDER BY criado_em DESC LIMIT 1
                ) cp ON true
                WHERE e.plano_ativo = false
                  AND e.plano_valido_ate IS NOT NULL
                  AND e.plano_valido_ate < CURRENT_DATE
                  AND NOT (e.trial_ativo = true AND e.trial_fim >= CURRENT_DATE)
                """);
        dto.setValorEmAtraso(valorAtraso);

        dto.setReceitaPorPlano(receitaPorPlano(dataInicio, dataFim));
        dto.setProjecaoMrr3Meses(projetarMrr());

        return dto;
    }

    // ── Receita agrupada por plano, no período ──────────────────────────
    private Map<String, BigDecimal> receitaPorPlano(LocalDate inicio, LocalDate fim) {
        Map<String, BigDecimal> resultado = new LinkedHashMap<>();

        jdbcTemplate.query("""
                SELECT e.plano, COALESCE(SUM(cp.valor), 0) as total
                FROM cobrancas_plano cp
                JOIN estabelecimentos e ON e.id = cp.estabelecimento_id
                WHERE cp.status = 'AUTORIZADO'
                  AND cp.criado_em::date BETWEEN ? AND ?
                GROUP BY e.plano
                ORDER BY e.plano
                """, rs -> {
            resultado.put(rs.getString("plano"), rs.getBigDecimal("total"));
        }, Date.valueOf(inicio), Date.valueOf(fim));

        return resultado;
    }

    // ── Projeção simples de MRR pros próximos 3 meses ────────────────────
    // Assume que o MRR atual se mantém estável, sem novos cadastros nem
    // cancelamentos — é uma projeção conservadora de "piso", não uma
    // previsão de crescimento. Serve como referência mínima de caixa.
    private List<PontoSerieDTO> projetarMrr() {
        BigDecimal mrrAtual = queryBigDecimal("""
                SELECT COALESCE(SUM(valor), 0) FROM cobrancas_plano
                WHERE tipo_cobranca = 'RECORRENTE'
                  AND status = 'AUTORIZADO'
                  AND periodo_inicio <= CURRENT_DATE
                  AND periodo_fim >= CURRENT_DATE
                """);

        List<PontoSerieDTO> projecao = new java.util.ArrayList<>();
        YearMonth mesAtual = YearMonth.now();
        for (int i = 1; i <= 3; i++) {
            YearMonth mes = mesAtual.plusMonths(i);
            String label = capitalizar(mes.getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt", "BR")))
                    .replace(".", "") + "/" + mes.getYear();
            projecao.add(new PontoSerieDTO(label, mrrAtual));
        }
        return projecao;
    }

    // ── Lista de cobranças/pagamentos com filtro opcional por status ────
    @Transactional(readOnly = true)
    public List<CobrancaAdminDTO> listarCobrancas(String status, LocalDate inicio, LocalDate fim) {
        StringBuilder sql = new StringBuilder("""
                SELECT cp.id, cp.estabelecimento_id, e.nome as nome_estabelecimento,
                       e.plano, cp.tipo_cobranca, cp.status, cp.valor,
                       cp.periodo_inicio, cp.periodo_fim, cp.criado_em
                FROM cobrancas_plano cp
                JOIN estabelecimentos e ON e.id = cp.estabelecimento_id
                WHERE 1=1
                """);

        List<Object> params = new java.util.ArrayList<>();

        if (status != null && !status.isBlank()) {
            sql.append(" AND cp.status = ? ");
            params.add(status);
        }
        if (inicio != null) {
            sql.append(" AND cp.criado_em::date >= ? ");
            params.add(Date.valueOf(inicio));
        }
        if (fim != null) {
            sql.append(" AND cp.criado_em::date <= ? ");
            params.add(Date.valueOf(fim));
        }

        sql.append(" ORDER BY cp.criado_em DESC ");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new CobrancaAdminDTO(
                rs.getLong("id"),
                rs.getLong("estabelecimento_id"),
                rs.getString("nome_estabelecimento"),
                rs.getString("plano"),
                rs.getString("tipo_cobranca"),
                rs.getString("status"),
                rs.getBigDecimal("valor"),
                toLocalDate(rs.getDate("periodo_inicio")),
                toLocalDate(rs.getDate("periodo_fim")),
                toLocalDateTime(rs.getTimestamp("criado_em"))
        ), params.toArray());
    }

    // ── Histórico de pagamentos de UM estabelecimento específico ────────
    @Transactional(readOnly = true)
    public List<CobrancaAdminDTO> listarCobrancasPorEstabelecimento(Long estabelecimentoId) {
        String sql = """
                SELECT cp.id, cp.estabelecimento_id, e.nome as nome_estabelecimento,
                       e.plano, cp.tipo_cobranca, cp.status, cp.valor,
                       cp.periodo_inicio, cp.periodo_fim, cp.criado_em
                FROM cobrancas_plano cp
                JOIN estabelecimentos e ON e.id = cp.estabelecimento_id
                WHERE cp.estabelecimento_id = ?
                ORDER BY cp.criado_em DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new CobrancaAdminDTO(
                rs.getLong("id"),
                rs.getLong("estabelecimento_id"),
                rs.getString("nome_estabelecimento"),
                rs.getString("plano"),
                rs.getString("tipo_cobranca"),
                rs.getString("status"),
                rs.getBigDecimal("valor"),
                toLocalDate(rs.getDate("periodo_inicio")),
                toLocalDate(rs.getDate("periodo_fim")),
                toLocalDateTime(rs.getTimestamp("criado_em"))
        ), estabelecimentoId);
    }

    // ── Lista de inadimplentes com dias em atraso ────────────────────────
    @Transactional(readOnly = true)
    public List<InadimplenteDTO> listarInadimplentes() {
        String sql = """
                SELECT e.id, e.nome, e.plano, e.plano_valido_ate,
                       (CURRENT_DATE - e.plano_valido_ate) as dias_atraso,
                       e.telefone, e.whatsapp_contato
                FROM estabelecimentos e
                WHERE e.plano_ativo = false
                  AND e.plano_valido_ate IS NOT NULL
                  AND e.plano_valido_ate < CURRENT_DATE
                  AND NOT (e.trial_ativo = true AND e.trial_fim >= CURRENT_DATE)
                ORDER BY dias_atraso DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new InadimplenteDTO(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getString("plano"),
                toLocalDate(rs.getDate("plano_valido_ate")),
                rs.getLong("dias_atraso"),
                rs.getString("telefone"),
                rs.getString("whatsapp_contato")
        ));
    }

    // ── Exporta cobranças em CSV ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public byte[] exportarCobrancasCsv(LocalDate inicio, LocalDate fim) {
        List<CobrancaAdminDTO> cobrancas = listarCobrancas(null, inicio, fim);
        DateTimeFormatter formatterData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter formatterDataHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(0xEF); out.write(0xBB); out.write(0xBF);
        } catch (Exception ignored) {}

        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            writer.println("Estabelecimento;Plano;Tipo;Status;Valor;Período Início;Período Fim;Data");
            for (CobrancaAdminDTO c : cobrancas) {
                writer.printf("%s;%s;%s;%s;%s;%s;%s;%s%n",
                        escapCsv(c.getNomeEstabelecimento()),
                        escapCsv(c.getPlano()),
                        escapCsv(c.getTipoCobranca()),
                        escapCsv(c.getStatus()),
                        c.getValor() != null ? c.getValor().toString().replace(".", ",") : "",
                        c.getPeriodoInicio() != null ? c.getPeriodoInicio().format(formatterData) : "",
                        c.getPeriodoFim() != null ? c.getPeriodoFim().format(formatterData) : "",
                        c.getCriadoEm() != null ? c.getCriadoEm().format(formatterDataHora) : ""
                );
            }
        }

        return out.toByteArray();
    }

    // ── Privados ──────────────────────────────────────────────────────

    private BigDecimal queryBigDecimal(String sql, Object... params) {
        BigDecimal resultado = jdbcTemplate.queryForObject(sql, BigDecimal.class, params);
        return resultado != null ? resultado : BigDecimal.ZERO;
    }

    private LocalDate toLocalDate(Date date) {
        return date != null ? date.toLocalDate() : null;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return texto;
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }

    private String escapCsv(String valor) {
        if (valor == null) return "";
        return valor.replace(";", ",").replace("\n", " ").replace("\r", " ");
    }
}