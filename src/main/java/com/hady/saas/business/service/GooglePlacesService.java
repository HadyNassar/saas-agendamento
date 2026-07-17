package com.hady.saas.business.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hady.saas.application.dto.SalaoExternoAdminDTO;
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

    // Cache da LISTA de salões válido por 60 horas (2,5 dias) — com 13
    // categorias buscadas por região (para cobrir todos os tipos de
    // estabelecimento), o cache mais longo compensa o aumento de chamadas
    // por busca, mantendo o custo total baixo sem perder atualidade real.
    private static final int CACHE_LISTA_HORAS = 60;

    // Cache do TELEFONE (Place Details) válido por 30 dias — telefone de
    // estabelecimento praticamente nunca muda. Uma vez buscado, fica
    // disponível pra qualquer prospecção futura sem custo adicional.
    private static final int CACHE_TELEFONE_DIAS = 30;

    public GooglePlacesService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Busca salões de estética/beleza próximos a um ponto.
    // Usa cache de 24h em banco antes de consultar a API paga do Google.
    @Transactional
    public List<SalaoExternoAdminDTO> buscarSaloesProximos(double lat, double lng, double raioKm) {
        String chaveBusca = "lista_" + gerarChaveBusca(lat, lng, raioKm);

        String cacheado = buscarDoCache(chaveBusca, CACHE_LISTA_HORAS);
        if (cacheado != null) {
            return desserializar(cacheado);
        }

        List<SalaoExternoAdminDTO> resultado = buscarNaApiGoogle(lat, lng, raioKm);
        salvarNoCache(chaveBusca, objetoParaJson(resultado));
        return resultado;
    }

    // Busca o TELEFONE de um salão específico (Place Details), usado quando
    // o usuário clica em um pin cinza pra prospectar. Cache de 30 dias —
    // uma vez buscado, não cobra de novo pro mesmo salão por um mês inteiro.
    // GET chamado sob demanda, nunca em lote (mantém custo baixo).
    @Transactional
    public String buscarTelefone(String googlePlaceId) {
        String chaveBusca = "telefone_" + googlePlaceId;

        String cacheado = buscarDoCache(chaveBusca, CACHE_TELEFONE_DIAS * 24);
        if (cacheado != null) {
            return cacheado.equals("null") ? null : cacheado;
        }

        String telefone = buscarTelefoneNaApiGoogle(googlePlaceId);
        salvarNoCache(chaveBusca, telefone != null ? telefone : "null");
        return telefone;
    }

    private String buscarTelefoneNaApiGoogle(String googlePlaceId) {
        try {
            String url = "https://maps.googleapis.com/maps/api/place/details/json"
                    + "?place_id=" + googlePlaceId
                    + "&fields=formatted_phone_number,international_phone_number"
                    + "&key=" + apiKey;

            String response = restTemplate.getForObject(URI.create(url), String.class);
            JsonNode root = objectMapper.readTree(response);

            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                System.err.println("Google Place Details retornou status: " + status);
                return null;
            }

            JsonNode result = root.path("result");
            if (result.has("formatted_phone_number")) {
                return result.path("formatted_phone_number").asText();
            }
            if (result.has("international_phone_number")) {
                return result.path("international_phone_number").asText();
            }
            return null;
        } catch (Exception e) {
            System.err.println("Erro ao buscar telefone no Google Place Details: " + e.getMessage());
            return null;
        }
    }

    // Arredonda lat/lng para não gerar uma chave de cache diferente a cada
    // pixel de movimento do mapa — duas buscas próximas reaproveitam o cache.
    private String gerarChaveBusca(double lat, double lng, double raioKm) {
        double latArredondada = Math.round(lat * 100.0) / 100.0;
        double lngArredondada = Math.round(lng * 100.0) / 100.0;
        return String.format(Locale.US, "%.2f_%.2f_%.0f", latArredondada, lngArredondada, raioKm);
    }

    private String buscarDoCache(String chave, int validoHoras) {
        try {
            List<String> resultado = jdbcTemplate.query(
                    "SELECT resultado_json FROM places_cache WHERE chave_busca = ? AND criado_em > NOW() - INTERVAL '" + validoHoras + " hours'",
                    (rs, rowNum) -> rs.getString("resultado_json"),
                    chave
            );
            return resultado.isEmpty() ? null : resultado.get(0);
        } catch (Exception e) {
            System.err.println("Erro ao ler cache do Places: " + e.getMessage());
            return null;
        }
    }

    private void salvarNoCache(String chave, String valor) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO places_cache (chave_busca, resultado_json, criado_em) VALUES (?, ?, NOW()) " +
                            "ON CONFLICT (chave_busca) DO UPDATE SET resultado_json = EXCLUDED.resultado_json, criado_em = NOW()",
                    chave, valor
            );
        } catch (Exception e) {
            System.err.println("Erro ao salvar cache do Places: " + e.getMessage());
        }
    }

    private String objetoParaJson(List<SalaoExternoAdminDTO> lista) {
        try {
            return objectMapper.writeValueAsString(lista);
        } catch (Exception e) {
            System.err.println("Erro ao serializar lista: " + e.getMessage());
            return "[]";
        }
    }

    private List<SalaoExternoAdminDTO> desserializar(String json) {
        try {
            return List.of(objectMapper.readValue(json, SalaoExternoAdminDTO[].class));
        } catch (Exception e) {
            System.err.println("Erro ao desserializar cache: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Keywords de busca por categoria — cobre as 14 categorias do
    // TipoEstabelecimento. Usamos keyword textual (não o parâmetro "type" do
    // Google, que só cobre poucos tipos oficiais) para pegar salão, spa,
    // studio de pilates, nutricionista etc, todos na mesma varredura.
    private static final String[] KEYWORDS_BUSCA = {
            "salão de beleza",
            "barbearia",
            "clínica de estética",
            "studio de manicure",
            "spa estética",
            "studio de sobrancelha",
            "massoterapia",
            "podologia",
            "nutricionista",
            "personal trainer",
            "studio de pilates",
            "academia de lutas",
            "yoga meditação"
    };

    private List<SalaoExternoAdminDTO> buscarNaApiGoogle(double lat, double lng, double raioKm) {
        // Usa um Map por place_id pra evitar duplicar o mesmo estabelecimento
        // caso ele apareça em mais de uma busca por keyword (ex: um spa que
        // também aparece em "clínica de estética").
        java.util.Map<String, SalaoExternoAdminDTO> resultadosPorId = new java.util.LinkedHashMap<>();
        int raioMetros = (int) (raioKm * 1000);

        for (String termoBusca : KEYWORDS_BUSCA) {
            try {
                buscarPorKeyword(lat, lng, raioMetros, termoBusca, resultadosPorId);
            } catch (Exception e) {
                System.err.println("Erro ao buscar '" + termoBusca + "' no Google Places: " + e.getMessage());
            }
        }

        return new ArrayList<>(resultadosPorId.values());
    }

    // Busca 1 página (20 resultados) por keyword — sem paginação profunda
    // aqui, porque já rodamos 13 keywords diferentes; multiplicar por 3
    // páginas cada deixaria o custo alto demais. 1 página por categoria
    // já cobre bem a maioria das cidades médias.
    private void buscarPorKeyword(double lat, double lng, int raioMetros, String termoBusca,
                                  java.util.Map<String, SalaoExternoAdminDTO> resultadosPorId) throws Exception {
        String keyword = URLEncoder.encode(termoBusca, StandardCharsets.UTF_8);

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                + "?location=" + lat + "," + lng
                + "&radius=" + raioMetros
                + "&keyword=" + keyword
                + "&key=" + apiKey;

        String response = restTemplate.getForObject(URI.create(url), String.class);
        JsonNode root = objectMapper.readTree(response);

        String status = root.path("status").asText();
        if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
            System.err.println("Google Places retornou status '" + status + "' para keyword '" + termoBusca + "'");
            return;
        }

        for (JsonNode place : root.path("results")) {
            String placeId = place.path("place_id").asText();
            if (resultadosPorId.containsKey(placeId)) continue; // já visto em outra keyword

            String nome = place.path("name").asText();
            String endereco = place.path("vicinity").asText();
            double placeLat = place.path("geometry").path("location").path("lat").asDouble();
            double placeLng = place.path("geometry").path("location").path("lng").asDouble();
            Double avaliacao = place.has("rating") ? place.path("rating").asDouble() : null;

            resultadosPorId.put(placeId,
                    new SalaoExternoAdminDTO(placeId, nome, endereco, placeLat, placeLng, avaliacao, null, termoBusca));
        }
    }
}