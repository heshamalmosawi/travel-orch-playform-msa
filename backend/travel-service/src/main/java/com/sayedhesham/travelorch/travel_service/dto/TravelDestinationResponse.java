package com.sayedhesham.travelorch.travel_service.dto;

import com.sayedhesham.travelorch.common.entity.travel.TravelDestination;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelDestinationResponse {

    private Long id;
    private Long destinationId;
    private Integer visitOrder;
    private LocalDate arrivalDate;
    private LocalDate departureDate;
    private String notes;
    private DestinationResponse destination;
    private LocalDateTime createdAt;

    public static TravelDestinationResponse fromEntity(TravelDestination td) {
        return TravelDestinationResponse.builder()
                .id(td.getId())
                .destinationId(td.getDestination() != null ? td.getDestination().getId() : null)
                .visitOrder(td.getVisitOrder())
                .arrivalDate(td.getArrivalDate())
                .departureDate(td.getDepartureDate())
                .notes(td.getNotes())
                .destination(td.getDestination() != null ? DestinationResponse.fromEntity(td.getDestination()) : null)
                .createdAt(td.getCreatedAt())
                .build();
    }
}
