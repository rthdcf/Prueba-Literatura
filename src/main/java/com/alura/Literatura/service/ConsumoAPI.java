package com.alura.Literatura.service;

import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class ConsumoAPI {

    private final HttpClient client;

    public ConsumoAPI() {
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    public String obtenerDatos(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Java HttpClient")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();

            if (code != 200) {
                throw new RuntimeException("Error al obtener datos de la API. Código: " + code);
            }

            String body = response.body();
            if (body == null || body.isBlank()) {
                throw new RuntimeException("La API respondió sin contenido.");
            }
            return body;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("La solicitud fue interrumpida", e);
        } catch (IOException e) {
            throw new RuntimeException("Error de conexión con la API", e);
        }
    }
}