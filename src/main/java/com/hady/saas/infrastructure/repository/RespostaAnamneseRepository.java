package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.anamnese.RespostaAnamnese;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RespostaAnamneseRepository extends JpaRepository<RespostaAnamnese, Long> {

    // Busca todas as respostas de um cliente
    List<RespostaAnamnese> findByClienteId(Long clienteId);

    // Verifica se cliente já respondeu uma pergunta específica
    Optional<RespostaAnamnese> findByClienteIdAndPerguntaId(Long clienteId, Long perguntaId);

    // Verifica se cliente já preencheu a anamnese
    boolean existsByClienteId(Long clienteId);
}
