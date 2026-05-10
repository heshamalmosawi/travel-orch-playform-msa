package com.sayedhesham.travelorch.travel_service.dto;

import com.sayedhesham.travelorch.common.entity.travel.Travel;
import com.sayedhesham.travelorch.common.enums.TravelStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelResponse {

    private Long id;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;
    private BigDecimal totalPrice;
    private TravelStatus status;
    private Long userId;
    private List<TravelDestinationResponse> destinations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TravelResponse fromEntity(Travel travel) {
        return TravelResponse.builder()
                .id(travel.getId())
                .title(travel.getTitle())
                .description(travel.getDescription())
                .startDate(travel.getStartDate())
                .endDate(travel.getEndDate())
                .durationDays(travel.getDurationDays())
                .totalPrice(travel.getTotalPrice())
                .status(travel.getStatus())
                .userId(travel.getUser() != null ? travel.getUser().getId() : null)
                .destinations(travel.getDestinations() != null
                        ? travel.getDestinations().stream()
                        .map(TravelDestinationResponse::fromEntity)
                        .toList()
                        : List.of())
                .createdAt(travel.getCreatedAt())
                .updatedAt(travel.getUpdatedAt())
                .build();
    }
}
