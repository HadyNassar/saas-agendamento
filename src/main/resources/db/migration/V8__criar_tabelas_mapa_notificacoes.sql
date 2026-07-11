-- Mapa: registra visualizações de perfis
CREATE TABLE mapa_visualizacoes (
                                    id SERIAL PRIMARY KEY,
                                    salao_id BIGINT,
                                    data_hora TIMESTAMP DEFAULT NOW(),
                                    origem VARCHAR(50)
);

-- Mapa: registra leads de salões não cadastrados
CREATE TABLE mapa_interesses (
                                 id SERIAL PRIMARY KEY,
                                 nome_cliente VARCHAR(255),
                                 contato VARCHAR(100),
                                 google_place_id VARCHAR(255),
                                 nome_salao_externo VARCHAR(255),
                                 data_hora TIMESTAMP DEFAULT NOW()
);

-- Notificações enviadas pelo admin para os salões
CREATE TABLE notificacoes (
                              id SERIAL PRIMARY KEY,
                              salao_id BIGINT NOT NULL,
                              titulo VARCHAR(255) NOT NULL,
                              mensagem TEXT NOT NULL,
                              tipo VARCHAR(50) DEFAULT 'info',
                              lida BOOLEAN DEFAULT FALSE,
                              criado_em TIMESTAMP DEFAULT NOW()
);