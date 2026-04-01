package com.sayedhesham.travelorch.common.entity.travel;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "travel_destinations", indexes = {
    @Index(name = "idx_travel_destinations_travel", columnList = "travel_id"),
    @Index(name = "idx_travel_destinations_destination", columnList = "destination_id")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"travel_id", "destination_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class TravelDestination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id", nullable = false)
    private Travel travel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @NotNull
    @Min(1)
    @Column(name = "visit_order", nullable = false)
    private Integer visitOrder;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
