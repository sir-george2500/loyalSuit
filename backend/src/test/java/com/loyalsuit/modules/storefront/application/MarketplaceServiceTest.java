package com.loyalsuit.modules.storefront.application;

import com.loyalsuit.config.MarketplaceProperties;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
import com.loyalsuit.modules.catalog.domain.port.CategoryRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductMediaRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.marketplace.domain.Vendor;
import com.loyalsuit.modules.marketplace.domain.VendorStatus;
import com.loyalsuit.modules.marketplace.domain.port.VendorRepository;
import com.loyalsuit.modules.storefront.application.dto.MarketplaceProductCard;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductMediaRepository mediaRepository;
    @Mock private VendorRepository vendorRepository;

    private final MarketplaceProperties properties = new MarketplaceProperties();
    private MarketplaceService service;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        properties.setSlug("loyalsuit");
        properties.setName("LoyalSuit");
        tenantId = UUID.randomUUID();
        service = new MarketplaceService(
                properties, tenantRepository, productRepository, categoryRepository, mediaRepository, vendorRepository);
    }

    private Tenant flagship() {
        Tenant t = new Tenant("LoyalSuit", "loyalsuit");
        ReflectionTestUtils.setField(t, "id", tenantId);
        t.setActive(true);
        return t;
    }

    private Product product(String slug, UUID id, UUID vendorId) {
        Product p = new Product(tenantId, "Widget", slug, BigDecimal.valueOf(20));
        p.setStatus(ProductStatus.ACTIVE);
        ReflectionTestUtils.setField(p, "id", id);
        if (vendorId != null) {
            p.setVendorId(vendorId);
        }
        return p;
    }

    @Test
    void products_houseFirst_attributeVendorElseLoyalSuit() {
        // Arrange — a house product (no vendor) and a vendor product
        UUID houseId = UUID.randomUUID();
        UUID vendorProdId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = new Vendor(tenantId, UUID.randomUUID(), "Bright Goods", "bright-goods");
        vendor.setStatus(VendorStatus.ACTIVE);
        ReflectionTestUtils.setField(vendor, "id", vendorId);

        when(tenantRepository.findBySlug("loyalsuit")).thenReturn(Optional.of(flagship()));
        when(vendorRepository.findActiveVendorIds(tenantId)).thenReturn(List.of(vendorId));
        when(productRepository.findVisibleActiveHouseFirst(eq(tenantId), eq(List.of(vendorId)), any()))
                .thenReturn(new PageImpl<>(List.of(product("house-mug", houseId, null),
                        product("vendor-cap", vendorProdId, vendorId))));
        when(categoryRepository.findAllByTenantId(tenantId)).thenReturn(List.of());
        when(mediaRepository.findByProductIdInAndPrimaryTrue(anyList())).thenReturn(List.of());
        when(vendorRepository.findByTenantIdAndIdIn(eq(tenantId), anyCollection())).thenReturn(List.of(vendor));

        // Act
        List<MarketplaceProductCard> cards = service.products(null, PageRequest.of(0, 24)).getContent();

        // Assert — the feed is filtered to active vendors (+ house), and attribution links the seller
        verify(productRepository).findVisibleActiveHouseFirst(eq(tenantId), eq(List.of(vendorId)), any());
        assertThat(cards).extracting(MarketplaceProductCard::slug).containsExactly("house-mug", "vendor-cap");
        assertThat(cards.get(0).soldBy()).isNull();
        assertThat(cards.get(0).soldBySlug()).isNull();
        assertThat(cards.get(1).soldBy()).isEqualTo("Bright Goods");
        assertThat(cards.get(1).soldBySlug()).isEqualTo("bright-goods");
    }

    @Test
    void vendor_returnsActiveStorefront_butHidesNonActive() {
        // Arrange
        Vendor pending = new Vendor(tenantId, UUID.randomUUID(), "Bright Goods", "bright-goods");
        when(tenantRepository.findBySlug("loyalsuit")).thenReturn(Optional.of(flagship()));
        when(vendorRepository.findBySlugAndTenantId("bright-goods", tenantId)).thenReturn(Optional.of(pending));

        // Act & Assert — a PENDING vendor has no public storefront
        assertThatThrownBy(() -> service.vendor("bright-goods")).isInstanceOf(NotFoundException.class);

        // ...but an ACTIVE one does
        pending.setStatus(VendorStatus.ACTIVE);
        assertThat(service.vendor("bright-goods").storeName()).isEqualTo("Bright Goods");
    }

    @Test
    void search_blankQuery_returnsEmpty_withoutResolvingTheStore() {
        // Act & Assert — a blank query never hits the catalogue or store
        assertThat(service.search("  ", PageRequest.of(0, 24)).getContent()).isEmpty();
        verifyNoInteractions(tenantRepository, productRepository, vendorRepository);
    }
}
