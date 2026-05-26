package com.sayedhesham.travelorch.travel_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsOverviewResponse {

    private BigDecimal totalIncome;
    private long totalBookings;
    private long totalManagers;
    private long totalOrganizedTravels;
    private BigDecimal lastMonthIncome;
}
