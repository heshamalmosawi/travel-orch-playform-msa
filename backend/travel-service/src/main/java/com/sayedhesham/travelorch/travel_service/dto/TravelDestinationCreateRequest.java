package com.sayedhesham.travelorch.travel_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelDestinationCreateRequest {

    @NotNull(message = "Destination ID is required")
    private Long destinationId;

    @NotNull(message = "Visit order is required")
    @Min(value = 1, message = "Visit order must be at least 1")
    private Integer visitOrder;

    private LocalDate arrivalDate;

    private LocalDate departureDate;

    private String notes;
}
