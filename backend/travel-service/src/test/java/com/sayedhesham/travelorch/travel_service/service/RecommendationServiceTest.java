package com.sayedhesham.travelorch.travel_service.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.sayedhesham.travelorch.common.entity.feedback.TravelFeedback;
import com.sayedhesham.travelorch.common.entity.payment.PaymentTransaction;
import com.sayedhesham.travelorch.common.entity.travel.Travel;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.enums.PaymentStatus;
import com.sayedhesham.travelorch.common.enums.TravelStatus;
import com.sayedhesham.travelorch.common.repository.feedback.TravelFeedbackRepository;
import com.sayedhesham.travelorch.common.repository.neo4j.TravelGraphRepository;
import com.sayedhesham.travelorch.common.repository.payment.PaymentTransactionRepository;
import com.sayedhesham.travelorch.common.repository.travel.TravelRepository;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;

import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock private TravelGraphRepository travelGraphRepository;
    @Mock private TravelRepository travelRepository;
    @Mock private TravelFeedbackRepository travelFeedbackRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks
    private RecommendationService recommendationService;

    private User user;
    private User manager;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(10L);
        user.setUsername("traveler");

        manager = new User();
        manager.setId(20L);
        manager.setFirstName("Jane");
        manager.setLastName("Doe");
    }

    private void setupTransaction() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    private Travel travel(long id, String title, double price) {
        Travel t = new Travel();
        t.setId(id);
        t.setManager(manager);
        t.setTitle(title);
        t.setStartDate(LocalDate.now().plusDays(30));
        t.setEndDate(LocalDate.now().plusDays(40));
        t.setTotalPrice(BigDecimal.valueOf(price));
        t.setStatus(TravelStatus.confirmed);
        return t;
    }

    private TravelFeedback feedback(int rating) {
        TravelFeedback f = new TravelFeedback();
        f.setRating(rating);
        return f;
    }

    @Test
    void getRecommendations_GraphHits_MapsInOrder() {
        setupTransaction();
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(user));
        when(travelGraphRepository.findRecommendedTravelIds(eq(10L), anyLong(), anyDouble(), anyInt()))
                .thenReturn(List.of(200L, 100L));
        when(travelRepository.findByIdWithDestinations(200L)).thenReturn(travel(200L, "Beach", 1000));
        when(travelRepository.findByIdWithDestinations(100L)).thenReturn(travel(100L, "Mountain", 2000));

        StepVerifier.create(recommendationService.getRecommendations("traveler"))
                .expectNextMatches(r -> r.getId().equals(200L) && r.getTitle().equals("Beach"))
                .expectNextMatches(r -> r.getId().equals(100L) && r.getTitle().equals("Mountain"))
                .verifyComplete();
    }

    @Test
    void getRecommendations_GraphHits_SkipsMissingTravels() {
        setupTransaction();
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(user));
        when(travelGraphRepository.findRecommendedTravelIds(eq(10L), anyLong(), anyDouble(), anyInt()))
                .thenReturn(List.of(200L, 999L));
        when(travelRepository.findByIdWithDestinations(200L)).thenReturn(travel(200L, "Beach", 1000));
        when(travelRepository.findByIdWithDestinations(999L)).thenReturn(null);

        StepVerifier.create(recommendationService.getRecommendations("traveler"))
                .expectNextMatches(r -> r.getId().equals(200L))
                .verifyComplete();
    }

    @Test
    void getRecommendations_EmptyGraph_ColdStartTopRatedExcludingPurchased() {
        setupTransaction();
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(user));
        when(travelGraphRepository.findRecommendedTravelIds(eq(10L), anyLong(), anyDouble(), anyInt()))
                .thenReturn(List.of());

        Travel purchased = travel(1L, "Purchased", 1000);
        Travel highRated = travel(2L, "HighRated", 1500);
        Travel lowRated = travel(3L, "LowRated", 1500);

        PaymentTransaction completed = new PaymentTransaction();
        completed.setStatus(PaymentStatus.completed);
        completed.setTravel(purchased);
        when(paymentTransactionRepository.findByBuyerId(10L)).thenReturn(List.of(completed));

        when(travelRepository.findAllUpcomingTravels(any(LocalDate.class), eq(TravelStatus.cancelled)))
                .thenReturn(List.of(purchased, lowRated, highRated));
        when(travelFeedbackRepository.findByTravelId(2L)).thenReturn(List.of(feedback(5), feedback(5)));
        when(travelFeedbackRepository.findByTravelId(3L)).thenReturn(List.of(feedback(2)));

        StepVerifier.create(recommendationService.getRecommendations("traveler"))
                .expectNextMatches(r -> r.getId().equals(2L))
                .expectNextMatches(r -> r.getId().equals(3L))
                .verifyComplete();
    }

    @Test
    void getRecommendations_UserNotFound_ThrowsIllegalArgument() {
        setupTransaction();
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        StepVerifier.create(recommendationService.getRecommendations("ghost"))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().equals("User not found: ghost"))
                .verify();
    }
}
