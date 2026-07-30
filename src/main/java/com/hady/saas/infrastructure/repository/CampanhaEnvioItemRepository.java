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

    @Query("""
        SELECT i FROM CampanhaEnvioItem i
        JOIN FETCH i.campanha c
        JOIN FETCH c.estabelecimento
        WHERE i.status = 'PENDENTE'
        ORDER BY i.criadoEm ASC
    """)
    List<CampanhaEnvioItem> buscarProximoLotePendente(Pageable pageable);

    // Usado por CampanhaEnvioService.confirmar() para pegar todos os itens
    // RASCUNHO de uma campanha na hora de liberá-los como PENDENTE.
    // Não precisa de JOIN FETCH: esse método só lê/atualiza campos do
    // próprio item (status, agendadoPara), nunca acessa item.getCampanha()
    // dentro da transação.
    List<CampanhaEnvioItem> findByCampanhaIdAndStatusOrderByCriadoEmAsc(Long campanhaId, String status);

    long countByCampanhaIdAndStatus(Long campanhaId, String status);

    long countByCampanhaId(Long campanhaId);

    @Modifying
    @Query("UPDATE CampanhaEnvioItem i SET i.status = 'PENDENTE' WHERE i.campanha.id = :campanhaId AND i.status = 'RASCUNHO'")
    int confirmarItensDaCampanha(@Param("campanhaId") Long campanhaId);

    @Modifying
    @Query("UPDATE CampanhaEnvioItem i SET i.status = 'CANCELADO' WHERE i.campanha.id = :campanhaId AND i.status = 'RASCUNHO'")
    int cancelarItensDaCampanha(@Param("campanhaId") Long campanhaId);
}