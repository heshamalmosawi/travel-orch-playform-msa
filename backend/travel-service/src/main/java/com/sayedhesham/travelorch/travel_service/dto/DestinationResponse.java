package com.sayedhesham.travelorch.travel_service.dto;

import com.sayedhesham.travelorch.common.document.DestinationDocument;
import com.sayedhesham.travelorch.common.entity.travel.Destination;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinationResponse {

    private Long id;
    private String name;
    private String description;
    private String country;
    private String city;
    private String region;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String imageBase64;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DestinationResponse fromEntity(Destination destination) {
        return DestinationResponse.builder()
                .id(destination.getId())
                .name(destination.getName())
                .description(destination.getDescription())
                .country(destination.getCountry())
                .city(destination.getCity())
                .region(destination.getRegion())
                .latitude(destination.getLatitude())
                .longitude(destination.getLongitude())
                .imageBase64(destination.getImageBase64())
                .createdAt(destination.getCreatedAt())
                .updatedAt(destination.getUpdatedAt())
                .build();
    }

    public static DestinationResponse fromDocument(DestinationDocument doc) {
        return DestinationResponse.builder()
                .id(doc.getId())
                .name(doc.getName())
                .description(doc.getDescription())
                .country(doc.getCountry())
                .city(doc.getCity())
                .region(doc.getRegion())
                .latitude(doc.getLatitude())
                .longitude(doc.getLongitude())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
