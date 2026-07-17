CREATE TABLE IF NOT EXISTS places_cache (
                                            id SERIAL PRIMARY KEY,
                                            chave_busca VARCHAR(100) NOT NULL UNIQUE, -- ex: "-20.56_-54.62_10" (lat_lng_raio arredondados)
    resultado_json TEXT NOT NULL,
    criado_em TIMESTAMP DEFAULT NOW()
    );

-- Índice para limpeza rápida de cache expirado
CREATE INDEX IF NOT EXISTS idx_places_cache_criado_em ON places_cache(criado_em);