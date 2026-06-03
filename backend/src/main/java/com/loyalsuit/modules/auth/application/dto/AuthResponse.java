package com.loyalsuit.modules.auth.application.dto;

import lombok.Getter;

@Getter
public class AuthResponse {

    private final String token;
    private final String tokenType = "Bearer";
    private final long expiresIn;
    private final UserProfile user;

    public AuthResponse(String token, long expiresIn, UserProfile user) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.user = user;
    }
}
