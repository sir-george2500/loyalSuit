package com.loyalsuit.modules.promotions.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.modules.cart.application.CartService;
import com.loyalsuit.modules.cart.application.dto.CartView;
import com.loyalsuit.modules.promotions.application.dto.CouponPreviewResponse;
import com.loyalsuit.modules.promotions.application.dto.CouponRequest;
import com.loyalsuit.modules.promotions.application.dto.CouponResponse;
import com.loyalsuit.modules.promotions.domain.Coupon;
import com.loyalsuit.modules.promotions.domain.DiscountType;
import com.loyalsuit.modules.promotions.domain.port.CouponRedemptionRepository;
import com.loyalsuit.modules.promotions.domain.port.CouponRepository;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock private CouponRepository couponRepository;
    @Mock private CouponRedemptionRepository redemptionRepository;
    @Mock private CartService cartService;
    @Mock private TenantRepository tenantRepository;

    @InjectMocks private CouponService service;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    private CouponRequest request(DiscountType type, String value) {
        CouponRequest r = new CouponRequest();
        r.setCode("save10");
        r.setDiscountType(type);
        r.setValue(new BigDecimal(value));
        return r;
    }

    private Coupon coupon(DiscountType type, String value) {
        Coupon c = new Coupon(tenantId, "SAVE10", type, new BigDecimal(value));
        ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
        return c;
    }

    private void stubCart(String subtotal) {
        when(cartService.view("acme", "tok"))
                .thenReturn(new CartView(List.of(), new BigDecimal(subtotal), 1, "USD"));
    }

    private void stubStore() {
        Tenant tenant = new Tenant("Acme", "acme");
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(tenant));
    }

    // ---- create -------------------------------------------------------------

    @Test
    void create_storesTheNormalizedCode() {
        // Arrange
        when(couponRepository.existsByTenantIdAndCode(tenantId, "SAVE10")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CouponResponse coupon = service.create(tenantId, request(DiscountType.PERCENT, "10"));

        // Assert
        assertThat(coupon.code()).isEqualTo("SAVE10");
        assertThat(coupon.timesRedeemed()).isZero();
    }

    @Test
    void create_aDuplicateCode_isRejected() {
        // Arrange
        when(couponRepository.existsByTenantIdAndCode(tenantId, "SAVE10")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.create(tenantId, request(DiscountType.PERCENT, "10")))
                .isInstanceOf(ConflictException.class);
        verify(couponRepository, never()).save(any());
    }

    @Test
    void create_aPercentOver100_isRejected() {
        // Arrange
        when(couponRepository.existsByTenantIdAndCode(tenantId, "SAVE10")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> service.create(tenantId, request(DiscountType.PERCENT, "150")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("between 0 and 100");
        verify(couponRepository, never()).save(any());
    }

    @Test
    void create_anInvalidWindow_isRejected() {
        // Arrange — end before start
        when(couponRepository.existsByTenantIdAndCode(tenantId, "SAVE10")).thenReturn(false);
        CouponRequest request = request(DiscountType.PERCENT, "10");
        request.setStartsAt(Instant.now());
        request.setEndsAt(Instant.now().minusSeconds(60));

        // Act & Assert
        assertThatThrownBy(() -> service.create(tenantId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must be after its start");
    }

    // ---- preview ------------------------------------------------------------

    @Test
    void preview_computesTheDiscountFromTheServerPricedCart() {
        // Arrange — 10% off a $80 cart = $8
        stubStore();
        when(couponRepository.findByTenantIdAndCode(tenantId, "SAVE10"))
                .thenReturn(Optional.of(coupon(DiscountType.PERCENT, "10")));
        stubCart("80.00");

        // Act
        CouponPreviewResponse preview = service.preview("acme", "save10", "tok");

        // Assert
        assertThat(preview.code()).isEqualTo("SAVE10");
        assertThat(preview.discountAmount()).isEqualByComparingTo("8.00");
    }

    @Test
    void preview_belowTheMinimumSubtotal_isRejected() {
        // Arrange — needs $100, cart is $80
        stubStore();
        Coupon coupon = coupon(DiscountType.PERCENT, "10");
        coupon.setMinOrderSubtotal(new BigDecimal("100.00"));
        when(couponRepository.findByTenantIdAndCode(tenantId, "SAVE10")).thenReturn(Optional.of(coupon));
        stubCart("80.00");

        // Act & Assert
        assertThatThrownBy(() -> service.preview("acme", "save10", "tok"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Spend at least");
    }

    @Test
    void preview_anUnknownOrInactiveCode_isRejected() {
        // Arrange
        stubStore();
        stubCart("80.00");
        when(couponRepository.findByTenantIdAndCode(tenantId, "SAVE10")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.preview("acme", "save10", "tok"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("isn't valid");
    }

    @Test
    void preview_whenTheUsageLimitIsReached_isRejected() {
        // Arrange — limit 5, already redeemed 5
        stubStore();
        Coupon coupon = coupon(DiscountType.PERCENT, "10");
        coupon.setUsageLimit(5);
        when(couponRepository.findByTenantIdAndCode(tenantId, "SAVE10")).thenReturn(Optional.of(coupon));
        stubCart("80.00");
        when(redemptionRepository.countByCouponId(coupon.getId())).thenReturn(5L);

        // Act & Assert
        assertThatThrownBy(() -> service.preview("acme", "save10", "tok"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("usage limit");
    }

    // ---- checkout (validate + redeem) ---------------------------------------

    @Test
    void validateForCheckout_returnsTheDiscountToApply() {
        // Arrange — 25% off a $200 subtotal = $50
        when(couponRepository.findByTenantIdAndCode(tenantId, "SAVE10"))
                .thenReturn(Optional.of(coupon(DiscountType.PERCENT, "25")));

        // Act
        var applied = service.validateForCheckout(tenantId, "save10", new BigDecimal("200.00"), "buyer@x.dev");

        // Assert
        assertThat(applied.code()).isEqualTo("SAVE10");
        assertThat(applied.discount()).isEqualByComparingTo("50.00");
    }

    @Test
    void validateForCheckout_whenTheCustomerHitTheirLimit_isRejected() {
        // Arrange — per-customer limit 1, this email already used it once
        Coupon coupon = coupon(DiscountType.PERCENT, "10");
        coupon.setPerCustomerLimit(1);
        when(couponRepository.findByTenantIdAndCode(tenantId, "SAVE10")).thenReturn(Optional.of(coupon));
        when(redemptionRepository.countByCouponIdAndCustomerEmail(coupon.getId(), "buyer@x.dev")).thenReturn(1L);

        // Act & Assert
        assertThatThrownBy(() ->
                service.validateForCheckout(tenantId, "save10", new BigDecimal("80.00"), "Buyer@X.dev"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("maximum number of times");
    }

    @Test
    void recordRedemption_writesALedgerRow() {
        // Arrange
        var applied = new com.loyalsuit.modules.promotions.application.dto.AppliedCoupon(
                UUID.randomUUID(), "SAVE10", new BigDecimal("10.00"));
        UUID orderId = UUID.randomUUID();

        // Act
        service.recordRedemption(tenantId, applied, orderId, "buyer@x.dev");

        // Assert
        verify(redemptionRepository).save(any());
    }
}
