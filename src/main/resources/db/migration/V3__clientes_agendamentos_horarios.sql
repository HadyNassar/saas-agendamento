-- V3__clientes_agendamentos_horarios.sql

CREATE TABLE IF NOT EXISTS clientes (
    id                   BIGSERIAL     PRIMARY KEY,
    nome                 VARCHAR(150)  NOT NULL,
    telefone             VARCHAR(20)   NOT NULL,
    email                VARCHAR(150),
    alergias             TEXT,
    preferencias         TEXT,
    restricoes           TEXT,
    observacoes          TEXT,
    total_faltas         INTEGER       NOT NULL DEFAULT 0,
    bloqueado            BOOLEAN       NOT NULL DEFAULT FALSE,
    motivo_bloqueio      TEXT,
    criado_em            TIMESTAMP     NOT NULL DEFAULT NOW(),
    estabelecimento_id   BIGINT        NOT NULL REFERENCES estabelecimentos(id),
    UNIQUE (telefone, estabelecimento_id)
);

CREATE TABLE IF NOT EXISTS agendamentos (
    id                   BIGSERIAL     PRIMARY KEY,
    cliente_id           BIGINT        NOT NULL REFERENCES clientes(id),
    profissional_id      BIGINT        NOT NULL REFERENCES profissionais(id),
    servico_id           BIGINT        NOT NULL REFERENCES servicos(id),
    estabelecimento_id   BIGINT        NOT NULL REFERENCES estabelecimentos(id),
    data_hora_inicio     TIMESTAMP     NOT NULL,
    data_hora_fim        TIMESTAMP     NOT NULL,
    status               VARCHAR(20)   NOT NULL DEFAULT 'AGENDADO',
    observacoes          TEXT,
    agendado_pelo_cliente BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em            TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS horarios_funcionamento (
    id                   BIGSERIAL     PRIMARY KEY,
    dia_semana           VARCHAR(20)   NOT NULL,
    hora_abertura        TIME          NOT NULL,
    hora_fechamento      TIME          NOT NULL,
    inicio_almoco        TIME,
    fim_almoco           TIME,
    intervalo_minutos    INTEGER       NOT NULL DEFAULT 0,
    aberto               BOOLEAN       NOT NULL DEFAULT TRUE,
    estabelecimento_id   BIGINT        NOT NULL REFERENCES estabelecimentos(id),
    UNIQUE (dia_semana, estabelecimento_id)
);

-- Indexes de performance
CREATE INDEX idx_agendamentos_estabelecimento ON agendamentos(estabelecimento_id);
CREATE INDEX idx_agendamentos_profissional ON agendamentos(profissional_id);
CREATE INDEX idx_agendamentos_data ON agendamentos(data_hora_inicio);
CREATE INDEX idx_clientes_estabelecimento ON clientes(estabelecimento_id);
