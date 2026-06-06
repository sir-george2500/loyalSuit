package com.loyalsuit.modules.catalog.infrastructure.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.modules.catalog.application.port.MediaStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Cloudinary-backed {@link MediaStorage}. The api_secret lives only here (server
 * side) via the injected {@link Cloudinary} bean — it is never exposed to clients.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloudinaryMediaStorage implements MediaStorage {

    private final Cloudinary cloudinary;

    @Override
    public StoredAsset upload(byte[] data, String folder) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(data, ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image",
                    "overwrite", false));
            String publicId = (String) result.get("public_id");
            String secureUrl = (String) result.get("secure_url");
            if (publicId == null || secureUrl == null) {
                throw new BusinessException("Image upload failed", HttpStatus.BAD_GATEWAY);
            }
            return new StoredAsset(publicId, secureUrl);
        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new BusinessException("Image upload failed, please try again", HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("invalidate", true));
        } catch (IOException e) {
            // Orphaning one remote asset must not block removing the DB row.
            log.warn("Cloudinary delete failed for publicId={}: {}", publicId, e.getMessage());
        }
    }
}
