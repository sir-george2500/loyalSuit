package com.loyalsuit.modules.catalog.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.catalog.application.dto.MediaResponse;
import com.loyalsuit.modules.catalog.application.port.MediaStorage;
import com.loyalsuit.modules.catalog.domain.ProductMedia;
import com.loyalsuit.modules.catalog.domain.port.ProductMediaRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Handles product image uploads. Security posture (untrusted input):
 * <ul>
 *   <li>The parent product must belong to the caller's tenant (access control).</li>
 *   <li>Bytes are validated by <b>magic number</b>, not the client-supplied MIME
 *       type — a file is only accepted if it really is a JPEG/PNG/GIF/WEBP.</li>
 *   <li>Size is capped server-side regardless of any client claim.</li>
 *   <li>Assets are stored under a per-tenant/per-product folder.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaUploadService {

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB

    private final ProductMediaRepository mediaRepository;
    private final ProductRepository productRepository;
    private final MediaStorage mediaStorage;

    public List<MediaResponse> list(UUID productId, UUID tenantId) {
        requireOwnedProduct(productId, tenantId);
        return mediaRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
                .map(MediaResponse::from)
                .toList();
    }

    @Transactional
    public MediaResponse upload(UUID productId, UUID tenantId, byte[] data) {
        requireOwnedProduct(productId, tenantId);

        if (data == null || data.length == 0) {
            throw new BusinessException("The uploaded file is empty");
        }
        if (data.length > MAX_BYTES) {
            throw new BusinessException("Image is too large (max 5 MB)");
        }
        if (detectImageType(data) == null) {
            throw new BusinessException("Only JPEG, PNG, GIF, or WEBP images are allowed");
        }

        String folder = "loyalsuit/" + tenantId + "/products/" + productId;
        MediaStorage.StoredAsset asset = mediaStorage.upload(data, folder);

        int existing = mediaRepository.countByProductId(productId);
        ProductMedia media = new ProductMedia(tenantId, productId, asset.publicId(), asset.secureUrl());
        media.setSortOrder(existing);
        media.setPrimary(existing == 0); // first image is the primary by default
        return MediaResponse.from(mediaRepository.save(media));
    }

    @Transactional
    public void delete(UUID productId, UUID mediaId, UUID tenantId) {
        requireOwnedProduct(productId, tenantId);

        ProductMedia media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new NotFoundException("Image", mediaId));
        if (!media.getProductId().equals(productId)) {
            throw new NotFoundException("Image", mediaId);
        }

        mediaStorage.delete(media.getPublicId());
        mediaRepository.deleteById(mediaId);

        // If the primary was removed, promote the next remaining image.
        if (media.isPrimary()) {
            mediaRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setPrimary(true);
                        mediaRepository.save(next);
                    });
        }
    }

    private void requireOwnedProduct(UUID productId, UUID tenantId) {
        productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", productId));
    }

    /**
     * Returns the detected image format, or null if the bytes are not one of the
     * accepted image types. Sniffs the file's leading magic bytes — never trusts a
     * client-supplied content type.
     */
    private static String detectImageType(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "jpeg";
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A) {
            return "png";
        }
        if (d.length >= 6 && d[0] == 'G' && d[1] == 'I' && d[2] == 'F' && d[3] == '8'
                && (d[4] == '7' || d[4] == '9') && d[5] == 'a') {
            return "gif";
        }
        if (d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
