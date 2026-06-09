package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.fotoprofissional.FotoProfissional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FotoProfissionalRepository extends JpaRepository<FotoProfissional, Long> {

    List<FotoProfissional> findByProfissionalIdOrderByCriadoEmDesc(Long profissionalId);

    long countByProfissionalId(Long profissionalId);
}
