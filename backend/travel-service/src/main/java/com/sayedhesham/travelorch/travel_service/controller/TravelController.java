package com.sayedhesham.travelorch.travel_service.controller;

import com.sayedhesham.travelorch.common.enums.TravelStatus;
import com.sayedhesham.travelorch.travel_service.dto.TravelCreateRequest;
import com.sayedhesham.travelorch.travel_service.dto.TravelResponse;
import com.sayedhesham.travelorch.travel_service.dto.TravelUpdateRequest;
import com.sayedhesham.travelorch.travel_service.service.TravelService;
import jakarta.validation.Valid;
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

    @GetMapping("/{id}")
    public Mono<ResponseEntity<TravelResponse>> getTravelById(@PathVariable Long id) {
        log.info("GET /travels/{} - Fetching travel", id);
        return travelService.getTravelById(id)
                .<ResponseEntity<TravelResponse>>map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("GET /travels/{} - Not found", id);
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @GetMapping("/user/{userId}")
    public Mono<ResponseEntity<Flux<TravelResponse>>> getTravelsByUser(@PathVariable Long userId) {
        log.info("GET /travels/user/{} - Fetching travels for user", userId);
        return Mono.just(ResponseEntity.ok(travelService.getTravelsByUser(userId)));
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
        return travelService.createTravel(request)
                .<ResponseEntity<TravelResponse>>map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .doOnNext(response -> log.info("POST /travels - Created id: {}", response.getBody().getId()));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<TravelResponse>> updateTravel(
            @PathVariable Long id,
            @Valid @RequestBody TravelUpdateRequest request) {
        log.info("PUT /travels/{} - Updating travel", id);
        return travelService.updateTravel(id, request)
                .<ResponseEntity<TravelResponse>>map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("PUT /travels/{} - Not found", id);
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteTravel(@PathVariable Long id) {
        log.info("DELETE /travels/{} - Deleting travel", id);
        return travelService.deleteTravel(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("DELETE /travels/{} - Not found", id);
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }
}
