package com.auction.client.service;

import com.auction.client.exception.ApiException;
import com.auction.client.session.SessionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ApiClient {
    private static final String BASE_URL = "http://localhost:8081";
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

            throw new ApiException("GET failed: " + response.statusCode() + " - " + response.body());
        } catch (IOException | InterruptedException e) {
            throw new ApiException("Cannot connect to server: " + e.getMessage(), e);
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

            throw new ApiException("POST failed: " + response.statusCode() + " - " + response.body());
        } catch (IOException | InterruptedException e) {
            throw new ApiException("Cannot connect to server: " + e.getMessage(), e);
        }
    }

    public <T> T post(String endpoint, Object requestBody, Class<T> responseType) {
        String jsonBody = gson.toJson(requestBody);
        String responseJson = post(endpoint, jsonBody);
        return gson.fromJson(responseJson, responseType);
    }

    public <T> T put(String endpoint, Object requestBody, Class<T> responseType) {
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

            throw new ApiException("PUT failed: " + response.statusCode() + " - " + response.body());
        } catch (IOException | InterruptedException e) {
            throw new ApiException("Cannot connect to server: " + e.getMessage(), e);
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

            throw new ApiException("DELETE failed: " + response.statusCode() + " - " + response.body());
        } catch (IOException | InterruptedException e) {
            throw new ApiException("Cannot connect to server: " + e.getMessage(), e);
        }
    }

    public <T> List<T> getList(String endpoint, Class<T> itemType) {
        String responseJson = get(endpoint);
        Type listType = TypeToken.getParameterized(List.class, itemType).getType();
        return gson.fromJson(responseJson, listType);
    }

    private void addAuthorizationHeader(HttpRequest.Builder builder) {
        String token = SessionManager.getToken();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
    }
}