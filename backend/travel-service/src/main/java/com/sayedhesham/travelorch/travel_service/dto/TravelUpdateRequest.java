package com.sayedhesham.travelorch.travel_service.dto;

import com.sayedhesham.travelorch.common.enums.TravelStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelUpdateRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 10000, message = "Description must not exceed 10000 characters")
    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer durationDays;

    private BigDecimal totalPrice;

    private TravelStatus status;
}
