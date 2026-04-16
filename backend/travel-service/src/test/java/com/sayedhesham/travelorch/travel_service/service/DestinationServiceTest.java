package com.sayedhesham.travelorch.travel_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.sayedhesham.travelorch.common.entity.travel.Destination;
import com.sayedhesham.travelorch.common.repository.travel.DestinationRepository;
import com.sayedhesham.travelorch.travel_service.dto.DestinationCreateRequest;
import com.sayedhesham.travelorch.travel_service.dto.DestinationResponse;
import com.sayedhesham.travelorch.travel_service.dto.DestinationUpdateRequest;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DestinationServiceTest {

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private DestinationService destinationService;

    private Destination testDestination;

    @BeforeEach
    void setUp() {
        testDestination = new Destination();
        testDestination.setId(1L);
        testDestination.setName("Paris");
        testDestination.setDescription("The City of Light");
        testDestination.setCountry("France");
        testDestination.setCity("Paris");
        testDestination.setRegion("Ile-de-France");
        testDestination.setLatitude(new BigDecimal("48.8566"));
        testDestination.setLongitude(new BigDecimal("2.3522"));
        testDestination.setImageBase64(null);
        testDestination.setCreatedAt(LocalDateTime.now());
        testDestination.setUpdatedAt(LocalDateTime.now());
    }

    private void setupTransactionTemplateInvocation() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    void getAllDestinations_Success() {
        setupTransactionTemplateInvocation();
        when(destinationRepository.findAll()).thenReturn(List.of(testDestination));

        Flux<DestinationResponse> result = destinationService.getAllDestinations();

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals("Paris", response.getName());
                    assertEquals("France", response.getCountry());
                    assertEquals("Paris", response.getCity());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getAllDestinations_EmptyList() {
        setupTransactionTemplateInvocation();
        when(destinationRepository.findAll()).thenReturn(List.of());

        Flux<DestinationResponse> result = destinationService.getAllDestinations();

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void searchDestinations_ByName() {
        setupTransactionTemplateInvocation();
        when(destinationRepository.findByNameContainingIgnoreCase("Par")).thenReturn(List.of(testDestination));

        StepVerifier.create(destinationService.searchDestinations("Par", null, null))
                .expectNextMatches(response -> {
                    assertEquals("Paris", response.getName());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void searchDestinations_ByCountry() {
        setupTransactionTemplateInvocation();
        when(destinationRepository.findByCountry("France")).thenReturn(List.of(testDestination));

        StepVerifier.create(destinationService.searchDestinations(null, "France", null))
                .expectNextMatches(response -> {
                    assertEquals("France", response.getCountry());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void searchDestinations_ByCity() {
        setupTransactionTemplateInvocation();
        when(destinationRepository.findByCity("Paris")).thenReturn(List.of(testDestination));

        StepVerifier.create(destinationService.searchDestinations(null, null, "Paris"))
                .expectNextMatches(response -> {
                    assertEquals("Paris", response.getCity());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void searchDestinations_ByCountryAndCity() {
        setupTransactionTemplateInvocation();
        when(destinationRepository.findByCountryAndCity("France", "Paris")).thenReturn(List.of(testDestination));

        StepVerifier.create(destinationService.searchDestinations(null, "France", "Paris"))
                .expectNextMatches(response -> {
                    assertEquals("France", response.getCountry());
                    assertEquals("Paris", response.getCity());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void searchDestinations_NoFilters_ReturnsAll() {
        setupTransactionTemplateInvocation();
        when(destinationRepository.findAll()).thenReturn(List.of(testDestination));

        StepVerifier.create(destinationService.searchDestinations(null, null, null))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getDestinationById_Success() {
        setupTransactionTemplateInvocation();
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(testDestination));

        StepVerifier.create(destinationService.getDestinationById(1L))
                .expectNextMatches(response -> {
                    assertEquals(1L, response.getId());
                    assertEquals("Paris", response.getName());
                    assertEquals("France", response.getCountry());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getDestinationById_NotFound() {
        setupTransactionTemplateInvocation();
        when(destinationRepository.findById(99L)).thenReturn(Optional.empty());

        StepVerifier.create(destinationService.getDestinationById(99L))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException
                                && throwable.getMessage().equals("Destination not found with id: 99")
                )
                .verify();
    }

    @Test
    void createDestination_Success() {
        setupTransactionTemplateInvocation();
        when(destinationRepository.save(any(Destination.class))).thenAnswer(invocation -> {
            Destination dest = invocation.getArgument(0);
            dest.setId(1L);
            dest.setCreatedAt(LocalDateTime.now());
            dest.setUpdatedAt(LocalDateTime.now());
            return dest;
        });

        DestinationCreateRequest request = DestinationCreateRequest.builder()
                .name("Tokyo")
                .description("Capital of Japan")
                .country("Japan")
                .city("Tokyo")
                .region("Kanto")
                .latitude(new BigDecimal("35.6762"))
                .longitude(new BigDecimal("139.6503"))
                .build();

        StepVerifier.create(destinationService.createDestination(request))
                .expectNextMatches(response -> {
                    assertNotNull(response.getId());
                    assertEquals("Tokyo", response.getName());
                    assertEquals("Japan", response.getCountry());
                    assertEquals("Tokyo", response.getCity());
                    assertEquals("Kanto", response.getRegion());
                    return true;
                })
                .verifyComplete();

        verify(destinationRepository).save(any(Destination.class));
    }

    @Test
    void updateDestination_UpdateName_Success() {
        setupTransactionTemplateInvocation();
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(testDestination));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DestinationUpdateRequest request = DestinationUpdateRequest.builder()
                .name("Paris - City of Light")
                .build();

        StepVerifier.create(destinationService.updateDestination(1L, request))
                .expectNextMatches(response -> {
                    assertEquals("Paris - City of Light", response.getName());
                    assertEquals("France", response.getCountry());
                    return true;
                })
                .verifyComplete();

        verify(destinationRepository).save(any(Destination.class));
    }

    @Test
    void updateDestination_UpdateAllFields_Success() {
        setupTransactionTemplateInvocation();
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(testDestination));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DestinationUpdateRequest request = DestinationUpdateRequest.builder()
                .name("New Paris")
                .description("Updated description")
                .country("Updated Country")
                .city("New City")
                .region("New Region")
                .latitude(new BigDecimal("50.0"))
                .longitude(new BigDecimal("3.0"))
                .imageBase64("base64data")
                .build();

        StepVerifier.create(destinationService.updateDestination(1L, request))
                .expectNextMatches(response -> {
                    assertEquals("New Paris", response.getName());
                    assertEquals("Updated description", response.getDescription());
                    assertEquals("Updated Country", response.getCountry());
                    assertEquals("New City", response.getCity());
                    assertEquals("New Region", response.getRegion());
                    assertEquals(new BigDecimal("50.0"), response.getLatitude());
                    assertEquals(new BigDecimal("3.0"), response.getLongitude());
                    assertEquals("base64data", response.getImageBase64());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void updateDestination_NotFound() {
        setupTransactionTemplateInvocation();
        when(destinationRepository.findById(99L)).thenReturn(Optional.empty());

        DestinationUpdateRequest request = DestinationUpdateRequest.builder()
                .name("New Name")
                .build();

        StepVerifier.create(destinationService.updateDestination(99L, request))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException
                                && throwable.getMessage().equals("Destination not found with id: 99")
                )
                .verify();
    }

    @Test
    void deleteDestination_Success() {
        setupTransactionTemplateInvocation();
        when(destinationRepository.existsById(1L)).thenReturn(true);

        StepVerifier.create(destinationService.deleteDestination(1L))
                .verifyComplete();

        verify(destinationRepository).deleteById(1L);
    }

    @Test
    void deleteDestination_NotFound() {
        setupTransactionTemplateInvocation();
        when(destinationRepository.existsById(99L)).thenReturn(false);

        StepVerifier.create(destinationService.deleteDestination(99L))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException
                                && throwable.getMessage().equals("Destination not found with id: 99")
                )
                .verify();
    }
}
