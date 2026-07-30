package com.hady.saas.business.service;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.whatsapp.CampanhaEnvio;
import com.hady.saas.domain.whatsapp.CampanhaEnvioItem;
import com.hady.saas.domain.whatsapp.WhatsAppMensagem;
import com.hady.saas.infrastructure.client.EvolutionApiClient;
import com.hady.saas.infrastructure.repository.CampanhaEnvioItemRepository;
import com.hady.saas.infrastructure.repository.CampanhaEnvioRepository;
import com.hady.saas.infrastructure.repository.WhatsAppMensagemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Processador da fila de campanhas de disparo em massa (CampanhaEnvio +
 * CampanhaEnvioItem). Fundação técnica para os itens 9/10 (PLATINUM) e para
 * a parte de campanha manual da seção 5-B — nenhum controller cria
 * CampanhaEnvio ainda, então este job não tem nada pra processar até que
 * essa próxima etapa seja construída.
 *
 * Design deliberado (fila via banco, sem RabbitMQ/Kafka):
 *   - Um @Scheduled roda a cada poucos segundos e processa um LOTE pequeno de
 *     itens PENDENTE por vez (TAMANHO_LOTE), não a fila inteira de uma vez —
 *     isso é o que limita a taxa de envio pra não estourar o rate limit da
 *     Evolution API, sem precisar de infraestrutura nova.
 *   - Cada item é processado e commitado individualmente (@Transactional por
 *     item, não por lote inteiro) — se o processo cair no meio de um lote,
 *     os itens já marcados como ENVIADO não são reprocessados.
 *   - Cada envio também grava um WhatsAppMensagem (tipo=CAMPANHA), pelo mesmo
 *     motivo que a resposta manual do inbox grava: aparecer no histórico da
 *     conversa do cliente, unificado com o resto.
 *
 * Escolha de intervalo/lote abaixo é um padrão meu, não é limite oficial
 * documentado pela Evolution API — ajustar aqui se filas grandes começarem
 * a demorar demais ou se a Evolution começar a bloquear por taxa.
 */
@Service
public class CampanhaEnvioProcessorJob {

    private static final Logger log = LoggerFactory.getLogger(CampanhaEnvioProcessorJob.class);

    // Quantos itens PENDENTE são processados a cada execução do job.
    private static final int TAMANHO_LOTE = 20;

    private final CampanhaEnvioRepository campanhaRepository;
    private final CampanhaEnvioItemRepository itemRepository;
    private final WhatsAppMensagemRepository whatsAppMensagemRepository;
    private final EvolutionApiClient evolutionApiClient;

    public CampanhaEnvioProcessorJob(CampanhaEnvioRepository campanhaRepository,
                                     CampanhaEnvioItemRepository itemRepository,
                                     WhatsAppMensagemRepository whatsAppMensagemRepository,
                                     EvolutionApiClient evolutionApiClient) {
        this.campanhaRepository = campanhaRepository;
        this.itemRepository = itemRepository;
        this.whatsAppMensagemRepository = whatsAppMensagemRepository;
        this.evolutionApiClient = evolutionApiClient;
    }

    // Roda a cada 10 segundos: intervalo curto o bastante pra não deixar uma
    // campanha pequena "parada" por muito tempo, mas processando só
    // TAMANHO_LOTE itens por vez — o efeito é um envio espaçado no tempo em
    // vez de uma rajada.
    @Scheduled(fixedDelay = 10 * 1000)
    public void processarLote() {
        Pageable lote = PageRequest.of(0, TAMANHO_LOTE);
        List<CampanhaEnvioItem> pendentes = itemRepository.buscarProximoLotePendente(lote);

        if (pendentes.isEmpty()) return;

        log.info("[CampanhaEnvio] Processando lote de {} item(ns) pendente(s)", pendentes.size());

        for (CampanhaEnvioItem item : pendentes) {
            // Defesa em profundidade: mesmo com a causa raiz do
            // LazyInitializationException corrigida (JOIN FETCH no
            // repository), um erro inesperado em UM item nunca pode travar
            // o lote inteiro — isso já aconteceu em produção (a mesma
            // exceção repetia a cada ciclo de 10s, sem nenhum item avançar,
            // porque a exceção escapava daqui e interrompia o
            // pendentes.stream()...forEach() logo abaixo antes de chegar
            // nele). Isolando por item, na pior hipótese só aquele item fica
            // sem ser marcado nesta rodada — o job tenta de novo no próximo
            // ciclo, sem bloquear os demais itens do lote.
            try {
                processarItem(item);
            } catch (Exception e) {
                log.error("[CampanhaEnvio] Erro inesperado processando item id={} (fora do fluxo normal de erro por item): {}",
                        item.getId(), e.getMessage(), e);
            }
        }

        // Atualiza o progresso agregado das campanhas afetadas por este lote
        // (pode haver mais de uma campanha misturada no mesmo lote).
        pendentes.stream()
                .map(i -> i.getCampanha().getId())
                .distinct()
                .forEach(campanhaId -> {
                    try {
                        atualizarProgressoCampanha(campanhaId);
                    } catch (Exception e) {
                        log.error("[CampanhaEnvio] Erro ao atualizar progresso da campanha id={}: {}",
                                campanhaId, e.getMessage(), e);
                    }
                });
    }

    @Transactional
    protected void processarItem(CampanhaEnvioItem item) {
        CampanhaEnvio campanha = item.getCampanha();
        Estabelecimento estabelecimento = campanha.getEstabelecimento();

        WhatsAppMensagem registro = new WhatsAppMensagem();
        registro.setEstabelecimento(estabelecimento);
        registro.setCliente(item.getCliente());
        registro.setTelefoneDestino(item.getTelefoneDestino());
        // Usa a mensagem FINAL do item (já com saudação personalizada e o
        // link do cupom, montada em CampanhaEnvioService.criarRascunho),
        // nunca campanha.getMensagem() puro — aquele é só o texto-base
        // digitado pelo dono, sem essas partes. Fallback pro texto-base só
        // por segurança, caso um item antigo tenha ficado sem mensagemFinal
        // preenchida (não deveria acontecer em itens novos).
        String mensagemParaEnviar = item.getMensagemFinal() != null
                ? item.getMensagemFinal()
                : campanha.getMensagem();
        registro.setMensagem(mensagemParaEnviar);
        registro.setTipo("CAMPANHA");
        registro.setDirecao("ENVIADA");

        try {
            if (estabelecimento.getEvolutionInstanceName() == null
                    || estabelecimento.getEvolutionInstanceName().isBlank()) {
                throw new IllegalStateException("WhatsApp não conectado para este estabelecimento");
            }

            evolutionApiClient.enviarMensagem(
                    estabelecimento.getEvolutionInstanceName(), item.getTelefoneDestino(), mensagemParaEnviar);

            registro.setStatus("ENVIADO");
            registro.setEnviadoEm(LocalDateTime.now());
            item.setStatus("ENVIADO");
        } catch (Exception e) {
            log.error("[CampanhaEnvio] Falha ao enviar item id={} da campanha id={}: {}",
                    item.getId(), campanha.getId(), e.getMessage(), e);
            registro.setStatus("ERRO");
            registro.setErroDetalhe(e.getMessage());
            item.setStatus("ERRO");
            item.setErroDetalhe(e.getMessage());
        }

        item.setProcessadoEm(LocalDateTime.now());
        itemRepository.save(item);
        whatsAppMensagemRepository.save(registro);
    }

    @Transactional
    protected void atualizarProgressoCampanha(Long campanhaId) {
        CampanhaEnvio campanha = campanhaRepository.findById(campanhaId).orElse(null);
        if (campanha == null) return;

        long total = itemRepository.countByCampanhaId(campanhaId);
        long enviados = itemRepository.countByCampanhaIdAndStatus(campanhaId, "ENVIADO");
        long erros = itemRepository.countByCampanhaIdAndStatus(campanhaId, "ERRO");
        long pendentes = total - enviados - erros;

        campanha.setTotalEnviados((int) enviados);
        campanha.setTotalErros((int) erros);

        if (pendentes == 0) {
            campanha.setStatus(erros > 0 ? "CONCLUIDA_COM_ERROS" : "CONCLUIDA");
            campanha.setConcluidoEm(LocalDateTime.now());
        } else if (!"EM_ANDAMENTO".equals(campanha.getStatus())) {
            campanha.setStatus("EM_ANDAMENTO");
        }

        campanhaRepository.save(campanha);
    }
}