package com.hady.saas.business.service;

import com.hady.saas.application.dto.EstabelecimentoMapaAdminDTO;
import com.hady.saas.application.dto.LeadAgrupadoDTO;
import com.hady.saas.application.dto.LeadMapaDTO;
import com.hady.saas.application.dto.MapaMetricaEstabelecimentoDTO;
import com.hady.saas.application.dto.SalaoExternoAdminDTO;
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
    private final GooglePlacesService googlePlacesService;

    public GestaoMapaService(EstabelecimentoRepository estabelecimentoRepository,
                             JdbcTemplate jdbcTemplate,
                             GooglePlacesService googlePlacesService) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.googlePlacesService = googlePlacesService;
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

    // ── Mapa completo pro admin master — TODOS os estabelecimentos
    // cadastrados próximos (mesmo os ocultos do mapa público), já com
    // telefone/WhatsApp incluído. Ferramenta de prospecção/gestão. ────────
    @Transactional(readOnly = true)
    public List<EstabelecimentoMapaAdminDTO> listarEstabelecimentosParaMapa(double lat, double lng, double raio) {
        return estabelecimentoRepository.buscarProximos(lat, lng, raio).stream()
                .map(e -> new EstabelecimentoMapaAdminDTO(
                        e.getId(),
                        e.getNome(),
                        e.getSlug(),
                        e.getTipo() != null ? e.getTipo().name() : null,
                        e.getBairro(),
                        e.getCidade(),
                        e.getLatitude(),
                        e.getLongitude(),
                        e.getFotoPerfil(),
                        e.temAcesso(),
                        e.isVisivelNoMapa(),
                        e.getTelefone(),
                        e.getWhatsappContato()
                ))
                .toList();
    }

    // ── Salões do Google ainda não cadastrados no sistema, com avaliação —
    // exclusivo admin master ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<SalaoExternoAdminDTO> listarExternosParaMapa(double lat, double lng, double raio) {
        List<SalaoExternoAdminDTO> todosDoGoogle = googlePlacesService.buscarSaloesProximos(lat, lng, raio);

        List<Estabelecimento> jaCadastrados = estabelecimentoRepository.buscarProximos(lat, lng, raio);
        List<String> nomesJaCadastrados = jaCadastrados.stream()
                .map(e -> e.getNome().trim().toLowerCase())
                .toList();

        return todosDoGoogle.stream()
                .filter(externo -> !nomesJaCadastrados.contains(externo.getNome().trim().toLowerCase()))
                .toList();
    }

    // ── Telefone sob demanda de um salão externo, via Google Place Details
    @Transactional(readOnly = true)
    public String buscarTelefoneExterno(String googlePlaceId) {
        return googlePlacesService.buscarTelefone(googlePlaceId);
    }

    // ── Lista todos os leads capturados (mais recentes primeiro) ────────
    @Transactional(readOnly = true)
    public List<LeadMapaDTO> listarLeads(String googlePlaceId) {
        String sql;
        Object[] params;

        if (googlePlaceId == null || googlePlaceId.isBlank()) {
            sql = """
                    SELECT id, nome_cliente, contato, google_place_id, nome_salao_externo, data_hora, visualizado
                    FROM mapa_interesses
                    ORDER BY data_hora DESC
                    """;
            params = new Object[]{};
        } else {
            sql = """
                    SELECT id, nome_cliente, contato, google_place_id, nome_salao_externo, data_hora, visualizado
                    FROM mapa_interesses
                    WHERE google_place_id = ?
                    ORDER BY data_hora DESC
                    """;
            params = new Object[]{googlePlaceId};
        }

        return jdbcTemplate.query(sql, (rs, rowNum) -> new LeadMapaDTO(
                rs.getLong("id"),
                rs.getString("nome_cliente"),
                rs.getString("contato"),
                rs.getString("google_place_id"),
                rs.getString("nome_salao_externo"),
                toLocalDateTime(rs.getTimestamp("data_hora")),
                rs.getBoolean("visualizado")
        ), params);
    }

    // ── Conta quantos leads ainda não foram vistos — badge no painel ────
    @Transactional(readOnly = true)
    public long contarLeadsNaoVisualizados() {
        Long resultado = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mapa_interesses WHERE visualizado = FALSE", Long.class
        );
        return resultado != null ? resultado : 0L;
    }

    // ── Marca um lead como visualizado ───────────────────────────────────
    @Transactional
    public void marcarLeadComoVisualizado(Long leadId) {
        jdbcTemplate.update("UPDATE mapa_interesses SET visualizado = TRUE WHERE id = ?", leadId);
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
        return valor.replace(";", ",").replace("\n", " ").replace("\r", " ");
    }
}