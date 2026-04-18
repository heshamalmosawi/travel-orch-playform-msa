package com.sayedhesham.travelorch.travel_service.service;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.sayedhesham.travelorch.common.entity.travel.Destination;
import com.sayedhesham.travelorch.common.entity.travel.Travel;
import com.sayedhesham.travelorch.common.entity.travel.TravelDestination;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.enums.TravelStatus;
import com.sayedhesham.travelorch.common.repository.accommodation.TravelAccommodationRepository;
import com.sayedhesham.travelorch.common.repository.activity.TravelActivityRepository;
import com.sayedhesham.travelorch.common.repository.transportation.TransportationSegmentRepository;
import com.sayedhesham.travelorch.common.repository.travel.DestinationRepository;
import com.sayedhesham.travelorch.common.repository.travel.TravelDestinationRepository;
import com.sayedhesham.travelorch.common.repository.travel.TravelRepository;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.travel_service.dto.*;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TravelServiceTest {

    @Mock
    private TravelRepository travelRepository;

    @Mock
    private TravelDestinationRepository travelDestinationRepository;

    @Mock
    private TravelActivityRepository travelActivityRepository;

    @Mock
    private TravelAccommodationRepository travelAccommodationRepository;

    @Mock
    private TransportationSegmentRepository transportationSegmentRepository;

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private TravelService travelService;

    private User testUser;
    private Travel testTravel;
    private Destination testDestination;
    private TravelDestination testTravelDestination;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testDestination = new Destination();
        testDestination.setId(10L);
        testDestination.setName("Paris");
        testDestination.setCountry("France");
        testDestination.setCity("Paris");

        testTravel = new Travel();
        testTravel.setId(100L);
        testTravel.setUser(testUser);
        testTravel.setTitle("Summer Trip");
        testTravel.setDescription("A summer vacation");
        testTravel.setStartDate(LocalDate.of(2026, 6, 1));
        testTravel.setEndDate(LocalDate.of(2026, 6, 15));
        testTravel.setDurationDays(14);
        testTravel.setTotalPrice(new BigDecimal("5000.00"));
        testTravel.setStatus(TravelStatus.draft);
        testTravel.setCreatedAt(LocalDateTime.now());
        testTravel.setUpdatedAt(LocalDateTime.now());

        testTravelDestination = new TravelDestination();
        testTravelDestination.setId(1000L);
        testTravelDestination.setTravel(testTravel);
        testTravelDestination.setDestination(testDestination);
        testTravelDestination.setVisitOrder(1);
        testTravelDestination.setArrivalDate(LocalDate.of(2026, 6, 1));
        testTravelDestination.setDepartureDate(LocalDate.of(2026, 6, 5));
        testTravelDestination.setNotes("First stop");
        testTravelDestination.setCreatedAt(LocalDateTime.now());
    }

    private void setupTransactionTemplateInvocation() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    void getAllTravels_Success() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findAll()).thenReturn(List.of(testTravel));

        Flux<TravelResponse> result = travelService.getAllTravels();

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(100L, response.getId());
                    assertEquals("Summer Trip", response.getTitle());
                    assertEquals(TravelStatus.draft, response.getStatus());
                    assertEquals(1L, response.getUserId());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getAllTravels_EmptyList() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findAll()).thenReturn(List.of());

        Flux<TravelResponse> result = travelService.getAllTravels();

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void getTravelById_Success() {
        setupTransactionTemplateInvocation();
        testTravel.getDestinations().add(testTravelDestination);
        when(travelRepository.findByIdWithDestinations(100L)).thenReturn(testTravel);

        StepVerifier.create(travelService.getTravelById(100L))
                .expectNextMatches(response -> {
                    assertEquals(100L, response.getId());
                    assertEquals("Summer Trip", response.getTitle());
                    assertEquals(1, response.getDestinations().size());
                    assertEquals("Paris", response.getDestinations().get(0).getDestination().getName());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getTravelById_NotFound() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findByIdWithDestinations(99L)).thenReturn(null);

        StepVerifier.create(travelService.getTravelById(99L))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException
                                && throwable.getMessage().equals("Travel not found with id: 99")
                )
                .verify();
    }

    @Test
    void getTravelsByUser_Success() {
        setupTransactionTemplateInvocation();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(travelRepository.findByUser(testUser)).thenReturn(List.of(testTravel));

        StepVerifier.create(travelService.getTravelsByUser(1L))
                .expectNextMatches(response -> {
                    assertEquals("Summer Trip", response.getTitle());
                    assertEquals(1L, response.getUserId());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getTravelsByUser_UserNotFound() {
        setupTransactionTemplateInvocation();
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        StepVerifier.create(travelService.getTravelsByUser(99L))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException
                                && throwable.getMessage().equals("User not found with id: 99")
                )
                .verify();
    }

    @Test
    void getTravelsByStatus_Success() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findByStatus(TravelStatus.draft)).thenReturn(List.of(testTravel));

        StepVerifier.create(travelService.getTravelsByStatus(TravelStatus.draft))
                .expectNextMatches(response -> {
                    assertEquals(TravelStatus.draft, response.getStatus());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void createTravel_WithoutDestinations_Success() {
        setupTransactionTemplateInvocation();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(travelRepository.save(any(Travel.class))).thenAnswer(invocation -> {
            Travel t = invocation.getArgument(0);
            t.setId(100L);
            t.setCreatedAt(LocalDateTime.now());
            t.setUpdatedAt(LocalDateTime.now());
            return t;
        });
        when(travelRepository.findByIdWithDestinations(100L)).thenReturn(null);

        TravelCreateRequest request = TravelCreateRequest.builder()
                .title("Winter Trip")
                .description("A winter getaway")
                .startDate(LocalDate.of(2026, 12, 1))
                .endDate(LocalDate.of(2026, 12, 10))
                .durationDays(9)
                .totalPrice(new BigDecimal("3000.00"))
                .userId(1L)
                .build();

        StepVerifier.create(travelService.createTravel(request))
                .expectNextMatches(response -> {
                    assertNotNull(response.getId());
                    assertEquals("Winter Trip", response.getTitle());
                    assertEquals(1L, response.getUserId());
                    return true;
                })
                .verifyComplete();

        verify(travelRepository).save(any(Travel.class));
    }

    @Test
    void createTravel_WithDestinations_Success() {
        setupTransactionTemplateInvocation();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(destinationRepository.findById(10L)).thenReturn(Optional.of(testDestination));
        when(travelRepository.save(any(Travel.class))).thenAnswer(invocation -> {
            Travel t = invocation.getArgument(0);
            t.setId(100L);
            t.setCreatedAt(LocalDateTime.now());
            t.setUpdatedAt(LocalDateTime.now());
            return t;
        });
        when(travelRepository.findByIdWithDestinations(100L)).thenAnswer(invocation -> {
            Travel reload = new Travel();
            reload.setId(100L);
            reload.setUser(testUser);
            reload.setTitle("Summer Trip");
            reload.setStartDate(LocalDate.of(2026, 6, 1));
            reload.setEndDate(LocalDate.of(2026, 6, 15));
            reload.setDurationDays(14);
            reload.setStatus(TravelStatus.draft);
            reload.setCreatedAt(LocalDateTime.now());
            reload.setUpdatedAt(LocalDateTime.now());

            TravelDestination td = new TravelDestination();
            td.setId(1000L);
            td.setTravel(reload);
            td.setDestination(testDestination);
            td.setVisitOrder(1);
            td.setArrivalDate(LocalDate.of(2026, 6, 1));
            td.setDepartureDate(LocalDate.of(2026, 6, 5));
            td.setNotes("First stop");
            td.setCreatedAt(LocalDateTime.now());
            reload.getDestinations().add(td);

            return reload;
        });

        TravelDestinationCreateRequest destRequest = TravelDestinationCreateRequest.builder()
                .destinationId(10L)
                .visitOrder(1)
                .arrivalDate(LocalDate.of(2026, 6, 1))
                .departureDate(LocalDate.of(2026, 6, 5))
                .notes("First stop")
                .build();

        TravelCreateRequest request = TravelCreateRequest.builder()
                .title("Summer Trip")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 15))
                .durationDays(14)
                .userId(1L)
                .destinations(List.of(destRequest))
                .build();

        StepVerifier.create(travelService.createTravel(request))
                .expectNextMatches(response -> {
                    assertEquals("Summer Trip", response.getTitle());
                    assertEquals(1, response.getDestinations().size());
                    assertEquals("Paris", response.getDestinations().get(0).getDestination().getName());
                    return true;
                })
                .verifyComplete();

        verify(travelRepository).save(any(Travel.class));
    }

    @Test
    void createTravel_UserNotFound() {
        setupTransactionTemplateInvocation();
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        TravelCreateRequest request = TravelCreateRequest.builder()
                .title("Trip")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 1, 5))
                .durationDays(4)
                .userId(99L)
                .build();

        StepVerifier.create(travelService.createTravel(request))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException
                                && throwable.getMessage().equals("User not found with id: 99")
                )
                .verify();

        verify(travelRepository, never()).save(any());
    }

    @Test
    void createTravel_DestinationNotFound() {
        setupTransactionTemplateInvocation();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(destinationRepository.findById(99L)).thenReturn(Optional.empty());

        TravelDestinationCreateRequest destRequest = TravelDestinationCreateRequest.builder()
                .destinationId(99L)
                .visitOrder(1)
                .build();

        TravelCreateRequest request = TravelCreateRequest.builder()
                .title("Trip")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 1, 5))
                .durationDays(4)
                .userId(1L)
                .destinations(List.of(destRequest))
                .build();

        StepVerifier.create(travelService.createTravel(request))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException
                                && throwable.getMessage().equals("Destination not found with id: 99")
                )
                .verify();

        verify(travelRepository, never()).save(any());
    }

    @Test
    void updateTravel_UpdateTitle_Success() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findById(100L)).thenReturn(Optional.of(testTravel));
        when(travelRepository.save(any(Travel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(travelRepository.findByIdWithDestinations(100L)).thenReturn(testTravel);

        TravelUpdateRequest request = TravelUpdateRequest.builder()
                .title("Updated Trip Title")
                .build();

        StepVerifier.create(travelService.updateTravel(100L, request))
                .expectNextMatches(response -> {
                    assertEquals("Updated Trip Title", response.getTitle());
                    return true;
                })
                .verifyComplete();

        verify(travelRepository).save(any(Travel.class));
    }

    @Test
    void updateTravel_UpdateStatus_Success() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findById(100L)).thenReturn(Optional.of(testTravel));
        when(travelRepository.save(any(Travel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(travelRepository.findByIdWithDestinations(100L)).thenReturn(testTravel);

        TravelUpdateRequest request = TravelUpdateRequest.builder()
                .status(TravelStatus.confirmed)
                .build();

        StepVerifier.create(travelService.updateTravel(100L, request))
                .expectNextMatches(response -> {
                    assertEquals(TravelStatus.confirmed, response.getStatus());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void updateTravel_NotFound() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findById(99L)).thenReturn(Optional.empty());

        TravelUpdateRequest request = TravelUpdateRequest.builder()
                .title("New Title")
                .build();

        StepVerifier.create(travelService.updateTravel(99L, request))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException
                                && throwable.getMessage().equals("Travel not found with id: 99")
                )
                .verify();
    }

    @Test
    void deleteTravel_Success() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findById(100L)).thenReturn(Optional.of(testTravel));

        StepVerifier.create(travelService.deleteTravel(100L))
                .verifyComplete();

        verify(travelActivityRepository).deleteByTravel(testTravel);
        verify(travelAccommodationRepository).deleteByTravel(testTravel);
        verify(transportationSegmentRepository).deleteByTravel(testTravel);
        verify(travelDestinationRepository).deleteByTravel(testTravel);
        verify(travelRepository).delete(testTravel);
    }

    @Test
    void deleteTravel_NotFound() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findById(99L)).thenReturn(Optional.empty());

        StepVerifier.create(travelService.deleteTravel(99L))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException
                                && throwable.getMessage().equals("Travel not found with id: 99")
                )
                .verify();

        verify(travelRepository, never()).delete(any());
    }
}
