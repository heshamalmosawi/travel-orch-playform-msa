package com.sayedhesham.travelorch.payment_service.service;

import com.sayedhesham.travelorch.common.entity.payment.PaymentMethod;
import com.sayedhesham.travelorch.common.entity.payment.PaymentTransaction;
import com.sayedhesham.travelorch.common.entity.travel.Travel;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.enums.PaymentProvider;
import com.sayedhesham.travelorch.common.enums.PaymentStatus;
import com.sayedhesham.travelorch.common.repository.neo4j.TravelGraphRepository;
import com.sayedhesham.travelorch.common.repository.payment.PaymentMethodRepository;
import com.sayedhesham.travelorch.common.repository.payment.PaymentTransactionRepository;
import com.sayedhesham.travelorch.common.repository.travel.TravelRepository;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.payment_service.dto.PaymentTransactionCreateRequest;
import com.sayedhesham.travelorch.payment_service.dto.PaymentTransactionResponse;
import com.sayedhesham.travelorch.payment_service.dto.TravelPurchaseRequest;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private static final Logger log = LoggerFactory.getLogger(PaymentTransactionService.class);

    private static final long MIN_DAYS_BEFORE_START = 3;

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TravelRepository travelRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;
    private final StripeClient stripeClient;
    private final TravelGraphRepository travelGraphRepository;

    @PreAuthorize("hasPermission('payments', 'read')")
    public Flux<PaymentTransactionResponse> getAllTransactions() {
        log.info("getAllTransactions - Fetching all transactions");
        return Mono.fromCallable(() -> transactionTemplate.execute(status ->
                paymentTransactionRepository.findAll().stream()
                        .map(PaymentTransactionResponse::fromEntity)
                        .toList()
        ))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getAllTransactions - Found {} transactions", list.size()))
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<PaymentTransactionResponse> getTransactionById(Long id, String currentUsername) {
        log.info("getTransactionById - Fetching transaction with id: {} for user: {}", id, currentUsername);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            PaymentTransaction transaction = paymentTransactionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Transaction not found with id: " + id));

            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

            boolean isOwner = transaction.getTravel() != null
                    && transaction.getTravel().getManager() != null
                    && transaction.getTravel().getManager().getId().equals(currentUser.getId());
            boolean canReadAny = hasPermission(currentUser, "payments", "read");

            if (!isOwner && !canReadAny) {
                log.warn("getTransactionById - User {} denied access to transaction id: {}", currentUsername, id);
                throw new SecurityException("You do not have permission to view this transaction");
            }

            return PaymentTransactionResponse.fromEntity(transaction);
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(t -> log.info("getTransactionById - Found transaction id: {}", t.getId()));
    }

    public Flux<PaymentTransactionResponse> getTransactionsByUser(Long userId, String currentUsername) {
        log.info("getTransactionsByUser - Fetching transactions for userId: {} requested by: {}", userId, currentUsername);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

            boolean isOwner = currentUser.getId().equals(userId);
            boolean canReadAny = hasPermission(currentUser, "payments", "read");

            if (!isOwner && !canReadAny) {
                log.warn("getTransactionsByUser - User {} denied access to userId: {}", currentUsername, userId);
                throw new SecurityException("You do not have permission to view these transactions");
            }

            return paymentTransactionRepository.findByTravelManagerId(userId).stream()
                    .map(PaymentTransactionResponse::fromEntity)
                    .toList();
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getTransactionsByUser - Found {} transactions for userId: {}", list.size(), userId))
                .flatMapMany(Flux::fromIterable);
    }

    public Flux<PaymentTransactionResponse> getTransactionsByTravel(Long travelId, String currentUsername) {
        log.info("getTransactionsByTravel - Fetching purchases for travelId: {} requested by: {}", travelId, currentUsername);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            Travel travel = travelRepository.findById(travelId)
                    .orElseThrow(() -> new IllegalArgumentException("Travel not found with id: " + travelId));

            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

            boolean isManager = travel.getManager() != null
                    && travel.getManager().getId().equals(currentUser.getId());
            boolean canReadAny = hasPermission(currentUser, "payments", "read");

            if (!isManager && !canReadAny) {
                log.warn("getTransactionsByTravel - User {} denied access to travelId: {}", currentUsername, travelId);
                throw new SecurityException("You do not have permission to view these purchases");
            }

            return paymentTransactionRepository.findByTravelId(travelId).stream()
                    .map(PaymentTransactionResponse::fromEntity)
                    .toList();
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getTransactionsByTravel - Found {} purchases for travelId: {}", list.size(), travelId))
                .flatMapMany(Flux::fromIterable);
    }

    @PreAuthorize("hasPermission('payments', 'write')")
    public Mono<PaymentTransactionResponse> createTransaction(PaymentTransactionCreateRequest request) {
        log.info("createTransaction - Creating payment: amount={}, currency={}, travelId={}",
                request.getAmount(), request.getCurrency(), request.getTravelId());
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            Travel travel = travelRepository.findById(request.getTravelId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Travel not found with id: " + request.getTravelId()));

            String currency = request.getCurrency() != null ? request.getCurrency().toLowerCase() : "usd";
            long amountInCents = request.getAmount()
                    .setScale(2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .longValueExact();

            PaymentIntent paymentIntent = createStripePaymentIntent(amountInCents, currency);
            log.info("createTransaction - Stripe PaymentIntent created: id={}, status={}",
                    paymentIntent.getId(), paymentIntent.getStatus());

            PaymentMethod paymentMethod = resolveStripePaymentMethod();

            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setTravel(travel);
            transaction.setPaymentMethod(paymentMethod);
            transaction.setAmount(request.getAmount());
            transaction.setCurrency(currency.toUpperCase());
            transaction.setStatus(PaymentStatus.pending);
            transaction.setProviderTransactionId(paymentIntent.getId());
            transaction.setPaymentIntentId(paymentIntent.getId());

            PaymentTransaction saved = paymentTransactionRepository.save(transaction);
            log.info("createTransaction - Saved transaction id: {}", saved.getId());

            return PaymentTransactionResponse.fromEntity(saved);
        }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<PaymentTransactionResponse> purchaseTravel(TravelPurchaseRequest request, String currentUsername) {
        log.info("purchaseTravel - User {} purchasing travelId={}", currentUsername, request.getTravelId());
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User buyer = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

            Travel travel = travelRepository.findById(request.getTravelId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Travel not found with id: " + request.getTravelId()));

            BigDecimal price = travel.getTotalPrice();
            if (price == null || price.signum() <= 0) {
                throw new IllegalStateException("This travel package is not available for purchase");
            }

            if (travel.getStartDate() == null
                    || ChronoUnit.DAYS.between(LocalDate.now(), travel.getStartDate()) < MIN_DAYS_BEFORE_START) {
                throw new IllegalStateException(
                        "Purchases close " + MIN_DAYS_BEFORE_START + " days before departure");
            }

            boolean alreadyPurchased = paymentTransactionRepository
                    .findByBuyerIdAndTravelId(buyer.getId(), travel.getId()).stream()
                    .anyMatch(t -> t.getStatus() != PaymentStatus.refunded
                            && t.getStatus() != PaymentStatus.failed);
            if (alreadyPurchased) {
                throw new IllegalStateException("You have already purchased this travel package");
            }

            long amountInCents = price
                    .setScale(2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .longValueExact();

            PaymentIntent paymentIntent = createStripePaymentIntent(amountInCents, "usd");
            log.info("purchaseTravel - Stripe PaymentIntent created: id={}, status={}",
                    paymentIntent.getId(), paymentIntent.getStatus());

            PaymentMethod paymentMethod = resolveStripePaymentMethod();

            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setTravel(travel);
            transaction.setBuyer(buyer);
            transaction.setPaymentMethod(paymentMethod);
            transaction.setAmount(price);
            transaction.setCurrency("USD");
            transaction.setStatus(PaymentStatus.completed);
            transaction.setProviderTransactionId(paymentIntent.getId());
            transaction.setPaymentIntentId(paymentIntent.getId());

            PaymentTransaction saved = paymentTransactionRepository.save(transaction);
            log.info("purchaseTravel - Saved purchase id: {} for buyer: {}", saved.getId(), buyer.getId());

            return PaymentTransactionResponse.fromEntity(saved);
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(resp -> recordPurchaseEdge(resp.getBuyerId(), resp.getTravelId()));
    }

    private void recordPurchaseEdge(Long buyerId, Long travelId) {
        if (buyerId == null || travelId == null) {
            return;
        }
        try {
            travelGraphRepository.recordPurchase(buyerId, travelId);
        } catch (Exception e) {
            log.warn("recordPurchaseEdge - graph write failed for buyer {} travel {}: {}", buyerId, travelId, e.getMessage());
        }
    }

    public Flux<PaymentTransactionResponse> getMyPurchases(String currentUsername) {
        log.info("getMyPurchases - Fetching purchases for user: {}", currentUsername);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User buyer = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

            return paymentTransactionRepository.findByBuyerId(buyer.getId()).stream()
                    .map(PaymentTransactionResponse::fromEntity)
                    .toList();
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getMyPurchases - Found {} purchases", list.size()))
                .flatMapMany(Flux::fromIterable);
    }

    public Flux<PaymentTransactionResponse> getPurchasesByUser(Long userId, String currentUsername) {
        log.info("getPurchasesByUser - Fetching purchases for userId: {} requested by: {}", userId, currentUsername);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

            boolean isOwner = currentUser.getId().equals(userId);
            boolean canReadAny = hasPermission(currentUser, "payments", "read")
                    || hasPermission(currentUser, "admin", "all");

            if (!isOwner && !canReadAny) {
                log.warn("getPurchasesByUser - User {} denied access to userId: {}", currentUsername, userId);
                throw new SecurityException("You do not have permission to view these purchases");
            }

            return paymentTransactionRepository.findByBuyerId(userId).stream()
                    .map(PaymentTransactionResponse::fromEntity)
                    .toList();
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getPurchasesByUser - Found {} purchases for userId: {}", list.size(), userId))
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<PaymentTransactionResponse> cancelAndRefund(Long id, String currentUsername) {
        log.info("cancelAndRefund - User {} cancelling transaction id: {}", currentUsername, id);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            PaymentTransaction transaction = paymentTransactionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Transaction not found with id: " + id));

            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

            boolean isBuyer = transaction.getBuyer() != null
                    && transaction.getBuyer().getId().equals(currentUser.getId());
            boolean isManager = transaction.getTravel() != null
                    && transaction.getTravel().getManager() != null
                    && transaction.getTravel().getManager().getId().equals(currentUser.getId());
            boolean isAdmin = hasPermission(currentUser, "admin", "all");

            if (!isBuyer && !isManager && !isAdmin) {
                log.warn("cancelAndRefund - User {} denied cancel of transaction id: {}", currentUsername, id);
                throw new SecurityException("You do not have permission to cancel this purchase");
            }

            if (transaction.getStatus() == PaymentStatus.refunded) {
                throw new IllegalStateException("This purchase has already been refunded");
            }

            transaction.setStatus(PaymentStatus.refunded);
            PaymentTransaction saved = paymentTransactionRepository.save(transaction);
            log.info("cancelAndRefund - Transaction id: {} marked refunded", saved.getId());

            return PaymentTransactionResponse.fromEntity(saved);
        }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private PaymentIntent createStripePaymentIntent(long amountInCents, String currency) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency)
                    .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .build())
                    .build();

            return stripeClient.v1().paymentIntents().create(params);
        } catch (StripeException e) {
            log.error("createStripePaymentIntent - Stripe API error: {}", e.getMessage());
            throw new RuntimeException("Failed to create Stripe PaymentIntent: " + e.getMessage(), e);
        }
    }

    private PaymentMethod resolveStripePaymentMethod() {
        return paymentMethodRepository.findByProviderAndIsTestMode(PaymentProvider.stripe, true)
                .orElseGet(() -> {
                    log.info("resolveStripePaymentMethod - No Stripe test PaymentMethod found, creating default");
                    PaymentMethod paymentMethod = new PaymentMethod();
                    paymentMethod.setProvider(PaymentProvider.stripe);
                    paymentMethod.setName("Stripe Test");
                    paymentMethod.setIsTestMode(true);
                    return paymentMethodRepository.save(paymentMethod);
                });
    }

    private boolean hasPermission(User user, String resource, String action) {
        return user.getRole() != null && user.getRole().getPermissions().stream()
                .anyMatch(permission ->
                        resource.equalsIgnoreCase(permission.getResource()) &&
                        action.equalsIgnoreCase(permission.getAction())
                );
    }
}
