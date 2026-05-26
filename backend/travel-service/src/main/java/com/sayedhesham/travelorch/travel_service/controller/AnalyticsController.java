package com.sayedhesham.travelorch.travel_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sayedhesham.travelorch.travel_service.dto.AnalyticsOverviewResponse;
import com.sayedhesham.travelorch.travel_service.dto.ManagerRankingResponse;
import com.sayedhesham.travelorch.travel_service.dto.MonthlyIncomeResponse;
import com.sayedhesham.travelorch.travel_service.dto.PagedResponse;
import com.sayedhesham.travelorch.travel_service.dto.TravelRankingResponse;
import com.sayedhesham.travelorch.travel_service.service.AnalyticsService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public Mono<ResponseEntity<AnalyticsOverviewResponse>> getOverview() {
        log.info("GET /analytics/overview");
        return analyticsService.getOverview()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/income")
    public Mono<ResponseEntity<Flux<MonthlyIncomeResponse>>> getIncome(
            @RequestParam(defaultValue = "6") int months) {
        log.info("GET /analytics/income?months={}", months);
        return Mono.just(ResponseEntity.ok(analyticsService.getIncome(months)));
    }

    @GetMapping("/managers/top")
    public Mono<ResponseEntity<PagedResponse<ManagerRankingResponse>>> getTopManagers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        log.info("GET /analytics/managers/top?page={}&size={}", page, size);
        return analyticsService.getTopManagers(page, size)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/travels/top")
    public Mono<ResponseEntity<PagedResponse<TravelRankingResponse>>> getTopTravels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        log.info("GET /analytics/travels/top?page={}&size={}", page, size);
        return analyticsService.getTopTravels(page, size)
                .map(ResponseEntity::ok);
    }
}
