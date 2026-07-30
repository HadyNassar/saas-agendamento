package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.whatsapp.CampanhaEnvioItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampanhaEnvioItemRepository extends JpaRepository<CampanhaEnvioItem, Long> {

    // Usado pelo job: busca um lote pequeno de itens PENDENTE de QUALQUER
    // campanha (ORDER BY criado_em garante ordem de chegada / FIFO entre
    // campanhas concorrentes). O tamanho do lote é controlado pelo Pageable
    // que o job passa, não por LIMIT fixo aqui.
    //
    // IMPORTANTE: itens de uma campanha em RASCUNHO (ainda não confirmada
    // pelo dono) NUNCA entram como PENDENTE — ver CampanhaEnvioService — por
    // isso esta query não precisa (e não deve) filtrar por status da
    // campanha: o status do ITEM já é a garantia de que só dispara o que foi
    // confirmado.
    //
    // JOIN FETCH campanha + estabelecimento: sem isso, o job (que roda em
    // ciclo @Scheduled próprio, sem uma sessão Hibernate aberta ligada à
    // requisição HTTP) recebe um proxy lazy de CampanhaEnvio.estabelecimento
    // e quebra com LazyInitializationException ("no session") ao tentar
    // acessá-lo em processarItem — isso já aconteceu em produção e travava o
    // lote inteiro, indefinidamente, a cada execução (o mesmo lote de itens
    // era pego repetidamente e nunca avançava, pois a exceção interrompia o
    // processamento antes de qualquer item ser marcado). Buscando tudo já
    // carregado aqui, numa única query, elimina o problema pela raiz.
    @Query("""
        SELECT i FROM CampanhaEnvioItem i
        JOIN FETCH i.campanha c
        JOIN FETCH c.estabelecimento
        WHERE i.status = 'PENDENTE'
        ORDER BY i.criadoEm ASC
    """)
    List<CampanhaEnvioItem> buscarProximoLotePendente(Pageable pageable);

    long countByCampanhaIdAndStatus(Long campanhaId, String status);

    long countByCampanhaId(Long campanhaId);

    // ── Transições em massa usadas por CampanhaEnvioService ────────────────
    // (chamadas sempre dentro de um método @Transactional do service, como
    // exigido pelo Spring Data para @Modifying)

    // Confirmação do dono: destrava todos os itens de uma campanha ainda em
    // RASCUNHO, liberando-os pro CampanhaEnvioProcessorJob pegar.
    @Modifying
    @Query("UPDATE CampanhaEnvioItem i SET i.status = 'PENDENTE' WHERE i.campanha.id = :campanhaId AND i.status = 'RASCUNHO'")
    int confirmarItensDaCampanha(@Param("campanhaId") Long campanhaId);

    // Cancelamento de um rascunho: marca os itens como CANCELADO em vez de
    // apagar as linhas, preservando o registro de quantos destinatários
    // aquele rascunho teria alcançado.
    @Modifying
    @Query("UPDATE CampanhaEnvioItem i SET i.status = 'CANCELADO' WHERE i.campanha.id = :campanhaId AND i.status = 'RASCUNHO'")
    int cancelarItensDaCampanha(@Param("campanhaId") Long campanhaId);
}