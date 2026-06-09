package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.servico.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> {

    // Lista serviços ativos de um estabelecimento
    List<Servico> findByEstabelecimentoIdAndAtivo(Long estabelecimentoId, boolean ativo);

    // Lista por categoria
    List<Servico> findByEstabelecimentoIdAndCategoriaAndAtivo(Long estabelecimentoId, String categoria, boolean ativo);

    // ✅ Busca por id filtrando pelo estabelecimento — evita LazyInitializationException
    Optional<Servico> findByIdAndEstabelecimentoId(Long id, Long estabelecimentoId);

    // Busca múltiplos serviços por lista de ids, garantindo que pertencem ao estabelecimento
    // Usado na vinculação profissional×serviço (N×N — vários profissionais podem ter o mesmo serviço)
    List<Servico> findByIdInAndEstabelecimentoId(List<Long> ids, Long estabelecimentoId);
}
