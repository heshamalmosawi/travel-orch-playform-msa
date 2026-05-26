package com.sayedhesham.travelorch.common.document;

import com.sayedhesham.travelorch.common.entity.travel.Destination;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "destinations")
public class DestinationDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String description;

    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String country;

    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String city;

    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String region;

    @Field(type = FieldType.Double)
    private BigDecimal latitude;

    @Field(type = FieldType.Double)
    private BigDecimal longitude;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static DestinationDocument fromEntity(Destination destination) {
        return DestinationDocument.builder()
                .id(destination.getId())
                .name(destination.getName())
                .description(destination.getDescription())
                .country(destination.getCountry())
                .city(destination.getCity())
                .region(destination.getRegion())
                .latitude(destination.getLatitude())
                .longitude(destination.getLongitude())
                .createdAt(destination.getCreatedAt())
                .updatedAt(destination.getUpdatedAt())
                .build();
    }
}
