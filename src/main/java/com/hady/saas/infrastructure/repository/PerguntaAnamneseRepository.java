package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.anamnese.PerguntaAnamnese;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerguntaAnamneseRepository extends JpaRepository<PerguntaAnamnese, Long> {

    // Lista perguntas ativas do estabelecimento ordenadas
    List<PerguntaAnamnese> findByEstabelecimentoIdAndAtivaOrderByOrdem(Long estabelecimentoId, boolean ativa);

    // Lista todas as perguntas (inclusive inativas) para o admin gerenciar
    List<PerguntaAnamnese> findByEstabelecimentoIdOrderByOrdem(Long estabelecimentoId);
}
