package com.sayedhesham.travelorch.travel_service.service;

import com.sayedhesham.travelorch.common.entity.feedback.TravelFeedback;
import com.sayedhesham.travelorch.common.entity.travel.Travel;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.enums.PaymentStatus;
import com.sayedhesham.travelorch.common.repository.feedback.TravelFeedbackRepository;
import com.sayedhesham.travelorch.common.repository.payment.PaymentTransactionRepository;
import com.sayedhesham.travelorch.common.repository.travel.TravelRepository;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.travel_service.dto.FeedbackCreateRequest;
import com.sayedhesham.travelorch.travel_service.dto.FeedbackResponse;
import com.sayedhesham.travelorch.travel_service.dto.FeedbackUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    private final TravelFeedbackRepository feedbackRepository;
    private final TravelRepository travelRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    public Mono<FeedbackResponse> createFeedback(String username, FeedbackCreateRequest request) {
        Long travelId = request.getTravelId();
        log.info("createFeedback - travelId: {} by user: {}", travelId, username);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User reviewer = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

            Travel travel = travelRepository.findById(travelId)
                    .orElseThrow(() -> new IllegalArgumentException("Travel not found with id: " + travelId));

            boolean hasParticipated = paymentTransactionRepository
                    .findByBuyerIdAndTravelId(reviewer.getId(), travelId)
                    .stream()
                    .anyMatch(t -> t.getStatus() == PaymentStatus.completed);

            if (!hasParticipated) {
                log.warn("createFeedback - User {} has no completed purchase for travel {}", username, travelId);
                throw new SecurityException("You must have a completed purchase to review this travel");
            }

            if (feedbackRepository.existsByTravelIdAndReviewerId(travelId, reviewer.getId())) {
                log.warn("createFeedback - User {} already reviewed travel {}", username, travelId);
                throw new IllegalStateException("You have already reviewed this travel package");
            }

            TravelFeedback feedback = new TravelFeedback();
            feedback.setTravelId(travelId);
            feedback.setManagerId(travel.getManager().getId());
            feedback.setReviewer(reviewer);
            feedback.setRating(request.getRating());
            feedback.setComment(request.getComment());

            TravelFeedback saved = feedbackRepository.save(feedback);
            log.info("createFeedback - Created feedback id: {}", saved.getId());
            return FeedbackResponse.fromEntity(saved);
        }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<FeedbackResponse> getFeedbacksForTravel(Long travelId) {
        log.info("getFeedbacksForTravel - travelId: {}", travelId);
        return Mono.fromCallable(() -> transactionTemplate.execute(status ->
                feedbackRepository.findByTravelId(travelId).stream()
                        .map(FeedbackResponse::fromEntity)
                        .toList()
        ))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getFeedbacksForTravel - Found {} feedbacks", list.size()))
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<FeedbackResponse> getMyFeedbackForTravel(Long travelId, String username) {
        log.info("getMyFeedbackForTravel - travelId: {} user: {}", travelId, username);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User reviewer = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
            return feedbackRepository.findByTravelIdAndReviewerId(travelId, reviewer.getId())
                    .map(FeedbackResponse::fromEntity)
                    .orElse(null);
        }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PreAuthorize("hasPermission('feedbacks', 'read')")
    public Flux<FeedbackResponse> getFeedbacksForManager(Long managerId) {
        log.info("getFeedbacksForManager - managerId: {}", managerId);
        return Mono.fromCallable(() -> transactionTemplate.execute(status ->
                feedbackRepository.findByManagerId(managerId).stream()
                        .map(FeedbackResponse::fromEntity)
                        .toList()
        ))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getFeedbacksForManager - Found {} feedbacks", list.size()))
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<FeedbackResponse> updateFeedback(Long feedbackId, String username, FeedbackUpdateRequest request) {
        log.info("updateFeedback - feedbackId: {} by user: {}", feedbackId, username);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            TravelFeedback feedback = feedbackRepository.findById(feedbackId)
                    .orElseThrow(() -> new IllegalArgumentException("Feedback not found with id: " + feedbackId));

            if (feedback.getReviewer() == null || !feedback.getReviewer().getUsername().equals(username)) {
                log.warn("updateFeedback - User {} denied updating feedback {}", username, feedbackId);
                throw new SecurityException("You can only edit your own reviews");
            }

            if (request.getRating() != null) {
                feedback.setRating(request.getRating());
            }
            if (request.getComment() != null) {
                feedback.setComment(request.getComment());
            }

            TravelFeedback updated = feedbackRepository.save(feedback);
            log.info("updateFeedback - Updated feedback id: {}", feedbackId);
            return FeedbackResponse.fromEntity(updated);
        }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteFeedback(Long feedbackId, String username) {
        log.info("deleteFeedback - feedbackId: {} by user: {}", feedbackId, username);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            TravelFeedback feedback = feedbackRepository.findById(feedbackId)
                    .orElseThrow(() -> new IllegalArgumentException("Feedback not found with id: " + feedbackId));

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

            boolean isReviewer = feedback.getReviewer() != null
                    && feedback.getReviewer().getId().equals(user.getId());
            boolean canManageAny = hasPermission(user, "feedbacks", "write");

            if (!isReviewer && !canManageAny) {
                log.warn("deleteFeedback - User {} denied deleting feedback {}", username, feedbackId);
                throw new SecurityException("You do not have permission to delete this review");
            }

            feedbackRepository.delete(feedback);
            log.info("deleteFeedback - Deleted feedback id: {}", feedbackId);
            return null;
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private boolean hasPermission(User user, String resource, String action) {
        return user.getRole() != null && user.getRole().getPermissions().stream()
                .anyMatch(p -> resource.equalsIgnoreCase(p.getResource())
                        && action.equalsIgnoreCase(p.getAction()));
    }
}
