package com.sayedhesham.travelorch.travel_service.controller;

import com.sayedhesham.travelorch.travel_service.dto.ReportCreateRequest;
import com.sayedhesham.travelorch.travel_service.dto.ReportResponse;
import com.sayedhesham.travelorch.travel_service.security.SecurityUtils;
import com.sayedhesham.travelorch.travel_service.service.ReportService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public Mono<ResponseEntity<ReportResponse>> createReport(
            @Valid @RequestBody ReportCreateRequest request) {
        log.info("POST /reports - managerId: {}", request.getManagerId());
        return SecurityUtils.getCurrentUsername()
                .flatMap(username -> reportService.createReport(username, request)
                        .<ResponseEntity<ReportResponse>>map(r -> ResponseEntity.status(HttpStatus.CREATED).body(r))
                )
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("POST /reports - Forbidden: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                })
                .onErrorResume(IllegalStateException.class, e -> {
                    log.warn("POST /reports - Conflict: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
                })
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("POST /reports - Not found: {}", e.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @GetMapping
    public Mono<ResponseEntity<Flux<ReportResponse>>> getAllReports() {
        log.info("GET /reports");
        return Mono.just(ResponseEntity.ok(reportService.getAllReports()));
    }
}
