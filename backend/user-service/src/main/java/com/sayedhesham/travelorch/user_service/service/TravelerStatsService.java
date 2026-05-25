package com.sayedhesham.travelorch.user_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.sayedhesham.travelorch.common.entity.payment.PaymentTransaction;
import com.sayedhesham.travelorch.common.entity.payment.PaymentMethod;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.enums.PaymentStatus;
import com.sayedhesham.travelorch.common.repository.feedback.TravelFeedbackRepository;
import com.sayedhesham.travelorch.common.repository.payment.PaymentTransactionRepository;
import com.sayedhesham.travelorch.common.repository.report.ManagerReportRepository;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.user_service.dto.TravelerStatsResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TravelerStatsService {

    private static final Logger log = LoggerFactory.getLogger(TravelerStatsService.class);

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final TravelFeedbackRepository travelFeedbackRepository;
    private final ManagerReportRepository managerReportRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    public Mono<TravelerStatsResponse> getStats(Long userId, String currentUsername) {
        log.info("getStats - Computing stats for user id: {} by user: {}", userId, currentUsername);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found: " + currentUsername));

            boolean isOwner = targetUser.getUsername().equals(currentUsername);
            boolean isAdmin = currentUser.isAdmin();
            boolean isManager = currentUser.hasRole("travel_manager");
            log.debug("getStats - isOwner: {}, isAdmin: {}, isManager: {}", isOwner, isAdmin, isManager);

            if (!isOwner && !isAdmin && !isManager) {
                log.warn("getStats - User {} denied access to stats for user id: {}", currentUsername, userId);
                throw new SecurityException("You do not have permission to view these statistics");
            }

            List<PaymentTransaction> transactions = paymentTransactionRepository.findByBuyerId(userId);

            long completed = transactions.stream()
                    .filter(t -> t.getStatus() == PaymentStatus.completed)
                    .count();

            long cancellations = transactions.stream()
                    .filter(t -> t.getStatus() == PaymentStatus.refunded)
                    .count();

            BigDecimal totalSpent = transactions.stream()
                    .filter(t -> t.getStatus() == PaymentStatus.completed)
                    .map(PaymentTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            var feedbacks = travelFeedbackRepository.findByReviewerId(userId);
            long totalReviews = feedbacks.size();
            double avgRating = feedbacks.stream()
                    .mapToInt(f -> f.getRating() != null ? f.getRating() : 0)
                    .average()
                    .orElse(0.0);

            long reportsFiled = managerReportRepository.countByReporterId(userId);

            Map<String, Long> preferredMethods = transactions.stream()
                    .filter(t -> t.getPaymentMethod() != null)
                    .collect(Collectors.groupingBy(
                            t -> t.getPaymentMethod().getName(),
                            Collectors.counting()
                    ));

            log.info("getStats - Stats computed for user id: {} (trips: {}, cancellations: {}, spent: {})",
                    userId, completed, cancellations, totalSpent);

            return TravelerStatsResponse.builder()
                    .totalTripsCompleted(completed)
                    .totalCancellations(cancellations)
                    .totalSpent(totalSpent)
                    .averageRatingGiven(avgRating)
                    .totalReviewsGiven(totalReviews)
                    .totalReportsFiled(reportsFiled)
                    .preferredPaymentMethods(preferredMethods)
                    .build();
        })).subscribeOn(Schedulers.boundedElastic());
    }
}
