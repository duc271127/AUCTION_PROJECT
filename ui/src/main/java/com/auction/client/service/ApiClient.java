package com.auction.client.service;

import com.auction.client.exception.ApiException;
import com.auction.client.session.SessionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

public class ApiClient {

    // URL SERVER PLAYIT
    private static final String BASE_URL =
            "http://lungs-decree.with.playit.plus:1125";

    private final HttpClient httpClient;
    private final Gson gson;

    public ApiClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public String get(String endpoint) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + endpoint))
                    .header("Content-Type", "application/json")
                    .GET();

            addAuthorizationHeader(builder);

            HttpResponse<String> response =
                    httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }

            throw new ApiException(
                    "GET failed: " +
                            response.statusCode() +
                            " - " +
                            response.body()
            );

        } catch (IOException | InterruptedException e) {
            throw new ApiException(
                    "Cannot connect to server: " + e.getMessage(),
                    e
            );
        }
    }

    public String post(String endpoint, String jsonBody) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            addAuthorizationHeader(builder);

            HttpResponse<String> response =
                    httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }

            throw new ApiException(
                    "POST failed: " +
                            response.statusCode() +
                            " - " +
                            response.body()
            );

        } catch (IOException | InterruptedException e) {
            throw new ApiException(
                    "Cannot connect to server: " + e.getMessage(),
                    e
            );
        }
    }

    public <T> T post(String endpoint,
                      Object requestBody,
                      Class<T> responseType) {

        String jsonBody = gson.toJson(requestBody);
        String responseJson = post(endpoint, jsonBody);

        return gson.fromJson(responseJson, responseType);
    }

    public <T> T postMultipartFile(String endpoint,
                                   String fieldName,
                                   Path filePath,
                                   Class<T> responseType) {
        try {
            String boundary = "AuctionUploadBoundary" + UUID.randomUUID();
            String fileName = filePath.getFileName().toString();
            String contentType = Files.probeContentType(filePath);

            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            byte[] prefix = (
                    "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n" +
                    "Content-Type: " + contentType + "\r\n\r\n"
            ).getBytes(StandardCharsets.UTF_8);
            byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

            HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.concat(
                    HttpRequest.BodyPublishers.ofByteArray(prefix),
                    HttpRequest.BodyPublishers.ofFile(filePath),
                    HttpRequest.BodyPublishers.ofByteArray(suffix)
            );

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + endpoint))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(body);

            addAuthorizationHeader(builder);

            HttpResponse<String> response =
                    httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return gson.fromJson(response.body(), responseType);
            }

            throw new ApiException(
                    "UPLOAD failed: " +
                            response.statusCode() +
                            " - " +
                            response.body()
            );
        } catch (IOException | InterruptedException e) {
            throw new ApiException("Cannot upload file: " + e.getMessage(), e);
        }
    }

    public String toAbsoluteUrl(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }

        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }

        return BASE_URL + (path.startsWith("/") ? path : "/" + path);
    }

    public <T> T put(String endpoint,
                     Object requestBody,
                     Class<T> responseType) {

        try {
            String jsonBody = gson.toJson(requestBody);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + endpoint))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody));

            addAuthorizationHeader(builder);

            HttpResponse<String> response =
                    httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return gson.fromJson(response.body(), responseType);
            }

            throw new ApiException(
                    "PUT failed: " +
                            response.statusCode() +
                            " - " +
                            response.body()
            );

        } catch (IOException | InterruptedException e) {
            throw new ApiException(
                    "Cannot connect to server: " + e.getMessage(),
                    e
            );
        }
    }

    public void delete(String endpoint) {

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + endpoint))
                    .header("Content-Type", "application/json")
                    .DELETE();

            addAuthorizationHeader(builder);

            HttpResponse<String> response =
                    httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return;
            }

            throw new ApiException(
                    "DELETE failed: " +
                            response.statusCode() +
                            " - " +
                            response.body()
            );

        } catch (IOException | InterruptedException e) {
            throw new ApiException(
                    "Cannot connect to server: " + e.getMessage(),
                    e
            );
        }
    }

    public <T> List<T> getList(String endpoint,
                               Class<T> itemType) {

        String responseJson = get(endpoint);

        Type listType =
                TypeToken.getParameterized(List.class, itemType).getType();

        return gson.fromJson(responseJson, listType);
    }

    private void addAuthorizationHeader(HttpRequest.Builder builder) {

        String token = SessionManager.getToken();

        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }

        if (SessionManager.getUserId() != null) {

            String userId =
                    SessionManager.getUserId().toString();

            builder.header("X-Seller-Id", userId);
            builder.header("X-Admin-Id", userId);
            builder.header("X-User-Id", userId);
        }
    }
}
