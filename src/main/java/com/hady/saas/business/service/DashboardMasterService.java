package com.hady.saas.business.service;

import com.hady.saas.application.dto.DashboardMasterDTO;
import com.hady.saas.application.dto.PontoSerieDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DashboardMasterService {

    private final JdbcTemplate jdbcTemplate;

    public DashboardMasterService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public DashboardMasterDTO gerarDashboard() {
        DashboardMasterDTO dto = new DashboardMasterDTO();

        preencherCartoesPrincipais(dto);
        preencherFinanceiro(dto);
        preencherDistribuicaoPorPlano(dto);
        preencherTrialVsPagante(dto);
        preencherSeriesTemporais(dto);
        preencherMetricasMapa(dto);

        return dto;
    }

    // ── Cartões principais ──────────────────────────────────────────────
    private void preencherCartoesPrincipais(DashboardMasterDTO dto) {
        dto.setTotalEstabelecimentos(contar("SELECT COUNT(*) FROM estabelecimentos"));

        dto.setEstabelecimentosAtivos(contar("""
                SELECT COUNT(*) FROM estabelecimentos
                WHERE (plano_ativo = true AND plano_valido_ate >= CURRENT_DATE)
                """));

        dto.setEstabelecimentosEmTrial(contar("""
                SELECT COUNT(*) FROM estabelecimentos
                WHERE trial_ativo = true AND trial_fim >= CURRENT_DATE
                """));

        dto.setEstabelecimentosInativos(contar("""
                SELECT COUNT(*) FROM estabelecimentos
                WHERE NOT (plano_ativo = true AND plano_valido_ate >= CURRENT_DATE)
                  AND NOT (trial_ativo = true AND trial_fim >= CURRENT_DATE)
                """));

        dto.setNovosHoje(contar("""
                SELECT COUNT(*) FROM estabelecimentos
                WHERE criado_em::date = CURRENT_DATE
                """));

        dto.setNovosSemana(contar("""
                SELECT COUNT(*) FROM estabelecimentos
                WHERE criado_em >= CURRENT_DATE - INTERVAL '7 days'
                """));

        dto.setNovosMes(contar("""
                SELECT COUNT(*) FROM estabelecimentos
                WHERE criado_em >= date_trunc('month', CURRENT_DATE)
                """));
    }

    // ── Financeiro: MRR, receita do mês, churn ──────────────────────────
    private void preencherFinanceiro(DashboardMasterDTO dto) {
        // MRR = soma de cobranças RECORRENTE com status AUTORIZADO cujo
        // período de vigência inclui hoje — representa a receita mensal
        // recorrente ativa neste exato momento, não apenas o que foi
        // pago no mês corrente (isso seria "receita do mês", diferente).
        BigDecimal mrr = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(valor), 0) FROM cobrancas_plano
                WHERE tipo_cobranca = 'RECORRENTE'
                  AND status = 'AUTORIZADO'
                  AND periodo_inicio <= CURRENT_DATE
                  AND periodo_fim >= CURRENT_DATE
                """, BigDecimal.class);
        dto.setMrrAtual(mrr != null ? mrr : BigDecimal.ZERO);

        // Receita do mês = tudo que foi efetivamente pago (qualquer tipo)
        // com data de criação dentro do mês corrente.
        BigDecimal receitaMes = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(valor), 0) FROM cobrancas_plano
                WHERE status = 'AUTORIZADO'
                  AND criado_em >= date_trunc('month', CURRENT_DATE)
                """, BigDecimal.class);
        dto.setReceitaMesAtual(receitaMes != null ? receitaMes : BigDecimal.ZERO);

        // Churn = % de estabelecimentos que estavam ativos no início do mês
        // e hoje não têm mais plano ativo nem trial ativo.
        Long ativosInicioMes = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM estabelecimentos
                WHERE plano_ativo = true
                  AND criado_em < date_trunc('month', CURRENT_DATE)
                """, Long.class);

        Long cancelaramNoMes = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM estabelecimentos
                WHERE plano_ativo = false
                  AND plano_valido_ate >= date_trunc('month', CURRENT_DATE)
                  AND plano_valido_ate < CURRENT_DATE
                """, Long.class);

        double churn = (ativosInicioMes != null && ativosInicioMes > 0 && cancelaramNoMes != null)
                ? (cancelaramNoMes.doubleValue() / ativosInicioMes.doubleValue()) * 100.0
                : 0.0;
        dto.setChurnMesAtual(Math.round(churn * 10.0) / 10.0); // 1 casa decimal
    }

    // ── Distribuição por plano (para o gráfico de pizza) ────────────────
    private void preencherDistribuicaoPorPlano(DashboardMasterDTO dto) {
        Map<String, Long> distribuicao = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT plano, COUNT(*) as total FROM estabelecimentos
                GROUP BY plano ORDER BY plano
                """, rs -> {
            distribuicao.put(rs.getString("plano"), rs.getLong("total"));
        });
        dto.setDistribuicaoPorPlano(distribuicao);
    }

    // ── Trial vs Pagante ──────────────────────────────────────────────
    private void preencherTrialVsPagante(DashboardMasterDTO dto) {
        dto.setTotalTrial(contar("""
                SELECT COUNT(*) FROM estabelecimentos
                WHERE trial_ativo = true AND trial_fim >= CURRENT_DATE
                """));

        dto.setTotalPagante(contar("""
                SELECT COUNT(*) FROM estabelecimentos
                WHERE plano_ativo = true AND plano_valido_ate >= CURRENT_DATE
                """));
    }

    // ── Séries temporais dos últimos 6 meses ────────────────────────────
    private void preencherSeriesTemporais(DashboardMasterDTO dto) {
        List<PontoSerieDTO> crescimento = new java.util.ArrayList<>();
        List<PontoSerieDTO> receita = new java.util.ArrayList<>();

        YearMonth mesAtual = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth mes = mesAtual.minusMonths(i);
            LocalDate inicio = mes.atDay(1);
            LocalDate fim = mes.atEndOfMonth();
            String label = capitalizar(mes.getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt", "BR")))
                    + "/" + mes.getYear();

            Long novosNoMes = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM estabelecimentos
                    WHERE criado_em::date BETWEEN ? AND ?
                    """, Long.class, inicio, fim);
            crescimento.add(new PontoSerieDTO(label, novosNoMes != null ? novosNoMes : 0));

            BigDecimal receitaNoMes = jdbcTemplate.queryForObject("""
                    SELECT COALESCE(SUM(valor), 0) FROM cobrancas_plano
                    WHERE status = 'AUTORIZADO'
                      AND criado_em::date BETWEEN ? AND ?
                    """, BigDecimal.class, inicio, fim);
            receita.add(new PontoSerieDTO(label, receitaNoMes != null ? receitaNoMes : BigDecimal.ZERO));
        }

        dto.setCrescimentoUsuariosPorMes(crescimento);
        dto.setReceitaPorMes(receita);
    }

    // ── Métricas do mapa público ─────────────────────────────────────────
    private void preencherMetricasMapa(DashboardMasterDTO dto) {
        dto.setVisualizacoesMapaHoje(contar("""
                SELECT COUNT(*) FROM mapa_visualizacoes
                WHERE data_hora::date = CURRENT_DATE
                """));

        dto.setLeadsGeradosTotal(contar("SELECT COUNT(*) FROM mapa_interesses"));
    }

    private long contar(String sql) {
        Long resultado = jdbcTemplate.queryForObject(sql, Long.class);
        return resultado != null ? resultado : 0L;
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return texto;
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }
}