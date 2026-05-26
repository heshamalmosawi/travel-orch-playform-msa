package com.sayedhesham.travelorch.travel_service.dto;

import com.sayedhesham.travelorch.common.enums.TravelStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelRankingResponse {

    private Long travelId;
    private String title;
    private String managerName;
    private long bookings;
    private BigDecimal revenue;
    private double averageRating;
    private long totalReviews;
    private TravelStatus status;
    private double performanceScore;
}
