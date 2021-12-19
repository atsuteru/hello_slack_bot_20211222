package com.example.kami_teru.proxies.businesscard;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

public class BusinessCardGeneratorClient {
    private static String getServiceUrl() {
        String url = System.getenv("BUSINESS_CARD_GEN_URL");
        if (url == null || url.isEmpty()) {
            return "https://business-card-webservice.herokuapp.com";
        }
        return url;
    }

    private static final WebTarget BUSINESS_CARD_GENERATOR = ClientBuilder.newClient()
        .target(getServiceUrl())
        .path("/api/businesscard/generate/as/pdf");

    public static Response generate(String templateName, 
        String name, String role, String organization) {
        return BUSINESS_CARD_GENERATOR
            .queryParam("template", templateName)
            .queryParam("name", name)
            .queryParam("role", role)
            .queryParam("company", organization)
            .request()
            .get();
    }
}
