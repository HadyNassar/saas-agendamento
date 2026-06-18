package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.cobranca.CobrancaPlano;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CobrancaPlanoRepository extends JpaRepository<CobrancaPlano, Long> {
    Optional<CobrancaPlano> findByMpPaymentId(String mpPaymentId);
}