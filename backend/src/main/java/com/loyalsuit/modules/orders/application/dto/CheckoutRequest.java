package com.loyalsuit.modules.orders.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/** Guest checkout details. Payment is cash, so no card data is collected. */
@Data
public class CheckoutRequest {

    /** The delivery zone the customer chose; its fee becomes shipping. Null = no delivery fee. */
    private UUID deliveryZoneId;

    /** A discount code to apply; the server re-validates and prices it. Null = no coupon. */
    @Size(max = 64)
    private String couponCode;

    @NotBlank(message = "Your name is required")
    @Size(max = 255)
    private String customerName;

    @Email(message = "Enter a valid email address")
    private String customerEmail;

    @Size(max = 40)
    private String customerPhone;

    @NotBlank(message = "A delivery address is required")
    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 120)
    private String city;

    @Size(max = 120)
    private String state;

    @Size(max = 20)
    private String postalCode;

    @Size(max = 120)
    private String country;

    @Size(max = 1000)
    private String notes;
}
