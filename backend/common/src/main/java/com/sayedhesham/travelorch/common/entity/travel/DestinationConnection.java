package com.sayedhesham.travelorch.common.entity.travel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "destination_connections", indexes = {
    @Index(name = "idx_connections_from", columnList = "from_destination_id"),
    @Index(name = "idx_connections_to", columnList = "to_destination_id")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"from_destination_id", "to_destination_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class DestinationConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_destination_id", nullable = false)
    private Destination fromDestination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_destination_id", nullable = false)
    private Destination toDestination;

    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "travel_time_hours", precision = 5, scale = 2)
    private BigDecimal travelTimeHours;

    @ElementCollection
    @CollectionTable(name = "destination_connection_transport_modes", 
                     joinColumns = @JoinColumn(name = "connection_id"))
    @Column(name = "transport_mode")
    private List<String> transportModes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
