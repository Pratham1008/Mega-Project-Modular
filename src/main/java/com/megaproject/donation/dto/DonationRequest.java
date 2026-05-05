package com.megaproject.donation.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DonationRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Minimum donation is ₹1")
    private Double amount;

    @NotBlank(message = "Purpose is required")
    private String purpose;

    private String message;
}
