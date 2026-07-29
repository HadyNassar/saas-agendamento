package com.hady.saas.webhook;

import com.hady.saas.business.service.BotAgendamentoService;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.whatsapp.ConversaWhatsApp;
import com.hady.saas.domain.whatsapp.WhatsAppMensagem;
import com.hady.saas.infrastructure.client.EvolutionApiClient;
import com.hady.saas.infrastructure.repository.ClienteRepository;
import com.hady.saas.infrastructure.repository.ConversaWhatsAppRepository;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.repository.WhatsAppMensagemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

// ⚠️ Rota pública — liberada no SecurityConfig (mesmo padrão do webhook do Mercado Pago)
//
// Recebe a mensagem, identifica o estabelecimento e o cliente, mantém a
// ConversaWhatsApp atualizada, grava o histórico e — se o robô estiver ligado
// para este estabelecimento/conversa e o plano permitir — encaminha o texto
// pro bot conversacional (BotAgendamentoService) e envia a resposta de volta
// pela Evolution API.
@RestController
@RequestMapping("/webhook/evolution")
public class EvolutionWebhook {

    private static final Logger log = LoggerFactory.getLogger(EvolutionWebhook.class);

    @Value("${evolution.api.key}")
    private String apiKeyEsperada;

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final ClienteRepository clienteRepository;
    private final ConversaWhatsAppRepository conversaRepository;
    private final WhatsAppMensagemRepository whatsAppMensagemRepository;
    private final BotAgendamentoService botAgendamentoService;
    private final EvolutionApiClient evolutionApiClient;

    public EvolutionWebhook(EstabelecimentoRepository estabelecimentoRepository,
                            ClienteRepository clienteRepository,
                            ConversaWhatsAppRepository conversaRepository,
                            WhatsAppMensagemRepository whatsAppMensagemRepository,
                            BotAgendamentoService botAgendamentoService,
                            EvolutionApiClient evolutionApiClient) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.clienteRepository = clienteRepository;
        this.conversaRepository = conversaRepository;
        this.whatsAppMensagemRepository = whatsAppMensagemRepository;
        this.botAgendamentoService = botAgendamentoService;
        this.evolutionApiClient = evolutionApiClient;
    }

    @PostMapping("/{token}")
    public ResponseEntity<Void> receber(
            @PathVariable String token,
            @RequestBody Map<String, Object> body) {

        // ── Validação da origem via token na URL ─────────────────────────
        // Trocamos a validação por header "apikey" para um token no path da
        // URL. Motivo: constatamos em produção que a Evolution (nas versões/
        // imagens mais recentes, ex. v1.8.6) nem sempre reenvia o header
        // "apikey" nas chamadas de webhook — o header chega ausente mesmo com
        // o webhook configurado corretamente. Um token no path é confiável
        // independente desse comportamento, pois não depende de a Evolution
        // mandar nenhum header especial: só de ela chamar a URL exata que
        // configuramos (que inclui o token secreto).
        if (apiKeyEsperada == null || apiKeyEsperada.isBlank()) {
            log.error("[Webhook Evolution] evolution.api.key não configurada — endpoint BLOQUEADO.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!apiKeyEsperada.equals(token)) {
            log.warn("[Webhook Evolution] token inválido na URL.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String event = String.valueOf(body.get("event"));
            String instanceName = String.valueOf(body.get("instance"));

            // Hoje só nos interessa mensagem recebida (mensagem enviada pelo próprio
            // número do salão via app do celular também dispara "messages.upsert" —
            // filtramos isso pelo campo fromMe dentro de data.key).
            if (!"messages.upsert".equals(event)) {
                return ResponseEntity.ok().build();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            if (data == null) return ResponseEntity.ok().build();

            @SuppressWarnings("unchecked")
            Map<String, Object> key = (Map<String, Object>) data.get("key");
            boolean fromMe = key != null && Boolean.TRUE.equals(key.get("fromMe"));
            if (fromMe) return ResponseEntity.ok().build(); // ignora eco de mensagem enviada pelo salão

            String remoteJid = key != null ? String.valueOf(key.get("remoteJid")) : null;
            String telefone = extrairTelefone(remoteJid);
            String textoRecebido = extrairTexto(data);

            if (telefone == null || textoRecebido == null) {
                return ResponseEntity.ok().build();
            }

            Optional<Estabelecimento> estabOpt = estabelecimentoRepository.findByEvolutionInstanceName(instanceName);
            if (estabOpt.isEmpty()) {
                log.warn("[Webhook Evolution] Instância '{}' não corresponde a nenhum estabelecimento.", instanceName);
                return ResponseEntity.ok().build();
            }
            Estabelecimento estabelecimento = estabOpt.get();

            Cliente cliente = clienteRepository
                    .findByTelefoneAndEstabelecimentoId(telefone, estabelecimento.getId())
                    .orElseGet(() -> criarClienteMinimo(telefone, estabelecimento));

            ConversaWhatsApp conversa = conversaRepository
                    .findByClienteIdAndEstabelecimentoId(cliente.getId(), estabelecimento.getId())
                    .orElseGet(() -> {
                        ConversaWhatsApp nova = new ConversaWhatsApp();
                        nova.setCliente(cliente);
                        nova.setEstabelecimento(estabelecimento);
                        return nova;
                    });

            // Quando a conversa é nova (cliente mandando mensagem pela 1ª vez), ela
            // ainda não tem ID (instância "transient" do Hibernate). Precisamos
            // persistir AGORA, antes de qualquer WhatsAppMensagem referenciá-la —
            // caso contrário o Hibernate lança TransientObjectException ao tentar
            // salvar uma mensagem que aponta para uma conversa ainda não salva.
            conversa = conversaRepository.save(conversa);

            // Checagem on-the-fly da expiração da pausa (sem job em background)
            conversa.limparPausaSeExpirada();
            conversa.setUltimaMensagemEm(LocalDateTime.now());
            conversa.setUltimaMensagemPreview(
                    textoRecebido.length() > 200 ? textoRecebido.substring(0, 200) : textoRecebido);

            WhatsAppMensagem mensagemRecebida = new WhatsAppMensagem();
            mensagemRecebida.setEstabelecimento(estabelecimento);
            mensagemRecebida.setCliente(cliente);
            mensagemRecebida.setConversa(conversa);
            mensagemRecebida.setTelefoneDestino(telefone);
            mensagemRecebida.setMensagem(textoRecebido);
            mensagemRecebida.setTipo("RECEBIDA_TEXTO");
            mensagemRecebida.setStatus("RECEBIDO");
            mensagemRecebida.setDirecao("RECEBIDA");
            whatsAppMensagemRepository.save(mensagemRecebida);

            // ── Bot conversacional (item 1, camada ELITE) ──────────────────
            // Só roda se: (a) o dono não pausou o robô globalmente, (b) essa
            // conversa específica não está pausada, e (c) o plano permite
            // (ELITE/PLATINUM — trial conta como ELITE). Nada disso afeta os
            // lembretes/confirmações automáticas do WhatsAppService, que são
            // um sistema à parte e continuam rodando sempre (seção 4).
            boolean podeResponderAutomaticamente = estabelecimento.isRobotAtivo()
                    && estabelecimento.getPlanoEfetivo() != Plano.STANDARD
                    && !conversa.isRobotPausadoAgora();

            if (podeResponderAutomaticamente) {
                try {
                    String resposta = botAgendamentoService.processarMensagem(
                            estabelecimento, cliente, conversa, textoRecebido);

                    if (resposta != null && !resposta.isBlank()) {
                        evolutionApiClient.enviarMensagem(instanceName, telefone, resposta);

                        WhatsAppMensagem mensagemEnviada = new WhatsAppMensagem();
                        mensagemEnviada.setEstabelecimento(estabelecimento);
                        mensagemEnviada.setCliente(cliente);
                        mensagemEnviada.setConversa(conversa);
                        mensagemEnviada.setTelefoneDestino(telefone);
                        mensagemEnviada.setMensagem(resposta);
                        mensagemEnviada.setTipo("ROBO_TEXTO");
                        mensagemEnviada.setStatus("ENVIADO");
                        mensagemEnviada.setDirecao("ENVIADA");
                        mensagemEnviada.setEnviadoEm(LocalDateTime.now());
                        whatsAppMensagemRepository.save(mensagemEnviada);
                    }
                } catch (Exception e) {
                    // Erro no bot não pode derrubar o processamento do webhook nem
                    // perder o histórico já gravado da mensagem recebida.
                    log.error("[Bot Agendamento] Falha ao processar/responder conversa id={}: {}",
                            conversa.getId(), e.getMessage(), e);
                }
            }

            // Salva por último: captura ultimaMensagemEm/preview + qualquer
            // mudança de estado/contexto feita pelo bot acima, tudo de uma vez.
            conversaRepository.save(conversa);

        } catch (Exception e) {
            // Sempre retorna 200 para a Evolution não ficar retentando em loop
            log.error("[Webhook Evolution] Erro ao processar evento", e);
        }

        return ResponseEntity.ok().build();
    }

    private Cliente criarClienteMinimo(String telefone, Estabelecimento estabelecimento) {
        Cliente cliente = new Cliente();
        cliente.setTelefone(telefone);
        cliente.setNome("Cliente WhatsApp " + telefone);
        cliente.setEstabelecimento(estabelecimento);
        return clienteRepository.save(cliente);
    }

    // remoteJid vem como "5567999999999@s.whatsapp.net" (ou @g.us para grupo, que ignoramos)
    private String extrairTelefone(String remoteJid) {
        if (remoteJid == null || remoteJid.contains("@g.us")) return null;
        String numero = remoteJid.replaceAll("[^0-9]", "");
        return numero.isBlank() ? null : numero;
    }

    @SuppressWarnings("unchecked")
    private String extrairTexto(Map<String, Object> data) {
        Object messageObj = data.get("message");
        if (!(messageObj instanceof Map)) return null;
        Map<String, Object> message = (Map<String, Object>) messageObj;

        if (message.get("conversation") instanceof String texto) return texto;

        if (message.get("extendedTextMessage") instanceof Map) {
            Map<String, Object> ext = (Map<String, Object>) message.get("extendedTextMessage");
            if (ext.get("text") instanceof String texto) return texto;
        }

        // Mensagens de botão/lista (respostas rápidas do fluxo do bot)
        if (message.get("buttonsResponseMessage") instanceof Map) {
            Map<String, Object> btn = (Map<String, Object>) message.get("buttonsResponseMessage");
            if (btn.get("selectedButtonId") instanceof String id) return id;
        }
        if (message.get("listResponseMessage") instanceof Map) {
            Map<String, Object> list = (Map<String, Object>) message.get("listResponseMessage");
            if (list.get("title") instanceof String titulo) return titulo;
        }

        return null;
    }
}