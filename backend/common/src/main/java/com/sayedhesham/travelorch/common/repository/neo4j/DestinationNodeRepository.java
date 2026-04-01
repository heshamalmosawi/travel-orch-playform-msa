package com.sayedhesham.travelorch.common.repository.neo4j;

import java.util.List;
import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sayedhesham.travelorch.common.entity.neo4j.DestinationNode;


/**
 * Neo4j Repository for DestinationNode graph operations.
 * Provides graph-specific queries for destination connections and path finding.
 */
@Repository
public interface DestinationNodeRepository extends Neo4jRepository<DestinationNode, Long> {

    /**
     * Find a destination node by its PostgreSQL ID
     */
    Optional<DestinationNode> findByPostgresId(Long postgresId);

    /**
     * Find all destination nodes in a specific country
     */
    List<DestinationNode> findByCountry(String country);

    /**
     * Find all destination nodes in a specific city
     */
    List<DestinationNode> findByCity(String city);

    /**
     * Find destination by name (case-insensitive)
     */
    @Query("MATCH (d:Destination) WHERE toLower(d.name) CONTAINS toLower($name) RETURN d")
    List<DestinationNode> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Find all directly connected destinations from a given destination
     */
    @Query("MATCH (d:Destination {postgresId: $postgresId})-[r:CONNECTS_TO]->(target:Destination) " +
           "RETURN target, r")
    List<DestinationNode> findDirectConnectionsFrom(@Param("postgresId") Long postgresId);

    /**
     * Find all destinations that connect to a given destination
     */
    @Query("MATCH (source:Destination)-[r:CONNECTS_TO]->(d:Destination {postgresId: $postgresId}) " +
           "RETURN source, r")
    List<DestinationNode> findDirectConnectionsTo(@Param("postgresId") Long postgresId);

    /**
     * Find the shortest path between two destinations (up to specified depth)
     */
    @Query("MATCH path = shortestPath((start:Destination {postgresId: $startId})-[r:CONNECTS_TO*1..$maxHops]->(end:Destination {postgresId: $endId})) " +
           "RETURN path")
    List<DestinationNode> findShortestPath(
        @Param("startId") Long startId,
        @Param("endId") Long endId,
        @Param("maxHops") Integer maxHops
    );

    /**
     * Find all paths between two destinations (limited by max hops)
     */
    @Query("MATCH path = (start:Destination {postgresId: $startId})-[r:CONNECTS_TO*1..$maxHops]->(end:Destination {postgresId: $endId}) " +
           "RETURN path LIMIT $limit")
    List<DestinationNode> findAllPaths(
        @Param("startId") Long startId,
        @Param("endId") Long endId,
        @Param("maxHops") Integer maxHops,
        @Param("limit") Integer limit
    );

    /**
     * Find destinations within a certain number of hops from a starting point
     */
    @Query("MATCH (start:Destination {postgresId: $postgresId})-[r:CONNECTS_TO*1..$hops]->(d:Destination) " +
           "RETURN DISTINCT d")
    List<DestinationNode> findDestinationsWithinHops(
        @Param("postgresId") Long postgresId,
        @Param("hops") Integer hops
    );

    /**
     * Find destinations reachable by specific transport mode
     */
    @Query("MATCH (start:Destination {postgresId: $postgresId})-[r:CONNECTS_TO]->(d:Destination) " +
           "WHERE $transportMode IN r.transportModes " +
           "RETURN d, r")
    List<DestinationNode> findByTransportMode(
        @Param("postgresId") Long postgresId,
        @Param("transportMode") String transportMode
    );

    /**
     * Find destinations within distance range from a starting point
     */
    @Query("MATCH (start:Destination {postgresId: $postgresId})-[r:CONNECTS_TO]->(d:Destination) " +
           "WHERE r.distanceKm >= $minDistance AND r.distanceKm <= $maxDistance " +
           "RETURN d, r")
    List<DestinationNode> findWithinDistanceRange(
        @Param("postgresId") Long postgresId,
        @Param("minDistance") Double minDistance,
        @Param("maxDistance") Double maxDistance
    );

    /**
     * Check if two destinations are connected (directly or indirectly)
     */
    @Query("MATCH path = (start:Destination {postgresId: $startId})-[r:CONNECTS_TO*1..$maxHops]->(end:Destination {postgresId: $endId}) " +
           "RETURN COUNT(path) > 0")
    Boolean areConnected(
        @Param("startId") Long startId,
        @Param("endId") Long endId,
        @Param("maxHops") Integer maxHops
    );

    /**
     * Count total connections for a destination
     */
    @Query("MATCH (d:Destination {postgresId: $postgresId})-[r:CONNECTS_TO]-() " +
           "RETURN COUNT(r)")
    Long countConnections(@Param("postgresId") Long postgresId);

    /**
     * Delete a destination and all its connections by PostgreSQL ID
     */
    @Query("MATCH (d:Destination {postgresId: $postgresId}) DETACH DELETE d")
    void deleteByPostgresId(@Param("postgresId") Long postgresId);
}
