package com.hady.saas.business.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hady.saas.application.dto.AssinaturaDTO;
import com.hady.saas.application.dto.PagamentoAvulsoPlanoDTO;
import com.hady.saas.application.dto.TrocarPlanoDTO;
import com.hady.saas.domain.cobranca.CobrancaPlano;
import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.enums.Role;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.config.MercadoPagoConfiguration;
import com.hady.saas.infrastructure.repository.CobrancaPlanoRepository;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.repository.UsuarioRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AssinaturaService {

    private static final String MP_API = "https://api.mercadopago.com";

    private final MercadoPagoConfiguration mpConfig;
    private final EstabelecimentoRepository estabelecimentoRepo;
    private final CobrancaPlanoRepository cobrancaRepo;
    private final UsuarioRepository usuarioRepo;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssinaturaService(MercadoPagoConfiguration mpConfig,
                             EstabelecimentoRepository estabelecimentoRepo,
                             CobrancaPlanoRepository cobrancaRepo,
                             UsuarioRepository usuarioRepo,
                             RestTemplate restTemplate) {
        this.mpConfig = mpConfig;
        this.estabelecimentoRepo = estabelecimentoRepo;
        this.cobrancaRepo = cobrancaRepo;
        this.usuarioRepo = usuarioRepo;
        this.restTemplate = restTemplate;
    }

    // ─── 1. Assinar plano RECORRENTE (primeira assinatura) ───────────────────
    @Transactional
    public Map<String, Object> assinarRecorrente(Long estabelecimentoId, AssinaturaDTO dto) {

        Estabelecimento estab = estabelecimentoRepo.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        Plano plano = Plano.valueOf(dto.getPlano().toUpperCase());
        String planId = mpConfig.getPlanoIdPorTipo(dto.getPlano());

        // Guarda o ID da assinatura antiga ANTES de criar a nova.
        // A nova é criada primeiro para garantir que o cliente não fique sem
        // plano caso a criação falhe. Só cancela a antiga após confirmação.
        String assinaturaAntiga = estab.getMpSubscriptionId();

        Map<String, Object> body = new HashMap<>();
        body.put("preapproval_plan_id", planId);
        body.put("payer_email", dto.getEmail());
        body.put("card_token_id", dto.getCardToken());
        body.put("status", "authorized");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headersComToken(mpConfig.getAccessTokenAssinaturas()));
        ResponseEntity<Map> response = restTemplate.postForEntity(MP_API + "/preapproval", request, Map.class);

        Map<String, Object> data = response.getBody();
        if (data == null) throw new RuntimeException("Erro ao criar assinatura no Mercado Pago");

        String subscriptionId = (String) data.get("id");
        String status = (String) data.get("status");

        // Só ativa o plano se o MP realmente autorizou o cartão.
        // Sem essa verificação, um status "pending" ativaria o plano gratuitamente.
        if (!"authorized".equals(status)) {
            throw new RuntimeException(
                    "Assinatura criada no Mercado Pago mas não foi autorizada (status=" + status +
                            "). Verifique os dados do cartão e tente novamente.");
        }

        // Nova assinatura autorizada — agora cancela a antiga com segurança.
        if (assinaturaAntiga != null) {
            try {
                cancelarAssinaturaMP(assinaturaAntiga);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(AssinaturaService.class)
                        .warn("[ASSINAR] Falha ao cancelar assinatura antiga {} no MP (pode já estar cancelada): {}",
                                assinaturaAntiga, e.getMessage());
            }
        }

        estab.setPlano(plano);
        estab.setMpSubscriptionId(subscriptionId);
        estab.setPlanoAtivo(true);
        estab.setPlanoValidoAte(LocalDate.now().plusMonths(1));
        estabelecimentoRepo.save(estab);

        CobrancaPlano cobranca = new CobrancaPlano();
        cobranca.setEstabelecimento(estab);
        cobranca.setPlano(plano.name());
        cobranca.setTipoCobranca("RECORRENTE");
        cobranca.setMpSubscriptionId(subscriptionId);
        cobranca.setValor(plano.getPreco());
        cobranca.setStatus("AUTORIZADO");
        cobranca.setPeriodoInicio(LocalDate.now());
        cobranca.setPeriodoFim(LocalDate.now().plusMonths(1));
        cobrancaRepo.save(cobranca);

        Map<String, Object> result = new HashMap<>();
        result.put("subscriptionId", subscriptionId);
        result.put("status", status);
        result.put("plano", plano.name());
        return result;
    }

    // ─── 2. Trocar plano (upgrade / downgrade) ───────────────────────────────
    //
    // Regras de negócio:
    //  - Exige assinatura recorrente ativa (mpSubscriptionId presente).
    //  - Impede trocar para o mesmo plano já ativo.
    //  - Cria a NOVA assinatura primeiro; só cancela a antiga se a criação
    //    vier com status "authorized". Isso garante que o cliente nunca fique
    //    sem plano ativo caso a criação da nova falhe.
    //  - Registra a operação em CobrancaPlano com tipoCobranca "TROCA".
    @Transactional
    public Map<String, Object> trocarPlano(Long estabelecimentoId, TrocarPlanoDTO dto) {

        Estabelecimento estab = estabelecimentoRepo.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        if (estab.getMpSubscriptionId() == null || !estab.isPlanoAtivo()) {
            throw new RuntimeException(
                    "Nenhuma assinatura recorrente ativa encontrada. " +
                            "Use /painel/assinatura/recorrente para assinar um plano.");
        }

        Plano novoPlano = Plano.valueOf(dto.getPlano().toUpperCase());

        if (novoPlano == estab.getPlano()) {
            throw new RuntimeException(
                    "Você já está no plano " + novoPlano.name() + ". Escolha um plano diferente.");
        }

        // Valida downgrade: impede trocar para plano com limite menor que o número
        // atual de funcionários ativos, evitando que o estabelecimento fique com
        // mais usuários do que o plano permite.
        if (novoPlano.getLimiteUsuarios() < estab.getPlano().getLimiteUsuarios()) {
            int totalFuncionarios = usuarioRepo.countByEstabelecimentoIdAndRoleNot(
                    estabelecimentoId, Role.ADMIN);
            if (totalFuncionarios > novoPlano.getLimiteUsuarios()) {
                throw new RuntimeException(
                        "Não é possível fazer downgrade para o plano " + novoPlano.name() +
                                " (limite: " + novoPlano.getLimiteUsuarios() + " funcionário(s)). " +
                                "Você possui " + totalFuncionarios + " funcionário(s) ativo(s). " +
                                "Desative os funcionários excedentes antes de trocar de plano.");
            }
        }

        // Valida downgrade de PLATINUM: planos STANDARD e ELITE permitem apenas 1 ADMIN.
        // Se o estabelecimento tiver 2 ADMINs (permitido só no PLATINUM), bloqueia o downgrade.
        if (novoPlano != Plano.PLATINUM && estab.getPlano() == Plano.PLATINUM) {
            int totalAdmins = usuarioRepo.countByEstabelecimentoIdAndRole(estabelecimentoId, Role.ADMIN);
            if (totalAdmins > 1) {
                throw new RuntimeException(
                        "Não é possível fazer downgrade para o plano " + novoPlano.name() +
                                " pois o estabelecimento possui " + totalAdmins + " ADMINs. " +
                                "O plano " + novoPlano.name() + " permite apenas 1 ADMIN. " +
                                "Remova os ADMINs excedentes antes de trocar de plano.");
            }
        }

        String planId = mpConfig.getPlanoIdPorTipo(dto.getPlano());

        // 1. Cria a NOVA assinatura no MP primeiro.
        //    Se isso falhar (timeout, 4xx, 5xx), a exceção sobe e o @Transactional
        //    reverte o banco — a assinatura antiga continua intacta no MP e no BD.
        Map<String, Object> body = new HashMap<>();
        body.put("preapproval_plan_id", planId);
        body.put("payer_email", dto.getEmail());
        body.put("card_token_id", dto.getCardToken());
        body.put("status", "authorized");

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headersComToken(mpConfig.getAccessTokenAssinaturas()));
        ResponseEntity<Map> response =
                restTemplate.postForEntity(MP_API + "/preapproval", request, Map.class);

        Map<String, Object> data = response.getBody();
        if (data == null) throw new RuntimeException("Erro ao criar nova assinatura no Mercado Pago");

        String novoSubscriptionId = (String) data.get("id");
        String status             = (String) data.get("status");

        // 2. Só cancela a antiga após confirmação da nova.
        //    Se o cancel falhar (ex: já cancelada no MP), apenas loga —
        //    o cliente não perde o novo plano por causa disso.
        if (!"authorized".equals(status)) {
            throw new RuntimeException(
                    "Nova assinatura criada no MP mas não foi autorizada (status=" + status +
                            "). Assinatura anterior mantida. Entre em contato com o suporte.");
        }

        String assinaturaAntiga = estab.getMpSubscriptionId();
        try {
            cancelarAssinaturaMP(assinaturaAntiga);
        } catch (Exception e) {
            // Loga mas não desfaz — a nova já foi criada e autorizada.
            // A assinatura antiga pode já ter sido cancelada pelo próprio MP.
            org.slf4j.LoggerFactory.getLogger(AssinaturaService.class)
                    .warn("[TROCA-PLANO] Falha ao cancelar assinatura antiga {} no MP (pode já estar cancelada): {}",
                            assinaturaAntiga, e.getMessage());
        }

        // 3. Atualiza o estabelecimento com o novo plano
        Plano planoAnterior = estab.getPlano();
        estab.setPlano(novoPlano);
        estab.setMpSubscriptionId(novoSubscriptionId);
        estab.setPlanoAtivo(true);
        estab.setPlanoValidoAte(LocalDate.now().plusMonths(1));
        estabelecimentoRepo.save(estab);

        // 4. Registra a troca como cobrança
        CobrancaPlano cobranca = new CobrancaPlano();
        cobranca.setEstabelecimento(estab);
        cobranca.setPlano(novoPlano.name());
        cobranca.setTipoCobranca("TROCA");
        cobranca.setMpSubscriptionId(novoSubscriptionId);
        cobranca.setValor(novoPlano.getPreco());
        cobranca.setStatus("AUTORIZADO");
        cobranca.setPeriodoInicio(LocalDate.now());
        cobranca.setPeriodoFim(LocalDate.now().plusMonths(1));
        cobrancaRepo.save(cobranca);

        Map<String, Object> result = new HashMap<>();
        result.put("planoAnterior",  planoAnterior.name());
        result.put("planoNovo",      novoPlano.name());
        result.put("subscriptionId", novoSubscriptionId);
        result.put("status",         status);
        result.put("validoAte",      LocalDate.now().plusMonths(1).toString());
        return result;
    }

    // ─── 3. Pagamento AVULSO de plano (cartão, 1x) ──────────────────────────
    @Transactional
    public Map<String, Object> pagarAvulso(Long estabelecimentoId, PagamentoAvulsoPlanoDTO dto) {

        Estabelecimento estab = estabelecimentoRepo.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        Plano plano = Plano.valueOf(dto.getPlano().toUpperCase());

        // FIX: paymentMethodId agora é @NotBlank no DTO — nunca será nulo/vazio aqui.
        // Removido o fallback hardcoded "master" que rejeitava Visa, Elo, Hipercard etc.
        String paymentMethodId = dto.getPaymentMethodId();

        Map<String, Object> body = new HashMap<>();
        body.put("transaction_amount", plano.getPreco());
        body.put("token", dto.getCardToken());
        body.put("description", plano.getDescricao() + " - Mensal");
        body.put("installments", 1);
        body.put("payment_method_id", paymentMethodId);
        body.put("payer", Map.of("email", dto.getEmail()));

        HttpHeaders headers = headersComToken(mpConfig.getAccessTokenAssinaturas());
        headers.set("X-Idempotency-Key", UUID.randomUUID().toString());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> data;
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(MP_API + "/v1/payments", request, Map.class);
            data = response.getBody();
        } catch (HttpClientErrorException ex) {
            try {
                data = objectMapper.readValue(ex.getResponseBodyAsString(), Map.class);
            } catch (Exception parseEx) {
                throw new RuntimeException("Erro ao processar resposta do Mercado Pago: " + ex.getResponseBodyAsString(), parseEx);
            }
        }

        if (data == null) throw new RuntimeException("Resposta vazia do Mercado Pago");

        Object statusObj       = data.get("status");
        Object statusDetailObj = data.get("status_detail");
        Object paymentId       = data.get("id");

        String status       = statusObj != null ? statusObj.toString() : null;
        String statusDetail = statusDetailObj != null ? statusDetailObj.toString() : null;
        boolean aprovado    = "approved".equals(status);

        if (aprovado) {
            estab.setPlano(plano);
            estab.setPlanoAtivo(true);
            estab.setPlanoValidoAte(LocalDate.now().plusMonths(1));
            estabelecimentoRepo.save(estab);
        }

        CobrancaPlano cobranca = new CobrancaPlano();
        cobranca.setEstabelecimento(estab);
        cobranca.setPlano(plano.name());
        cobranca.setTipoCobranca("AVULSO");
        cobranca.setMpPaymentId(String.valueOf(paymentId));
        cobranca.setValor(plano.getPreco());
        cobranca.setStatus(status != null ? status.toUpperCase() : "ERRO");
        cobranca.setPeriodoInicio(LocalDate.now());
        cobranca.setPeriodoFim(aprovado ? LocalDate.now().plusMonths(1) : null);
        cobrancaRepo.save(cobranca);

        Map<String, Object> result = new HashMap<>();
        result.put("paymentId",    paymentId);
        result.put("status",       status);
        result.put("statusDetail", statusDetail);
        result.put("plano",        plano.name());
        result.put("aprovado",     aprovado);
        return result;
    }

    // ─── 4. Cancelar assinatura ──────────────────────────────────────────────
    @Transactional
    public void cancelarAssinatura(Long estabelecimentoId) {
        Estabelecimento estab = estabelecimentoRepo.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        if (estab.getMpSubscriptionId() != null) {
            try {
                cancelarAssinaturaMP(estab.getMpSubscriptionId());
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(AssinaturaService.class)
                        .warn("[CANCELAR] Falha ao cancelar assinatura {} no MP (pode já estar cancelada): {}",
                                estab.getMpSubscriptionId(), e.getMessage());
            }
            estab.setMpSubscriptionId(null);
        }
        estab.setPlanoAtivo(false);
        estabelecimentoRepo.save(estab);
    }

    // ─── 5. Status do plano atual ────────────────────────────────────────────
    public Map<String, Object> statusPlano(Long estabelecimentoId) {
        Estabelecimento estab = estabelecimentoRepo.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        Map<String, Object> result = new HashMap<>();
        result.put("plano",          estab.getPlano() != null ? estab.getPlano().name() : null);
        result.put("ativo",          estab.isPlanoAtivo());
        result.put("validoAte",      estab.getPlanoValidoAte());
        result.put("subscriptionId", estab.getMpSubscriptionId());
        return result;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private void cancelarAssinaturaMP(String subscriptionId) {
        Map<String, String> body = Map.of("status", "cancelled");
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headersComToken(mpConfig.getAccessTokenAssinaturas()));
        restTemplate.exchange(MP_API + "/preapproval/" + subscriptionId, HttpMethod.PUT, request, Map.class);
    }

    private HttpHeaders headersComToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}