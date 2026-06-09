package com.hady.saas.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Rotas públicas que queremos proteger
    private static final String[] ROTAS_LIMITADAS = {
            "/app/",
            "/auth/login",
            "/auth/registro",
            "/auth/esqueci-senha"   // proteção contra spam de e-mail e enumeração de usuários
    };

    // Limite: 60 requisições por minuto por IP
    private static final int LIMITE = 60;
    private static final long JANELA_MS = 60_000L;

    // ip -> [contador, timestamp inicio da janela]
    private final Map<String, long[]> contadores = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Só aplica nas rotas públicas sensíveis
        boolean rotaLimitada = false;
        for (String rota : ROTAS_LIMITADAS) {
            if (uri.startsWith(rota)) { rotaLimitada = true; break; }
        }

        if (!rotaLimitada) {
            chain.doFilter(request, response);
            return;
        }

        String ip = getIp(request);
        long agora = System.currentTimeMillis();

        contadores.compute(ip, (k, v) -> {
            if (v == null || agora - v[1] > JANELA_MS) {
                return new long[]{1, agora}; // nova janela
            }
            v[0]++;
            return v;
        });

        long[] estado = contadores.get(ip);
        long restante = JANELA_MS - (agora - estado[1]);

        response.setHeader("X-RateLimit-Limit", String.valueOf(LIMITE));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, LIMITE - estado[0])));
        response.setHeader("X-RateLimit-Reset", String.valueOf(restante / 1000));

        if (estado[0] > LIMITE) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Muitas requisições. Tente novamente em instantes.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    // IPs de proxies/load-balancers confiáveis (ex: Render, Railway, Heroku).
    // Só aceitamos X-Forwarded-For quando a requisição vem de um desses IPs,
    // impedindo que clientes externos forjem o cabeçalho para burlar o rate limit.
    // Adicione ao application.properties:
    //   rate-limit.trusted-proxies=10.0.0.0/8,172.16.0.0/12
    // Deixe vazio para ignorar X-Forwarded-For completamente (ambientes sem proxy).
    @org.springframework.beans.factory.annotation.Value("${rate-limit.trusted-proxies:}")
    private String trustedProxiesRaw;

    private String getIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        // Só confia em X-Forwarded-For se vier de um proxy configurado como confiável
        if (trustedProxiesRaw != null && !trustedProxiesRaw.isBlank()) {
            boolean isTrustedProxy = java.util.Arrays.stream(trustedProxiesRaw.split(","))
                    .map(String::trim)
                    .anyMatch(remoteAddr::startsWith);

            if (isTrustedProxy) {
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
            }
        }

        return remoteAddr;
    }
    // Limpeza delegada para RateLimitCleanupTask (@Scheduled a cada 5 min).
    // O método permanece aqui para ser chamado por ela sem expor o Map diretamente
    // em mais de um lugar.
    public void limparContadoresExpirados() {
        long agora = System.currentTimeMillis();
        contadores.entrySet().removeIf(entry -> agora - entry.getValue()[1] > JANELA_MS);
    }
}