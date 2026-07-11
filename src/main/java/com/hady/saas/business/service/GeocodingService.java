package com.hady.saas.business.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeocodingService {

    @Value("${google.maps.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public double[] geocodificarEndereco(String endereco) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                    .queryParam("address", endereco)
                    .queryParam("key", apiKey)
                    .queryParam("region", "br")
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
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
            System.err.println("Erro ao geocodificar: " + e.getMessage());
        }
        return null;
    }
}