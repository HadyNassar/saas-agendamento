package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.profissional.Profissional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    // JOIN FETCH garante que usuario é carregado junto, evitando LazyInitializationException
    // e permitindo que o ProfissionalResponseDTO acesse p.getUsuario().getRole()
    @Query("SELECT p FROM Profissional p LEFT JOIN FETCH p.usuario WHERE p.estabelecimento.id = :estabId AND p.ativo = :ativo")
    List<Profissional> findByEstabelecimentoIdAndAtivo(@Param("estabId") Long estabelecimentoId, @Param("ativo") boolean ativo);

    Optional<Profissional> findByUsuarioId(Long usuarioId);

    // JOIN FETCH usuario para permitir acesso ao usuarioId fora de transação (ex: excluir)
    @Query("SELECT p FROM Profissional p LEFT JOIN FETCH p.usuario WHERE p.id = :id AND p.estabelecimento.id = :estabId")
    Optional<Profissional> findByIdAndEstabelecimentoId(@Param("id") Long id, @Param("estabId") Long estabelecimentoId);
}
