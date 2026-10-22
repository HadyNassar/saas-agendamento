package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.whatsapp.ConversaWhatsApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversaWhatsAppRepository extends JpaRepository<ConversaWhatsApp, Long> {

    Optional<ConversaWhatsApp> findByClienteIdAndEstabelecimentoId(Long clienteId, Long estabelecimentoId);

    @Query("""
           SELECT c FROM ConversaWhatsApp c
           JOIN FETCH c.cliente
           WHERE c.estabelecimento.id = :estabelecimentoId
           ORDER BY c.ultimaMensagemEm DESC NULLS LAST
           """)
    List<ConversaWhatsApp> findByEstabelecimentoIdOrderByUltimaMensagemEmDesc(@Param("estabelecimentoId") Long estabelecimentoId);

    @Query("""
           SELECT c FROM ConversaWhatsApp c
           JOIN FETCH c.cliente
           WHERE c.estabelecimento.id = :estabelecimentoId
             AND (LOWER(c.cliente.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
                  OR c.cliente.telefone LIKE CONCAT('%', :busca, '%'))
           ORDER BY c.ultimaMensagemEm DESC NULLS LAST
           """)
    List<ConversaWhatsApp> buscarPorNomeOuTelefone(@Param("estabelecimentoId") Long estabelecimentoId,
                                                   @Param("busca") String busca);
}