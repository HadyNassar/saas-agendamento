package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.enums.StatusIndicacao;
import com.hady.saas.domain.indicacao.Indicacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndicacaoRepository extends JpaRepository<Indicacao, Long> {

    Optional<Indicacao> findByCodigo(String codigo);

    // Indicação pendente (aguardando o indicado concluir o primeiro atendimento)
    // vinculada a um cliente indicado específico. Usada no gatilho de conclusão
    // do agendamento (AgendamentoService.concluir).
    Optional<Indicacao> findByIndicadoIdAndStatus(Long indicadoId, StatusIndicacao status);

    List<Indicacao> findByEstabelecimentoIdOrderByCriadoEmDesc(Long estabelecimentoId);

    @Query("""
        SELECT i FROM Indicacao i
        WHERE i.status IN :statusAbertos
          AND i.dataExpiracao < :agora
    """)
    List<Indicacao> findExpiradasNaoProcessadas(@Param("statusAbertos") List<StatusIndicacao> statusAbertos,
                                                @Param("agora") LocalDateTime agora);
}