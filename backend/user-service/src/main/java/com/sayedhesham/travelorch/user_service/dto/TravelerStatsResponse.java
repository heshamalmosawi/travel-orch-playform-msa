package com.sayedhesham.travelorch.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelerStatsResponse {

    private long totalTripsCompleted;
    private long totalCancellations;
    private BigDecimal totalSpent;
    private double averageRatingGiven;
    private long totalReviewsGiven;
    private long totalReportsFiled;
    private Map<String, Long> preferredPaymentMethods;
}
