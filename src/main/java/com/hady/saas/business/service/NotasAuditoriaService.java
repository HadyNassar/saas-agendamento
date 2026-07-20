package com.hady.saas.business.service;

import com.hady.saas.application.dto.LogAuditoriaDTO;
import com.hady.saas.application.dto.NotaInternaDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotasAuditoriaService {

    private final JdbcTemplate jdbcTemplate;

    public NotasAuditoriaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── Notas internas ────────────────────────────────────────────────

    @Transactional
    public NotaInternaDTO adicionarNota(Long estabelecimentoId, String texto, String criadoPor) {
        jdbcTemplate.update(
                "INSERT INTO notas_internas (estabelecimento_id, texto, criado_por) VALUES (?, ?, ?)",
                estabelecimentoId, texto, criadoPor
        );

        // Busca a nota recém-criada pra devolver com id e criado_em preenchidos
        return jdbcTemplate.queryForObject("""
                SELECT id, estabelecimento_id, texto, criado_por, criado_em
                FROM notas_internas
                WHERE estabelecimento_id = ?
                ORDER BY criado_em DESC
                LIMIT 1
                """, (rs, rowNum) -> new NotaInternaDTO(
                rs.getLong("id"),
                rs.getLong("estabelecimento_id"),
                rs.getString("texto"),
                rs.getString("criado_por"),
                toLocalDateTime(rs.getTimestamp("criado_em"))
        ), estabelecimentoId);
    }

    @Transactional(readOnly = true)
    public List<NotaInternaDTO> listarNotas(Long estabelecimentoId) {
        String sql = """
                SELECT id, estabelecimento_id, texto, criado_por, criado_em
                FROM notas_internas
                WHERE estabelecimento_id = ?
                ORDER BY criado_em DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new NotaInternaDTO(
                rs.getLong("id"),
                rs.getLong("estabelecimento_id"),
                rs.getString("texto"),
                rs.getString("criado_por"),
                toLocalDateTime(rs.getTimestamp("criado_em"))
        ), estabelecimentoId);
    }

    @Transactional
    public void excluirNota(Long notaId) {
        jdbcTemplate.update("DELETE FROM notas_internas WHERE id = ?", notaId);
    }

    // ── Log de auditoria ──────────────────────────────────────────────

    // Chamado a partir de outros services (GestaoUsuarios, Financeiro, Cupom,
    // GestaoMapa) sempre que uma ação administrativa sensível acontece.
    // estabelecimentoId pode ser null para ações que não são específicas
    // de um estabelecimento (ex: criar cupom genérico).
    @Transactional
    public void registrar(String adminEmail, String acao, Long estabelecimentoId, String detalhes) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO log_auditoria (admin_email, acao, estabelecimento_id, detalhes) VALUES (?, ?, ?, ?)",
                    adminEmail, acao, estabelecimentoId, detalhes
            );
        } catch (Exception e) {
            // Auditoria nunca deve quebrar a ação principal que está sendo registrada
            System.err.println("Falha ao registrar auditoria: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<LogAuditoriaDTO> listarAuditoria(Long estabelecimentoId, String acao) {
        StringBuilder sql = new StringBuilder("""
                SELECT la.id, la.admin_email, la.acao, la.estabelecimento_id,
                       e.nome as nome_estabelecimento, la.detalhes, la.criado_em
                FROM log_auditoria la
                LEFT JOIN estabelecimentos e ON e.id = la.estabelecimento_id
                WHERE 1=1
                """);

        List<Object> params = new java.util.ArrayList<>();

        if (estabelecimentoId != null) {
            sql.append(" AND la.estabelecimento_id = ? ");
            params.add(estabelecimentoId);
        }
        if (acao != null && !acao.isBlank()) {
            sql.append(" AND la.acao = ? ");
            params.add(acao);
        }

        sql.append(" ORDER BY la.criado_em DESC LIMIT 500 ");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new LogAuditoriaDTO(
                rs.getLong("id"),
                rs.getString("admin_email"),
                rs.getString("acao"),
                rs.getObject("estabelecimento_id") != null ? rs.getLong("estabelecimento_id") : null,
                rs.getString("nome_estabelecimento"),
                rs.getString("detalhes"),
                toLocalDateTime(rs.getTimestamp("criado_em"))
        ), params.toArray());
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}