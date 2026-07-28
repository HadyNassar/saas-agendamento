-- Item 17 — indicação/referral automatizada (ver domain.indicacao.Indicacao).
--
-- Fluxo: cliente A (indicador) recebe link único após nota alta na avaliação
-- (AGUARDANDO_USO, expira em 15 dias). Cliente B (indicado) abre o link e é
-- vinculado (AGUARDANDO_PRIMEIRO_ATENDIMENTO). Quando o primeiro agendamento
-- do indicado é concluído, a indicação vira CONCLUIDA e dois cupons pessoais
-- de uso único são gerados (um pro indicador, um pro indicado). Job de
-- expiração marca EXPIRADA quem passar de 15 dias sem concluir.

CREATE TABLE indicacoes (
                            id                          BIGSERIAL PRIMARY KEY,
                            estabelecimento_id          BIGINT NOT NULL REFERENCES estabelecimentos(id),
                            indicador_id                BIGINT NOT NULL REFERENCES clientes(id),
                            indicado_id                 BIGINT REFERENCES clientes(id),
                            codigo                      VARCHAR(40) NOT NULL UNIQUE,
                            agendamento_referencia_id   BIGINT REFERENCES agendamentos(id),
                            cupom_indicador_id          BIGINT REFERENCES cupom_estabelecimento(id),
                            cupom_indicado_id           BIGINT REFERENCES cupom_estabelecimento(id),
                            status                      VARCHAR(40) NOT NULL DEFAULT 'AGUARDANDO_USO',
                            data_expiracao              TIMESTAMP NOT NULL,
                            usado_em                    TIMESTAMP,
                            concluida_em                TIMESTAMP,
                            criado_em                   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Busca pelo código do link (GET /app/{slug}/indicar/{codigo})
CREATE INDEX idx_indicacoes_codigo
    ON indicacoes (codigo);

-- Consulta do job de expiração: pendentes (AGUARDANDO_*) que já passaram
-- da data de expiração
CREATE INDEX idx_indicacoes_status_expiracao
    ON indicacoes (status, data_expiracao);