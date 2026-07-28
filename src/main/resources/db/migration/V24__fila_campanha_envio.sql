-- V24: fundação da fila de disparo em massa (CampanhaEnvio + CampanhaEnvioItem).
--
-- Suporte técnico para os itens 9/10 (PLATINUM: campanha segmentada
-- automática, reativação por regras) e para a parte de campanha manual da
-- seção 5-B (sem a parte de cupom, que continua fora de escopo por ora).
--
-- Decisão de arquitetura: fila via banco processada por um @Scheduled
-- (CampanhaEnvioProcessorJob), sem RabbitMQ/Kafka. campanha_envio é só o
-- cabeçalho/resumo de progresso; campanha_envio_item é a fila real, um item
-- por destinatário, processado em lotes pequenos para respeitar rate limit
-- da Evolution API.
--
-- Nenhum controller cria CampanhaEnvio ainda — esta migration só estabelece
-- o schema; o endpoint de criação (manual, pelo painel, ou automático, por
-- regra) é a próxima etapa.

CREATE TABLE campanha_envio (
                                id BIGSERIAL PRIMARY KEY,
                                estabelecimento_id BIGINT NOT NULL REFERENCES estabelecimentos(id),
                                filtro_estagio VARCHAR(20) NULL,
                                mensagem TEXT NOT NULL,
                                origem VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
                                status VARCHAR(20) NOT NULL DEFAULT 'CRIADA',
                                total_destinatarios INTEGER NOT NULL DEFAULT 0,
                                total_enviados INTEGER NOT NULL DEFAULT 0,
                                total_erros INTEGER NOT NULL DEFAULT 0,
                                criado_em TIMESTAMP NOT NULL DEFAULT now(),
                                concluido_em TIMESTAMP NULL
);

CREATE TABLE campanha_envio_item (
                                     id BIGSERIAL PRIMARY KEY,
                                     campanha_id BIGINT NOT NULL REFERENCES campanha_envio(id),
                                     cliente_id BIGINT NOT NULL REFERENCES clientes(id),
                                     telefone_destino VARCHAR(20) NOT NULL,
                                     status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
                                     erro_detalhe TEXT NULL,
                                     processado_em TIMESTAMP NULL,
                                     criado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_campanha_envio_estabelecimento ON campanha_envio (estabelecimento_id);

-- Índice de suporte à query do job (busca lote de PENDENTE em ordem de
-- chegada, atravessando todas as campanhas).
CREATE INDEX idx_campanha_envio_item_status_criado ON campanha_envio_item (status, criado_em);
CREATE INDEX idx_campanha_envio_item_campanha ON campanha_envio_item (campanha_id);