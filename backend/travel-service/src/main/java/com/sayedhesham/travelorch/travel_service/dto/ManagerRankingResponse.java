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
public class ManagerRankingResponse {

    private Long managerId;
    private String name;
    private String email;
    private long organizedTravels;
    private long totalBookings;
    private BigDecimal totalRevenue;
    private double averageRating;
    private long totalReviews;
    private long totalReports;
    private double performanceScore;
}
