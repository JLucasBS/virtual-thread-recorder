package dev.jlucasbs.gravador.service;

import dev.jlucasbs.gravador.config.AppConfig;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class NetworkService {
    private final AppConfig config;
    private final HttpClient httpClient;

    public NetworkService(AppConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean uploadFile(File file) {
        if (!file.exists()) return false;

        System.out.println("🚀 Iniciando upload: " + file.getName());
        String signedUrl = getSignedUrl(file.getName());

        if (signedUrl == null) {
            System.err.println("❌ Falha ao obter URL para: " + file.getName());
            return false;
        }

        return sendToS3(signedUrl, file);
    }

    private String getSignedUrl(String filename) {
        try {
            String endpoint = config.backendUrl() + "?filename=" + filename;
            var request = HttpRequest.newBuilder().uri(URI.create(endpoint)).GET().build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body().replaceAll("\"", "");
            }
        } catch (Exception e) {
            // Log silencioso em produção
        }
        return null;
    }

    private boolean sendToS3(String signedUrl, File file) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(signedUrl))
                    .header("Content-Type", "audio/wav")
                    .PUT(HttpRequest.BodyPublishers.ofFile(file.toPath()))
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }
}