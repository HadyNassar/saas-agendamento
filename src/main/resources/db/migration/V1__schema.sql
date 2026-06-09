-- ============================================================
-- V1__schema_completo.sql
-- Schema completo do Agendara SaaS
-- Para resetar em ambiente local:
--   1. DROP DATABASE saas;
--   2. CREATE DATABASE saas;
--   3. Coloque só este arquivo em db/migration/
--   4. Suba o projeto — Flyway aplica tudo do zero
-- ============================================================

-- ── Estabelecimentos ─────────────────────────────────────────────────────────
CREATE TABLE estabelecimentos (
    id                      BIGSERIAL       PRIMARY KEY,
    nome                    VARCHAR(255)    NOT NULL,
    slug                    VARCHAR(255)    NOT NULL UNIQUE,
    tipo                    VARCHAR(50)     NOT NULL,           -- SALAO | BARBEARIA | CLINICA_ESTETICA
    plano                   VARCHAR(50)     NOT NULL,           -- STANDARD | ELITE | PLATINUM
    telefone                VARCHAR(50),
    endereco                VARCHAR(500),
    criado_em               TIMESTAMP       NOT NULL DEFAULT NOW(),
    ativo                   BOOLEAN         NOT NULL DEFAULT TRUE,

    -- WhatsApp / Evolution API
    evolution_instance_name VARCHAR(255)    UNIQUE,
    whatsapp_conectado      BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Antecipação de agendamento
    meses_antecipacao       INTEGER         NOT NULL DEFAULT 3,

    -- Mercado Pago — assinatura do plano
    plano_ativo             BOOLEAN         NOT NULL DEFAULT FALSE,
    plano_valido_ate        DATE,
    mp_subscription_id      VARCHAR(255),

    -- Mercado Pago — marketplace (split de pagamento), token criptografado em AES-256
    mp_access_token         VARCHAR(1000),
    mp_user_id              VARCHAR(255),
    mp_conectado            BOOLEAN         NOT NULL DEFAULT FALSE,
    aceita_pagamento        BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Sinal para agendamento
    exigir_sinal            BOOLEAN         NOT NULL DEFAULT FALSE,
    tipo_sinal              VARCHAR(20),                        -- fixo | percentual
    valor_sinal             NUMERIC(10, 2),

    -- Trial gratuito de 5 dias
    trial_ativo             BOOLEAN         NOT NULL DEFAULT FALSE,
    trial_inicio            DATE,
    trial_fim               DATE,
    trial_usado             BOOLEAN         NOT NULL DEFAULT FALSE
);

-- ── Usuários ─────────────────────────────────────────────────────────────────
CREATE TABLE usuarios (
    id                  BIGSERIAL       PRIMARY KEY,
    email               VARCHAR(255)    NOT NULL UNIQUE,
    senha               VARCHAR(255)    NOT NULL,
    nome                VARCHAR(255)    NOT NULL,
    role                VARCHAR(50)     NOT NULL,               -- ADMIN | FUNCIONARIO | FUNCIONARIO_PLATINUM
    estabelecimento_id  BIGINT          NOT NULL REFERENCES estabelecimentos(id) ON DELETE CASCADE
);

-- ── Tokens de recuperação de senha ───────────────────────────────────────────
CREATE TABLE password_reset_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    token       VARCHAR(255)    NOT NULL UNIQUE,
    usuario_id  BIGINT          NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    expira_em   TIMESTAMP       NOT NULL,
    usado       BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_prt_token      ON password_reset_tokens(token);
CREATE INDEX idx_prt_usuario_id ON password_reset_tokens(usuario_id);

-- ── Especialidades ────────────────────────────────────────────────────────────
CREATE TABLE especialidades (
    id                  BIGSERIAL       PRIMARY KEY,
    nome                VARCHAR(255)    NOT NULL,
    estabelecimento_id  BIGINT          NOT NULL REFERENCES estabelecimentos(id) ON DELETE CASCADE
);

-- ── Profissionais ─────────────────────────────────────────────────────────────
CREATE TABLE profissionais (
    id                  BIGSERIAL       PRIMARY KEY,
    nome                VARCHAR(255)    NOT NULL,
    telefone            VARCHAR(50),
    foto_perfil         VARCHAR(500),
    comissao_percent    NUMERIC(5, 2)   NOT NULL DEFAULT 0,
    ativo               BOOLEAN         NOT NULL DEFAULT TRUE,
    estabelecimento_id  BIGINT          NOT NULL REFERENCES estabelecimentos(id) ON DELETE CASCADE,
    usuario_id          BIGINT          UNIQUE   REFERENCES usuarios(id) ON DELETE SET NULL
);

-- ── Profissional ↔ Especialidades (N:N) ──────────────────────────────────────
CREATE TABLE profissional_especialidades (
    profissional_id     BIGINT  NOT NULL REFERENCES profissionais(id)  ON DELETE CASCADE,
    especialidade_id    BIGINT  NOT NULL REFERENCES especialidades(id) ON DELETE CASCADE,
    PRIMARY KEY (profissional_id, especialidade_id)
);

-- ── Serviços ─────────────────────────────────────────────────────────────────
CREATE TABLE servicos (
    id                      BIGSERIAL       PRIMARY KEY,
    nome                    VARCHAR(255)    NOT NULL,
    descricao               TEXT,
    duracao_minutos         INTEGER         NOT NULL,
    preco                   NUMERIC(10, 2)  NOT NULL,
    categoria               VARCHAR(255),
    ativo                   BOOLEAN         NOT NULL DEFAULT TRUE,
    custo_material_fixo     NUMERIC(10, 2)  NOT NULL DEFAULT 0,
    custo_material_percent  NUMERIC(5, 2)   NOT NULL DEFAULT 0,
    estabelecimento_id      BIGINT          NOT NULL REFERENCES estabelecimentos(id) ON DELETE CASCADE
);

-- ── Clientes ─────────────────────────────────────────────────────────────────
CREATE TABLE clientes (
    id                      BIGSERIAL       PRIMARY KEY,
    nome                    VARCHAR(255)    NOT NULL,
    telefone                VARCHAR(50)     NOT NULL,
    email                   VARCHAR(255),
    alergias                TEXT,
    preferencias            TEXT,
    restricoes              TEXT,
    observacoes             TEXT,
    total_faltas            INTEGER         NOT NULL DEFAULT 0,
    bloqueado               BOOLEAN         NOT NULL DEFAULT FALSE,
    motivo_bloqueio         TEXT,
    ultimo_lembrete_retorno TIMESTAMP,
    criado_em               TIMESTAMP       NOT NULL DEFAULT NOW(),
    estabelecimento_id      BIGINT          NOT NULL REFERENCES estabelecimentos(id) ON DELETE CASCADE
);

CREATE INDEX idx_clientes_estab_telefone ON clientes(estabelecimento_id, telefone);

-- ── Horários de funcionamento ─────────────────────────────────────────────────
CREATE TABLE horarios_funcionamento (
    id                  BIGSERIAL       PRIMARY KEY,
    dia_semana          VARCHAR(20)     NOT NULL,               -- MONDAY, TUESDAY...
    hora_abertura       TIME            NOT NULL,
    hora_fechamento     TIME            NOT NULL,
    inicio_almoco       TIME,
    fim_almoco          TIME,
    intervalo_minutos   INTEGER         NOT NULL DEFAULT 0,
    aberto              BOOLEAN         NOT NULL DEFAULT TRUE,
    estabelecimento_id  BIGINT          NOT NULL REFERENCES estabelecimentos(id) ON DELETE CASCADE
);

-- ── Agendamentos ──────────────────────────────────────────────────────────────
CREATE TABLE agendamentos (
    id                      BIGSERIAL       PRIMARY KEY,
    cliente_id              BIGINT          NOT NULL REFERENCES clientes(id),
    profissional_id         BIGINT          NOT NULL REFERENCES profissionais(id),
    servico_id              BIGINT          NOT NULL REFERENCES servicos(id),
    estabelecimento_id      BIGINT          NOT NULL REFERENCES estabelecimentos(id) ON DELETE CASCADE,
    data_hora_inicio        TIMESTAMP       NOT NULL,
    data_hora_fim           TIMESTAMP       NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'AGENDADO',  -- AGENDADO | CONCLUIDO | CANCELADO | FALTOU
    observacoes             TEXT,
    agendado_pelo_cliente   BOOLEAN         NOT NULL DEFAULT FALSE,
    avaliacao_enviada       BOOLEAN         NOT NULL DEFAULT FALSE,
    criado_em               TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agendamentos_estab_data ON agendamentos(estabelecimento_id, data_hora_inicio);
CREATE INDEX idx_agendamentos_profissional ON agendamentos(profissional_id, data_hora_inicio);

-- ── Avaliações de atendimento ─────────────────────────────────────────────────
CREATE TABLE avaliacoes_atendimento (
    id                  BIGSERIAL   PRIMARY KEY,
    agendamento_id      BIGINT      NOT NULL UNIQUE REFERENCES agendamentos(id),
    cliente_id          BIGINT      NOT NULL REFERENCES clientes(id),
    estabelecimento_id  BIGINT      NOT NULL REFERENCES estabelecimentos(id) ON DELETE CASCADE,
    nota                INTEGER     NOT NULL CHECK (nota BETWEEN 1 AND 5),
    comentario          TEXT,
    oculta              BOOLEAN     NOT NULL DEFAULT FALSE,
    criado_em           TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ── Anamnese — perguntas ──────────────────────────────────────────────────────
CREATE TABLE perguntas_anamnese (
    id                  BIGSERIAL       PRIMARY KEY,
    texto               VARCHAR(1000)   NOT NULL,
    tipo                VARCHAR(50)     NOT NULL,               -- SIM_NAO | SIM_NAO_DETALHE | TEXTO_LIVRE
    ordem               INTEGER         NOT NULL DEFAULT 0,
    ativa               BOOLEAN         NOT NULL DEFAULT TRUE,
    estabelecimento_id  BIGINT          NOT NULL REFERENCES estabelecimentos(id) ON DELETE CASCADE
);

-- ── Anamnese — respostas ──────────────────────────────────────────────────────
CREATE TABLE respostas_anamnese (
    id                      BIGSERIAL   PRIMARY KEY,
    cliente_id              BIGINT      NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    pergunta_id             BIGINT      NOT NULL REFERENCES perguntas_anamnese(id) ON DELETE CASCADE,
    resposta                TEXT        NOT NULL,
    detalhe                 TEXT,
    preenchido_pelo_cliente BOOLEAN     NOT NULL DEFAULT TRUE,
    preenchido_em           TIMESTAMP   NOT NULL DEFAULT NOW(),
    editado_em              TIMESTAMP
);

-- ── Fotos de clientes (antes/depois) ─────────────────────────────────────────
CREATE TABLE fotos_cliente (
    id              BIGSERIAL       PRIMARY KEY,
    cliente_id      BIGINT          NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    tipo            VARCHAR(10)     NOT NULL,                   -- ANTES | DEPOIS
    caminho         VARCHAR(500)    NOT NULL,
    nome_original   VARCHAR(255),
    criado_em       TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ── Fotos de portfólio do profissional ───────────────────────────────────────
CREATE TABLE fotos_profissional (
    id              BIGSERIAL       PRIMARY KEY,
    profissional_id BIGINT          NOT NULL REFERENCES profissionais(id) ON DELETE CASCADE,
    caminho         VARCHAR(500)    NOT NULL,
    nome_original   VARCHAR(255),
    criado_em       TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ── Mensagens WhatsApp (log) ──────────────────────────────────────────────────
CREATE TABLE whatsapp_mensagens (
    id                  BIGSERIAL       PRIMARY KEY,
    estabelecimento_id  BIGINT          NOT NULL REFERENCES estabelecimentos(id) ON DELETE CASCADE,
    agendamento_id      BIGINT          REFERENCES agendamentos(id) ON DELETE SET NULL,
    cliente_id          BIGINT          REFERENCES clientes(id) ON DELETE SET NULL,
    telefone_destino    VARCHAR(20)     NOT NULL,
    mensagem            TEXT            NOT NULL,
    tipo                VARCHAR(30)     NOT NULL DEFAULT 'NOTIFICACAO',  -- CONFIRMACAO | LEMBRETE_24H | CANCELAMENTO | FALTA
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDENTE',     -- PENDENTE | ENVIADO | ERRO
    erro_detalhe        TEXT,
    enviado_em          TIMESTAMP,
    criado_em           TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ── Cobranças de plano ────────────────────────────────────────────────────────
CREATE TABLE cobrancas_plano (
    id                  BIGSERIAL       PRIMARY KEY,
    estabelecimento_id  BIGINT          NOT NULL REFERENCES estabelecimentos(id) ON DELETE CASCADE,
    plano               VARCHAR(50)     NOT NULL,
    tipo_cobranca       VARCHAR(20)     NOT NULL,               -- RECORRENTE | AVULSO
    mp_payment_id       VARCHAR(255),
    mp_subscription_id  VARCHAR(255),
    valor               NUMERIC(10, 2)  NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDENTE',  -- PENDENTE | AUTORIZADO | CANCELADO | REJEITADO
    periodo_inicio      DATE,
    periodo_fim         DATE,
    criado_em           TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ── Transações (split de pagamento) ──────────────────────────────────────────
CREATE TABLE transacoes (
    id                      BIGSERIAL       PRIMARY KEY,
    estabelecimento_id      BIGINT          NOT NULL REFERENCES estabelecimentos(id) ON DELETE CASCADE,
    agendamento_id          BIGINT          REFERENCES agendamentos(id) ON DELETE SET NULL,
    mp_payment_id           VARCHAR(255),
    valor_total             NUMERIC(10, 2)  NOT NULL,
    taxa_plataforma         NUMERIC(10, 2)  NOT NULL,
    valor_estabelecimento   NUMERIC(10, 2)  NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDENTE',  -- PENDENTE | APROVADO | REJEITADO | REEMBOLSADO | CANCELADO
    metodo_pagamento        VARCHAR(30),                        -- pix | credit_card | debit_card
    pix_qr_code             TEXT,
    pix_qr_code_base64      TEXT,
    criado_em               TIMESTAMP       NOT NULL DEFAULT NOW(),
    atualizado_em           TIMESTAMP       NOT NULL DEFAULT NOW()
);
