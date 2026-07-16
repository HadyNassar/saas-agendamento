package com.hady.saas.business.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class GeocodingService {

    @Value("${google.maps.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public double[] geocodificarEndereco(String endereco) {
        try {
            String enderecoCodificado = URLEncoder.encode(endereco, StandardCharsets.UTF_8);

            String url = "https://maps.googleapis.com/maps/api/geocode/json"
                    + "?address=" + enderecoCodificado
                    + "&region=br"
                    + "&key=" + apiKey;

            String response = restTemplate.getForObject(URI.create(url), String.class);
            JsonNode root = objectMapper.readTree(response);

            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                System.err.println("Geocoding retornou status: " + status + " para endereço: " + endereco);
                return null;
            }

            JsonNode results = root.path("results");
            if (!results.isEmpty()) {
                JsonNode location = results.get(0)
                        .path("geometry")
                        .path("location");

                double lat = location.path("lat").asDouble();
                double lng = location.path("lng").asDouble();
                return new double[]{lat, lng};
            }
        } catch (Exception e) {
            System.err.println("Erro ao geocodificar '" + endereco + "': " + e.getMessage());
        }
        return null;
    }
}