package com.sayedhesham.travelorch.travel_service.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Key stats for the logged-in travel manager's dashboard:
 * total income (sum of completed payments on their travels),
 * number of organized trips, and number of travelers (completed bookings).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardResponse {

    private BigDecimal totalIncome;
    private long totalTravels;
    private long totalTravelers;
}
