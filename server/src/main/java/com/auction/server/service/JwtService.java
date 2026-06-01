package com.auction.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.auction.server.dto.UserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Objects;

@Service
public class JwtService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.jwt.secret:auction-project-dev-secret-change-me-please}")
    private String secret;

    @Value("${app.jwt.expiration-seconds:86400}")
    private long expirationSeconds;

    public String generateToken(UserDto user) {
        try {
            Instant now = Instant.now();
            Instant exp = now.plusSeconds(expirationSeconds);

            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", user.username);
            payload.put("userId", user.id.toString());
            payload.put("email", user.email);
            payload.put("role", user.role);
            payload.put("iat", now.getEpochSecond());
            payload.put("exp", exp.getEpochSecond());

            String encodedHeader = base64Url(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64Url(objectMapper.writeValueAsBytes(payload));
            String unsignedToken = encodedHeader + "." + encodedPayload;
            String signature = base64Url(hmacSha256(unsignedToken));

            return unsignedToken + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate JWT token", e);
        }
    }

    private byte[] hmacSha256(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public Map<String, Object> parseAndValidate(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format");
            }

            String unsignedToken = parts[0] + "." + parts[1];
            String expectedSignature = base64Url(hmacSha256(unsignedToken));

            if (!Objects.equals(expectedSignature, parts[2])) {
                throw new IllegalArgumentException("Invalid JWT signature");
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> payload = objectMapper.readValue(
                    payloadBytes,
                    new TypeReference<Map<String, Object>>() {}
            );

            Object exp = payload.get("exp");
            if (exp instanceof Number number && number.longValue() < Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("JWT expired");
            }

            return payload;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public String getUsername(String token) {
        Object sub = parseAndValidate(token).get("sub");
        return sub == null ? null : sub.toString();
    }
}
