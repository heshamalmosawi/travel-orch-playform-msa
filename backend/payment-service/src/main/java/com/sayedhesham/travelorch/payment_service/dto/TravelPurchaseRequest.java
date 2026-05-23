package com.sayedhesham.travelorch.payment_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPurchaseRequest {

    @NotNull(message = "Travel ID is required")
    @Positive(message = "Travel ID must be positive")
    private Long travelId;
}
