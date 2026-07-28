package com.hady.saas.business.service;

import com.hady.saas.application.dto.EstatisticasClienteDTO;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.cliente.ClienteEstagioHistorico;
import com.hady.saas.domain.enums.EstagioCliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import com.hady.saas.infrastructure.repository.ClienteEstagioHistoricoRepository;
import com.hady.saas.infrastructure.repository.ClienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Segmentação de cliente (EstagioCliente): NOVO, RECORRENTE, VIP, INATIVO.
 *
 * calcular() continua sendo um cálculo sob demanda (usado pelo endpoint
 * GET /painel/clientes/{id}/estagio e por qualquer leitura pontual) — não
 * depende de nada estar persistido, então sempre reflete a realidade atual.
 *
 * A partir da migration V23, o resultado também é PERSISTIDO em
 * Cliente.estagioAtual/estagioAtualizadoEm via recalcularEPersistir(), com uma
 * linha de histórico gravada em cliente_estagio_historico sempre que o
 * estágio muda. Isso existe porque os itens 10/11 (reativação por regras,
 * funil/CRM) precisam saber QUANDO um cliente mudou de estágio (ex: disparar
 * uma mensagem de "sentimos sua falta" no exato momento em que ele vira
 * INATIVO), e não dá pra derivar isso de um valor calculado sob demanda.
 *
 * Ponto de chamada: AgendamentoService.concluir(), já que CONCLUIDO é o único
 * status que entra na conta de visitas/última visita (ver
 * AgendamentoRepository.buscarEstatisticasCliente). Nenhum job/cron novo foi
 * introduzido — o recálculo é sempre disparado por uma ação real do sistema.
 *
 * Usado tanto pelo bot conversacional (personalizar tom) quanto pelas campanhas
 * de marketing (PLATINUM).
 *
 * Regras (confirmadas com o dono do produto em 27/07/2026):
 *   - NOVO:       0 ou 1 atendimento concluído
 *   - INATIVO:    última visita há mais de DIAS_INATIVIDADE dias (e não é NOVO)
 *   - VIP:        MINIMO_VISITAS_VIP ou mais atendimentos concluídos
 *   - RECORRENTE: os demais casos
 */
@Service
public class ClienteSegmentacaoService {

    // Regra de negócio confirmada com o dono do produto em 27/07/2026
    // (substituindo os valores padrão anteriores de 60 dias / 6 visitas).
    private static final int DIAS_INATIVIDADE = 35;
    private static final long MINIMO_VISITAS_VIP = 10;

    private static final Logger log = LoggerFactory.getLogger(ClienteSegmentacaoService.class);

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ClienteEstagioHistoricoRepository historicoRepository;

    public ClienteSegmentacaoService(AgendamentoRepository agendamentoRepository,
                                     ClienteRepository clienteRepository,
                                     ClienteEstagioHistoricoRepository historicoRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.clienteRepository = clienteRepository;
        this.historicoRepository = historicoRepository;
    }

    public EstatisticasClienteDTO calcular(Long clienteId, Long estabelecimentoId) {
        Object[] linha = agendamentoRepository.buscarEstatisticasCliente(clienteId, estabelecimentoId);

        long totalVisitas = linha[0] != null ? ((Number) linha[0]).longValue() : 0L;
        BigDecimal ticketMedio = linha[1] != null ? (BigDecimal) linha[1] : null;
        LocalDateTime ultimaVisita = (LocalDateTime) linha[2];

        EstagioCliente estagio = calcularEstagio(totalVisitas, ultimaVisita);

        return new EstatisticasClienteDTO(estagio, totalVisitas, ticketMedio, ultimaVisita);
    }

    /**
     * Recalcula o EstagioCliente e PERSISTE o resultado em Cliente.estagioAtual
     * (só grava se o valor mudou, e nesse caso registra a transição em
     * cliente_estagio_historico). Chamado a partir de
     * AgendamentoService.concluir() — não há job/cron novo, o gatilho é sempre
     * o próprio agendamento sendo marcado como concluído.
     *
     * Deliberadamente não propaga exceção: uma falha aqui (ex: cliente já
     * removido) não pode impedir a conclusão do agendamento em si, que é a
     * operação principal que o chamador está executando.
     */
    @Transactional
    public void recalcularEPersistir(Long clienteId, Long estabelecimentoId) {
        try {
            Cliente cliente = clienteRepository.findByIdAndEstabelecimentoId(clienteId, estabelecimentoId)
                    .orElse(null);
            if (cliente == null) {
                log.warn("[Segmentação] Cliente id={} não encontrado para estabelecimento id={}, pulando recálculo.",
                        clienteId, estabelecimentoId);
                return;
            }

            EstatisticasClienteDTO estatisticas = calcular(clienteId, estabelecimentoId);
            EstagioCliente estagioNovo = estatisticas.getEstagio();
            EstagioCliente estagioAnterior = cliente.getEstagioAtual();

            if (estagioAnterior == estagioNovo) {
                return; // nada mudou, não precisa gravar nem tocar a linha do cliente
            }

            cliente.setEstagioAtual(estagioNovo);
            cliente.setEstagioAtualizadoEm(LocalDateTime.now());
            clienteRepository.save(cliente);

            Estabelecimento estabelecimento = cliente.getEstabelecimento();
            ClienteEstagioHistorico registro =
                    new ClienteEstagioHistorico(cliente, estabelecimento, estagioAnterior, estagioNovo);
            historicoRepository.save(registro);

            log.info("[Segmentação] Cliente id={} mudou de estágio: {} -> {}",
                    clienteId, estagioAnterior, estagioNovo);
        } catch (Exception e) {
            log.error("[Segmentação] Falha ao recalcular/persistir estágio do cliente id={}: {}",
                    clienteId, e.getMessage(), e);
        }
    }

    private EstagioCliente calcularEstagio(long totalVisitas, LocalDateTime ultimaVisita) {
        if (totalVisitas <= 1) {
            return EstagioCliente.NOVO;
        }
        if (ultimaVisita != null && ultimaVisita.isBefore(LocalDateTime.now().minusDays(DIAS_INATIVIDADE))) {
            return EstagioCliente.INATIVO;
        }
        if (totalVisitas >= MINIMO_VISITAS_VIP) {
            return EstagioCliente.VIP;
        }
        return EstagioCliente.RECORRENTE;
    }
}