package com.sayedhesham.travelorch.travel_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sayedhesham.travelorch.common.enums.TravelStatus;
import com.sayedhesham.travelorch.travel_service.dto.TravelCreateRequest;
import com.sayedhesham.travelorch.travel_service.dto.TravelResponse;
import com.sayedhesham.travelorch.travel_service.dto.TravelUpdateRequest;
import com.sayedhesham.travelorch.travel_service.security.SecurityUtils;
import com.sayedhesham.travelorch.travel_service.service.TravelService;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/travels")
public class TravelController {

    private static final Logger log = LoggerFactory.getLogger(TravelController.class);

    private final TravelService travelService;

    public TravelController(TravelService travelService) {
        this.travelService = travelService;
    }

    @GetMapping
    public Mono<ResponseEntity<Flux<TravelResponse>>> getAllTravels() {
        log.info("GET /travels - Fetching all travels");
        return Mono.just(ResponseEntity.ok(travelService.getAllTravels()));
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<Flux<TravelResponse>>> getMyTravels() {
        log.info("GET /travels/me - Fetching current user's travels");
        return SecurityUtils.getCurrentUsername()
                .flatMap(currentUsername
                        -> travelService.getMyTravels(currentUsername)
                        .collectList()
                        .map(list -> ResponseEntity.ok(Flux.fromIterable(list)))
                )
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("GET /travels/me - Forbidden: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .<Flux<TravelResponse>>body(Flux.empty()));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .<Flux<TravelResponse>>body(Flux.empty())));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<TravelResponse>> getTravelById(@PathVariable Long id) {
        log.info("GET /travels/{} - Fetching travel", id);
        return SecurityUtils.getCurrentUsername()
                .flatMap(currentUsername
                        -> travelService.getTravelById(id, currentUsername)
                        .<ResponseEntity<TravelResponse>>map(ResponseEntity::ok)
                )
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("GET /travels/{} - Not found", id);
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("GET /travels/{} - Forbidden: {}", id, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @GetMapping("/user/{userId}")
    public Mono<ResponseEntity<Flux<TravelResponse>>> getTravelsByUser(@PathVariable Long userId) {
        log.info("GET /travels/user/{} - Fetching travels for user", userId);
        return SecurityUtils.getCurrentUsername()
                .flatMap(currentUsername
                        -> travelService.getTravelsByUser(userId, currentUsername)
                        .collectList()
                        .map(list -> ResponseEntity.ok(Flux.fromIterable(list)))
                )
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("GET /travels/user/{} - Forbidden: {}", userId, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .<Flux<TravelResponse>>body(Flux.empty()));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .<Flux<TravelResponse>>body(Flux.empty())));
    }

    @GetMapping("/upcoming")
    public Mono<ResponseEntity<Flux<TravelResponse>>> getUpcomingTravels() {
        log.info("GET /travels/upcoming - Fetching upcoming travels");
        return Mono.just(ResponseEntity.ok(travelService.getUpcomingTravels()));
    }

    @GetMapping("/status/{status}")
    public Mono<ResponseEntity<Flux<TravelResponse>>> getTravelsByStatus(@PathVariable TravelStatus status) {
        log.info("GET /travels/status/{} - Fetching travels by status", status);
        return Mono.just(ResponseEntity.ok(travelService.getTravelsByStatus(status)));
    }

    @PostMapping
    public Mono<ResponseEntity<TravelResponse>> createTravel(
            @Valid @RequestBody TravelCreateRequest request) {
        log.info("POST /travels - Creating travel: {}", request.getTitle());
        return SecurityUtils.getCurrentUsername()
                .flatMap(currentUsername
                        -> travelService.createTravel(request, currentUsername)
                        .<ResponseEntity<TravelResponse>>map(created
                                -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                        .doOnNext(response -> log.info("POST /travels - Created id: {}",
                        response.getBody().getId()))
                )
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("POST /travels - Forbidden: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<TravelResponse>> updateTravel(
            @PathVariable Long id,
            @Valid @RequestBody TravelUpdateRequest request) {
        log.info("PUT /travels/{} - Updating travel", id);
        return SecurityUtils.getCurrentUsername()
                .flatMap(currentUsername
                        -> travelService.updateTravel(id, request, currentUsername)
                        .<ResponseEntity<TravelResponse>>map(ResponseEntity::ok)
                )
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("PUT /travels/{} - Not found", id);
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("PUT /travels/{} - Forbidden: {}", id, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteTravel(@PathVariable Long id) {
        log.info("DELETE /travels/{} - Deleting travel", id);
        return SecurityUtils.getCurrentUsername()
                .flatMap(currentUsername
                        -> travelService.deleteTravel(id, currentUsername)
                        .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                )
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("DELETE /travels/{} - Not found", id);
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("DELETE /travels/{} - Forbidden: {}", id, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }
}
