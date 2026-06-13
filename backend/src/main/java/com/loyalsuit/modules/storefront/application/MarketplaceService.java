package com.loyalsuit.modules.storefront.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.config.MarketplaceProperties;
import com.loyalsuit.modules.catalog.domain.Category;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductMedia;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
import com.loyalsuit.modules.catalog.domain.port.CategoryRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductMediaRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.marketplace.domain.Vendor;
import com.loyalsuit.modules.marketplace.domain.VendorStatus;
import com.loyalsuit.modules.marketplace.domain.port.VendorRepository;
import com.loyalsuit.modules.storefront.application.dto.MarketplaceProductCard;
import com.loyalsuit.modules.storefront.application.dto.StoreCategory;
import com.loyalsuit.modules.storefront.application.dto.StoreView;
import com.loyalsuit.modules.storefront.application.dto.VendorStorefront;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read model for the LoyalSuit marketplace — the single public storefront. Everything resolves the
 * one canonical flagship tenant (see {@link MarketplaceProperties}); products are shown house-first
 * (the operator's own, then vendors') and attributed to their seller. Anonymous: no JWT, no tenant
 * from a principal. Internal fields (SKU, status, exact stock) are never exposed.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketplaceService {

    private final MarketplaceProperties properties;
    private final TenantRepository tenantRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMediaRepository mediaRepository;
    private final VendorRepository vendorRepository;

    /** Public profile of the flagship store (name, slug, currency, logo). */
    public StoreView store() {
        return StoreView.from(marketplaceTenant());
    }

    /** Active categories of the marketplace, for the storefront filter. */
    public List<StoreCategory> categories() {
        Tenant tenant = marketplaceTenant();
        return categoryRepository.findAllByTenantId(tenant.getId()).stream()
                .filter(Category::isActive)
                .map(StoreCategory::from)
                .toList();
    }

    /** Browse the marketplace catalogue, house-first, optionally filtered by category. */
    public PageResponse<MarketplaceProductCard> products(String categorySlug, Pageable pageable) {
        Tenant tenant = marketplaceTenant();

        Page<Product> page;
        if (categorySlug != null && !categorySlug.isBlank()) {
            UUID categoryId = categoryRepository.findBySlugAndTenantId(categorySlug, tenant.getId())
                    .filter(Category::isActive)
                    .map(Category::getId)
                    .orElseThrow(() -> new NotFoundException("Category", categorySlug));
            page = productRepository.findByTenantIdAndStatusAndCategoryId(
                    tenant.getId(), ProductStatus.ACTIVE, categoryId, pageable);
        } else {
            page = productRepository.findActiveByTenantHouseFirst(tenant.getId(), pageable);
        }
        return toCards(tenant, page);
    }

    /** Search the marketplace catalogue by product name. A blank query returns nothing. */
    public PageResponse<MarketplaceProductCard> search(String query, Pageable pageable) {
        String q = query == null ? "" : query.trim();
        if (q.isBlank()) {
            return new PageResponse<>(Page.empty(pageable));
        }
        Tenant tenant = marketplaceTenant();
        return toCards(tenant, productRepository.searchActive(tenant.getId(), q, pageable));
    }

    /** A vendor's public storefront profile within the marketplace (only ACTIVE vendors are visible). */
    public VendorStorefront vendor(String vendorSlug) {
        return VendorStorefront.from(requireActiveVendor(vendorSlug));
    }

    /** The ACTIVE products on a vendor's storefront. */
    public PageResponse<MarketplaceProductCard> vendorProducts(String vendorSlug, Pageable pageable) {
        Tenant tenant = marketplaceTenant();
        Vendor vendor = requireActiveVendor(vendorSlug);
        return toCards(tenant, productRepository.findByTenantIdAndVendorIdAndStatus(
                tenant.getId(), vendor.getId(), ProductStatus.ACTIVE, pageable));
    }

    /** Map a product page to cards, batching category names, primary images, and seller attribution. */
    private PageResponse<MarketplaceProductCard> toCards(Tenant tenant, Page<Product> page) {
        List<Product> products = page.getContent();

        Map<UUID, String> categoryNames = categoryRepository.findAllByTenantId(tenant.getId()).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        Map<UUID, String> primaryImages = mediaRepository
                .findByProductIdInAndPrimaryTrue(products.stream().map(Product::getId).toList())
                .stream()
                .collect(Collectors.toMap(ProductMedia::getProductId, ProductMedia::getUrl, (a, b) -> a));
        List<UUID> vendorIds = products.stream()
                .map(Product::getVendorId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, Vendor> vendorsById = vendorRepository.findByTenantIdAndIdIn(tenant.getId(), vendorIds).stream()
                .collect(Collectors.toMap(Vendor::getId, v -> v));

        Instant now = Instant.now();
        return new PageResponse<>(page.map(p -> {
            Vendor vendor = p.getVendorId() != null ? vendorsById.get(p.getVendorId()) : null;
            // Only link to a storefront the public can actually see (an ACTIVE vendor).
            boolean linkable = vendor != null && vendor.getStatus() == VendorStatus.ACTIVE;
            return new MarketplaceProductCard(
                    p.getId(),
                    p.getName(),
                    p.getSlug(),
                    p.effectivePrice(now),
                    p.onSale(now) ? p.getPrice() : p.getCompareAtPrice(),
                    primaryImages.get(p.getId()),
                    p.getCategoryId() != null ? categoryNames.get(p.getCategoryId()) : null,
                    vendor != null ? vendor.getStoreName() : null,
                    linkable ? vendor.getSlug() : null);
        }));
    }

    /** Resolve an ACTIVE vendor by slug within the marketplace, or 404 (pending/suspended are hidden). */
    private Vendor requireActiveVendor(String vendorSlug) {
        Tenant tenant = marketplaceTenant();
        return vendorRepository.findBySlugAndTenantId(vendorSlug, tenant.getId())
                .filter(v -> v.getStatus() == VendorStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Vendor", vendorSlug));
    }

    /** Resolve the one canonical marketplace store; it is seeded at startup, so this should always exist. */
    private Tenant marketplaceTenant() {
        return tenantRepository.findBySlug(properties.getSlug())
                .filter(Tenant::isActive)
                .orElseThrow(() -> new NotFoundException("Marketplace", properties.getSlug()));
    }
}
