package com.hady.saas.business.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hady.saas.application.dto.PagamentoAgendamentoDTO;
import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.transacao.Transacao;
import com.hady.saas.infrastructure.config.MercadoPagoConfiguration;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import com.hady.saas.infrastructure.repository.TransacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class PagamentoSplitService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoSplitService.class);

    // Comissão da plataforma Agendara: 1,5%
    private static final BigDecimal TAXA_PLATAFORMA = new BigDecimal("0.015");
    private static final String MP_API = "https://api.mercadopago.com";

    private final MercadoPagoConfiguration mpConfig;
    private final AgendamentoRepository agendamentoRepo;
    private final TransacaoRepository transacaoRepo;
    private final com.hady.saas.infrastructure.repository.EstabelecimentoRepository estabelecimentoRepo;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PagamentoSplitService(MercadoPagoConfiguration mpConfig,
                                 AgendamentoRepository agendamentoRepo,
                                 TransacaoRepository transacaoRepo,
                                 com.hady.saas.infrastructure.repository.EstabelecimentoRepository estabelecimentoRepo,
                                 RestTemplate restTemplate) {
        this.mpConfig = mpConfig;
        this.agendamentoRepo = agendamentoRepo;
        this.transacaoRepo = transacaoRepo;
        this.estabelecimentoRepo = estabelecimentoRepo;
        this.restTemplate = restTemplate;
    }

    // ─── Processar pagamento com split 1,5% ──────────────────────────────────
    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> processarPagamento(String slug, PagamentoAgendamentoDTO dto) {

        Agendamento agendamento = agendamentoRepo.findByIdWithDetails(dto.getAgendamentoId())
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        Estabelecimento estab = agendamento.getEstabelecimento();

        if (!estab.isAceitaPagamento() || !estab.isMpConectado()) {
            throw new RuntimeException("Este estabelecimento não aceita pagamento online.");
        }

        // Calcula o valor a cobrar: sinal (fixo ou %) ou valor total do serviço.
        //
        // CORREÇÃO: se um CupomEstabelecimento foi aplicado a este agendamento
        // (seção 5-B / item 8), a BASE de cálculo do sinal precisa ser o preço
        // JÁ COM DESCONTO, não o preço cheio do serviço — senão o cliente
        // pagaria sinal sobre um valor que ele nunca vai pagar de verdade.
        // agendamento.getValorDescontoAplicado() é gravado por
        // CupomEstabelecimentoService.aplicarNoAgendamento() no momento em
        // que o cupom é consumido (POST /agendar), então já está disponível
        // aqui quando o pagamento do sinal é processado depois.
        BigDecimal precoServico = agendamento.getServico().getPreco();
        if (agendamento.getValorDescontoAplicado() != null) {
            precoServico = precoServico.subtract(agendamento.getValorDescontoAplicado()).max(BigDecimal.ZERO);
        }
        BigDecimal valorTotal;

        if (estab.isExigirSinal() && estab.getValorSinal() != null) {
            if ("percentual".equals(estab.getTipoSinal())) {
                // Sinal percentual: ex. 50% de R$50 = R$25
                valorTotal = precoServico
                        .multiply(estab.getValorSinal())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            } else {
                // Sinal fixo: ex. R$1,00
                valorTotal = estab.getValorSinal().setScale(2, RoundingMode.HALF_UP);
            }
            // Garante que o sinal não ultrapasse o preço do serviço
            if (valorTotal.compareTo(precoServico) > 0) {
                valorTotal = precoServico;
            }
        } else {
            valorTotal = precoServico;
        }

        BigDecimal taxa                 = valorTotal.multiply(TAXA_PLATAFORMA).setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorEstabelecimento = valorTotal.subtract(taxa);

        log.info("[PagamentoSplit] Valor a cobrar: {} (sinal={} tipo={} valorSinal={})",
                valorTotal, estab.isExigirSinal(), estab.getTipoSinal(), estab.getValorSinal());

        // Monta o body do pagamento
        Map<String, Object> body = new HashMap<>();
        body.put("transaction_amount", valorTotal);
        body.put("description", "Agendamento: " + agendamento.getServico().getNome());
        body.put("payer", Map.of("email", dto.getEmailPagador()));
        body.put("application_fee", taxa); // ← 1,5% vai para sua conta Agendara

        if ("pix".equals(dto.getMetodoPagamento())) {
            body.put("payment_method_id", "pix");
        } else {
            body.put("payment_method_id", dto.getMetodoPagamento());
            body.put("token", dto.getCardToken());
            body.put("installments", dto.getParcelas());
        }

        // Usa o token OAuth do ESTABELECIMENTO — dinheiro vai para a conta dele
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(estab.getMpAccessToken());
        headers.set("X-Idempotency-Key", java.util.UUID.randomUUID().toString());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        // ─── Captura a resposta mesmo quando o MP retorna 4xx ────────────────
        Map<String, Object> data;
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    MP_API + "/v1/payments", request, Map.class);
            data = response.getBody();
        } catch (HttpClientErrorException ex) {
            try {
                data = objectMapper.readValue(ex.getResponseBodyAsString(), Map.class);
                log.warn("[PagamentoSplit] MP retornou {} — body: {}",
                        ex.getStatusCode(), ex.getResponseBodyAsString());
            } catch (Exception parseEx) {
                throw new RuntimeException(
                        "Erro ao processar resposta do Mercado Pago: " + ex.getResponseBodyAsString(), parseEx);
            }
        }

        if (data == null) throw new RuntimeException("Resposta vazia do Mercado Pago");

        Object statusObj = data.get("status");
        Object paymentId = data.get("id");
        String status    = statusObj != null ? statusObj.toString() : null;

        // Persiste a transação (independentemente do status)
        Transacao transacao = new Transacao();
        transacao.setEstabelecimento(estab);
        transacao.setAgendamento(agendamento);
        transacao.setMpPaymentId(String.valueOf(paymentId));
        transacao.setValorTotal(valorTotal);
        transacao.setTaxaPlataforma(taxa);
        transacao.setValorEstabelecimento(valorEstabelecimento);
        transacao.setMetodoPagamento(dto.getMetodoPagamento());
        transacao.setStatus(mapearStatus(status));

        if ("pix".equals(dto.getMetodoPagamento())) {
            try {
                Map<String, Object> poi = (Map<String, Object>) data.get("point_of_interaction");
                if (poi != null) {
                    Map<String, Object> txData = (Map<String, Object>) poi.get("transaction_data");
                    if (txData != null) {
                        transacao.setPixQrCode((String) txData.get("qr_code"));
                        transacao.setPixQrCodeBase64((String) txData.get("qr_code_base64"));
                    }
                }
            } catch (Exception ignored) {}
        }

        transacaoRepo.save(transacao);
        log.info("[PagamentoSplit] Transação salva — id={} status={} valor={} taxa={}",
                transacao.getId(), transacao.getStatus(), valorTotal, taxa);

        Map<String, Object> result = new HashMap<>();
        result.put("transacaoId", transacao.getId());
        result.put("mpPaymentId", paymentId);
        result.put("status", status);
        result.put("valorTotal", valorTotal);
        result.put("taxaPlataforma", taxa);
        result.put("metodoPagamento", dto.getMetodoPagamento());

        if ("pix".equals(dto.getMetodoPagamento())) {
            result.put("pixQrCode", transacao.getPixQrCode());
            result.put("pixQrCodeBase64", transacao.getPixQrCodeBase64());
        }

        return result;
    }

    @Transactional
    public void toggleAceitaPagamento(Long estabelecimentoId, boolean ativo) {
        Estabelecimento estabelecimento = estabelecimentoRepo.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        if (ativo && !estabelecimento.isMpConectado()) {
            throw new RuntimeException("Conecte sua conta do Mercado Pago antes de ativar o pagamento.");
        }

        estabelecimento.setAceitaPagamento(ativo);
        estabelecimentoRepo.save(estabelecimento);
    }

    // ─── Consultar status de uma transação (polling do pix) ──────────────────
    // CORRIGIDO: recebe slug para validar que a transação pertence ao estabelecimento
    // da URL, impedindo IDOR entre tenants.
    // FIX: @Transactional adicionado — salva o status atualizado de forma segura.
    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> consultarTransacao(Long transacaoId, String slug) {

        Transacao transacao = transacaoRepo.findById(transacaoId)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));

        // Valida isolamento de tenant: a transação deve pertencer ao estabelecimento do slug.
        if (!transacao.getEstabelecimento().getSlug().equals(slug)) {
            throw new RuntimeException("Transação não pertence a este estabelecimento");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(transacao.getEstabelecimento().getMpAccessToken());
        headers.set("X-Idempotency-Key", java.util.UUID.randomUUID().toString());
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                MP_API + "/v1/payments/" + transacao.getMpPaymentId(),
                HttpMethod.GET, request, Map.class);

        Map<String, Object> data = response.getBody();
        String mpStatus   = data != null ? (String) data.get("status") : "pending";
        String novoStatus = mapearStatus(mpStatus);

        transacao.setStatus(novoStatus);
        transacaoRepo.save(transacao);

        Map<String, Object> result = new HashMap<>();
        result.put("transacaoId", transacao.getId());
        result.put("status", novoStatus);
        result.put("mpStatus", mpStatus);
        result.put("pixQrCode", transacao.getPixQrCode());
        result.put("pixQrCodeBase64", transacao.getPixQrCodeBase64());
        return result;
    }

    private String mapearStatus(String mpStatus) {
        if (mpStatus == null) return "PENDENTE";
        return switch (mpStatus) {
            case "approved"              -> "APROVADO";
            case "pending", "in_process" -> "PENDENTE";
            case "rejected"              -> "REJEITADO";
            case "refunded"              -> "REEMBOLSADO";
            case "cancelled"             -> "CANCELADO";
            default                      -> mpStatus.toUpperCase();
        };
    }
}