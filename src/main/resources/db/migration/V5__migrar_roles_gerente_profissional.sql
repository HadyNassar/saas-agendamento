-- Migração: renomear roles antigas para os novos nomes
-- FUNCIONARIO          → GERENTE
-- FUNCIONARIO_PLATINUM → PROFISSIONAL

UPDATE usuarios SET role = 'GERENTE'       WHERE role = 'FUNCIONARIO';
UPDATE usuarios SET role = 'PROFISSIONAL'  WHERE role = 'FUNCIONARIO_PLATINUM';
