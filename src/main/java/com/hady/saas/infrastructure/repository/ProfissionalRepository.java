package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.profissional.Profissional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    List<Profissional> findByEstabelecimentoIdAndAtivo(Long estabelecimentoId, boolean ativo);

    Optional<Profissional> findByUsuarioId(Long usuarioId);

    // ✅ evita LazyInitializationException no ProfissionalService
    Optional<Profissional> findByIdAndEstabelecimentoId(Long id, Long estabelecimentoId);
}
