-- Tabela de vínculo N×N entre profissionais e serviços.
--
-- Design intencional: um serviço pode ser vinculado a MÚLTIPLOS profissionais.
-- Ex: "Corte feminino" pode estar vinculado a Ana, João e Maria ao mesmo tempo.
-- No agendamento público, o cliente escolhe o profissional e então vê apenas
-- os serviços que aquele profissional realiza.
--
-- Renomear V__... para V{PROXIMO_NUMERO}__ antes de executar via Flyway,
-- ex: V5__profissional_servicos.sql
-- Se o projeto usa ddl-auto=update/create, o Hibernate cria esta tabela
-- automaticamente via @JoinTable — a migration é boa prática, não obrigatória.

CREATE TABLE IF NOT EXISTS profissional_servicos (
    profissional_id BIGINT NOT NULL,
    servico_id      BIGINT NOT NULL,
    PRIMARY KEY (profissional_id, servico_id),
    CONSTRAINT fk_ps_profissional
        FOREIGN KEY (profissional_id) REFERENCES profissionais(id) ON DELETE CASCADE,
    CONSTRAINT fk_ps_servico
        FOREIGN KEY (servico_id) REFERENCES servicos(id) ON DELETE CASCADE
);

-- Índice para buscas inversas: "quais profissionais fazem este serviço?"
CREATE INDEX IF NOT EXISTS idx_ps_servico ON profissional_servicos (servico_id);
