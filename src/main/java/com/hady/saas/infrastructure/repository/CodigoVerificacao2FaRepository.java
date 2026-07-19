package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.usuario.CodigoVerificacao2FA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CodigoVerificacao2FARepository extends JpaRepository<CodigoVerificacao2FA, Long> {

    // Busca o código válido mais recente para conferência no login.
    @Query("""
            SELECT c FROM CodigoVerificacao2FA c
            WHERE c.usuarioId = :usuarioId
              AND c.codigo = :codigo
              AND c.usado = false
              AND c.expiraEm > :agora
            ORDER BY c.criadoEm DESC
            """)
    Optional<CodigoVerificacao2FA> buscarCodigoValido(@Param("usuarioId") Long usuarioId,
                                                      @Param("codigo") String codigo,
                                                      @Param("agora") LocalDateTime agora);
}