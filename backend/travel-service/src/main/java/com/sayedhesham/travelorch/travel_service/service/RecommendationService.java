package com.sayedhesham.travelorch.travel_service.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.sayedhesham.travelorch.common.entity.feedback.TravelFeedback;
import com.sayedhesham.travelorch.common.entity.payment.PaymentTransaction;
import com.sayedhesham.travelorch.common.entity.travel.Travel;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.enums.PaymentStatus;
import com.sayedhesham.travelorch.common.enums.TravelStatus;
import com.sayedhesham.travelorch.common.repository.feedback.TravelFeedbackRepository;
import com.sayedhesham.travelorch.common.repository.neo4j.TravelGraphRepository;
import com.sayedhesham.travelorch.common.repository.payment.PaymentTransactionRepository;
import com.sayedhesham.travelorch.common.repository.travel.TravelRepository;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.travel_service.dto.TravelResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private static final int RECOMMENDATION_LIMIT = 5;
    private static final double PRICE_TOLERANCE = 500.0;

    private final TravelGraphRepository travelGraphRepository;
    private final TravelRepository travelRepository;
    private final TravelFeedbackRepository travelFeedbackRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    public Flux<TravelResponse> getRecommendations(String currentUsername) {
        log.info("getRecommendations - for user: {}", currentUsername);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User user = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

            long today = LocalDate.now().toEpochDay();
            List<Long> graphIds = travelGraphRepository.findRecommendedTravelIds(
                    user.getId(), today, PRICE_TOLERANCE, RECOMMENDATION_LIMIT);

            if (graphIds != null && !graphIds.isEmpty()) {
                log.info("getRecommendations - graph returned {} ids for user {}", graphIds.size(), currentUsername);
                return loadInOrder(graphIds);
            }

            log.info("getRecommendations - empty graph, falling back to top-rated for user {}", currentUsername);
            return coldStart(user.getId());
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    private List<TravelResponse> loadInOrder(List<Long> travelIds) {
        return travelIds.stream()
                .map(travelRepository::findByIdWithDestinations)
                .filter(java.util.Objects::nonNull)
                .map(TravelResponse::fromEntity)
                .toList();
    }

    private List<TravelResponse> coldStart(Long userId) {
        Set<Long> purchasedTravelIds = paymentTransactionRepository.findByBuyerId(userId).stream()
                .filter(t -> t.getStatus() == PaymentStatus.completed && t.getTravel() != null)
                .map(t -> t.getTravel().getId())
                .collect(Collectors.toSet());

        return travelRepository.findAllUpcomingTravels(LocalDate.now(), TravelStatus.cancelled).stream()
                .filter(t -> !purchasedTravelIds.contains(t.getId()))
                .sorted(Comparator.comparingDouble(this::averageRating).reversed())
                .limit(RECOMMENDATION_LIMIT)
                .map(TravelResponse::fromEntity)
                .toList();
    }

    private double averageRating(Travel travel) {
        return travelFeedbackRepository.findByTravelId(travel.getId()).stream()
                .mapToInt(TravelFeedback::getRating)
                .average()
                .orElse(0.0);
    }

    @PreAuthorize("hasPermission('admin', 'all')")
    public Mono<Void> backfill() {
        log.info("backfill - rebuilding recommendation graph from Postgres");
        return Mono.fromRunnable(() -> transactionTemplate.executeWithoutResult(status -> {
            int travels = 0;
            for (Travel travel : travelRepository.findAll()) {
                syncTravelGraph(travel);
                travels++;
            }

            int purchases = 0;
            for (PaymentTransaction tx : paymentTransactionRepository.findByStatus(PaymentStatus.completed)) {
                if (tx.getBuyer() == null || tx.getTravel() == null) {
                    continue;
                }
                try {
                    travelGraphRepository.recordPurchase(tx.getBuyer().getId(), tx.getTravel().getId());
                    purchases++;
                } catch (Exception e) {
                    log.warn("backfill - purchase edge failed for tx {}: {}", tx.getId(), e.getMessage());
                }
            }

            int reviews = 0;
            for (TravelFeedback feedback : travelFeedbackRepository.findAll()) {
                if (feedback.getReviewer() == null) {
                    continue;
                }
                try {
                    travelGraphRepository.recordReview(
                            feedback.getReviewer().getId(), feedback.getTravelId(), feedback.getRating());
                    reviews++;
                } catch (Exception e) {
                    log.warn("backfill - review edge failed for feedback {}: {}", feedback.getId(), e.getMessage());
                }
            }

            log.info("backfill - synced {} travels, {} purchases, {} reviews", travels, purchases, reviews);
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private void syncTravelGraph(Travel travel) {
        try {
            Double price = travel.getTotalPrice() != null ? travel.getTotalPrice().doubleValue() : null;
            Long startEpochDay = travel.getStartDate() != null ? travel.getStartDate().toEpochDay() : null;
            String status = travel.getStatus() != null ? travel.getStatus().name() : null;
            Long managerId = travel.getManager() != null ? travel.getManager().getId() : null;

            travelGraphRepository.upsertTravel(
                    travel.getId(), travel.getTitle(), managerId, price, startEpochDay, status);
            travelGraphRepository.clearVisits(travel.getId());

            List<String> countries = travel.getDestinations().stream()
                    .map(td -> td.getDestination() != null ? td.getDestination().getCountry() : null)
                    .filter(c -> c != null && !c.isBlank())
                    .distinct()
                    .toList();
            if (!countries.isEmpty()) {
                travelGraphRepository.linkCountries(travel.getId(), countries);
            }
        } catch (Exception e) {
            log.warn("backfill - travel sync failed for travelId {}: {}", travel.getId(), e.getMessage());
        }
    }
}
