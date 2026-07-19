-- Permite ocultar um estabelecimento do mapa público sem desativar a conta
-- inteira (ex: salão pediu pra sair do mapa mas continua usando o sistema
-- normalmente para agenda/clientes).
ALTER TABLE estabelecimentos
    ADD COLUMN IF NOT EXISTS visivel_no_mapa BOOLEAN DEFAULT TRUE;