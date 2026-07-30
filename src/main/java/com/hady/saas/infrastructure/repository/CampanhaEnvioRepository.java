package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.whatsapp.CampanhaEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampanhaEnvioRepository extends JpaRepository<CampanhaEnvio, Long> {

    // JOIN FETCH em profissionais (ManyToMany) e cupomEstabelecimento
    // (ManyToOne LAZY): CampanhaEnvioResumoDTO acessa os dois pra montar a
    // resposta do painel, e esse acesso acontece no controller, DEPOIS que o
    // método do service já retornou — não dá pra confiar em uma transação
    // ainda aberta ali. Buscando tudo já carregado aqui, numa única query,
    // evita o mesmo LazyInitializationException que já aconteceu antes
    // com CampanhaEnvioItem (ver CampanhaEnvioItemRepository). DISTINCT
    // evita duplicar a linha por causa do JOIN com a coleção.
    @Query("""
        SELECT DISTINCT c FROM CampanhaEnvio c
        LEFT JOIN FETCH c.profissionais
        LEFT JOIN FETCH c.cupomEstabelecimento
        WHERE c.estabelecimento.id = :estabelecimentoId
        ORDER BY c.criadoEm DESC
    """)
    List<CampanhaEnvio> findByEstabelecimentoIdOrderByCriadoEmDesc(@Param("estabelecimentoId") Long estabelecimentoId);

    // Usado pelo controller de campanha manual (seção 5-B) pra garantir que o
    // dono só enxerga/mexe nas campanhas do próprio estabelecimento, mesmo
    // sabendo o id de uma campanha de outro salão.
    @Query("""
        SELECT DISTINCT c FROM CampanhaEnvio c
        LEFT JOIN FETCH c.profissionais
        LEFT JOIN FETCH c.cupomEstabelecimento
        WHERE c.id = :id AND c.estabelecimento.id = :estabelecimentoId
    """)
    Optional<CampanhaEnvio> findByIdAndEstabelecimentoId(@Param("id") Long id, @Param("estabelecimentoId") Long estabelecimentoId);

    @Query("""
        SELECT c FROM CampanhaEnvio c
        WHERE c.status = 'CRIADA'
        ORDER BY c.criadoEm ASC
    """)
    List<CampanhaEnvio> buscarPendentesDeProcessamento();

    // Usado para impor o intervalo mínimo entre campanhas (anti-banimento):
    // busca a campanha CONFIRMADA mais recente do estabelecimento (qualquer
    // status exceto RASCUNHO/CANCELADA, já que essas nunca chegaram a
    // disparar nada de verdade). Considera o momento da CONFIRMAÇÃO, não da
    // criação do rascunho — por isso ordena por criadoEm da campanha
    // CRIADA/EM_ANDAMENTO/CONCLUIDA/CONCLUIDA_COM_ERROS mais recente.
    @Query("""
        SELECT c FROM CampanhaEnvio c
        WHERE c.estabelecimento.id = :estabelecimentoId
          AND c.status NOT IN ('RASCUNHO', 'CANCELADA')
        ORDER BY c.criadoEm DESC
        LIMIT 1
    """)
    Optional<CampanhaEnvio> buscarUltimaConfirmada(@Param("estabelecimentoId") Long estabelecimentoId);
}