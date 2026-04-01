package com.sayedhesham.travelorch.common.entity.transportation;

import com.sayedhesham.travelorch.common.entity.travel.Destination;
import com.sayedhesham.travelorch.common.entity.travel.Travel;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "transportation_segments", indexes = {
    @Index(name = "idx_transportation_travel", columnList = "travel_id")
})
@Getter
@Setter
@NoArgsConstructor
public class TransportationSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id", nullable = false)
    private Travel travel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_destination_id", nullable = false)
    private Destination fromDestination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_destination_id", nullable = false)
    private Destination toDestination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transportation_method_id", nullable = false)
    private TransportationMethod transportationMethod;

    @NotNull
    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @NotNull
    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @NotNull
    @Column(name = "arrival_date", nullable = false)
    private LocalDate arrivalDate;

    @NotNull
    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @NotNull
    @Column(name = "price_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePaid;

    @Column(name = "booking_reference", length = 100)
    private String bookingReference;

    @ElementCollection
    @CollectionTable(name = "transportation_segment_seats", 
                     joinColumns = @JoinColumn(name = "segment_id"))
    @Column(name = "seat_number")
    private List<String> seatNumbers;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
