package com.sayedhesham.travelorch.common.repository.neo4j;

import com.sayedhesham.travelorch.common.entity.neo4j.TravelNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelGraphRepository extends Neo4jRepository<TravelNode, Long> {

    @Query("MERGE (t:TravelNode {travelId: $travelId}) " +
           "SET t.title = $title, t.managerId = $managerId, t.totalPrice = $totalPrice, " +
           "t.startDateEpochDay = $startDateEpochDay, t.status = $status")
    void upsertTravel(
            @Param("travelId") Long travelId,
            @Param("title") String title,
            @Param("managerId") Long managerId,
            @Param("totalPrice") Double totalPrice,
            @Param("startDateEpochDay") Long startDateEpochDay,
            @Param("status") String status);

    @Query("MATCH (t:TravelNode {travelId: $travelId})-[r:VISITS]->() DELETE r")
    void clearVisits(@Param("travelId") Long travelId);

    @Query("UNWIND $countries AS c " +
           "MERGE (co:Country {name: c}) " +
           "WITH co, c " +
           "MATCH (t:TravelNode {travelId: $travelId}) " +
           "MERGE (t)-[:VISITS]->(co)")
    void linkCountries(@Param("travelId") Long travelId, @Param("countries") List<String> countries);

    @Query("MATCH (t:TravelNode {travelId: $travelId}) DETACH DELETE t")
    void deleteTravel(@Param("travelId") Long travelId);

    @Query("MERGE (u:User {userId: $userId}) " +
           "MERGE (t:TravelNode {travelId: $travelId}) " +
           "MERGE (u)-[:PURCHASED]->(t)")
    void recordPurchase(@Param("userId") Long userId, @Param("travelId") Long travelId);

    @Query("MERGE (u:User {userId: $userId}) " +
           "MERGE (t:TravelNode {travelId: $travelId}) " +
           "MERGE (u)-[r:REVIEWED]->(t) SET r.rating = $rating")
    void recordReview(
            @Param("userId") Long userId,
            @Param("travelId") Long travelId,
            @Param("rating") Integer rating);

    @Query("MATCH (:User {userId: $userId})-[r:REVIEWED]->(:TravelNode {travelId: $travelId}) DELETE r")
    void deleteReview(@Param("userId") Long userId, @Param("travelId") Long travelId);

    @Query("MATCH (u:User {userId: $userId})-[r:PURCHASED|REVIEWED]->(seed:TravelNode) " +
           "WITH u, seed, coalesce(r.rating, 3) AS weight " +
           "MATCH (seed)-[:VISITS]->(c:Country)<-[:VISITS]-(rec:TravelNode) " +
           "WHERE rec <> seed AND rec.status <> 'cancelled' AND rec.startDateEpochDay >= $today " +
           "  AND NOT (u)-[:PURCHASED]->(rec) " +
           "WITH rec, sum(weight * (1 " +
           "     + CASE WHEN rec.managerId = seed.managerId THEN 1 ELSE 0 END " +
           "     + CASE WHEN abs(rec.totalPrice - seed.totalPrice) <= $priceTolerance THEN 1 ELSE 0 END)) AS score " +
           "RETURN rec.travelId AS travelId ORDER BY score DESC LIMIT $limit")
    List<Long> findRecommendedTravelIds(
            @Param("userId") Long userId,
            @Param("today") Long today,
            @Param("priceTolerance") Double priceTolerance,
            @Param("limit") Integer limit);
}
