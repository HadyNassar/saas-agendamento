package com.hady.saas.business.service;

import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.whatsapp.CampanhaEnvio;
import com.hady.saas.domain.whatsapp.CampanhaEnvioItem;
import com.hady.saas.domain.enums.EstagioCliente;
import com.hady.saas.infrastructure.repository.CampanhaEnvioItemRepository;
import com.hady.saas.infrastructure.repository.CampanhaEnvioRepository;
import com.hady.saas.infrastructure.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Regra de negócio da campanha de disparo em massa MANUAL (seção 5-B do
 * documento — o dono do estabelecimento dispara para um segmento de
 * clientes). Reaproveita 100% a fundação técnica que já existia
 * (CampanhaEnvio / CampanhaEnvioItem / CampanhaEnvioProcessorJob) — este
 * service é só quem finalmente cria e alimenta essa fila.
 *
 * Ciclo de vida da campanha (decisão de design deste service, para atender
 * ao pedido de exigir confirmação explícita do dono antes de qualquer
 * disparo — não é regra de negócio previamente validada com o dono do
 * produto, é a forma que pareceu mais segura de implementar o requisito):
 *
 *   RASCUNHO -> (dono revisa e confirma) -> CRIADA -> (job processa em
 *   lotes) -> EM_ANDAMENTO -> CONCLUIDA / CONCLUIDA_COM_ERROS
 *                  \-> (dono desiste antes de confirmar) -> CANCELADA
 *
 * O pulo do gato pra garantir que nada é disparado sem confirmação: os
 * CampanhaEnvioItem de um rascunho são criados com status RASCUNHO (não
 * PENDENTE). O CampanhaEnvioProcessorJob só enxerga itens com status
 * PENDENTE — ou seja, uma campanha em rascunho é estruturalmente invisível
 * pro job, mesmo que ele rode antes da confirmação. Confirmar a campanha é
 * a única ação que promove os itens de RASCUNHO para PENDENTE (em massa,
 * numa única query), e é feito na mesma transação que muda o status do
 * cabeçalho — não existe estado intermediário onde só um dos dois mudou.
 */
@Service
public class CampanhaEnvioService {

    private final CampanhaEnvioRepository campanhaRepository;
    private final CampanhaEnvioItemRepository itemRepository;
    private final ClienteRepository clienteRepository;

    public CampanhaEnvioService(CampanhaEnvioRepository campanhaRepository,
                                CampanhaEnvioItemRepository itemRepository,
                                ClienteRepository clienteRepository) {
        this.campanhaRepository = campanhaRepository;
        this.itemRepository = itemRepository;
        this.clienteRepository = clienteRepository;
    }

    public static class CampanhaEnvioException extends RuntimeException {
        public CampanhaEnvioException(String mensagem) { super(mensagem); }
    }

    // ── Criação do rascunho ─────────────────────────────────────────────────

    @Transactional
    public CampanhaEnvio criarRascunho(Estabelecimento estabelecimento, EstagioCliente filtroEstagio, String mensagem) {
        if (filtroEstagio == null) {
            throw new CampanhaEnvioException("Campo 'filtroEstagio' é obrigatório.");
        }
        if (mensagem == null || mensagem.isBlank()) {
            throw new CampanhaEnvioException("Campo 'mensagem' é obrigatório.");
        }

        // Clientes bloqueados nunca entram numa campanha, independente do
        // estágio em que estejam — evita reabrir contato com quem o dono do
        // salão explicitamente bloqueou.
        List<Cliente> destinatarios = clienteRepository
                .findByEstabelecimentoIdAndEstagioAtual(estabelecimento.getId(), filtroEstagio)
                .stream()
                .filter(c -> !c.isBloqueado())
                .toList();

        if (destinatarios.isEmpty()) {
            throw new CampanhaEnvioException(
                    "Nenhum cliente encontrado para o segmento '" + filtroEstagio + "'. Nenhum rascunho foi criado.");
        }

        CampanhaEnvio campanha = new CampanhaEnvio();
        campanha.setEstabelecimento(estabelecimento);
        campanha.setFiltroEstagio(filtroEstagio);
        campanha.setMensagem(mensagem);
        campanha.setOrigem("MANUAL");
        campanha.setStatus("RASCUNHO");
        campanha.setTotalDestinatarios(destinatarios.size());
        campanha = campanhaRepository.save(campanha);

        for (Cliente cliente : destinatarios) {
            CampanhaEnvioItem item = new CampanhaEnvioItem(campanha, cliente, cliente.getTelefone());
            item.setStatus("RASCUNHO");
            itemRepository.save(item);
        }

        return campanha;
    }

    // ── Confirmação (dispara de verdade, na prática libera pro job) ────────

    @Transactional
    public CampanhaEnvio confirmar(Estabelecimento estabelecimento, Long campanhaId) {
        CampanhaEnvio campanha = buscarDoEstabelecimento(estabelecimento, campanhaId);

        if (!"RASCUNHO".equals(campanha.getStatus())) {
            throw new CampanhaEnvioException(
                    "Esta campanha não pode mais ser confirmada (status atual: " + campanha.getStatus() + ").");
        }
        if (estabelecimento.getEvolutionInstanceName() == null
                || estabelecimento.getEvolutionInstanceName().isBlank()) {
            throw new CampanhaEnvioException("WhatsApp não está conectado para este estabelecimento.");
        }

        int promovidos = itemRepository.confirmarItensDaCampanha(campanha.getId());
        if (promovidos == 0) {
            // Não deveria acontecer (campanha em RASCUNHO sempre tem itens em
            // RASCUNHO), mas evita deixar uma campanha "CRIADA" sem nenhum
            // item pendente, o que ficaria presa em EM_ANDAMENTO pra sempre.
            throw new CampanhaEnvioException("Esta campanha não tem destinatários pendentes de confirmação.");
        }

        campanha.setStatus("CRIADA");
        return campanhaRepository.save(campanha);
    }

    // ── Cancelamento de um rascunho ainda não confirmado ────────────────────

    @Transactional
    public CampanhaEnvio cancelar(Estabelecimento estabelecimento, Long campanhaId) {
        CampanhaEnvio campanha = buscarDoEstabelecimento(estabelecimento, campanhaId);

        if (!"RASCUNHO".equals(campanha.getStatus())) {
            throw new CampanhaEnvioException(
                    "Só é possível cancelar uma campanha que ainda está em rascunho (status atual: "
                            + campanha.getStatus() + ").");
        }

        itemRepository.cancelarItensDaCampanha(campanha.getId());
        campanha.setStatus("CANCELADA");
        campanha.setConcluidoEm(LocalDateTime.now());
        return campanhaRepository.save(campanha);
    }

    // ── Leitura ──────────────────────────────────────────────────────────────

    public List<CampanhaEnvio> listar(Estabelecimento estabelecimento) {
        return campanhaRepository.findByEstabelecimentoIdOrderByCriadoEmDesc(estabelecimento.getId());
    }

    public CampanhaEnvio buscarDoEstabelecimento(Estabelecimento estabelecimento, Long campanhaId) {
        return campanhaRepository.findByIdAndEstabelecimentoId(campanhaId, estabelecimento.getId())
                .orElseThrow(() -> new CampanhaEnvioException("Campanha não encontrada."));
    }
}