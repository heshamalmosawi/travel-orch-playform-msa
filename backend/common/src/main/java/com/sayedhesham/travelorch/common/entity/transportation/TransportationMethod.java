package com.sayedhesham.travelorch.common.entity.transportation;

import com.sayedhesham.travelorch.common.entity.base.BaseEntity;
import com.sayedhesham.travelorch.common.enums.TransportationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "transportation_methods", indexes = {
    @Index(name = "idx_transportation_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
public class TransportationMethod extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransportationType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "price_per_km", precision = 10, scale = 2)
    private BigDecimal pricePerKm = BigDecimal.ZERO;
}
