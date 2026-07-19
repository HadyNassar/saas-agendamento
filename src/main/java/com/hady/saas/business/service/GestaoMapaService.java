package com.hady.saas.business.service;

import com.hady.saas.application.dto.LeadAgrupadoDTO;
import com.hady.saas.application.dto.LeadMapaDTO;
import com.hady.saas.application.dto.MapaMetricaEstabelecimentoDTO;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class GestaoMapaService {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final JdbcTemplate jdbcTemplate;

    public GestaoMapaService(EstabelecimentoRepository estabelecimentoRepository,
                             JdbcTemplate jdbcTemplate) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── Métricas por estabelecimento cadastrado ──────────────────────────
    @Transactional(readOnly = true)
    public List<MapaMetricaEstabelecimentoDTO> listarMetricas() {
        List<Estabelecimento> todos = estabelecimentoRepository.findAll();

        return todos.stream()
                .map(e -> new MapaMetricaEstabelecimentoDTO(
                        e.getId(),
                        e.getNome(),
                        e.getSlug(),
                        e.getCidade(),
                        e.getBairro(),
                        e.isVisivelNoMapa(),
                        e.isAtivo(),
                        contarVisualizacoes(e.getId(), null),
                        contarVisualizacoes(e.getId(), 30),
                        contarVisualizacoesHoje(e.getId())
                ))
                .toList();
    }

    // ── Ativa/oculta um estabelecimento do mapa público ──────────────────
    @Transactional
    public void alterarVisibilidade(Long estabelecimentoId, boolean visivel) {
        Estabelecimento e = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
        e.setVisivelNoMapa(visivel);
        estabelecimentoRepository.save(e);
    }

    // ── Lista todos os leads capturados (mais recentes primeiro) ────────
    @Transactional(readOnly = true)
    public List<LeadMapaDTO> listarLeads(String googlePlaceId) {
        String sql = """
                SELECT id, nome_cliente, contato, google_place_id, nome_salao_externo, data_hora
                FROM mapa_interesses
                WHERE (? IS NULL OR google_place_id = ?)
                ORDER BY data_hora DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new LeadMapaDTO(
                rs.getLong("id"),
                rs.getString("nome_cliente"),
                rs.getString("contato"),
                rs.getString("google_place_id"),
                rs.getString("nome_salao_externo"),
                toLocalDateTime(rs.getTimestamp("data_hora"))
        ), googlePlaceId, googlePlaceId);
    }

    // ── Leads agrupados por salão — para priorizar abordagem comercial ──
    @Transactional(readOnly = true)
    public List<LeadAgrupadoDTO> listarLeadsAgrupados() {
        String sql = """
                SELECT google_place_id, nome_salao_externo, COUNT(*) as total
                FROM mapa_interesses
                GROUP BY google_place_id, nome_salao_externo
                ORDER BY total DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new LeadAgrupadoDTO(
                rs.getString("google_place_id"),
                rs.getString("nome_salao_externo"),
                rs.getLong("total")
        ));
    }

    // ── Exporta todos os leads em CSV ────────────────────────────────────
    @Transactional(readOnly = true)
    public byte[] exportarLeadsCsv() {
        List<LeadMapaDTO> leads = listarLeads(null);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // BOM UTF-8 no início — garante acentuação correta ao abrir no Excel
        try {
            out.write(0xEF); out.write(0xBB); out.write(0xBF);
        } catch (Exception ignored) {}

        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            writer.println("Nome do Cliente;Contato;Salão de Interesse;Data e Hora");
            for (LeadMapaDTO lead : leads) {
                writer.printf("%s;%s;%s;%s%n",
                        escapCsv(lead.getNomeCliente()),
                        escapCsv(lead.getContato()),
                        escapCsv(lead.getNomeSalaoExterno()),
                        lead.getDataHora() != null ? lead.getDataHora().format(formatter) : ""
                );
            }
        }

        return out.toByteArray();
    }

    // ── Privados ──────────────────────────────────────────────────────

    private long contarVisualizacoes(Long estabelecimentoId, Integer ultimosDias) {
        String sql = ultimosDias == null
                ? "SELECT COUNT(*) FROM mapa_visualizacoes WHERE salao_id = ?"
                : "SELECT COUNT(*) FROM mapa_visualizacoes WHERE salao_id = ? AND data_hora >= NOW() - INTERVAL '" + ultimosDias + " days'";
        Long resultado = jdbcTemplate.queryForObject(sql, Long.class, estabelecimentoId);
        return resultado != null ? resultado : 0L;
    }

    private long contarVisualizacoesHoje(Long estabelecimentoId) {
        Long resultado = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mapa_visualizacoes WHERE salao_id = ? AND data_hora::date = CURRENT_DATE",
                Long.class, estabelecimentoId
        );
        return resultado != null ? resultado : 0L;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private String escapCsv(String valor) {
        if (valor == null) return "";
        // Escapa ponto-e-vírgula e quebras de linha que quebrariam o CSV
        return valor.replace(";", ",").replace("\n", " ").replace("\r", " ");
    }
}