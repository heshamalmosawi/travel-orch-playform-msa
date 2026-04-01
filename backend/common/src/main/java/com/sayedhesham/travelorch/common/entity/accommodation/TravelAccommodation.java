package com.sayedhesham.travelorch.common.entity.accommodation;

import com.sayedhesham.travelorch.common.entity.travel.Travel;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "travel_accommodations", indexes = {
    @Index(name = "idx_travel_accommodations_travel", columnList = "travel_id")
})
@Getter
@Setter
@NoArgsConstructor
public class TravelAccommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id", nullable = false)
    private Travel travel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @NotNull
    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @NotNull
    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @NotNull
    @Min(1)
    @Column(name = "number_of_rooms", nullable = false)
    private Integer numberOfRooms;

    @NotNull
    @Column(name = "price_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePaid;

    @Column(name = "booking_reference", length = 100)
    private String bookingReference;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
