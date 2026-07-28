package com.hady.saas.application.controller;

import com.hady.saas.application.dto.ConversaWhatsAppResumoDTO;
import com.hady.saas.application.dto.PausarConversaDTO;
import com.hady.saas.application.dto.ResponderConversaDTO;
import com.hady.saas.application.dto.WhatsAppMensagemInboxDTO;
import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.whatsapp.ConversaWhatsApp;
import com.hady.saas.domain.whatsapp.WhatsAppMensagem;
import com.hady.saas.infrastructure.client.EvolutionApiClient;
import com.hady.saas.infrastructure.repository.ConversaWhatsAppRepository;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.repository.WhatsAppMensagemRepository;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Aba de controle do robô conversacional (painel do dono do estabelecimento).
 * Disponível a partir do plano ELITE (mesmo plano que libera o bot em si —
 * seção 4 do documento: "não faz sentido vender bot sem dar controle sobre ele").
 *
 * IMPORTANTE: nada aqui afeta lembretes/confirmações automáticas (WhatsAppService),
 * que continuam rodando sempre, mesmo com o robô conversacional desligado.
 */
@RestController
@RequestMapping("/painel/whatsapp/robo")
public class WhatsAppRoboController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppRoboController.class);

    // Item 5 (escolha padrão minha, não é regra de negócio validada — confirmado
    // que faz sentido ter *algum* teto): pausa automática do robô quando o dono
    // responde manualmente pelo inbox expira às 20h do mesmo dia (ou do dia
    // seguinte, se já passou das 20h quando ele respondeu). Reaproveita o mesmo
    // mecanismo de pausadoAte/limparPausaSeExpirada que a pausa manual da seção 4
    // já usa — diferença é só quem define o prazo. Pausa MANUAL (via /pausar sem
    // minutos) continua sem prazo, só volta quando o dono reativar.
    private static final int HORA_RETORNO_AUTOMATICO_APOS_RESPOSTA_MANUAL = 20;

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final ConversaWhatsAppRepository conversaRepository;
    private final WhatsAppMensagemRepository whatsAppMensagemRepository;
    private final EvolutionApiClient evolutionApiClient;
    private final AuthUtil authUtil;

    public WhatsAppRoboController(EstabelecimentoRepository estabelecimentoRepository,
                                  ConversaWhatsAppRepository conversaRepository,
                                  WhatsAppMensagemRepository whatsAppMensagemRepository,
                                  EvolutionApiClient evolutionApiClient,
                                  AuthUtil authUtil) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.conversaRepository = conversaRepository;
        this.whatsAppMensagemRepository = whatsAppMensagemRepository;
        this.evolutionApiClient = evolutionApiClient;
        this.authUtil = authUtil;
    }

    private Estabelecimento getEstab(Authentication auth) {
        return estabelecimentoRepository.findById(authUtil.getEstabelecimentoId(auth))
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
    }

    // Bloqueia todo o controller pra quem não tem ELITE/PLATINUM (nem trial, que cai em ELITE)
    private ResponseEntity<Map<String, Object>> bloqueioDePlanoOuNull(Estabelecimento est) {
        if (est.getPlanoEfetivo() == Plano.STANDARD) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(Map.of("erro", "O robô conversacional está disponível a partir do plano Elite."));
        }
        return null;
    }

    // ── Toggle geral ──────────────────────────────────────────────────────

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> status(Authentication auth) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        return ResponseEntity.ok(Map.of("ativo", est.isRobotAtivo()));
    }

    @PostMapping("/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> toggle(Authentication auth, @RequestBody Map<String, Boolean> body) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        Boolean ativo = body.get("ativo");
        if (ativo == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Campo 'ativo' é obrigatório."));
        }
        est.setRobotAtivo(ativo);
        estabelecimentoRepository.save(est);
        return ResponseEntity.ok(Map.of("ativo", est.isRobotAtivo()));
    }

    // ── Inbox de conversas ────────────────────────────────────────────────

    @GetMapping("/conversas")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> listarConversas(Authentication auth,
                                             @RequestParam(required = false) String busca) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        List<ConversaWhatsApp> conversas = (busca == null || busca.isBlank())
                ? conversaRepository.findByEstabelecimentoIdOrderByUltimaMensagemEmDesc(est.getId())
                : conversaRepository.buscarPorNomeOuTelefone(est.getId(), busca.trim());

        List<ConversaWhatsAppResumoDTO> resposta = conversas.stream()
                .map(ConversaWhatsAppResumoDTO::new)
                .toList();

        return ResponseEntity.ok(resposta);
    }

    // ── Histórico de mensagens de uma conversa (item 5) ────────────────────

    @GetMapping("/conversas/{id}/mensagens")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> listarMensagens(Authentication auth, @PathVariable Long id) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        ConversaWhatsApp conversa = buscarConversaDoEstabelecimento(id, est);

        List<WhatsAppMensagemInboxDTO> mensagens = whatsAppMensagemRepository
                .findByConversaIdOrderByCriadoEmAsc(conversa.getId())
                .stream()
                .map(WhatsAppMensagemInboxDTO::new)
                .toList();

        return ResponseEntity.ok(mensagens);
    }

    // ── Resposta manual pelo painel (item 5) ───────────────────────────────
    //
    // O dono digita uma mensagem no painel, ela é enviada de verdade pela
    // Evolution API (mesmo client usado pelo bot) e fica gravada no histórico
    // com direcao=ENVIADA/tipo=MANUAL_PAINEL — assim aparece junto com o resto
    // da conversa no inbox, distinguível de ROBO_TEXTO e das notificações
    // automáticas de sempre.
    //
    // Regra confirmada: responder manualmente pausa o bot conversacional
    // automaticamente para essa conversa (evita o robô responder por cima logo
    // em seguida), com retorno automático às 20h do dia (ou do dia seguinte, se
    // já passou das 20h) — diferente da pausa manual da aba de controle, que
    // fica pausada até o dono reativar.
    @PostMapping("/conversas/{id}/responder")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> responder(Authentication auth, @PathVariable Long id,
                                       @RequestBody ResponderConversaDTO dto) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        if (dto == null || dto.getMensagem() == null || dto.getMensagem().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Campo 'mensagem' é obrigatório."));
        }

        ConversaWhatsApp conversa = buscarConversaDoEstabelecimento(id, est);
        String telefone = conversa.getCliente().getTelefone();

        if (est.getEvolutionInstanceName() == null || est.getEvolutionInstanceName().isBlank()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("erro", "WhatsApp não está conectado para este estabelecimento."));
        }

        WhatsAppMensagem mensagemEnviada = new WhatsAppMensagem();
        mensagemEnviada.setEstabelecimento(est);
        mensagemEnviada.setCliente(conversa.getCliente());
        mensagemEnviada.setConversa(conversa);
        mensagemEnviada.setTelefoneDestino(telefone);
        mensagemEnviada.setMensagem(dto.getMensagem());
        mensagemEnviada.setTipo("MANUAL_PAINEL");
        mensagemEnviada.setDirecao("ENVIADA");

        try {
            evolutionApiClient.enviarMensagem(est.getEvolutionInstanceName(), telefone, dto.getMensagem());
            mensagemEnviada.setStatus("ENVIADO");
            mensagemEnviada.setEnviadoEm(LocalDateTime.now());
        } catch (Exception e) {
            log.error("[Inbox WhatsApp] Falha ao enviar resposta manual da conversa id={}: {}",
                    conversa.getId(), e.getMessage(), e);
            mensagemEnviada.setStatus("ERRO");
            mensagemEnviada.setErroDetalhe(e.getMessage());
        }
        whatsAppMensagemRepository.save(mensagemEnviada);

        // Pausa automática do bot pra essa conversa (regra confirmada acima),
        // só se a conversa já não estiver pausada manualmente sem prazo (nesse
        // caso, não sobrescrevemos o "sem prazo" da aba de controle por um
        // prazo automático mais curto).
        boolean jaPausadaSemPrazo = conversa.isRobotPausado() && conversa.getPausadoAte() == null;
        if (!jaPausadaSemPrazo) {
            conversa.setRobotPausado(true);
            conversa.setPausadoAte(calcularProximoRetornoAutomatico());
        }
        conversa.setUltimaMensagemEm(LocalDateTime.now());
        conversa.setUltimaMensagemPreview(
                dto.getMensagem().length() > 200 ? dto.getMensagem().substring(0, 200) : dto.getMensagem());
        conversaRepository.save(conversa);

        return ResponseEntity.ok(new WhatsAppMensagemInboxDTO(mensagemEnviada));
    }

    // Calcula as 20h de hoje, ou as 20h de amanhã se já passou das 20h agora.
    private LocalDateTime calcularProximoRetornoAutomatico() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime hoje20h = agora.toLocalDate().atTime(HORA_RETORNO_AUTOMATICO_APOS_RESPOSTA_MANUAL, 0);
        return agora.isBefore(hoje20h) ? hoje20h : hoje20h.plusDays(1);
    }

    // ── Pausar / retomar individual ───────────────────────────────────────

    @PostMapping("/conversas/{id}/pausar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> pausar(Authentication auth, @PathVariable Long id,
                                    @RequestBody(required = false) PausarConversaDTO dto) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        ConversaWhatsApp conversa = buscarConversaDoEstabelecimento(id, est);

        conversa.setRobotPausado(true);
        Integer minutos = dto != null ? dto.getMinutos() : null;
        conversa.setPausadoAte(minutos != null ? LocalDateTime.now().plusMinutes(minutos) : null);
        conversaRepository.save(conversa);

        return ResponseEntity.ok(new ConversaWhatsAppResumoDTO(conversa));
    }

    @PostMapping("/conversas/{id}/retomar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> retomar(Authentication auth, @PathVariable Long id) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        ConversaWhatsApp conversa = buscarConversaDoEstabelecimento(id, est);

        conversa.setRobotPausado(false);
        conversa.setPausadoAte(null);
        conversaRepository.save(conversa);

        return ResponseEntity.ok(new ConversaWhatsAppResumoDTO(conversa));
    }

    // Garante que a conversa pertence ao estabelecimento logado (evita um dono
    // mexer na conversa de outro salão só trocando o id na URL)
    private ConversaWhatsApp buscarConversaDoEstabelecimento(Long conversaId, Estabelecimento est) {
        ConversaWhatsApp conversa = conversaRepository.findById(conversaId)
                .orElseThrow(() -> new RuntimeException("Conversa não encontrada"));
        if (!conversa.getEstabelecimento().getId().equals(est.getId())) {
            throw new RuntimeException("Conversa não pertence a este estabelecimento");
        }
        return conversa;
    }
}