package com.loyalsuit.modules.apikeys.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.apikeys.application.dto.ApiKeyResponse;
import com.loyalsuit.modules.apikeys.application.dto.CreatedApiKeyResponse;
import com.loyalsuit.modules.apikeys.domain.ApiKey;
import com.loyalsuit.modules.apikeys.domain.port.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Issues and revokes tenant API keys. Keys are random 30-byte secrets, prefixed {@code lsk_};
 * only their SHA-256 hash is stored. The plaintext is returned exactly once at creation.
 */
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String PREFIX = "lsk_";
    private static final int SECRET_BYTES = 30;

    private final ApiKeyRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> list(UUID tenantId) {
        return repository.findByTenantId(tenantId).stream().map(ApiKeyService::toResponse).toList();
    }

    @Transactional
    public CreatedApiKeyResponse create(UUID tenantId, UUID userId, String name) {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String plaintext = PREFIX + token;
        String hash = sha256Hex(plaintext);

        // Astronomically unlikely, but never store a colliding hash.
        if (repository.existsByKeyHash(hash)) {
            throw new BusinessException("Could not generate a unique key, please try again.");
        }

        ApiKey key = new ApiKey(tenantId);
        key.setName(name.trim());
        key.setKeyHash(hash);
        key.setKeyPrefix(plaintext.substring(0, 12));
        key.setLastFour(token.substring(token.length() - 4));
        key.setCreatedBy(userId);

        return new CreatedApiKeyResponse(toResponse(repository.save(key)), plaintext);
    }

    @Transactional
    public ApiKeyResponse revoke(UUID tenantId, UUID id) {
        ApiKey key = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("API key", id));
        if (key.isActive()) {
            key.setRevokedAt(Instant.now());
            key = repository.save(key);
        }
        return toResponse(key);
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static ApiKeyResponse toResponse(ApiKey k) {
        return new ApiKeyResponse(
                k.getId(), k.getName(), k.getKeyPrefix(), k.getLastFour(), k.isActive(),
                k.getCreatedAt(), k.getLastUsedAt(), k.getRevokedAt());
    }
}
