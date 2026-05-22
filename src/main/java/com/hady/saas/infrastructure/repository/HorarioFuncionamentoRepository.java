package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.horario.HorarioFuncionamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface HorarioFuncionamentoRepository extends JpaRepository<HorarioFuncionamento, Long> {

    List<HorarioFuncionamento> findByEstabelecimentoId(Long estabelecimentoId);

    Optional<HorarioFuncionamento> findByEstabelecimentoIdAndDiaSemana(Long estabelecimentoId, String diaSemana);
}
