package com.sayedhesham.travelorch.travel_service.controller;

import com.sayedhesham.travelorch.travel_service.dto.ReportCreateRequest;
import com.sayedhesham.travelorch.travel_service.dto.ReportResponse;
import com.sayedhesham.travelorch.travel_service.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    private WebTestClient webTestClient;
    private ReportResponse reportResponse;
    private ReportResponse anotherReportResponse;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(reportController)
                .webFilter((exchange, chain) ->
                        chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(getAuthentication()))
                )
                .build();

        reportResponse = ReportResponse.builder()
                .id(1L)
                .managerId(20L)
                .reporterId(10L)
                .reporterUsername("reporter")
                .reason("Unprofessional conduct")
                .createdAt(LocalDateTime.now())
                .build();

        anotherReportResponse = ReportResponse.builder()
                .id(2L)
                .managerId(21L)
                .reporterId(10L)
                .reporterUsername("reporter")
                .reason("Late refund")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private UsernamePasswordAuthenticationToken getAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "reporter", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // -------------------------------------------------------------------------
    // POST /reports
    // -------------------------------------------------------------------------

    @Test
    void createReport_Returns201Created() {
        ReportCreateRequest request = new ReportCreateRequest();
        request.setManagerId(20L);
        request.setReason("Unprofessional conduct");

        when(reportService.createReport(eq("reporter"), any(ReportCreateRequest.class)))
                .thenReturn(Mono.just(reportResponse));

        webTestClient.post()
                .uri("/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ReportResponse.class)
                .value(r -> {
                    assertEquals(1L, r.getId());
                    assertEquals(20L, r.getManagerId());
                    assertEquals("reporter", r.getReporterUsername());
                    assertEquals("Unprofessional conduct", r.getReason());
                });
    }

    @Test
    void createReport_NotRegularUser_Returns403Forbidden() {
        ReportCreateRequest request = new ReportCreateRequest();
        request.setManagerId(20L);

        when(reportService.createReport(eq("reporter"), any(ReportCreateRequest.class)))
                .thenReturn(Mono.error(new SecurityException("Only regular users can report travel managers")));

        webTestClient.post()
                .uri("/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void createReport_AlreadyReported_Returns409Conflict() {
        ReportCreateRequest request = new ReportCreateRequest();
        request.setManagerId(20L);

        when(reportService.createReport(eq("reporter"), any(ReportCreateRequest.class)))
                .thenReturn(Mono.error(new IllegalStateException("You have already reported this manager")));

        webTestClient.post()
                .uri("/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void createReport_ManagerNotFound_Returns404() {
        ReportCreateRequest request = new ReportCreateRequest();
        request.setManagerId(999L);

        when(reportService.createReport(eq("reporter"), any(ReportCreateRequest.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Manager not found with id: 999")));

        webTestClient.post()
                .uri("/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }

    // -------------------------------------------------------------------------
    // GET /reports
    // -------------------------------------------------------------------------

    @Test
    void getAllReports_ReturnsOk() {
        when(reportService.getAllReports())
                .thenReturn(Flux.just(reportResponse, anotherReportResponse));

        webTestClient.get()
                .uri("/reports")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ReportResponse.class)
                .hasSize(2)
                .value(list -> {
                    assertEquals(20L, list.get(0).getManagerId());
                    assertEquals(21L, list.get(1).getManagerId());
                });
    }

    @Test
    void getAllReports_EmptyList_ReturnsOk() {
        when(reportService.getAllReports())
                .thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/reports")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ReportResponse.class)
                .hasSize(0);
    }
}
