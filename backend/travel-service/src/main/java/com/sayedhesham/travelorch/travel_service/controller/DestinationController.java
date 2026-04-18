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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sayedhesham.travelorch.travel_service.dto.DestinationCreateRequest;
import com.sayedhesham.travelorch.travel_service.dto.DestinationResponse;
import com.sayedhesham.travelorch.travel_service.dto.DestinationUpdateRequest;
import com.sayedhesham.travelorch.travel_service.service.DestinationService;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/destinations")
public class DestinationController {

    private static final Logger log = LoggerFactory.getLogger(DestinationController.class);

    private final DestinationService destinationService;

    public DestinationController(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @GetMapping
    public Mono<ResponseEntity<Flux<DestinationResponse>>> getAllDestinations() {
        log.info("GET /destinations - Fetching all destinations");
        return Mono.just(ResponseEntity.ok(destinationService.getAllDestinations()));
    }

    @GetMapping("/search")
    public Mono<ResponseEntity<Flux<DestinationResponse>>> searchDestinations(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String city) {
        log.info("GET /destinations/search - name: {}, country: {}, city: {}", name, country, city);
        return Mono.just(ResponseEntity.ok(destinationService.searchDestinations(name, country, city)));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<DestinationResponse>> getDestinationById(@PathVariable Long id) {
        log.info("GET /destinations/{} - Fetching destination", id);
        return destinationService.getDestinationById(id)
                .<ResponseEntity<DestinationResponse>>map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("GET /destinations/{} - Not found", id);
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @PostMapping
    public Mono<ResponseEntity<DestinationResponse>> createDestination(
            @Valid @RequestBody DestinationCreateRequest request) {
        log.info("POST /destinations - Creating destination: {}", request.getName());
        return destinationService.createDestination(request)
                .<ResponseEntity<DestinationResponse>>map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .doOnNext(response -> log.info("POST /destinations - Created id: {}", response.getBody().getId()));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<DestinationResponse>> updateDestination(
            @PathVariable Long id,
            @Valid @RequestBody DestinationUpdateRequest request) {
        log.info("PUT /destinations/{} - Updating destination", id);
        return destinationService.updateDestination(id, request)
                .<ResponseEntity<DestinationResponse>>map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("PUT /destinations/{} - Not found", id);
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteDestination(@PathVariable Long id) {
        log.info("DELETE /destinations/{} - Deleting destination", id);
        return destinationService.deleteDestination(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("DELETE /destinations/{} - Not found", id);
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }
}
