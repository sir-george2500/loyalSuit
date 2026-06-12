package com.loyalsuit.modules.storefront.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.catalog.domain.Category;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductMedia;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
import com.loyalsuit.modules.catalog.domain.port.CategoryRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductMediaRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductVariantRepository;
import com.loyalsuit.modules.inventory.domain.port.StockRepository;
import com.loyalsuit.modules.storefront.application.dto.StoreCategory;
import com.loyalsuit.modules.storefront.application.dto.StoreProductDetail;
import com.loyalsuit.modules.storefront.application.dto.StoreProductSummary;
import com.loyalsuit.modules.storefront.application.dto.StoreSummary;
import com.loyalsuit.modules.storefront.application.dto.StoreVariant;
import com.loyalsuit.modules.storefront.application.dto.StoreView;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Public storefront read model. It resolves the tenant from the store slug (NOT a
 * JWT — these endpoints are anonymous) and exposes only customer-facing data:
 * active stores, ACTIVE products, active categories, and an in-stock flag. Internal
 * fields (SKU, barcode, status, vendor, exact quantities) are never returned.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorefrontService {

    private final TenantRepository tenantRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductMediaRepository mediaRepository;
    private final StockRepository stockRepository;

    /**
     * The public marketplace index: every active store that has at least one published product, so a
     * shopper landing here always sees somewhere they can actually buy. Counts are fetched in a single
     * grouped query (no per-store round trips), and stores are ordered by how stocked they are.
     */
    public List<StoreSummary> listStores() {
        List<Tenant> active = tenantRepository.findAllActive();
        if (active.isEmpty()) {
            return List.of();
        }
        Map<UUID, Long> counts = productRepository.countActiveProductsByTenant(
                active.stream().map(Tenant::getId).toList());
        return active.stream()
                .map(t -> StoreSummary.from(t, counts.getOrDefault(t.getId(), 0L)))
                .filter(s -> s.productCount() > 0) // hide empty stores — nothing to shop there yet
                .sorted(Comparator.comparingLong(StoreSummary::productCount).reversed()
                        .thenComparing(StoreSummary::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public StoreView getStore(String storeSlug) {
        return StoreView.from(requireStore(storeSlug));
    }

    public List<StoreCategory> categories(String storeSlug) {
        Tenant tenant = requireStore(storeSlug);
        return categoryRepository.findAllByTenantId(tenant.getId()).stream()
                .filter(Category::isActive)
                .map(StoreCategory::from)
                .toList();
    }

    public PageResponse<StoreProductSummary> products(String storeSlug, String categorySlug, Pageable pageable) {
        Tenant tenant = requireStore(storeSlug);

        Page<Product> page;
        if (categorySlug != null && !categorySlug.isBlank()) {
            UUID categoryId = categoryRepository.findBySlugAndTenantId(categorySlug, tenant.getId())
                    .filter(Category::isActive)
                    .map(Category::getId)
                    .orElseThrow(() -> new NotFoundException("Category", categorySlug));
            page = productRepository.findByTenantIdAndStatusAndCategoryId(
                    tenant.getId(), ProductStatus.ACTIVE, categoryId, pageable);
        } else {
            page = productRepository.findByTenantIdAndStatus(tenant.getId(), ProductStatus.ACTIVE, pageable);
        }

        // Build lookups once for the whole page — no N+1.
        Map<UUID, String> categoryNames = categoryRepository.findAllByTenantId(tenant.getId()).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        Map<UUID, String> primaryImages = mediaRepository
                .findByProductIdInAndPrimaryTrue(page.getContent().stream().map(Product::getId).toList())
                .stream()
                .collect(Collectors.toMap(ProductMedia::getProductId, ProductMedia::getUrl, (a, b) -> a));

        Instant now = Instant.now();
        return new PageResponse<>(page.map(p -> new StoreProductSummary(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.effectivePrice(now),
                // On sale, the regular price becomes the struck-through "was" price.
                p.onSale(now) ? p.getPrice() : p.getCompareAtPrice(),
                primaryImages.get(p.getId()),
                p.getCategoryId() != null ? categoryNames.get(p.getCategoryId()) : null)));
    }

    public StoreProductDetail product(String storeSlug, String productSlug) {
        Tenant tenant = requireStore(storeSlug);

        Product product = productRepository.findBySlugAndTenantId(productSlug, tenant.getId())
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE) // never expose unpublished products
                .orElseThrow(() -> new NotFoundException("Product", productSlug));

        String categoryName = product.getCategoryId() == null ? null
                : categoryRepository.findByIdAndTenantId(product.getCategoryId(), tenant.getId())
                        .map(Category::getName)
                        .orElse(null);

        List<String> images = mediaRepository.findByProductIdOrderBySortOrderAsc(product.getId()).stream()
                .map(ProductMedia::getUrl)
                .toList();

        List<StoreVariant> variants = variantRepository.findByProductId(product.getId()).stream()
                .map(StoreVariant::from)
                .toList();

        boolean inStock = stockRepository.hasStock(product.getId(), tenant.getId());

        Instant now = Instant.now();
        return new StoreProductDetail(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.effectivePrice(now),
                product.onSale(now) ? product.getPrice() : product.getCompareAtPrice(),
                product.isDigital(),
                categoryName,
                images,
                variants,
                inStock);
    }

    /** Resolve an active store by slug, or 404. Suspended/inactive stores are hidden. */
    private Tenant requireStore(String storeSlug) {
        return tenantRepository.findBySlug(storeSlug)
                .filter(Tenant::isActive)
                .orElseThrow(() -> new NotFoundException("Store", storeSlug));
    }
}
