package com.hady.saas.business.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.whatsapp.BotContexto;
import com.hady.saas.domain.whatsapp.ConversaWhatsApp;
import com.hady.saas.infrastructure.repository.ConversaWhatsAppRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Item 2 (seção 5, camada ELITE): "responda 1 para confirmar, 2 para cancelar"
 * nos lembretes de 24h/2h.
 *
 * Responsabilidade separada de WhatsAppService de propósito: WhatsAppService só
 * formata e envia texto; aqui é decidido SE o prompt interativo entra na mensagem
 * (depende de plano/robô/estado da conversa) e é feita a preparação da
 * ConversaWhatsApp para que o EvolutionWebhook capture a resposta depois.
 *
 * IMPORTANTE: isto não muda em nada o comportamento dos lembretes no plano
 * STANDARD — lá a mensagem continua exatamente igual a antes (sem prompt),
 * porque elegivel() retorna false. Mantém a separação bot vs. mensagens
 * automáticas: só ENTRA no "modo bot" quando o plano permite.
 */
@Service
public class LembreteConfirmacaoService {

    private final ConversaWhatsAppRepository conversaRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LembreteConfirmacaoService(ConversaWhatsAppRepository conversaRepository) {
        this.conversaRepository = conversaRepository;
    }

    /**
     * Decide se este lembrete pode incluir o prompt "responda 1/2" e será
     * de fato capturado pelo webhook. Critérios (escolha minha, ajustar se o
     * dono do produto quiser regra diferente):
     *   - plano ELITE/PLATINUM (trial conta como ELITE) — feature paga, igual ao bot;
     *   - robô ligado para o estabelecimento;
     *   - conversa não pausada individualmente;
     *   - conversa está LIVRE — evita atropelar um fluxo de agendamento em
     *     andamento (ex: cliente no meio de uma marcação quando o lembrete de
     *     OUTRO horário dele dispara).
     */
    @Transactional(readOnly = true)
    public boolean elegivel(Agendamento agendamento) {
        Estabelecimento estabelecimento = agendamento.getEstabelecimento();
        if (estabelecimento.getPlanoEfetivo() == Plano.STANDARD) return false;
        if (!estabelecimento.isRobotAtivo()) return false;

        Cliente cliente = agendamento.getCliente();
        if (cliente == null || cliente.getTelefone() == null) return false;

        return conversaRepository.findByClienteIdAndEstabelecimentoId(cliente.getId(), estabelecimento.getId())
                .map(conversa -> {
                    conversa.limparPausaSeExpirada();
                    if (conversa.isRobotPausadoAgora()) return false;
                    String estado = conversa.getEstado();
                    return estado == null || ConversaWhatsApp.ESTADO_LIVRE.equals(estado);
                })
                .orElse(true); // sem conversa ainda = nunca conversou = livre por definição
    }

    /**
     * Marca a conversa como aguardando a resposta 1/2 para este agendamento
     * específico. Chamado logo depois que o lembrete (com o prompt) foi enviado.
     */
    @Transactional
    public void prepararConversaParaConfirmacao(Agendamento agendamento) {
        Cliente cliente = agendamento.getCliente();
        Estabelecimento estabelecimento = agendamento.getEstabelecimento();

        ConversaWhatsApp conversa = conversaRepository
                .findByClienteIdAndEstabelecimentoId(cliente.getId(), estabelecimento.getId())
                .orElseGet(() -> {
                    ConversaWhatsApp nova = new ConversaWhatsApp();
                    nova.setCliente(cliente);
                    nova.setEstabelecimento(estabelecimento);
                    return nova;
                });

        BotContexto contexto = new BotContexto();
        contexto.setAgendamentoId(agendamento.getId());
        try {
            conversa.setContexto(objectMapper.writeValueAsString(contexto));
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serializar contexto de confirmação de lembrete", e);
        }
        conversa.setEstado(ConversaWhatsApp.ESTADO_AGUARDANDO_CONFIRMACAO_LEMBRETE);
        conversaRepository.save(conversa);
    }
}