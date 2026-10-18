package com.dremioplatform.identity.service.dto;

public record AccessTokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
    public static AccessTokenResponse bearer(String accessToken, long expiresInSeconds) {
        return new AccessTokenResponse(accessToken, "Bearer", expiresInSeconds);
    }
}
