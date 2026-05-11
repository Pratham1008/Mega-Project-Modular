package com.megaproject.donation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private String orderId;
    private double amount;
    private String currency;
    private String keyId; // Provide the key ID to the frontend to initialize checkout
}
