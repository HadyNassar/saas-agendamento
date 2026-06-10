package com.hady.saas.business.service;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.oauth.OAuthState;
import com.hady.saas.infrastructure.config.MercadoPagoConfiguration;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.repository.OAuthStateRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class MarketplaceOAuthService {

    private static final long STATE_TTL_SEGUNDOS = 600L;

    private final MercadoPagoConfiguration mpConfig;
    private final EstabelecimentoRepository estabelecimentoRepo;
    private final OAuthStateRepository oauthStateRepo;
    private final RestTemplate restTemplate;

    public MarketplaceOAuthService(MercadoPagoConfiguration mpConfig,
                                   EstabelecimentoRepository estabelecimentoRepo,
                                   OAuthStateRepository oauthStateRepo,
                                   RestTemplate restTemplate) {
        this.mpConfig = mpConfig;
        this.estabelecimentoRepo = estabelecimentoRepo;
        this.oauthStateRepo = oauthStateRepo;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public String gerarUrlAutorizacao(Long estabelecimentoId) {
        // Limpa states expirados
        oauthStateRepo.deleteExpired(Instant.now().minusSeconds(STATE_TTL_SEGUNDOS));

        String state = UUID.randomUUID().toString();
        oauthStateRepo.save(new OAuthState(state, estabelecimentoId, Instant.now()));

        return "https://auth.mercadopago.com/authorization"
                + "?client_id="    + mpConfig.getAppId()
                + "&response_type=code"
                + "&platform_id=mp"
                + "&redirect_uri=" + mpConfig.getRedirectUri()
                + "&state="        + state;
    }

    @Transactional
    public Long resolverState(String state) {
        OAuthState entry = oauthStateRepo.findById(state)
                .orElseThrow(() -> new RuntimeException(
                        "Link de autorização inválido ou já utilizado. Inicie o processo novamente."));

        oauthStateRepo.deleteById(state);

        if (entry.getCriadoEm().isBefore(Instant.now().minusSeconds(STATE_TTL_SEGUNDOS))) {
            throw new RuntimeException(
                    "Link de autorização expirado (válido por 10 minutos). Inicie o processo novamente.");
        }

        return entry.getEstabelecimentoId();
    }

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

    @Transactional
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