package com.loyalsuit.modules.promotions.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CouponTest {

    private Coupon percent(String value) {
        return new Coupon(UUID.randomUUID(), "save", DiscountType.PERCENT, new BigDecimal(value));
    }

    private Coupon fixed(String value) {
        return new Coupon(UUID.randomUUID(), "save", DiscountType.FIXED_AMOUNT, new BigDecimal(value));
    }

    @Test
    void discountFor_percent_takesThePercentageOfSubtotal() {
        assertThat(percent("10").discountFor(new BigDecimal("100.00"))).isEqualByComparingTo("10.00");
    }

    @Test
    void discountFor_percent_isCappedByMaxDiscount() {
        // Arrange — 50% of 100 = 50, but capped at 30
        Coupon coupon = percent("50");
        coupon.setMaxDiscount(new BigDecimal("30.00"));

        // Act & Assert
        assertThat(coupon.discountFor(new BigDecimal("100.00"))).isEqualByComparingTo("30.00");
    }

    @Test
    void discountFor_neverExceedsTheSubtotal() {
        // A $20 fixed coupon on a $15 cart only takes off $15.
        assertThat(fixed("20").discountFor(new BigDecimal("15.00"))).isEqualByComparingTo("15.00");
    }

    @Test
    void code_isStoredUpperCasedAndTrimmed() {
        assertThat(new Coupon(UUID.randomUUID(), "  save10 ", DiscountType.PERCENT, BigDecimal.TEN).getCode())
                .isEqualTo("SAVE10");
    }

    @Test
    void isWithinWindow_respectsOpenEndedBounds() {
        Coupon coupon = percent("10");
        assertThat(coupon.isWithinWindow(Instant.now())).isTrue(); // no window = always valid

        coupon.setEndsAt(Instant.now().minusSeconds(60));
        assertThat(coupon.isWithinWindow(Instant.now())).isFalse(); // ended
    }
}
