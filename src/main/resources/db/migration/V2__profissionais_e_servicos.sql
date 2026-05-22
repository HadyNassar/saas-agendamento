-- V2__profissionais_e_servicos.sql

CREATE TABLE IF NOT EXISTS especialidades (
    id                   BIGSERIAL     PRIMARY KEY,
    nome                 VARCHAR(100)  NOT NULL,
    estabelecimento_id   BIGINT        NOT NULL REFERENCES estabelecimentos(id)
);

CREATE TABLE IF NOT EXISTS servicos (
    id                   BIGSERIAL     PRIMARY KEY,
    nome                 VARCHAR(150)  NOT NULL,
    descricao            TEXT,
    duracao_minutos      INTEGER       NOT NULL,
    preco                NUMERIC(10,2) NOT NULL,
    categoria            VARCHAR(100),
    ativo                BOOLEAN       NOT NULL DEFAULT TRUE,
    estabelecimento_id   BIGINT        NOT NULL REFERENCES estabelecimentos(id)
);

CREATE TABLE IF NOT EXISTS profissionais (
    id                   BIGSERIAL     PRIMARY KEY,
    nome                 VARCHAR(150)  NOT NULL,
    telefone             VARCHAR(20),
    foto_perfil          VARCHAR(300),
    ativo                BOOLEAN       NOT NULL DEFAULT TRUE,
    estabelecimento_id   BIGINT        NOT NULL REFERENCES estabelecimentos(id),
    usuario_id           BIGINT        UNIQUE REFERENCES usuarios(id)
);

-- Tabela de vínculo entre profissional e suas especialidades (N para N)
CREATE TABLE IF NOT EXISTS profissional_especialidades (
    profissional_id      BIGINT        NOT NULL REFERENCES profissionais(id),
    especialidade_id     BIGINT        NOT NULL REFERENCES especialidades(id),
    PRIMARY KEY (profissional_id, especialidade_id)
);

-- Indexes de performance
CREATE INDEX idx_profissionais_estabelecimento ON profissionais(estabelecimento_id);
CREATE INDEX idx_servicos_estabelecimento ON servicos(estabelecimento_id);
CREATE INDEX idx_especialidades_estabelecimento ON especialidades(estabelecimento_id);
