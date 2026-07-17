package com.hady.saas.business.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hady.saas.application.dto.SalaoExternoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class GooglePlacesService {

    @Value("${google.maps.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JdbcTemplate jdbcTemplate;

    // Cache válido por 6 horas — reduz drasticamente o custo da API.
    // A mesma região buscada várias vezes no dia usa o resultado salvo,
    // em vez de chamar o Google de novo a cada acesso do usuário.
    private static final int CACHE_HORAS = 6;

    public GooglePlacesService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Busca salões de estética/beleza próximos a um ponto.
    // Usa cache em banco antes de consultar a API paga do Google.
    @Transactional
    public List<SalaoExternoDTO> buscarSaloesProximos(double lat, double lng, double raioKm) {
        String chaveBusca = gerarChaveBusca(lat, lng, raioKm);

        String cacheado = buscarDoCache(chaveBusca);
        if (cacheado != null) {
            return desserializar(cacheado);
        }

        List<SalaoExternoDTO> resultado = buscarNaApiGoogle(lat, lng, raioKm);
        salvarNoCache(chaveBusca, resultado);
        return resultado;
    }

    // Arredonda lat/lng para não gerar uma chave de cache diferente a cada
    // pixel de movimento do mapa — duas buscas próximas reaproveitam o cache.
    private String gerarChaveBusca(double lat, double lng, double raioKm) {
        double latArredondada = Math.round(lat * 100.0) / 100.0;
        double lngArredondada = Math.round(lng * 100.0) / 100.0;
        return String.format(Locale.US, "%.2f_%.2f_%.0f", latArredondada, lngArredondada, raioKm);
    }

    private String buscarDoCache(String chave) {
        try {
            List<String> resultado = jdbcTemplate.query(
                    "SELECT resultado_json FROM places_cache WHERE chave_busca = ? AND criado_em > NOW() - INTERVAL '" + CACHE_HORAS + " hours'",
                    (rs, rowNum) -> rs.getString("resultado_json"),
                    chave
            );
            return resultado.isEmpty() ? null : resultado.get(0);
        } catch (Exception e) {
            System.err.println("Erro ao ler cache do Places: " + e.getMessage());
            return null;
        }
    }

    private void salvarNoCache(String chave, List<SalaoExternoDTO> resultado) {
        try {
            String json = objectMapper.writeValueAsString(resultado);
            jdbcTemplate.update(
                    "INSERT INTO places_cache (chave_busca, resultado_json, criado_em) VALUES (?, ?, NOW()) " +
                            "ON CONFLICT (chave_busca) DO UPDATE SET resultado_json = EXCLUDED.resultado_json, criado_em = NOW()",
                    chave, json
            );
        } catch (Exception e) {
            System.err.println("Erro ao salvar cache do Places: " + e.getMessage());
        }
    }

    private List<SalaoExternoDTO> desserializar(String json) {
        try {
            return List.of(objectMapper.readValue(json, SalaoExternoDTO[].class));
        } catch (Exception e) {
            System.err.println("Erro ao desserializar cache: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<SalaoExternoDTO> buscarNaApiGoogle(double lat, double lng, double raioKm) {
        List<SalaoExternoDTO> resultados = new ArrayList<>();
        try {
            int raioMetros = (int) (raioKm * 1000);
            String keyword = URLEncoder.encode("salão de beleza estética", StandardCharsets.UTF_8);

            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                    + "?location=" + lat + "," + lng
                    + "&radius=" + raioMetros
                    + "&keyword=" + keyword
                    + "&type=beauty_salon"
                    + "&key=" + apiKey;

            String response = restTemplate.getForObject(URI.create(url), String.class);
            JsonNode root = objectMapper.readTree(response);

            String status = root.path("status").asText();
            if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                System.err.println("Google Places retornou status: " + status);
                return resultados;
            }

            for (JsonNode place : root.path("results")) {
                String placeId = place.path("place_id").asText();
                String nome = place.path("name").asText();
                String endereco = place.path("vicinity").asText();
                double placeLat = place.path("geometry").path("location").path("lat").asDouble();
                double placeLng = place.path("geometry").path("location").path("lng").asDouble();
                Double avaliacao = place.has("rating") ? place.path("rating").asDouble() : null;

                resultados.add(new SalaoExternoDTO(placeId, nome, endereco, placeLat, placeLng, avaliacao, null));
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar no Google Places: " + e.getMessage());
        }
        return resultados;
    }
}