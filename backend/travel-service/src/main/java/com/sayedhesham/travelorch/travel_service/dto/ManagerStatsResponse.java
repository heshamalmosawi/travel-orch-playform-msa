package com.sayedhesham.travelorch.travel_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerStatsResponse {

    private long totalPackages;
    private double averageRating;
    private long totalReviews;
    private long totalReports;
}
