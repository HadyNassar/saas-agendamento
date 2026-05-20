package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.profissional.Profissional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    // Lista profissionais ativos de um estabelecimento
    List<Profissional> findByEstabelecimentoIdAndAtivo(Long estabelecimentoId, boolean ativo);

    // Busca profissional por usuário vinculado
    Optional<Profissional> findByUsuarioId(Long usuarioId);

    // Verifica se usuário já é profissional
    boolean existsByUsuarioId(Long usuarioId);
}
