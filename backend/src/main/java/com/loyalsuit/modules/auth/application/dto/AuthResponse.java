package com.loyalsuit.modules.auth.application.dto;

import lombok.Getter;

@Getter
public class AuthResponse {

    private final String token;
    private final String tokenType;
    private final long expiresIn;
    private final UserProfile user;
    /** When true, the password step passed but a 2FA code is required to finish. */
    private final boolean mfaRequired;
    /** Short-lived challenge token to submit with the 2FA code (only when mfaRequired). */
    private final String mfaToken;

    public AuthResponse(String token, long expiresIn, UserProfile user) {
        this.token = token;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.user = user;
        this.mfaRequired = false;
        this.mfaToken = null;
    }

    private AuthResponse(String mfaToken) {
        this.token = null;
        this.tokenType = null;
        this.expiresIn = 0;
        this.user = null;
        this.mfaRequired = true;
        this.mfaToken = mfaToken;
    }

    /** A response that asks the client to complete the second factor. */
    public static AuthResponse mfaChallenge(String mfaToken) {
        return new AuthResponse(mfaToken);
    }
}
