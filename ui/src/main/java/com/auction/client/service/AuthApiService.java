package com.auction.client.service;

import com.auction.client.dto.request.LoginByEmailRequest;
import com.auction.client.dto.request.LoginRequest;
import com.auction.client.dto.request.RegisterByEmailRequest;
import com.auction.client.dto.request.RegisterRequest;
import com.auction.client.dto.response.LoginResponse;
import com.auction.client.dto.response.RegisterResponse;
import com.auction.client.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AuthApiService {
    private final ApiClient apiClient;
    private final ObjectMapper objectMapper;

    public AuthApiService() {
        this.apiClient = new ApiClient();
        this.objectMapper = new ObjectMapper();
    }

    public LoginResponse login(String credential, String password) {
        try {
            String responseBody;

            if (credential != null && credential.contains("@")) {
                LoginByEmailRequest request = new LoginByEmailRequest(credential, password);
                String jsonBody = objectMapper.writeValueAsString(request);
                responseBody = apiClient.post("/api/auth/login-email", jsonBody);
            } else {
                LoginRequest request = new LoginRequest(credential, password);
                String jsonBody = objectMapper.writeValueAsString(request);
                responseBody = apiClient.post("/api/auth/login", jsonBody);
            }

            return objectMapper.readValue(responseBody, LoginResponse.class);
        } catch (Exception e) {
            throw new ApiException("Login failed: " + e.getMessage(), e);
        }
    }

    public RegisterResponse register(String username, String email, String password, String role) {
        try {
            String responseBody;

            if (email != null && !email.isBlank()) {
                RegisterByEmailRequest request = new RegisterByEmailRequest(
                        email.trim(),
                        password,
                        role
                );
                String jsonBody = objectMapper.writeValueAsString(request);
                responseBody = apiClient.post("/api/auth/register-email", jsonBody);
            } else {
                RegisterRequest request = new RegisterRequest(
                        username.trim(),
                        password,
                        role
                );
                String jsonBody = objectMapper.writeValueAsString(request);
                responseBody = apiClient.post("/api/auth/register", jsonBody);
            }

            return objectMapper.readValue(responseBody, RegisterResponse.class);
        } catch (Exception e) {
            throw new ApiException("Register failed: " + e.getMessage(), e);
        }
    }
}