package com.loyalsuit.modules.catalog.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.catalog.application.dto.CreateProductRequest;
import com.loyalsuit.modules.catalog.application.dto.ProductResponse;
import com.loyalsuit.modules.catalog.application.dto.UpdateProductRequest;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
import com.loyalsuit.modules.catalog.domain.port.CategoryRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    /** Role name that owns its own products; matches the JWT role claim. */
    private static final String VENDOR_ROLE = "VENDOR";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public PageResponse<ProductResponse> listByTenant(UUID tenantId, Pageable pageable) {
        return new PageResponse<>(
                productRepository.findByTenantId(tenantId, pageable).map(ProductResponse::new));
    }

    public PageResponse<ProductResponse> listActiveByTenant(UUID tenantId, Pageable pageable) {
        return new PageResponse<>(
                productRepository.findByTenantIdAndStatus(tenantId, ProductStatus.ACTIVE, pageable)
                        .map(ProductResponse::new));
    }

    public PageResponse<ProductResponse> listByCategory(UUID categoryId, UUID tenantId, Pageable pageable) {
        return new PageResponse<>(
                productRepository.findByCategoryIdAndTenantId(categoryId, tenantId, pageable)
                        .map(ProductResponse::new));
    }

    public ProductResponse getById(UUID id, UUID tenantId) {
        return new ProductResponse(loadProduct(id, tenantId));
    }

    /**
     * Creates a product for the current tenant. Ownership is derived from the
     * authenticated principal — a VENDOR's products are stamped with their own id,
     * everyone else's are house products (no vendor). The client never supplies it.
     */
    @Transactional
    public ProductResponse create(CreateProductRequest request, UUID tenantId, UUID actorId, String actorRole) {
        if (productRepository.existsBySlugAndTenantId(request.getSlug(), tenantId)) {
            throw new ConflictException("Product slug already exists: " + request.getSlug());
        }
        validateCategory(request.getCategoryId(), tenantId);

        Product product = new Product(tenantId, request.getName(), request.getSlug(), request.getPrice());
        product.setDescription(request.getDescription());
        product.setCompareAtPrice(request.getCompareAtPrice());
        product.setSku(request.getSku());
        product.setBarcode(request.getBarcode());
        product.setCategoryId(request.getCategoryId());
        product.setVendorId(VENDOR_ROLE.equals(actorRole) ? actorId : null);
        product.setDigital(request.isDigital());

        return new ProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request, UUID tenantId) {
        Product product = loadProduct(id, tenantId);

        if (!product.getSlug().equals(request.getSlug())
                && productRepository.existsBySlugAndTenantId(request.getSlug(), tenantId)) {
            throw new ConflictException("Product slug already exists: " + request.getSlug());
        }
        validateCategory(request.getCategoryId(), tenantId);

        product.setName(request.getName());
        product.setSlug(request.getSlug());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCompareAtPrice(request.getCompareAtPrice());
        product.setSku(request.getSku());
        product.setBarcode(request.getBarcode());
        product.setCategoryId(request.getCategoryId());
        product.setDigital(request.isDigital());

        return new ProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse publish(UUID id, UUID tenantId) {
        Product product = loadProduct(id, tenantId);
        product.activate();
        return new ProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse unpublish(UUID id, UUID tenantId) {
        Product product = loadProduct(id, tenantId);
        product.deactivate();
        return new ProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse archive(UUID id, UUID tenantId) {
        Product product = loadProduct(id, tenantId);
        product.archive();
        return new ProductResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(UUID id, UUID tenantId) {
        loadProduct(id, tenantId);
        productRepository.deleteById(id);
    }

    private Product loadProduct(UUID id, UUID tenantId) {
        return productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", id));
    }

    /** A product may only reference a category that exists within the same tenant. */
    private void validateCategory(UUID categoryId, UUID tenantId) {
        if (categoryId != null && categoryRepository.findByIdAndTenantId(categoryId, tenantId).isEmpty()) {
            throw new BusinessException("Category does not exist in this store");
        }
    }
}
