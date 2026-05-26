package com.sayedhesham.travelorch.travel_service.service;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sayedhesham.travelorch.common.repository.neo4j.TravelGraphRepository;
import com.sayedhesham.travelorch.travel_service.dto.DestinationResponse;
import com.sayedhesham.travelorch.travel_service.dto.TravelDestinationResponse;
import com.sayedhesham.travelorch.travel_service.dto.TravelResponse;

import lombok.RequiredArgsConstructor;

/**
 * Mirrors travel/feedback changes into the Neo4j recommendation graph. Every method is best-effort:
 * a graph failure is logged and swallowed so it never breaks the owning Postgres transaction.
 */
@Service
@RequiredArgsConstructor
public class GraphSyncService {

    private static final Logger log = LoggerFactory.getLogger(GraphSyncService.class);

    private final TravelGraphRepository travelGraphRepository;

    public void syncTravel(TravelResponse travel) {
        try {
            Double price = travel.getTotalPrice() != null ? travel.getTotalPrice().doubleValue() : null;
            Long startEpochDay = travel.getStartDate() != null ? travel.getStartDate().toEpochDay() : null;
            String status = travel.getStatus() != null ? travel.getStatus().name() : null;

            travelGraphRepository.upsertTravel(
                    travel.getId(), travel.getTitle(), travel.getManagerId(), price, startEpochDay, status);
            travelGraphRepository.clearVisits(travel.getId());

            List<String> countries = travel.getDestinations() == null ? List.of()
                    : travel.getDestinations().stream()
                            .map(TravelDestinationResponse::getDestination)
                            .filter(Objects::nonNull)
                            .map(DestinationResponse::getCountry)
                            .filter(c -> c != null && !c.isBlank())
                            .distinct()
                            .toList();
            if (!countries.isEmpty()) {
                travelGraphRepository.linkCountries(travel.getId(), countries);
            }
        } catch (Exception e) {
            log.warn("syncTravel - graph sync failed for travelId {}: {}", travel.getId(), e.getMessage());
        }
    }

    public void removeTravel(Long travelId) {
        try {
            travelGraphRepository.deleteTravel(travelId);
        } catch (Exception e) {
            log.warn("removeTravel - graph delete failed for travelId {}: {}", travelId, e.getMessage());
        }
    }

    public void recordReview(Long userId, Long travelId, Integer rating) {
        if (userId == null || travelId == null) {
            return;
        }
        try {
            travelGraphRepository.recordReview(userId, travelId, rating);
        } catch (Exception e) {
            log.warn("recordReview - graph write failed for userId {} travelId {}: {}", userId, travelId, e.getMessage());
        }
    }

    public void removeReview(Long userId, Long travelId) {
        if (userId == null || travelId == null) {
            return;
        }
        try {
            travelGraphRepository.deleteReview(userId, travelId);
        } catch (Exception e) {
            log.warn("removeReview - graph delete failed for userId {} travelId {}: {}", userId, travelId, e.getMessage());
        }
    }
}
