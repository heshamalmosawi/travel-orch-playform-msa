package com.sayedhesham.travelorch.travel_service.controller;

import com.sayedhesham.travelorch.travel_service.dto.DestinationCreateRequest;
import com.sayedhesham.travelorch.travel_service.dto.DestinationResponse;
import com.sayedhesham.travelorch.travel_service.dto.DestinationUpdateRequest;
import com.sayedhesham.travelorch.travel_service.service.DestinationService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DestinationControllerTest {

    @Mock
    private DestinationService destinationService;

    @InjectMocks
    private DestinationController destinationController;

    private WebTestClient webTestClient;
    private DestinationResponse destinationResponse;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(destinationController)
                .webFilter((exchange, chain) ->
                        chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(getAuthentication()))
                )
                .build();

        destinationResponse = DestinationResponse.builder()
                .id(1L)
                .name("Paris")
                .description("The City of Light")
                .country("France")
                .city("Paris")
                .region("Ile-de-France")
                .latitude(new BigDecimal("48.8566"))
                .longitude(new BigDecimal("2.3522"))
                .imageBase64(null)
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
    void getAllDestinations_Success() {
        when(destinationService.getAllDestinations()).thenReturn(Flux.just(destinationResponse));

        webTestClient.get()
                .uri("/destinations")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DestinationResponse.class)
                .hasSize(1)
                .value(responses -> {
                    DestinationResponse response = responses.getFirst();
                    assertEquals("Paris", response.getName());
                    assertEquals("France", response.getCountry());
                });
    }

    @Test
    void getAllDestinations_EmptyList() {
        when(destinationService.getAllDestinations()).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/destinations")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DestinationResponse.class)
                .hasSize(0);
    }

    @Test
    void searchDestinations_ByName() {
        when(destinationService.searchDestinations("Par", null, null))
                .thenReturn(Flux.just(destinationResponse));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/destinations/search")
                        .queryParam("name", "Par")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DestinationResponse.class)
                .hasSize(1)
                .value(responses -> assertEquals("Paris", responses.getFirst().getName()));
    }

    @Test
    void searchDestinations_ByCountryAndCity() {
        when(destinationService.searchDestinations(null, "France", "Paris"))
                .thenReturn(Flux.just(destinationResponse));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/destinations/search")
                        .queryParam("country", "France")
                        .queryParam("city", "Paris")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DestinationResponse.class)
                .hasSize(1);
    }

    @Test
    void getDestinationById_Success() {
        when(destinationService.getDestinationById(1L)).thenReturn(Mono.just(destinationResponse));

        webTestClient.get()
                .uri("/destinations/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(DestinationResponse.class)
                .value(response -> {
                    assertEquals(1L, response.getId());
                    assertEquals("Paris", response.getName());
                    assertEquals("France", response.getCountry());
                    assertEquals("Paris", response.getCity());
                });
    }

    @Test
    void getDestinationById_NotFound() {
        when(destinationService.getDestinationById(99L))
                .thenReturn(Mono.error(new IllegalArgumentException("Destination not found with id: 99")));

        webTestClient.get()
                .uri("/destinations/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createDestination_Success() {
        DestinationCreateRequest request = DestinationCreateRequest.builder()
                .name("Tokyo")
                .description("Capital of Japan")
                .country("Japan")
                .city("Tokyo")
                .region("Kanto")
                .latitude(new BigDecimal("35.6762"))
                .longitude(new BigDecimal("139.6503"))
                .build();

        DestinationResponse createdResponse = DestinationResponse.builder()
                .id(2L)
                .name("Tokyo")
                .description("Capital of Japan")
                .country("Japan")
                .city("Tokyo")
                .region("Kanto")
                .latitude(new BigDecimal("35.6762"))
                .longitude(new BigDecimal("139.6503"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(destinationService.createDestination(any(DestinationCreateRequest.class)))
                .thenReturn(Mono.just(createdResponse));

        webTestClient.post()
                .uri("/destinations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(DestinationResponse.class)
                .value(response -> {
                    assertEquals(2L, response.getId());
                    assertEquals("Tokyo", response.getName());
                    assertEquals("Japan", response.getCountry());
                });
    }

    @Test
    void updateDestination_Success() {
        DestinationUpdateRequest updateRequest = DestinationUpdateRequest.builder()
                .name("Paris - Updated")
                .description("Updated description")
                .build();

        DestinationResponse updatedResponse = DestinationResponse.builder()
                .id(1L)
                .name("Paris - Updated")
                .description("Updated description")
                .country("France")
                .city("Paris")
                .build();

        when(destinationService.updateDestination(eq(1L), any(DestinationUpdateRequest.class)))
                .thenReturn(Mono.just(updatedResponse));

        webTestClient.put()
                .uri("/destinations/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(DestinationResponse.class)
                .value(response -> {
                    assertEquals("Paris - Updated", response.getName());
                    assertEquals("Updated description", response.getDescription());
                });
    }

    @Test
    void updateDestination_NotFound() {
        DestinationUpdateRequest updateRequest = DestinationUpdateRequest.builder()
                .name("New Name")
                .build();

        when(destinationService.updateDestination(eq(99L), any(DestinationUpdateRequest.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Destination not found with id: 99")));

        webTestClient.put()
                .uri("/destinations/99")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteDestination_Success() {
        when(destinationService.deleteDestination(1L)).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/destinations/1")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void deleteDestination_NotFound() {
        when(destinationService.deleteDestination(99L))
                .thenReturn(Mono.error(new IllegalArgumentException("Destination not found with id: 99")));

        webTestClient.delete()
                .uri("/destinations/99")
                .exchange()
                .expectStatus().isNotFound();
    }
}
