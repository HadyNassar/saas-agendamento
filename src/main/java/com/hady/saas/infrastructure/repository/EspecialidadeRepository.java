package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.profissional.Especialidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EspecialidadeRepository extends JpaRepository<Especialidade, Long> {

    List<Especialidade> findByEstabelecimentoId(Long estabelecimentoId);
}
