package com.loyalsuit.modules.loyalty.application.dto;

import java.math.BigDecimal;

/**
 * A customer's loyalty standing: their points and what those points are worth as a
 * redeemable discount at the platform's point value.
 */
public record LoyaltyBalanceResponse(int points, BigDecimal redeemableValue) {
}
