ALTER TABLE estabelecimentos
    ADD COLUMN IF NOT EXISTS meses_antecipacao INTEGER NOT NULL DEFAULT 3;
