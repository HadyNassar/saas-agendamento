-- ============================================================
-- V2 — Adiciona coluna senha_atualizada_em em usuarios
--
-- Usada pelo JwtAuthenticationFilter para invalidar tokens
-- emitidos antes da última troca de senha, sem precisar de
-- blacklist ou Redis. Nullable — usuários existentes continuam
-- funcionando normalmente até trocarem a senha pela primeira vez.
-- ============================================================

ALTER TABLE usuarios
    ADD COLUMN senha_atualizada_em TIMESTAMP;
