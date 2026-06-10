package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.oauth.OAuthState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

public interface OAuthStateRepository extends JpaRepository<OAuthState, String> {

    @Modifying
    @Transactional
    @Query("DELETE FROM OAuthState o WHERE o.criadoEm < :limite")
    void deleteExpired(Instant limite);
}