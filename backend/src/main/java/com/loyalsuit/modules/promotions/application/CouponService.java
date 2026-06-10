package com.loyalsuit.modules.promotions.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.cart.application.CartService;
import com.loyalsuit.modules.promotions.application.dto.AppliedCoupon;
import com.loyalsuit.modules.promotions.application.dto.CouponPreviewResponse;
import com.loyalsuit.modules.promotions.application.dto.CouponRequest;
import com.loyalsuit.modules.promotions.application.dto.CouponResponse;
import com.loyalsuit.modules.promotions.domain.Coupon;
import com.loyalsuit.modules.promotions.domain.CouponRedemption;
import com.loyalsuit.modules.promotions.domain.DiscountType;
import com.loyalsuit.modules.promotions.domain.port.CouponRedemptionRepository;
import com.loyalsuit.modules.promotions.domain.port.CouponRepository;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Coupons: admin authoring of discount codes, and the storefront preview of what a code
 * would take off the current cart. The discount math lives on the {@link Coupon}; this
 * service owns the rules around it (uniqueness, validity window, minimum spend, usage
 * limit) so the preview and (later) checkout agree. Subtotals come from the server-priced
 * cart, never the client.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;
    private final CartService cartService;
    private final TenantRepository tenantRepository;

    // ---- admin --------------------------------------------------------------

    @Transactional
    public CouponResponse create(UUID tenantId, CouponRequest request) {
        String code = Coupon.normalize(request.getCode());
        if (couponRepository.existsByTenantIdAndCode(tenantId, code)) {
            throw new ConflictException("A coupon with code " + code + " already exists");
        }
        validateRules(request);
        Coupon coupon = new Coupon(tenantId, code, request.getDiscountType(), request.getValue());
        apply(coupon, request);
        return CouponResponse.from(couponRepository.save(coupon), 0);
    }

    @Transactional
    public CouponResponse update(UUID id, UUID tenantId, CouponRequest request) {
        Coupon coupon = load(id, tenantId);
        String code = Coupon.normalize(request.getCode());
        if (!code.equals(coupon.getCode()) && couponRepository.existsByTenantIdAndCode(tenantId, code)) {
            throw new ConflictException("A coupon with code " + code + " already exists");
        }
        validateRules(request);
        coupon.setCode(code);
        coupon.setDiscountType(request.getDiscountType());
        coupon.setValue(request.getValue());
        apply(coupon, request);
        Coupon saved = couponRepository.save(coupon);
        return CouponResponse.from(saved, redemptionRepository.countByCouponId(saved.getId()));
    }

    @Transactional
    public CouponResponse setActive(UUID id, UUID tenantId, boolean active) {
        Coupon coupon = load(id, tenantId);
        coupon.setActive(active);
        Coupon saved = couponRepository.save(coupon);
        return CouponResponse.from(saved, redemptionRepository.countByCouponId(saved.getId()));
    }

    public PageResponse<CouponResponse> list(UUID tenantId, Pageable pageable) {
        return new PageResponse<>(couponRepository.findByTenantId(tenantId, pageable)
                .map(coupon -> CouponResponse.from(coupon, redemptionRepository.countByCouponId(coupon.getId()))));
    }

    // ---- storefront (public preview) ----------------------------------------

    /** What {@code code} would take off the cart now — validates the rules but redeems nothing. */
    public CouponPreviewResponse preview(String storeSlug, String code, String cartToken) {
        Tenant tenant = tenantRepository.findBySlug(storeSlug)
                .filter(Tenant::isActive)
                .orElseThrow(() -> new NotFoundException("Store", storeSlug));
        BigDecimal subtotal = cartService.view(storeSlug, cartToken).subtotal();
        Coupon coupon = requireRedeemable(tenant.getId(), code, subtotal, null);
        return new CouponPreviewResponse(coupon.getCode(), coupon.getDescription(), coupon.discountFor(subtotal));
    }

    // ---- checkout (orders module collaborates here) -------------------------

    /** Validate a code for an order and return the discount to apply, or throw. Includes the
     *  per-customer limit (the preview can't, having no identity). Records nothing yet. */
    public AppliedCoupon validateForCheckout(UUID tenantId, String code, BigDecimal subtotal, String customerEmail) {
        Coupon coupon = requireRedeemable(tenantId, code, subtotal, customerEmail);
        return new AppliedCoupon(coupon.getId(), coupon.getCode(), coupon.discountFor(subtotal));
    }

    /** Record a redemption against the placed order. The unique index on order_id is the
     *  backstop against a double-record. */
    @Transactional
    public void recordRedemption(UUID tenantId, AppliedCoupon coupon, UUID orderId, String customerEmail) {
        redemptionRepository.save(
                new CouponRedemption(tenantId, coupon.couponId(), orderId, customerEmail, coupon.discount()));
    }

    // ---- helpers ------------------------------------------------------------

    /** All the rules that gate a redemption (active, window, minimum, usage + per-customer
     *  limits). Returns the coupon when it may be applied to {@code subtotal}. */
    private Coupon requireRedeemable(UUID tenantId, String code, BigDecimal subtotal, String customerEmail) {
        Coupon coupon = couponRepository.findByTenantIdAndCode(tenantId, Coupon.normalize(code))
                .filter(Coupon::isActive)
                .orElseThrow(() -> new BusinessException("That code isn't valid"));
        if (!coupon.isWithinWindow(Instant.now())) {
            throw new BusinessException("That code isn't available right now");
        }
        if (!coupon.meetsMinimum(subtotal)) {
            throw new BusinessException("Spend at least " + coupon.getMinOrderSubtotal() + " to use this code");
        }
        if (coupon.getUsageLimit() != null
                && redemptionRepository.countByCouponId(coupon.getId()) >= coupon.getUsageLimit()) {
            throw new BusinessException("That code has reached its usage limit");
        }
        if (coupon.getPerCustomerLimit() != null && StringUtils.hasText(customerEmail)
                && redemptionRepository.countByCouponIdAndCustomerEmail(
                        coupon.getId(), customerEmail.trim().toLowerCase()) >= coupon.getPerCustomerLimit()) {
            throw new BusinessException("You've already used this code the maximum number of times");
        }
        return coupon;
    }

    private void apply(Coupon coupon, CouponRequest request) {
        coupon.setDescription(trimToNull(request.getDescription()));
        coupon.setMinOrderSubtotal(request.getMinOrderSubtotal());
        coupon.setMaxDiscount(request.getDiscountType() == DiscountType.PERCENT ? request.getMaxDiscount() : null);
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setPerCustomerLimit(request.getPerCustomerLimit());
        coupon.setStartsAt(request.getStartsAt());
        coupon.setEndsAt(request.getEndsAt());
        if (request.getActive() != null) {
            coupon.setActive(request.getActive());
        }
    }

    private static void validateRules(CouponRequest request) {
        BigDecimal value = request.getValue();
        if (request.getDiscountType() == DiscountType.PERCENT) {
            if (value.signum() <= 0 || value.compareTo(HUNDRED) > 0) {
                throw new BusinessException("A percentage discount must be between 0 and 100");
            }
        } else if (value.signum() <= 0) {
            throw new BusinessException("A fixed discount must be greater than zero");
        }
        if (request.getStartsAt() != null && request.getEndsAt() != null
                && !request.getEndsAt().isAfter(request.getStartsAt())) {
            throw new BusinessException("The end of the validity window must be after its start");
        }
    }

    private Coupon load(UUID id, UUID tenantId) {
        return couponRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Coupon", id));
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
