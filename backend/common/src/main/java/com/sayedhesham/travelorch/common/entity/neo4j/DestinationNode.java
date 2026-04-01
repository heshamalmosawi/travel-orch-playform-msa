package com.sayedhesham.travelorch.common.entity.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Neo4j Node representing a travel destination.
 * This mirrors the PostgreSQL Destination entity for graph-based queries.
 * The postgresId field links back to the relational database.
 */
@Node("Destination")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinationNode {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * Reference to the PostgreSQL destination ID for data synchronization
     */
    @Property("postgresId")
    private Long postgresId;

    @Property("name")
    private String name;

    @Property("country")
    private String country;

    @Property("city")
    private String city;

    @Property("region")
    private String region;

    @Property("latitude")
    private Double latitude;

    @Property("longitude")
    private Double longitude;

    @Property("createdAt")
    private LocalDateTime createdAt;

    @Property("updatedAt")
    private LocalDateTime updatedAt;

    /**
     * Outgoing connections to other destinations.
     * Each connection has properties like distance, travel time, and transport modes.
     */
    @Relationship(type = "CONNECTS_TO", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private List<DestinationConnection> connectionsTo = new ArrayList<>();

    /**
     * Incoming connections from other destinations.
     */
    @Relationship(type = "CONNECTS_TO", direction = Relationship.Direction.INCOMING)
    @Builder.Default
    private List<DestinationConnection> connectionsFrom = new ArrayList<>();

    /**
     * Helper method to add an outgoing connection
     */
    public void addConnectionTo(DestinationConnection connection) {
        if (this.connectionsTo == null) {
            this.connectionsTo = new ArrayList<>();
        }
        this.connectionsTo.add(connection);
    }

    /**
     * Helper method to remove an outgoing connection
     */
    public void removeConnectionTo(DestinationConnection connection) {
        if (this.connectionsTo != null) {
            this.connectionsTo.remove(connection);
        }
    }
}
