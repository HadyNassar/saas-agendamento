package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.servico.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> {

    // Lista serviços ativos de um estabelecimento
    List<Servico> findByEstabelecimentoIdAndAtivo(Long estabelecimentoId, boolean ativo);

    // Lista por categoria
    List<Servico> findByEstabelecimentoIdAndCategoriaAndAtivo(Long estabelecimentoId, String categoria, boolean ativo);
}
