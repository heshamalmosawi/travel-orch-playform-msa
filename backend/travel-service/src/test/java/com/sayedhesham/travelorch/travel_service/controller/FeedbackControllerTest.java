package com.sayedhesham.travelorch.travel_service.controller;

import com.sayedhesham.travelorch.travel_service.dto.FeedbackCreateRequest;
import com.sayedhesham.travelorch.travel_service.dto.FeedbackResponse;
import com.sayedhesham.travelorch.travel_service.dto.FeedbackUpdateRequest;
import com.sayedhesham.travelorch.travel_service.service.FeedbackService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackControllerTest {

    @Mock
    private FeedbackService feedbackService;

    @InjectMocks
    private FeedbackController feedbackController;

    private WebTestClient webTestClient;
    private FeedbackResponse feedbackResponse;
    private FeedbackResponse anotherFeedbackResponse;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(feedbackController)
                .webFilter((exchange, chain) ->
                        chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(getAuthentication()))
                )
                .build();

        feedbackResponse = FeedbackResponse.builder()
                .id(1L)
                .travelId(100L)
                .managerId(20L)
                .reviewerId(10L)
                .reviewerUsername("reviewer")
                .rating(4)
                .comment("Great trip!")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        anotherFeedbackResponse = FeedbackResponse.builder()
                .id(2L)
                .travelId(101L)
                .managerId(20L)
                .reviewerId(10L)
                .reviewerUsername("reviewer")
                .rating(5)
                .comment("Perfect!")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private UsernamePasswordAuthenticationToken getAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "reviewer", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // -------------------------------------------------------------------------
    // POST /feedbacks
    // -------------------------------------------------------------------------

    @Test
    void createFeedback_Returns201Created() {
        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setTravelId(100L);
        request.setRating(4);
        request.setComment("Great trip!");

        when(feedbackService.createFeedback(eq("reviewer"), any(FeedbackCreateRequest.class)))
                .thenReturn(Mono.just(feedbackResponse));

        webTestClient.post()
                .uri("/feedbacks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FeedbackResponse.class)
                .value(r -> {
                    assertEquals(1L, r.getId());
                    assertEquals(100L, r.getTravelId());
                    assertEquals(4, r.getRating());
                    assertEquals("Great trip!", r.getComment());
                    assertEquals("reviewer", r.getReviewerUsername());
                });
    }

    @Test
    void createFeedback_NoCompletedPurchase_Returns403Forbidden() {
        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setTravelId(100L);
        request.setRating(3);

        when(feedbackService.createFeedback(eq("reviewer"), any(FeedbackCreateRequest.class)))
                .thenReturn(Mono.error(new SecurityException("You must have a completed purchase")));

        webTestClient.post()
                .uri("/feedbacks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void createFeedback_AlreadyReviewed_Returns409Conflict() {
        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setTravelId(100L);
        request.setRating(3);

        when(feedbackService.createFeedback(eq("reviewer"), any(FeedbackCreateRequest.class)))
                .thenReturn(Mono.error(new IllegalStateException("You have already reviewed this travel package")));

        webTestClient.post()
                .uri("/feedbacks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void createFeedback_TravelNotFound_Returns404() {
        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setTravelId(999L);
        request.setRating(3);

        when(feedbackService.createFeedback(eq("reviewer"), any(FeedbackCreateRequest.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Travel not found with id: 999")));

        webTestClient.post()
                .uri("/feedbacks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }

    // -------------------------------------------------------------------------
    // GET /feedbacks?travelId=X
    // -------------------------------------------------------------------------

    @Test
    void getFeedbacks_ByTravelId_ReturnsOk() {
        when(feedbackService.getFeedbacksForTravel(100L))
                .thenReturn(Flux.just(feedbackResponse));

        webTestClient.get()
                .uri("/feedbacks?travelId=100")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(FeedbackResponse.class)
                .hasSize(1)
                .value(list -> {
                    assertEquals(1L, list.get(0).getId());
                    assertEquals(100L, list.get(0).getTravelId());
                    assertEquals(4, list.get(0).getRating());
                });
    }

    @Test
    void getFeedbacks_ByTravelId_EmptyList_ReturnsOk() {
        when(feedbackService.getFeedbacksForTravel(100L))
                .thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/feedbacks?travelId=100")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(FeedbackResponse.class)
                .hasSize(0);
    }

    @Test
    void getFeedbacks_ByTravelId_MultipleReviews_ReturnsAll() {
        FeedbackResponse secondReview = FeedbackResponse.builder()
                .id(3L)
                .travelId(100L)
                .managerId(20L)
                .reviewerId(11L)
                .reviewerUsername("another_reviewer")
                .rating(5)
                .comment("Amazing!")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(feedbackService.getFeedbacksForTravel(100L))
                .thenReturn(Flux.just(feedbackResponse, secondReview));

        webTestClient.get()
                .uri("/feedbacks?travelId=100")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(FeedbackResponse.class)
                .hasSize(2);
    }

    // -------------------------------------------------------------------------
    // GET /feedbacks?managerId=X
    // -------------------------------------------------------------------------

    @Test
    void getFeedbacks_ByManagerId_ReturnsOk() {
        when(feedbackService.getFeedbacksForManager(20L))
                .thenReturn(Flux.just(feedbackResponse, anotherFeedbackResponse));

        webTestClient.get()
                .uri("/feedbacks?managerId=20")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(FeedbackResponse.class)
                .hasSize(2)
                .value(list -> {
                    assertEquals(20L, list.get(0).getManagerId());
                    assertEquals(20L, list.get(1).getManagerId());
                });
    }

    @Test
    void getFeedbacks_ByManagerId_EmptyList_ReturnsOk() {
        when(feedbackService.getFeedbacksForManager(20L))
                .thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/feedbacks?managerId=20")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(FeedbackResponse.class)
                .hasSize(0);
    }

    @Test
    void getFeedbacks_ByManagerId_ReviewsSpanMultiplePackages() {
        when(feedbackService.getFeedbacksForManager(20L))
                .thenReturn(Flux.just(feedbackResponse, anotherFeedbackResponse));

        webTestClient.get()
                .uri("/feedbacks?managerId=20")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(FeedbackResponse.class)
                .value(list -> {
                    assertEquals(100L, list.get(0).getTravelId());
                    assertEquals(101L, list.get(1).getTravelId());
                });
    }

    // -------------------------------------------------------------------------
    // GET /feedbacks (no params) → 400
    // -------------------------------------------------------------------------

    @Test
    void getFeedbacks_NoParams_Returns400BadRequest() {
        webTestClient.get()
                .uri("/feedbacks")
                .exchange()
                .expectStatus().isBadRequest();
    }

    // -------------------------------------------------------------------------
    // GET /feedbacks/me?travelId=X
    // -------------------------------------------------------------------------

    @Test
    void getMyFeedback_Found_ReturnsOk() {
        when(feedbackService.getMyFeedbackForTravel(100L, "reviewer"))
                .thenReturn(Mono.just(feedbackResponse));

        webTestClient.get()
                .uri("/feedbacks/me?travelId=100")
                .exchange()
                .expectStatus().isOk()
                .expectBody(FeedbackResponse.class)
                .value(r -> {
                    assertEquals(1L, r.getId());
                    assertEquals("reviewer", r.getReviewerUsername());
                    assertEquals(4, r.getRating());
                });
    }

    @Test
    void getMyFeedback_NotFound_Returns404() {
        when(feedbackService.getMyFeedbackForTravel(100L, "reviewer"))
                .thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/feedbacks/me?travelId=100")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getMyFeedback_UserNotFound_Returns404() {
        when(feedbackService.getMyFeedbackForTravel(100L, "reviewer"))
                .thenReturn(Mono.error(new IllegalArgumentException("User not found: reviewer")));

        webTestClient.get()
                .uri("/feedbacks/me?travelId=100")
                .exchange()
                .expectStatus().isNotFound();
    }

    // -------------------------------------------------------------------------
    // PUT /feedbacks/{id}
    // -------------------------------------------------------------------------

    @Test
    void updateFeedback_Returns200Ok() {
        FeedbackUpdateRequest request = new FeedbackUpdateRequest();
        request.setRating(5);
        request.setComment("Updated comment");

        FeedbackResponse updated = FeedbackResponse.builder()
                .id(1L)
                .travelId(100L)
                .managerId(20L)
                .reviewerId(10L)
                .reviewerUsername("reviewer")
                .rating(5)
                .comment("Updated comment")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(feedbackService.updateFeedback(eq(1L), eq("reviewer"), any(FeedbackUpdateRequest.class)))
                .thenReturn(Mono.just(updated));

        webTestClient.put()
                .uri("/feedbacks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(FeedbackResponse.class)
                .value(r -> {
                    assertEquals(5, r.getRating());
                    assertEquals("Updated comment", r.getComment());
                });
    }

    @Test
    void updateFeedback_NotFound_Returns404() {
        FeedbackUpdateRequest request = new FeedbackUpdateRequest();
        request.setRating(5);

        when(feedbackService.updateFeedback(eq(99L), eq("reviewer"), any(FeedbackUpdateRequest.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Feedback not found with id: 99")));

        webTestClient.put()
                .uri("/feedbacks/99")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateFeedback_NotOwner_Returns403Forbidden() {
        FeedbackUpdateRequest request = new FeedbackUpdateRequest();
        request.setRating(1);

        when(feedbackService.updateFeedback(eq(1L), eq("reviewer"), any(FeedbackUpdateRequest.class)))
                .thenReturn(Mono.error(new SecurityException("You can only edit your own reviews")));

        webTestClient.put()
                .uri("/feedbacks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isForbidden();
    }

    // -------------------------------------------------------------------------
    // DELETE /feedbacks/{id}
    // -------------------------------------------------------------------------

    @Test
    void deleteFeedback_Returns204NoContent() {
        when(feedbackService.deleteFeedback(1L, "reviewer"))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/feedbacks/1")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void deleteFeedback_NotFound_Returns404() {
        when(feedbackService.deleteFeedback(99L, "reviewer"))
                .thenReturn(Mono.error(new IllegalArgumentException("Feedback not found with id: 99")));

        webTestClient.delete()
                .uri("/feedbacks/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteFeedback_NotOwner_Returns403Forbidden() {
        when(feedbackService.deleteFeedback(1L, "reviewer"))
                .thenReturn(Mono.error(new SecurityException("You do not have permission to delete this review")));

        webTestClient.delete()
                .uri("/feedbacks/1")
                .exchange()
                .expectStatus().isForbidden();
    }

    // -------------------------------------------------------------------------
    // GET /feedbacks/user/{userId}
    // -------------------------------------------------------------------------

    @Test
    void getFeedbacksByReviewer_ReturnsOk() {
        when(feedbackService.getFeedbacksByReviewer(eq(10L), eq("reviewer")))
                .thenReturn(Flux.just(feedbackResponse, anotherFeedbackResponse));

        webTestClient.get()
                .uri("/feedbacks/user/10")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(FeedbackResponse.class)
                .hasSize(2)
                .value(list -> {
                    assertEquals(10L, list.get(0).getReviewerId());
                    assertEquals(10L, list.get(1).getReviewerId());
                });
    }

    @Test
    void getFeedbacksByReviewer_Forbidden() {
        when(feedbackService.getFeedbacksByReviewer(eq(10L), eq("reviewer")))
                .thenReturn(Flux.error(new SecurityException("You do not have permission to view these reviews")));

        webTestClient.get()
                .uri("/feedbacks/user/10")
                .exchange()
                .expectStatus().isForbidden();
    }
}
