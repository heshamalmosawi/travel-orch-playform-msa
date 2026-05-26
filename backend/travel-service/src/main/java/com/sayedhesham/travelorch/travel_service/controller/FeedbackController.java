package com.sayedhesham.travelorch.travel_service.controller;

import com.sayedhesham.travelorch.travel_service.dto.FeedbackCreateRequest;
import com.sayedhesham.travelorch.travel_service.dto.FeedbackResponse;
import com.sayedhesham.travelorch.travel_service.dto.FeedbackUpdateRequest;
import com.sayedhesham.travelorch.travel_service.security.SecurityUtils;
import com.sayedhesham.travelorch.travel_service.service.FeedbackService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/feedbacks")
public class FeedbackController {

    private static final Logger log = LoggerFactory.getLogger(FeedbackController.class);

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public Mono<ResponseEntity<FeedbackResponse>> createFeedback(
            @Valid @RequestBody FeedbackCreateRequest request) {
        log.info("POST /feedbacks - travelId: {}", request.getTravelId());
        return SecurityUtils.getCurrentUsername()
                .flatMap(username -> feedbackService.createFeedback(username, request)
                        .<ResponseEntity<FeedbackResponse>>map(f -> ResponseEntity.status(HttpStatus.CREATED).body(f))
                )
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("POST /feedbacks - Forbidden: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                })
                .onErrorResume(IllegalStateException.class, e -> {
                    log.warn("POST /feedbacks - Conflict: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
                })
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("POST /feedbacks - Not found: {}", e.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    // GET /feedbacks?travelId=X  or  GET /feedbacks?managerId=X
    @GetMapping
    public Mono<ResponseEntity<Flux<FeedbackResponse>>> getFeedbacks(
            @RequestParam(required = false) Long travelId,
            @RequestParam(required = false) Long managerId) {
        if (travelId != null) {
            log.info("GET /feedbacks?travelId={}", travelId);
            return Mono.just(ResponseEntity.ok(feedbackService.getFeedbacksForTravel(travelId)));
        }
        if (managerId != null) {
            log.info("GET /feedbacks?managerId={}", managerId);
            return Mono.just(ResponseEntity.ok(feedbackService.getFeedbacksForManager(managerId)));
        }
        return Mono.just(ResponseEntity.badRequest().<Flux<FeedbackResponse>>build());
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<FeedbackResponse>> getMyFeedback(
            @RequestParam Long travelId) {
        log.info("GET /feedbacks/me?travelId={}", travelId);
        return SecurityUtils.getCurrentUsername()
                .flatMap(username -> feedbackService.getMyFeedbackForTravel(travelId, username)
                        .<ResponseEntity<FeedbackResponse>>map(ResponseEntity::ok)
                        .defaultIfEmpty(ResponseEntity.notFound().build())
                )
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.notFound().build()))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    // GET /feedbacks/user/{userId} — feedbacks raised by a user (admin or self)
    @GetMapping("/user/{userId}")
    public Mono<ResponseEntity<Flux<FeedbackResponse>>> getFeedbacksByReviewer(@PathVariable Long userId) {
        log.info("GET /feedbacks/user/{} - Fetching feedbacks raised by user", userId);
        return SecurityUtils.getCurrentUsername()
                .flatMap(username -> feedbackService.getFeedbacksByReviewer(userId, username)
                        .collectList()
                        .map(list -> ResponseEntity.ok(Flux.fromIterable(list))))
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("GET /feedbacks/user/{} - Forbidden: {}", userId, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .<Flux<FeedbackResponse>>body(Flux.empty()));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .<Flux<FeedbackResponse>>body(Flux.empty())));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<FeedbackResponse>> updateFeedback(
            @PathVariable Long id,
            @Valid @RequestBody FeedbackUpdateRequest request) {
        log.info("PUT /feedbacks/{}", id);
        return SecurityUtils.getCurrentUsername()
                .flatMap(username -> feedbackService.updateFeedback(id, username, request)
                        .<ResponseEntity<FeedbackResponse>>map(ResponseEntity::ok)
                )
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("PUT /feedbacks/{} - Forbidden: {}", id, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                })
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("PUT /feedbacks/{} - Not found", id);
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteFeedback(@PathVariable Long id) {
        log.info("DELETE /feedbacks/{}", id);
        return SecurityUtils.getCurrentUsername()
                .flatMap(username -> feedbackService.deleteFeedback(id, username)
                        .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                )
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("DELETE /feedbacks/{} - Forbidden: {}", id, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                })
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("DELETE /feedbacks/{} - Not found", id);
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }
}
