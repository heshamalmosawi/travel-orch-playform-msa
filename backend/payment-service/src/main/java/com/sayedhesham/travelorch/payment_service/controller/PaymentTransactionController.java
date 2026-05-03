package com.sayedhesham.travelorch.payment_service.controller;

import com.sayedhesham.travelorch.payment_service.dto.PaymentTransactionCreateRequest;
import com.sayedhesham.travelorch.payment_service.dto.PaymentTransactionResponse;
import com.sayedhesham.travelorch.payment_service.security.SecurityUtils;
import com.sayedhesham.travelorch.payment_service.service.PaymentTransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/transactions")
public class PaymentTransactionController {

    private static final Logger log = LoggerFactory.getLogger(PaymentTransactionController.class);

    private final PaymentTransactionService paymentTransactionService;

    public PaymentTransactionController(PaymentTransactionService paymentTransactionService) {
        this.paymentTransactionService = paymentTransactionService;
    }

    @GetMapping
    public Mono<ResponseEntity<Flux<PaymentTransactionResponse>>> getAllTransactions() {
        log.info("GET /transactions - Fetching all transactions");
        return Mono.just(ResponseEntity.ok(paymentTransactionService.getAllTransactions()));
    }

    @GetMapping("/user/{userId}")
    public Mono<ResponseEntity<Flux<PaymentTransactionResponse>>> getTransactionsByUser(@PathVariable Long userId) {
        log.info("GET /transactions/user/{} - Fetching transactions for user", userId);
        return SecurityUtils.getCurrentUsername()
                .flatMap(currentUsername ->
                        paymentTransactionService.getTransactionsByUser(userId, currentUsername)
                                .collectList()
                                .map(list -> ResponseEntity.ok(Flux.fromIterable(list))))
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("GET /transactions/user/{} - Forbidden: {}", userId, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .<Flux<PaymentTransactionResponse>>body(Flux.empty()));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .<Flux<PaymentTransactionResponse>>body(Flux.empty())));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<PaymentTransactionResponse>> getTransactionById(@PathVariable Long id) {
        log.info("GET /transactions/{} - Fetching transaction", id);
        return SecurityUtils.getCurrentUsername()
                .flatMap(currentUsername ->
                        paymentTransactionService.getTransactionById(id, currentUsername)
                                .<ResponseEntity<PaymentTransactionResponse>>map(ResponseEntity::ok))
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("GET /transactions/{} - Not found", id);
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("GET /transactions/{} - Forbidden: {}", id, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @PostMapping
    public Mono<ResponseEntity<PaymentTransactionResponse>> createTransaction(
            @Valid @RequestBody PaymentTransactionCreateRequest request) {
        log.info("POST /transactions - Creating payment: amount={}, travelId={}",
                request.getAmount(), request.getTravelId());
        return paymentTransactionService.createTransaction(request)
                .<ResponseEntity<PaymentTransactionResponse>>map(created ->
                        ResponseEntity.status(HttpStatus.CREATED).body(created))
                .doOnNext(response -> log.info("POST /transactions - Created id: {}",
                        response.getBody().getId()));
    }
}
