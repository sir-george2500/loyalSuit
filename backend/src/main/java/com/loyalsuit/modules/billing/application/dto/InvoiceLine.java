package com.loyalsuit.modules.billing.application.dto;

import java.math.BigDecimal;

/** A single line on an invoice. */
public record InvoiceLine(String description, int quantity, BigDecimal unitPrice, BigDecimal total) {}
