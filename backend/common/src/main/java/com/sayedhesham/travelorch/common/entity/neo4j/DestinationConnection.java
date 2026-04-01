package com.sayedhesham.travelorch.common.entity.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Neo4j Relationship Properties for connections between destinations.
 * This represents the CONNECTS_TO relationship with its properties.
 */
@RelationshipProperties
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinationConnection {

    @RelationshipId
    private Long id;

    /**
     * Reference to the PostgreSQL destination_connections ID for data synchronization
     */
    private Long postgresId;

    /**
     * The target destination node
     */
    @TargetNode
    private DestinationNode targetDestination;

    /**
     * Distance in kilometers between the two destinations
     */
    private BigDecimal distanceKm;

    /**
     * Estimated travel time in hours
     */
    private BigDecimal travelTimeHours;

    /**
     * Available transport modes for this connection (e.g., FLIGHT, TRAIN, BUS, CAR)
     */
    private List<String> transportModes;

    /**
     * Indicates if this connection is currently active
     */
    private Boolean isActive;

    /**
     * When this connection was created
     */
    private LocalDateTime createdAt;

    /**
     * When this connection was last updated
     */
    private LocalDateTime updatedAt;
}
