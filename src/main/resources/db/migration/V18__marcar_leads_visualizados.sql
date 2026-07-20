ALTER TABLE mapa_interesses ADD COLUMN IF NOT EXISTS visualizado BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_mapa_interesses_visualizado ON mapa_interesses(visualizado);