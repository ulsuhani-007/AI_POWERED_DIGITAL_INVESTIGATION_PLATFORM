package com.investigation.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.investigation.config.AppConfig;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Centralized REST API Client utilizing Java 21 HttpClient and Jackson.
 */
public class ApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private String getBaseUrl() {
        String url = AppConfig.getInstance().getBackendBaseUrl();
        if (url == null || url.isBlank()) {
            url = "http://localhost:8080";
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    public boolean testConnection() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/api/cases"))
                    .timeout(Duration.ofSeconds(3))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            boolean success = response.statusCode() >= 200 && response.statusCode() < 500;
            AppConfig.getInstance().setBackendConnected(success);
            return success;
        } catch (Exception e) {
            AppConfig.getInstance().setBackendConnected(false);
            return false;
        }
    }

    public <T> T get(String path, Class<T> responseClass) throws IOException, InterruptedException {
        String uriStr = getBaseUrl() + (path.startsWith("/") ? path : "/" + path);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uriStr))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), responseClass);
        } else {
            throw new IOException("HTTP " + response.statusCode() + " on GET " + uriStr + ": " + response.body());
        }
    }

    public <T> List<T> getList(String path, Class<T> elementClass) throws IOException, InterruptedException {
        String uriStr = getBaseUrl() + (path.startsWith("/") ? path : "/" + path);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uriStr))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementClass);
            return objectMapper.readValue(response.body(), listType);
        } else if (response.statusCode() == 404) {
            return Collections.emptyList();
        } else {
            throw new IOException("HTTP " + response.statusCode() + " on GET " + uriStr + ": " + response.body());
        }
    }

    public <T> T post(String path, Object body, Class<T> responseClass) throws IOException, InterruptedException {
        String uriStr = getBaseUrl() + (path.startsWith("/") ? path : "/" + path);
        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uriStr))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            if (responseClass == Void.class || response.body().isBlank()) {
                return null;
            }
            return objectMapper.readValue(response.body(), responseClass);
        } else {
            throw new IOException("HTTP " + response.statusCode() + " on POST " + uriStr + ": " + response.body());
        }
    }

    public <T> T put(String path, Object body, Class<T> responseClass) throws IOException, InterruptedException {
        String uriStr = getBaseUrl() + (path.startsWith("/") ? path : "/" + path);
        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uriStr))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            if (responseClass == Void.class || response.body().isBlank()) {
                return null;
            }
            return objectMapper.readValue(response.body(), responseClass);
        } else {
            throw new IOException("HTTP " + response.statusCode() + " on PUT " + uriStr + ": " + response.body());
        }
    }

    public boolean delete(String path) throws IOException, InterruptedException {
        String uriStr = getBaseUrl() + (path.startsWith("/") ? path : "/" + path);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uriStr))
                .timeout(Duration.ofSeconds(10))
                .DELETE()
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    /**
     * Uploads evidence file using multipart/form-data.
     */
    public <T> T uploadMultipart(String path, File file, String description, Class<T> responseClass, Consumer<Double> progress) throws IOException, InterruptedException {
        String boundary = "---AegisBoundary" + UUID.randomUUID().toString().replace("-", "");
        String uriStr = getBaseUrl() + (path.startsWith("/") ? path : "/" + path);

        byte[] fileBytes = Files.readAllBytes(file.toPath());
        String lineBreak = "\r\n";

        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("--").append(boundary).append(lineBreak);
        headerBuilder.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getName()).append("\"").append(lineBreak);
        headerBuilder.append("Content-Type: text/csv").append(lineBreak).append(lineBreak);

        StringBuilder descBuilder = new StringBuilder();
        if (description != null && !description.isBlank()) {
            descBuilder.append(lineBreak).append("--").append(boundary).append(lineBreak);
            descBuilder.append("Content-Disposition: form-data; name=\"description\"").append(lineBreak).append(lineBreak);
            descBuilder.append(description);
        }

        String closingBoundary = lineBreak + "--" + boundary + "--" + lineBreak;

        byte[] headerBytes = headerBuilder.toString().getBytes();
        byte[] descBytes = descBuilder.toString().getBytes();
        byte[] closingBytes = closingBoundary.getBytes();

        byte[] totalPayload = new byte[headerBytes.length + fileBytes.length + descBytes.length + closingBytes.length];
        System.arraycopy(headerBytes, 0, totalPayload, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, totalPayload, headerBytes.length, fileBytes.length);
        System.arraycopy(descBytes, 0, totalPayload, headerBytes.length + fileBytes.length, descBytes.length);
        System.arraycopy(closingBytes, 0, totalPayload, headerBytes.length + fileBytes.length + descBytes.length, closingBytes.length);

        if (progress != null) progress.accept(0.5);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uriStr))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(totalPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (progress != null) progress.accept(1.0);

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), responseClass);
        } else {
            throw new IOException("HTTP " + response.statusCode() + " on file upload: " + response.body());
        }
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
