package com.hady.saas.business.service;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.config.MercadoPagoConfiguration;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketplaceOAuthService {

    private final MercadoPagoConfiguration mpConfig;
    private final EstabelecimentoRepository estabelecimentoRepo;
    // FIX: RestTemplate injetado via construtor para reutilizar o pool de conexões
    // e os timeouts configurados em RestTemplateConfig (5s connect / 15s read).
    // Antes, processarCallback() criava new RestTemplate() por chamada, ignorando
    // as configurações e podendo causar hang indefinido em falhas do MP.
    private final RestTemplate restTemplate;

    // ─── Cache de state OAuth ────────────────────────────────────────────────
    // Mapeia UUID opaco → (estabelecimentoId, instante de criação).
    // TTL: 10 minutos. Entradas expiradas são limpas na próxima chamada a
    // gerarUrlAutorizacao() e também ao resolver o state.
    //
    // ⚠️  ATENÇÃO — REQUISITO DE DEPLOY: este cache é in-memory.
    // Em produção com mais de uma instância (Render, Railway, ECS etc.), o callback
    // do MP pode chegar numa instância diferente da que gerou o state, resultando em
    // "Link de autorização inválido". O deploy DEVE ser single-instance enquanto
    // este cache existir.
    //
    // Para escalar horizontalmente, substitua por Redis:
    //   redisTemplate.opsForValue().set("oauth:state:" + state, estabelecimentoId, 10, MINUTES)
    private static final long STATE_TTL_SEGUNDOS = 600L;
    private record OAuthState(Long estabelecimentoId, Instant criadoEm) {}
    private final ConcurrentHashMap<String, OAuthState> stateCache = new ConcurrentHashMap<>();

    public MarketplaceOAuthService(MercadoPagoConfiguration mpConfig,
                                   EstabelecimentoRepository estabelecimentoRepo,
                                   RestTemplate restTemplate) {
        this.mpConfig = mpConfig;
        this.estabelecimentoRepo = estabelecimentoRepo;
        this.restTemplate = restTemplate;
    }

    // ─── URL de autorização — redireciona o estabelecimento ao MP ────────────
    // CORRIGIDO: state é agora um UUID opaco, não o estabelecimentoId diretamente.
    // Isso impede que um atacante forje o parâmetro state para conectar sua conta
    // MP ao estabelecimento de outra pessoa (CSRF no OAuth callback).
    public String gerarUrlAutorizacao(Long estabelecimentoId) {
        // Limpa entradas expiradas a cada nova geração
        Instant limite = Instant.now().minusSeconds(STATE_TTL_SEGUNDOS);
        stateCache.entrySet().removeIf(e -> e.getValue().criadoEm().isBefore(limite));

        String state = UUID.randomUUID().toString();
        stateCache.put(state, new OAuthState(estabelecimentoId, Instant.now()));

        return "https://auth.mercadopago.com/authorization"
                + "?client_id="     + mpConfig.getAppId()
                + "&response_type=code"
                + "&platform_id=mp"
                + "&redirect_uri="  + mpConfig.getRedirectUri()
                + "&state="         + state;
    }

    // ─── Resolve o state opaco para estabelecimentoId ─────────────────────────
    // Remove a entrada do cache (one-time use) e valida o TTL.
    // Lança RuntimeException se o state for inválido, já usado ou expirado.
    public Long resolverState(String state) {
        OAuthState entry = stateCache.remove(state);
        if (entry == null) {
            throw new RuntimeException(
                    "Link de autorização inválido ou já utilizado. Inicie o processo novamente.");
        }
        if (entry.criadoEm().isBefore(Instant.now().minusSeconds(STATE_TTL_SEGUNDOS))) {
            throw new RuntimeException(
                    "Link de autorização expirado (válido por 10 minutos). Inicie o processo novamente.");
        }
        return entry.estabelecimentoId();
    }

    // ─── Callback: troca o code pelo access_token do estabelecimento ─────────
    @Transactional
    @SuppressWarnings("unchecked")
    public void processarCallback(String code, Long estabelecimentoId) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_secret", mpConfig.getClientSecret());
        body.add("client_id",     mpConfig.getAppId());
        body.add("grant_type",    "authorization_code");
        body.add("code",          code);
        body.add("redirect_uri",  mpConfig.getRedirectUri());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.mercadopago.com/oauth/token",
                request,
                Map.class
        );

        Map<String, Object> data = response.getBody();
        if (data == null) throw new RuntimeException("Resposta vazia do Mercado Pago OAuth");

        String accessToken = (String) data.get("access_token");
        String userId      = String.valueOf(data.get("user_id"));

        Estabelecimento estab = estabelecimentoRepo.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        estab.setMpAccessToken(accessToken);
        estab.setMpUserId(userId);
        estab.setMpConectado(true);
        estabelecimentoRepo.save(estab);
    }

    // ─── Desconectar conta MP ────────────────────────────────────────────────
    public void desconectar(Long estabelecimentoId) {
        Estabelecimento estab = estabelecimentoRepo.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        estab.setMpAccessToken(null);
        estab.setMpUserId(null);
        estab.setMpConectado(false);
        estab.setAceitaPagamento(false);
        estabelecimentoRepo.save(estab);
    }
}