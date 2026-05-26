package com.sayedhesham.travelorch.travel_service.controller;

import com.sayedhesham.travelorch.common.enums.TravelStatus;
import com.sayedhesham.travelorch.travel_service.dto.*;
import com.sayedhesham.travelorch.travel_service.service.TravelService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelControllerTest {

    @Mock
    private TravelService travelService;

    @InjectMocks
    private TravelController travelController;

    private WebTestClient webTestClient;
    private TravelResponse travelResponse;
    private DestinationResponse destinationResponse;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(travelController)
                .webFilter((exchange, chain) ->
                        chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(getAuthentication()))
                )
                .build();

        destinationResponse = DestinationResponse.builder()
                .id(10L)
                .name("Paris")
                .description("The City of Light")
                .country("France")
                .city("Paris")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TravelDestinationResponse tdResponse = TravelDestinationResponse.builder()
                .id(1000L)
                .destinationId(10L)
                .visitOrder(1)
                .arrivalDate(LocalDate.of(2026, 6, 1))
                .departureDate(LocalDate.of(2026, 6, 5))
                .notes("First stop")
                .destination(destinationResponse)
                .createdAt(LocalDateTime.now())
                .build();

        travelResponse = TravelResponse.builder()
                .id(100L)
                .title("Summer Trip")
                .description("A summer vacation")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 15))
                .totalPrice(new BigDecimal("5000.00"))
                .status(TravelStatus.draft)
                .managerId(1L)
                .destinations(List.of(tdResponse))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private UsernamePasswordAuthenticationToken getAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "admin", null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    void getAllTravels_Success() {
        when(travelService.getAllTravels()).thenReturn(Flux.just(travelResponse));

        webTestClient.get()
                .uri("/travels")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TravelResponse.class)
                .hasSize(1)
                .value(responses -> {
                    TravelResponse response = responses.getFirst();
                    assertEquals("Summer Trip", response.getTitle());
                    assertEquals(TravelStatus.draft, response.getStatus());
                });
    }

    @Test
    void getAllTravels_EmptyList() {
        when(travelService.getAllTravels()).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/travels")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TravelResponse.class)
                .hasSize(0);
    }

    @Test
    void getTravelById_Success() {
        when(travelService.getTravelById(100L, "admin")).thenReturn(Mono.just(travelResponse));

        webTestClient.get()
                .uri("/travels/100")
                .exchange()
                .expectStatus().isOk()
                .expectBody(TravelResponse.class)
                .value(response -> {
                    assertEquals(100L, response.getId());
                    assertEquals("Summer Trip", response.getTitle());
                    assertEquals(1, response.getDestinations().size());
                });
    }

    @Test
    void getTravelById_NotFound() {
        when(travelService.getTravelById(99L, "admin"))
                .thenReturn(Mono.error(new IllegalArgumentException("Travel not found with id: 99")));

        webTestClient.get()
                .uri("/travels/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getTravelsByUser_Success() {
        when(travelService.getTravelsByUser(1L, "admin")).thenReturn(Flux.just(travelResponse));

        webTestClient.get()
                .uri("/travels/user/1")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TravelResponse.class)
                .hasSize(1)
                .value(responses -> assertEquals(1L, responses.getFirst().getManagerId()));
    }

    @Test
    void getTravelsByStatus_Success() {
        when(travelService.getTravelsByStatus(TravelStatus.draft)).thenReturn(Flux.just(travelResponse));

        webTestClient.get()
                .uri("/travels/status/draft")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TravelResponse.class)
                .hasSize(1)
                .value(responses -> assertEquals(TravelStatus.draft, responses.getFirst().getStatus()));
    }

    @Test
    void createTravel_Success() {
        TravelCreateRequest request = TravelCreateRequest.builder()
                .title("Winter Trip")
                .description("A winter getaway")
                .startDate(LocalDate.of(2026, 12, 1))
                .endDate(LocalDate.of(2026, 12, 10))
                .totalPrice(new BigDecimal("3000.00"))
                .build();

        TravelResponse createdResponse = TravelResponse.builder()
                .id(101L)
                .title("Winter Trip")
                .description("A winter getaway")
                .startDate(LocalDate.of(2026, 12, 1))
                .endDate(LocalDate.of(2026, 12, 10))
                .totalPrice(new BigDecimal("3000.00"))
                .status(TravelStatus.draft)
                .managerId(1L)
                .destinations(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(travelService.createTravel(any(TravelCreateRequest.class), eq("admin")))
                .thenReturn(Mono.just(createdResponse));

        webTestClient.post()
                .uri("/travels")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TravelResponse.class)
                .value(response -> {
                    assertEquals(101L, response.getId());
                    assertEquals("Winter Trip", response.getTitle());
                    assertEquals(1L, response.getManagerId());
                });
    }

    @Test
    void updateTravel_Success() {
        TravelUpdateRequest updateRequest = TravelUpdateRequest.builder()
                .title("Updated Trip")
                .status(TravelStatus.confirmed)
                .build();

        TravelResponse updatedResponse = TravelResponse.builder()
                .id(100L)
                .title("Updated Trip")
                .description("A summer vacation")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 15))
                .totalPrice(new BigDecimal("5000.00"))
                .status(TravelStatus.confirmed)
                .managerId(1L)
                .destinations(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(travelService.updateTravel(eq(100L), any(TravelUpdateRequest.class), eq("admin")))
                .thenReturn(Mono.just(updatedResponse));

        webTestClient.put()
                .uri("/travels/100")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TravelResponse.class)
                .value(response -> {
                    assertEquals("Updated Trip", response.getTitle());
                    assertEquals(TravelStatus.confirmed, response.getStatus());
                });
    }

    @Test
    void updateTravel_NotFound() {
        TravelUpdateRequest updateRequest = TravelUpdateRequest.builder()
                .title("New Title")
                .build();

        when(travelService.updateTravel(eq(99L), any(TravelUpdateRequest.class), eq("admin")))
                .thenReturn(Mono.error(new IllegalArgumentException("Travel not found with id: 99")));

        webTestClient.put()
                .uri("/travels/99")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteTravel_Success() {
        when(travelService.deleteTravel(100L, "admin")).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/travels/100")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void deleteTravel_NotFound() {
        when(travelService.deleteTravel(99L, "admin"))
                .thenReturn(Mono.error(new IllegalArgumentException("Travel not found with id: 99")));

        webTestClient.delete()
                .uri("/travels/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getTravelById_Forbidden() {
        when(travelService.getTravelById(200L, "admin"))
                .thenReturn(Mono.error(new SecurityException("You do not have permission to view this travel")));

        webTestClient.get()
                .uri("/travels/200")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void getTravelsByUser_Forbidden() {
        when(travelService.getTravelsByUser(2L, "admin"))
                .thenReturn(Flux.error(new SecurityException("You do not have permission to view these travels")));

        webTestClient.get()
                .uri("/travels/user/2")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void updateTravel_Forbidden() {
        TravelUpdateRequest updateRequest = TravelUpdateRequest.builder()
                .title("Forbidden Update")
                .build();

        when(travelService.updateTravel(eq(200L), any(TravelUpdateRequest.class), eq("admin")))
                .thenReturn(Mono.error(new SecurityException("You do not have permission to update this travel")));

        webTestClient.put()
                .uri("/travels/200")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void deleteTravel_Forbidden() {
        when(travelService.deleteTravel(200L, "admin"))
                .thenReturn(Mono.error(new SecurityException("You do not have permission to delete this travel")));

        webTestClient.delete()
                .uri("/travels/200")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void getUpcomingTravels_Success() {
        TravelResponse upcomingResponse = TravelResponse.builder()
                .id(200L)
                .title("Beach Getaway")
                .description("A relaxing beach trip")
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(17))
                .totalPrice(new BigDecimal("2000.00"))
                .status(TravelStatus.confirmed)
                .managerId(1L)
                .destinations(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(travelService.getUpcomingTravels()).thenReturn(Flux.just(upcomingResponse, travelResponse));

        webTestClient.get()
                .uri("/travels/upcoming")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TravelResponse.class)
                .hasSize(2)
                .value(responses -> {
                    assertEquals(200L, responses.get(0).getId());
                    assertEquals("Beach Getaway", responses.get(0).getTitle());
                    assertEquals(TravelStatus.confirmed, responses.get(0).getStatus());
                    assertEquals(100L, responses.get(1).getId());
                });
    }

    @Test
    void getUpcomingTravels_EmptyList() {
        when(travelService.getUpcomingTravels()).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/travels/upcoming")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TravelResponse.class)
                .hasSize(0);
    }

    // -------------------------------------------------------------------------
    // GET /travels/user/{userId}/upcoming
    // -------------------------------------------------------------------------

    @Test
    void getUpcomingTravelsByManager_ReturnsOk() {
        TravelResponse upcomingForManager = TravelResponse.builder()
                .id(300L)
                .title("Safari Adventure")
                .description("An African safari")
                .startDate(LocalDate.now().plusDays(20))
                .endDate(LocalDate.now().plusDays(27))
                .totalPrice(new BigDecimal("8000.00"))
                .status(TravelStatus.confirmed)
                .managerId(1L)
                .destinations(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(travelService.getUpcomingByManager(1L)).thenReturn(Flux.just(upcomingForManager));

        webTestClient.get()
                .uri("/travels/user/1/upcoming")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TravelResponse.class)
                .hasSize(1)
                .value(list -> {
                    assertEquals(300L, list.get(0).getId());
                    assertEquals("Safari Adventure", list.get(0).getTitle());
                    assertEquals(TravelStatus.confirmed, list.get(0).getStatus());
                });
    }

    @Test
    void getUpcomingTravelsByManager_EmptyList_ReturnsOk() {
        when(travelService.getUpcomingByManager(1L)).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/travels/user/1/upcoming")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TravelResponse.class)
                .hasSize(0);
    }

    @Test
    void getUpcomingTravelsByManager_MultiplePackages_ReturnsAll() {
        TravelResponse r1 = TravelResponse.builder()
                .id(301L).title("Alps Ski Trip")
                .startDate(LocalDate.now().plusDays(10)).endDate(LocalDate.now().plusDays(17))
                .totalPrice(new BigDecimal("4000.00")).status(TravelStatus.confirmed)
                .managerId(1L).destinations(List.of())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        TravelResponse r2 = TravelResponse.builder()
                .id(302L).title("Island Escape")
                .startDate(LocalDate.now().plusDays(30)).endDate(LocalDate.now().plusDays(37))
                .totalPrice(new BigDecimal("3500.00")).status(TravelStatus.draft)
                .managerId(1L).destinations(List.of())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(travelService.getUpcomingByManager(1L)).thenReturn(Flux.just(r1, r2));

        webTestClient.get()
                .uri("/travels/user/1/upcoming")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TravelResponse.class)
                .hasSize(2)
                .value(list -> {
                    assertEquals(301L, list.get(0).getId());
                    assertEquals(302L, list.get(1).getId());
                });
    }

    // -------------------------------------------------------------------------
    // GET /travels/manager/{managerId}/stats
    // -------------------------------------------------------------------------

    @Test
    void getManagerStats_ReturnsOk() {
        ManagerStatsResponse stats = ManagerStatsResponse.builder()
                .totalPackages(5L)
                .averageRating(4.2)
                .totalReviews(10L)
                .build();

        when(travelService.getManagerStats(1L)).thenReturn(Mono.just(stats));

        webTestClient.get()
                .uri("/travels/manager/1/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ManagerStatsResponse.class)
                .value(r -> {
                    assertEquals(5L, r.getTotalPackages());
                    assertEquals(4.2, r.getAverageRating(), 0.001);
                    assertEquals(10L, r.getTotalReviews());
                });
    }

    @Test
    void getManagerStats_NoPackages_ReturnsZeros() {
        ManagerStatsResponse stats = ManagerStatsResponse.builder()
                .totalPackages(0L)
                .averageRating(0.0)
                .totalReviews(0L)
                .build();

        when(travelService.getManagerStats(99L)).thenReturn(Mono.just(stats));

        webTestClient.get()
                .uri("/travels/manager/99/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ManagerStatsResponse.class)
                .value(r -> {
                    assertEquals(0L, r.getTotalPackages());
                    assertEquals(0.0, r.getAverageRating(), 0.001);
                    assertEquals(0L, r.getTotalReviews());
                });
    }

    @Test
    void getManagerStats_PerfectRatings_ReturnsAverageFive() {
        ManagerStatsResponse stats = ManagerStatsResponse.builder()
                .totalPackages(2L)
                .averageRating(5.0)
                .totalReviews(4L)
                .build();

        when(travelService.getManagerStats(1L)).thenReturn(Mono.just(stats));

        webTestClient.get()
                .uri("/travels/manager/1/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ManagerStatsResponse.class)
                .value(r -> assertEquals(5.0, r.getAverageRating(), 0.001));
    }
}
