package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.cliente.ClienteEstagioHistorico;
import com.hady.saas.domain.enums.EstagioCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteEstagioHistoricoRepository extends JpaRepository<ClienteEstagioHistorico, Long> {

    List<ClienteEstagioHistorico> findByClienteIdOrderByCriadoEmDesc(Long clienteId);

    List<ClienteEstagioHistorico> findByEstabelecimentoIdOrderByCriadoEmDesc(Long estabelecimentoId);

    // Usada por CampanhaAutomaticaService (itens 9/10/11): transições
    // recentes ainda não avaliadas pelo gatilho automático. O parâmetro
    // `estagios` permite restringir quais estágios disparam mensagem — hoje
    // o service passa os 4 valores de EstagioCliente (item 11, funil
    // completo), mas o método fica genérico caso essa lista precise mudar.
    List<ClienteEstagioHistorico> findByProcessadoCampanhaAutomaticaFalseAndEstagioNovoIn(
            List<EstagioCliente> estagios);
}