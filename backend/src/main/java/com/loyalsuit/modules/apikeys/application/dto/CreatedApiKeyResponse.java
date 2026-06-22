package com.loyalsuit.modules.apikeys.application.dto;

/**
 * Returned once, immediately after a key is created. {@code plaintext} is the only time the
 * full secret is ever exposed — the server stores only its hash.
 */
public record CreatedApiKeyResponse(ApiKeyResponse key, String plaintext) {}
