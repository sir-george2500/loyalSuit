package com.loyalsuit.modules.catalog.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.catalog.application.dto.VariantRequest;
import com.loyalsuit.modules.catalog.application.dto.VariantResponse;
import com.loyalsuit.modules.catalog.domain.ProductVariant;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages variants of a product. The {@code product_variants} table is scoped to a
 * tenant only through its parent product, so every operation first confirms the
 * parent product belongs to the caller's tenant — this is the access-control
 * boundary that prevents touching another tenant's variants.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;

    public List<VariantResponse> list(UUID productId, UUID tenantId) {
        requireOwnedProduct(productId, tenantId);
        return variantRepository.findByProductId(productId).stream()
                .map(VariantResponse::from)
                .toList();
    }

    @Transactional
    public VariantResponse create(UUID productId, UUID tenantId, VariantRequest request) {
        requireOwnedProduct(productId, tenantId);

        ProductVariant variant = new ProductVariant(productId, request.getName(), request.getPrice());
        variant.setSku(request.getSku());
        return VariantResponse.from(variantRepository.save(variant));
    }

    @Transactional
    public VariantResponse update(UUID productId, UUID variantId, UUID tenantId, VariantRequest request) {
        requireOwnedProduct(productId, tenantId);
        ProductVariant variant = requireVariantOfProduct(variantId, productId);

        variant.setName(request.getName());
        variant.setSku(request.getSku());
        variant.setPrice(request.getPrice());
        return VariantResponse.from(variantRepository.save(variant));
    }

    @Transactional
    public void delete(UUID productId, UUID variantId, UUID tenantId) {
        requireOwnedProduct(productId, tenantId);
        ProductVariant variant = requireVariantOfProduct(variantId, productId);
        variantRepository.deleteById(variant.getId());
    }

    /** The parent product must exist within the caller's tenant. */
    private void requireOwnedProduct(UUID productId, UUID tenantId) {
        productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", productId));
    }

    /** The variant must exist and belong to the product named in the path. */
    private ProductVariant requireVariantOfProduct(UUID variantId, UUID productId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new NotFoundException("Variant", variantId));
        if (!variant.getProductId().equals(productId)) {
            // Don't reveal that the variant exists under a different product.
            throw new NotFoundException("Variant", variantId);
        }
        return variant;
    }
}
