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
import com.loyalsuit.modules.marketplace.application.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Product catalog. Vendor-aware: a VENDOR only ever sees and manages their own
 * products (sub-tenant isolation — another vendor's product reads as not-found),
 * and may only create/edit while their vendor account is ACTIVE. Admins and staff
 * operate on all of the tenant's products.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    /** Role name that owns its own products; matches the JWT role claim. */
    private static final String VENDOR_ROLE = "VENDOR";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final VendorService vendorService;

    public PageResponse<ProductResponse> listByTenant(UUID tenantId, Pageable pageable) {
        return new PageResponse<>(
                productRepository.findByTenantId(tenantId, pageable).map(ProductResponse::new));
    }

    /**
     * Lists products for the acting principal: a vendor sees only their own,
     * everyone else sees the whole tenant catalog.
     */
    public PageResponse<ProductResponse> listForActor(UUID tenantId, UUID actorId, String actorRole, Pageable pageable) {
        if (isVendor(actorRole)) {
            return new PageResponse<>(
                    productRepository.findByTenantIdAndVendorId(tenantId, actorId, pageable).map(ProductResponse::new));
        }
        return listByTenant(tenantId, pageable);
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

    public ProductResponse getById(UUID id, UUID tenantId, UUID actorId, String actorRole) {
        return new ProductResponse(loadForActor(id, tenantId, actorId, actorRole));
    }

    /**
     * Creates a product for the current tenant. Ownership is derived from the
     * authenticated principal — a VENDOR's products are stamped with their own id,
     * everyone else's are house products (no vendor). The client never supplies it.
     */
    @Transactional
    public ProductResponse create(CreateProductRequest request, UUID tenantId, UUID actorId, String actorRole) {
        requireActiveVendor(actorId, actorRole);
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
        applySale(product, request.getPrice(), request.getSalePrice(),
                request.getSaleStartsAt(), request.getSaleEndsAt());

        return new ProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request, UUID tenantId, UUID actorId, String actorRole) {
        requireActiveVendor(actorId, actorRole);
        Product product = loadForActor(id, tenantId, actorId, actorRole);

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
        applySale(product, request.getPrice(), request.getSalePrice(),
                request.getSaleStartsAt(), request.getSaleEndsAt());

        return new ProductResponse(productRepository.save(product));
    }

    /** Schedules (or clears) a flash deal, validating it sits below the list price and that
     *  the window's end is after its start. A null sale price clears any existing deal. */
    private static void applySale(Product product, java.math.BigDecimal price, java.math.BigDecimal salePrice,
                                  java.time.Instant startsAt, java.time.Instant endsAt) {
        if (salePrice == null) {
            product.setSalePrice(null);
            product.setSaleStartsAt(null);
            product.setSaleEndsAt(null);
            return;
        }
        if (salePrice.compareTo(price) >= 0) {
            throw new BusinessException("The sale price must be below the regular price");
        }
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new BusinessException("The sale must end after it starts");
        }
        product.setSalePrice(salePrice);
        product.setSaleStartsAt(startsAt);
        product.setSaleEndsAt(endsAt);
    }

    @Transactional
    public ProductResponse publish(UUID id, UUID tenantId, UUID actorId, String actorRole) {
        requireActiveVendor(actorId, actorRole);
        Product product = loadForActor(id, tenantId, actorId, actorRole);
        product.activate();
        return new ProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse unpublish(UUID id, UUID tenantId, UUID actorId, String actorRole) {
        requireActiveVendor(actorId, actorRole);
        Product product = loadForActor(id, tenantId, actorId, actorRole);
        product.deactivate();
        return new ProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse archive(UUID id, UUID tenantId, UUID actorId, String actorRole) {
        requireActiveVendor(actorId, actorRole);
        Product product = loadForActor(id, tenantId, actorId, actorRole);
        product.archive();
        return new ProductResponse(productRepository.save(product));
    }

    /** Delete is admin-only (no VENDOR on the endpoint), so it stays tenant-scoped. */
    @Transactional
    public void delete(UUID id, UUID tenantId) {
        loadProduct(id, tenantId);
        productRepository.deleteById(id);
    }

    private Product loadProduct(UUID id, UUID tenantId) {
        return productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", id));
    }

    /** Loads a tenant product, but a vendor may only ever touch their own (else 404). */
    private Product loadForActor(UUID id, UUID tenantId, UUID actorId, String actorRole) {
        Product product = loadProduct(id, tenantId);
        if (isVendor(actorRole) && !actorId.equals(product.getVendorId())) {
            throw new NotFoundException("Product", id);
        }
        return product;
    }

    private boolean isVendor(String actorRole) {
        return VENDOR_ROLE.equals(actorRole);
    }

    /** A vendor can only create/edit products while their account is active. */
    private void requireActiveVendor(UUID actorId, String actorRole) {
        if (isVendor(actorRole) && !vendorService.isActiveVendor(actorId)) {
            throw new BusinessException("Your vendor account is not active");
        }
    }

    /** A product may only reference a category that exists within the same tenant. */
    private void validateCategory(UUID categoryId, UUID tenantId) {
        if (categoryId != null && categoryRepository.findByIdAndTenantId(categoryId, tenantId).isEmpty()) {
            throw new BusinessException("Category does not exist in this store");
        }
    }
}
