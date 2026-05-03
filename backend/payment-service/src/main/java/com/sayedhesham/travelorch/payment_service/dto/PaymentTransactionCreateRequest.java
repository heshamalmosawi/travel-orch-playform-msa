package com.sayedhesham.travelorch.payment_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionCreateRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @Size(max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency;

    @NotNull(message = "Travel ID is required")
    @Positive(message = "Travel ID must be positive")
    private Long travelId;
}
