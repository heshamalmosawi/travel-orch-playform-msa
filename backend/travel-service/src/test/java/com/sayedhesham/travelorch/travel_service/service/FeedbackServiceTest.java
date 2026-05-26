package com.sayedhesham.travelorch.travel_service.service;

import com.sayedhesham.travelorch.common.entity.feedback.TravelFeedback;
import com.sayedhesham.travelorch.common.entity.payment.PaymentTransaction;
import com.sayedhesham.travelorch.common.entity.rbac.Permission;
import com.sayedhesham.travelorch.common.entity.rbac.Role;
import com.sayedhesham.travelorch.common.entity.travel.Travel;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.enums.PaymentStatus;
import com.sayedhesham.travelorch.common.enums.TravelStatus;
import com.sayedhesham.travelorch.common.repository.feedback.TravelFeedbackRepository;
import com.sayedhesham.travelorch.common.repository.payment.PaymentTransactionRepository;
import com.sayedhesham.travelorch.common.repository.travel.TravelRepository;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.travel_service.dto.FeedbackCreateRequest;
import com.sayedhesham.travelorch.travel_service.dto.FeedbackResponse;
import com.sayedhesham.travelorch.travel_service.dto.FeedbackUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock private TravelFeedbackRepository feedbackRepository;
    @Mock private TravelRepository travelRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private GraphSyncService graphSyncService;

    @InjectMocks
    private FeedbackService feedbackService;

    private User reviewer;
    private User manager;
    private User adminUser;
    private Travel travel;
    private TravelFeedback feedback;

    @BeforeEach
    void setUp() {
        Permission feedbackWritePermission = new Permission();
        feedbackWritePermission.setId(1L);
        feedbackWritePermission.setResource("feedbacks");
        feedbackWritePermission.setAction("write");

        Permission travelReadPermission = new Permission();
        travelReadPermission.setId(2L);
        travelReadPermission.setResource("travels");
        travelReadPermission.setAction("read");

        Permission adminPermission = new Permission();
        adminPermission.setId(3L);
        adminPermission.setResource("admin");
        adminPermission.setAction("all");

        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName("user");
        userRole.setPermissions(new HashSet<>());

        Role managerRole = new Role();
        managerRole.setId(2L);
        managerRole.setName("travel_manager");
        managerRole.setPermissions(new HashSet<>());

        Role adminRole = new Role();
        adminRole.setId(3L);
        adminRole.setName("admin");
        Set<Permission> adminPermissions = new HashSet<>();
        adminPermissions.add(feedbackWritePermission);
        adminPermissions.add(adminPermission);
        adminRole.setPermissions(adminPermissions);

        reviewer = new User();
        reviewer.setId(10L);
        reviewer.setUsername("reviewer");
        reviewer.setEmail("reviewer@example.com");
        reviewer.setRole(userRole);

        manager = new User();
        manager.setId(20L);
        manager.setUsername("manager");
        manager.setEmail("manager@example.com");
        manager.setRole(managerRole);

        adminUser = new User();
        adminUser.setId(30L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(adminRole);

        travel = new Travel();
        travel.setId(100L);
        travel.setManager(manager);
        travel.setTitle("Summer Trip");
        travel.setStartDate(LocalDate.of(2026, 6, 1));
        travel.setEndDate(LocalDate.of(2026, 6, 15));
        travel.setTotalPrice(new BigDecimal("5000.00"));
        travel.setStatus(TravelStatus.confirmed);
        travel.setCreatedAt(LocalDateTime.now());
        travel.setUpdatedAt(LocalDateTime.now());

        feedback = new TravelFeedback();
        feedback.setId(1L);
        feedback.setTravelId(100L);
        feedback.setManagerId(20L);
        feedback.setReviewer(reviewer);
        feedback.setRating(4);
        feedback.setComment("Great trip!");
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setUpdatedAt(LocalDateTime.now());
    }

    private void setupTransaction() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    private PaymentTransaction completedPayment() {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setStatus(PaymentStatus.completed);
        return tx;
    }

    private PaymentTransaction pendingPayment() {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setStatus(PaymentStatus.pending);
        return tx;
    }

    // -------------------------------------------------------------------------
    // createFeedback
    // -------------------------------------------------------------------------

    @Test
    void createFeedback_Success() {
        setupTransaction();
        when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
        when(travelRepository.findById(100L)).thenReturn(Optional.of(travel));
        when(paymentTransactionRepository.findByBuyerIdAndTravelId(10L, 100L))
                .thenReturn(List.of(completedPayment()));
        when(feedbackRepository.existsByTravelIdAndReviewerId(100L, 10L)).thenReturn(false);
        when(feedbackRepository.save(any(TravelFeedback.class))).thenAnswer(inv -> {
            TravelFeedback saved = inv.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setTravelId(100L);
        request.setRating(5);
        request.setComment("Amazing!");

        StepVerifier.create(feedbackService.createFeedback("reviewer", request))
                .expectNextMatches(response -> {
                    assertEquals(100L, response.getTravelId());
                    assertEquals(20L, response.getManagerId());
                    assertEquals(5, response.getRating());
                    assertEquals("Amazing!", response.getComment());
                    assertEquals("reviewer", response.getReviewerUsername());
                    return true;
                })
                .verifyComplete();

        verify(feedbackRepository).save(any(TravelFeedback.class));
        verify(graphSyncService).recordReview(10L, 100L, 5);
    }

    @Test
    void createFeedback_ReviewerNotFound_ThrowsIllegalArgument() {
        setupTransaction();
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setTravelId(100L);
        request.setRating(3);

        StepVerifier.create(feedbackService.createFeedback("ghost", request))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().equals("User not found: ghost"))
                .verify();

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void createFeedback_TravelNotFound_ThrowsIllegalArgument() {
        setupTransaction();
        when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
        when(travelRepository.findById(999L)).thenReturn(Optional.empty());

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setTravelId(999L);
        request.setRating(3);

        StepVerifier.create(feedbackService.createFeedback("reviewer", request))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().equals("Travel not found with id: 999"))
                .verify();

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void createFeedback_NoCompletedPurchase_ThrowsSecurity() {
        setupTransaction();
        when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
        when(travelRepository.findById(100L)).thenReturn(Optional.of(travel));
        when(paymentTransactionRepository.findByBuyerIdAndTravelId(10L, 100L))
                .thenReturn(List.of(pendingPayment()));

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setTravelId(100L);
        request.setRating(3);

        StepVerifier.create(feedbackService.createFeedback("reviewer", request))
                .expectErrorMatches(ex -> ex instanceof SecurityException
                        && ex.getMessage().contains("completed purchase"))
                .verify();

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void createFeedback_NoPurchaseAtAll_ThrowsSecurity() {
        setupTransaction();
        when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
        when(travelRepository.findById(100L)).thenReturn(Optional.of(travel));
        when(paymentTransactionRepository.findByBuyerIdAndTravelId(10L, 100L))
                .thenReturn(List.of());

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setTravelId(100L);
        request.setRating(3);

        StepVerifier.create(feedbackService.createFeedback("reviewer", request))
                .expectErrorMatches(ex -> ex instanceof SecurityException)
                .verify();
    }

    @Test
    void createFeedback_AlreadyReviewed_ThrowsIllegalState() {
        setupTransaction();
        when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
        when(travelRepository.findById(100L)).thenReturn(Optional.of(travel));
        when(paymentTransactionRepository.findByBuyerIdAndTravelId(10L, 100L))
                .thenReturn(List.of(completedPayment()));
        when(feedbackRepository.existsByTravelIdAndReviewerId(100L, 10L)).thenReturn(true);

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setTravelId(100L);
        request.setRating(3);

        StepVerifier.create(feedbackService.createFeedback("reviewer", request))
                .expectErrorMatches(ex -> ex instanceof IllegalStateException
                        && ex.getMessage().contains("already reviewed"))
                .verify();

        verify(feedbackRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getFeedbacksForTravel
    // -------------------------------------------------------------------------

    @Test
    void getFeedbacksForTravel_ReturnsList() {
        setupTransaction();
        TravelFeedback second = new TravelFeedback();
        second.setId(2L);
        second.setTravelId(100L);
        second.setManagerId(20L);
        second.setReviewer(reviewer);
        second.setRating(3);
        second.setComment("Decent");
        second.setCreatedAt(LocalDateTime.now());
        second.setUpdatedAt(LocalDateTime.now());

        when(feedbackRepository.findByTravelId(100L)).thenReturn(List.of(feedback, second));

        StepVerifier.create(feedbackService.getFeedbacksForTravel(100L))
                .expectNextMatches(r -> r.getId().equals(1L) && r.getRating() == 4)
                .expectNextMatches(r -> r.getId().equals(2L) && r.getRating() == 3)
                .verifyComplete();
    }

    @Test
    void getFeedbacksForTravel_EmptyList() {
        setupTransaction();
        when(feedbackRepository.findByTravelId(100L)).thenReturn(List.of());

        StepVerifier.create(feedbackService.getFeedbacksForTravel(100L))
                .verifyComplete();
    }

    // -------------------------------------------------------------------------
    // getMyFeedbackForTravel
    // -------------------------------------------------------------------------

    @Test
    void getMyFeedbackForTravel_Found() {
        setupTransaction();
        when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
        when(feedbackRepository.findByTravelIdAndReviewerId(100L, 10L))
                .thenReturn(Optional.of(feedback));

        StepVerifier.create(feedbackService.getMyFeedbackForTravel(100L, "reviewer"))
                .expectNextMatches(r -> r.getId().equals(1L) && r.getRating() == 4)
                .verifyComplete();
    }

    @Test
    void getMyFeedbackForTravel_NotFound_ReturnsEmpty() {
        setupTransaction();
        when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
        when(feedbackRepository.findByTravelIdAndReviewerId(100L, 10L))
                .thenReturn(Optional.empty());

        StepVerifier.create(feedbackService.getMyFeedbackForTravel(100L, "reviewer"))
                .verifyComplete();
    }

    @Test
    void getMyFeedbackForTravel_UserNotFound_ThrowsIllegalArgument() {
        setupTransaction();
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        StepVerifier.create(feedbackService.getMyFeedbackForTravel(100L, "ghost"))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().equals("User not found: ghost"))
                .verify();
    }

    // -------------------------------------------------------------------------
    // getFeedbacksForManager  (no longer requires permission)
    // -------------------------------------------------------------------------

    @Test
    void getFeedbacksForManager_ReturnsList() {
        setupTransaction();
        TravelFeedback anotherFeedback = new TravelFeedback();
        anotherFeedback.setId(3L);
        anotherFeedback.setTravelId(101L);
        anotherFeedback.setManagerId(20L);
        anotherFeedback.setReviewer(reviewer);
        anotherFeedback.setRating(5);
        anotherFeedback.setComment("Perfect!");
        anotherFeedback.setCreatedAt(LocalDateTime.now());
        anotherFeedback.setUpdatedAt(LocalDateTime.now());

        when(feedbackRepository.findByManagerId(20L)).thenReturn(List.of(feedback, anotherFeedback));

        StepVerifier.create(feedbackService.getFeedbacksForManager(20L))
                .expectNextMatches(r -> r.getId().equals(1L) && r.getTravelId().equals(100L))
                .expectNextMatches(r -> r.getId().equals(3L) && r.getTravelId().equals(101L))
                .verifyComplete();
    }

    @Test
    void getFeedbacksForManager_EmptyList() {
        setupTransaction();
        when(feedbackRepository.findByManagerId(20L)).thenReturn(List.of());

        StepVerifier.create(feedbackService.getFeedbacksForManager(20L))
                .verifyComplete();
    }

    @Test
    void getFeedbacksForManager_AggregatesAcrossMultiplePackages() {
        setupTransaction();
        TravelFeedback f2 = new TravelFeedback();
        f2.setId(2L);
        f2.setTravelId(101L);
        f2.setManagerId(20L);
        f2.setReviewer(reviewer);
        f2.setRating(2);
        f2.setCreatedAt(LocalDateTime.now());
        f2.setUpdatedAt(LocalDateTime.now());

        TravelFeedback f3 = new TravelFeedback();
        f3.setId(3L);
        f3.setTravelId(102L);
        f3.setManagerId(20L);
        f3.setReviewer(reviewer);
        f3.setRating(5);
        f3.setCreatedAt(LocalDateTime.now());
        f3.setUpdatedAt(LocalDateTime.now());

        when(feedbackRepository.findByManagerId(20L)).thenReturn(List.of(feedback, f2, f3));

        StepVerifier.create(feedbackService.getFeedbacksForManager(20L))
                .expectNextCount(3)
                .verifyComplete();
    }

    // -------------------------------------------------------------------------
    // updateFeedback
    // -------------------------------------------------------------------------

    @Test
    void updateFeedback_UpdatesRatingAndComment_Success() {
        setupTransaction();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(feedbackRepository.save(any(TravelFeedback.class))).thenAnswer(inv -> inv.getArgument(0));

        FeedbackUpdateRequest request = new FeedbackUpdateRequest();
        request.setRating(5);
        request.setComment("Changed my mind, excellent!");

        StepVerifier.create(feedbackService.updateFeedback(1L, "reviewer", request))
                .expectNextMatches(r -> r.getRating() == 5 && "Changed my mind, excellent!".equals(r.getComment()))
                .verifyComplete();

        verify(feedbackRepository).save(any(TravelFeedback.class));
        verify(graphSyncService).recordReview(10L, 100L, 5);
    }

    @Test
    void updateFeedback_UpdatesOnlyRating_Success() {
        setupTransaction();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(feedbackRepository.save(any(TravelFeedback.class))).thenAnswer(inv -> inv.getArgument(0));

        FeedbackUpdateRequest request = new FeedbackUpdateRequest();
        request.setRating(2);

        StepVerifier.create(feedbackService.updateFeedback(1L, "reviewer", request))
                .expectNextMatches(r -> r.getRating() == 2 && "Great trip!".equals(r.getComment()))
                .verifyComplete();
    }

    @Test
    void updateFeedback_UpdatesOnlyComment_Success() {
        setupTransaction();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(feedbackRepository.save(any(TravelFeedback.class))).thenAnswer(inv -> inv.getArgument(0));

        FeedbackUpdateRequest request = new FeedbackUpdateRequest();
        request.setComment("Updated comment");

        StepVerifier.create(feedbackService.updateFeedback(1L, "reviewer", request))
                .expectNextMatches(r -> r.getRating() == 4 && "Updated comment".equals(r.getComment()))
                .verifyComplete();
    }

    @Test
    void updateFeedback_FeedbackNotFound_ThrowsIllegalArgument() {
        setupTransaction();
        when(feedbackRepository.findById(99L)).thenReturn(Optional.empty());

        StepVerifier.create(feedbackService.updateFeedback(99L, "reviewer", new FeedbackUpdateRequest()))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().equals("Feedback not found with id: 99"))
                .verify();

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void updateFeedback_NotReviewer_ThrowsSecurity() {
        setupTransaction();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

        FeedbackUpdateRequest request = new FeedbackUpdateRequest();
        request.setRating(1);

        StepVerifier.create(feedbackService.updateFeedback(1L, "somebody-else", request))
                .expectErrorMatches(ex -> ex instanceof SecurityException
                        && ex.getMessage().contains("own reviews"))
                .verify();

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void updateFeedback_NullReviewer_ThrowsSecurity() {
        setupTransaction();
        TravelFeedback orphan = new TravelFeedback();
        orphan.setId(5L);
        orphan.setTravelId(100L);
        orphan.setManagerId(20L);
        orphan.setReviewer(null);
        orphan.setRating(3);
        when(feedbackRepository.findById(5L)).thenReturn(Optional.of(orphan));

        StepVerifier.create(feedbackService.updateFeedback(5L, "reviewer", new FeedbackUpdateRequest()))
                .expectErrorMatches(ex -> ex instanceof SecurityException)
                .verify();
    }

    // -------------------------------------------------------------------------
    // deleteFeedback
    // -------------------------------------------------------------------------

    @Test
    void deleteFeedback_AsReviewer_Success() {
        setupTransaction();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));

        StepVerifier.create(feedbackService.deleteFeedback(1L, "reviewer"))
                .verifyComplete();

        verify(feedbackRepository).delete(feedback);
        verify(graphSyncService).removeReview(10L, 100L);
    }

    @Test
    void deleteFeedback_AsAdminWithFeedbackWritePermission_Success() {
        setupTransaction();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        StepVerifier.create(feedbackService.deleteFeedback(1L, "admin"))
                .verifyComplete();

        verify(feedbackRepository).delete(feedback);
    }

    @Test
    void deleteFeedback_FeedbackNotFound_ThrowsIllegalArgument() {
        setupTransaction();
        when(feedbackRepository.findById(99L)).thenReturn(Optional.empty());

        StepVerifier.create(feedbackService.deleteFeedback(99L, "reviewer"))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().equals("Feedback not found with id: 99"))
                .verify();

        verify(feedbackRepository, never()).delete(any());
    }

    @Test
    void deleteFeedback_UserNotFound_ThrowsIllegalArgument() {
        setupTransaction();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        StepVerifier.create(feedbackService.deleteFeedback(1L, "ghost"))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().equals("User not found: ghost"))
                .verify();

        verify(feedbackRepository, never()).delete(any());
    }

    @Test
    void deleteFeedback_NeitherReviewerNorAdmin_ThrowsSecurity() {
        setupTransaction();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));

        StepVerifier.create(feedbackService.deleteFeedback(1L, "manager"))
                .expectErrorMatches(ex -> ex instanceof SecurityException
                        && ex.getMessage().contains("permission"))
                .verify();

        verify(feedbackRepository, never()).delete(any());
    }

    @Test
    void deleteFeedback_NullReviewer_AdminCanStillDelete() {
        setupTransaction();
        TravelFeedback orphan = new TravelFeedback();
        orphan.setId(7L);
        orphan.setTravelId(100L);
        orphan.setManagerId(20L);
        orphan.setReviewer(null);
        orphan.setRating(3);
        when(feedbackRepository.findById(7L)).thenReturn(Optional.of(orphan));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        StepVerifier.create(feedbackService.deleteFeedback(7L, "admin"))
                .verifyComplete();

        verify(feedbackRepository).delete(orphan);
    }

    // -------------------------------------------------------------------------
    // getFeedbacksByReviewer
    // -------------------------------------------------------------------------

    @Test
    void getFeedbacksByReviewer_AsSelf_Success() {
        setupTransaction();
        when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
        when(feedbackRepository.findByReviewerId(10L)).thenReturn(List.of(feedback));

        StepVerifier.create(feedbackService.getFeedbacksByReviewer(10L, "reviewer"))
                .expectNextMatches(r -> r.getId().equals(1L) && r.getReviewerId().equals(10L))
                .verifyComplete();
    }

    @Test
    void getFeedbacksByReviewer_AsAdminWithAdminAll_Success() {
        setupTransaction();
        // adminUser has feedbacks:write + admin:all but NOT feedbacks:read — exercises the admin.all branch
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(feedbackRepository.findByReviewerId(10L)).thenReturn(List.of(feedback));

        StepVerifier.create(feedbackService.getFeedbacksByReviewer(10L, "admin"))
                .expectNextMatches(r -> r.getId().equals(1L))
                .verifyComplete();
    }

    @Test
    void getFeedbacksByReviewer_WithFeedbacksReadPermission_Success() {
        setupTransaction();
        Permission readPerm = new Permission();
        readPerm.setResource("feedbacks");
        readPerm.setAction("read");
        Role staffRole = new Role();
        staffRole.setId(9L);
        staffRole.setName("staff");
        Set<Permission> perms = new HashSet<>();
        perms.add(readPerm);
        staffRole.setPermissions(perms);
        User staff = new User();
        staff.setId(40L);
        staff.setUsername("staff");
        staff.setRole(staffRole);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(feedbackRepository.findByReviewerId(10L)).thenReturn(List.of(feedback));

        StepVerifier.create(feedbackService.getFeedbacksByReviewer(10L, "staff"))
                .expectNextMatches(r -> r.getId().equals(1L))
                .verifyComplete();
    }

    @Test
    void getFeedbacksByReviewer_NotSelf_NoPermission_ThrowsSecurity() {
        setupTransaction();
        // manager has empty permissions and is not the reviewer
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));

        StepVerifier.create(feedbackService.getFeedbacksByReviewer(10L, "manager"))
                .expectErrorMatches(ex -> ex instanceof SecurityException
                        && ex.getMessage().equals("You do not have permission to view these reviews"))
                .verify();

        verify(feedbackRepository, never()).findByReviewerId(any());
    }

    @Test
    void getFeedbacksByReviewer_UserNotFound_ThrowsIllegalArgument() {
        setupTransaction();
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        StepVerifier.create(feedbackService.getFeedbacksByReviewer(10L, "ghost"))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().equals("User not found: ghost"))
                .verify();
    }
}
