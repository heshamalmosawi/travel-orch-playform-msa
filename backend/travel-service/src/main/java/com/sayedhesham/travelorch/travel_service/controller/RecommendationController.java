package com.sayedhesham.travelorch.travel_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sayedhesham.travelorch.travel_service.dto.TravelResponse;
import com.sayedhesham.travelorch.travel_service.security.SecurityUtils;
import com.sayedhesham.travelorch.travel_service.service.RecommendationService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    private static final Logger log = LoggerFactory.getLogger(RecommendationController.class);

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public Mono<ResponseEntity<Flux<TravelResponse>>> getRecommendations() {
        log.info("GET /recommendations - Fetching personalized recommendations");
        return SecurityUtils.getCurrentUsername()
                .flatMap(currentUsername
                        -> recommendationService.getRecommendations(currentUsername)
                        .collectList()
                        .map(list -> ResponseEntity.ok(Flux.fromIterable(list)))
                )
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("GET /recommendations - User not found: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .<Flux<TravelResponse>>body(Flux.empty()));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .<Flux<TravelResponse>>body(Flux.empty())));
    }

    @PostMapping("/backfill")
    public Mono<ResponseEntity<Void>> backfill() {
        log.info("POST /recommendations/backfill - Rebuilding recommendation graph");
        return recommendationService.backfill()
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(org.springframework.security.access.AccessDeniedException.class, e -> {
                    log.warn("POST /recommendations/backfill - Forbidden: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                });
    }
}
