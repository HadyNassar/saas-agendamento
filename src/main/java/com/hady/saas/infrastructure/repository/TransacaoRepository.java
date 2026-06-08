package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.transacao.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    Optional<Transacao> findByMpPaymentId(String mpPaymentId);
}
