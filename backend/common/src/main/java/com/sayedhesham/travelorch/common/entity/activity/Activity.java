package com.sayedhesham.travelorch.common.entity.activity;

import com.sayedhesham.travelorch.common.entity.base.BaseEntity;
import com.sayedhesham.travelorch.common.entity.travel.Destination;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "activities", indexes = {
    @Index(name = "idx_activities_destination", columnList = "destination_id"),
    @Index(name = "idx_activities_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
public class Activity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Size(max = 100)
    @Column(length = 100)
    private String category;

    @Column(name = "duration_hours", precision = 5, scale = 2)
    private BigDecimal durationHours;

    @Column(precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "age_restriction")
    private Integer ageRestriction;

    @Column(name = "image_base64", columnDefinition = "TEXT")
    private String imageBase64;
}
