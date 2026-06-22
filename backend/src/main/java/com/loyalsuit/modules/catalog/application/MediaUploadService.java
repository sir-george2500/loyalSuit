package com.loyalsuit.modules.catalog.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.media.ImageBytes;
import com.loyalsuit.common.media.MediaStorage;
import com.loyalsuit.modules.catalog.application.dto.MediaResponse;
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
        ImageBytes.validate(data);

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
}
