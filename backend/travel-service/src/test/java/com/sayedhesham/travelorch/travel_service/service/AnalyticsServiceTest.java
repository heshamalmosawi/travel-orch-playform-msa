package com.sayedhesham.travelorch.travel_service.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.sayedhesham.travelorch.common.entity.feedback.TravelFeedback;
import com.sayedhesham.travelorch.common.entity.rbac.Permission;
import com.sayedhesham.travelorch.common.entity.rbac.Role;
import com.sayedhesham.travelorch.common.entity.report.ManagerReport;
import com.sayedhesham.travelorch.common.entity.travel.Travel;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.enums.PaymentStatus;
import com.sayedhesham.travelorch.common.enums.TravelStatus;
import com.sayedhesham.travelorch.common.repository.feedback.TravelFeedbackRepository;
import com.sayedhesham.travelorch.common.repository.payment.PaymentTransactionRepository;
import com.sayedhesham.travelorch.common.repository.report.ManagerReportRepository;
import com.sayedhesham.travelorch.common.repository.travel.TravelRepository;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.travel_service.dto.AnalyticsOverviewResponse;
import com.sayedhesham.travelorch.travel_service.dto.ManagerRankingResponse;
import com.sayedhesham.travelorch.travel_service.dto.MonthlyIncomeResponse;
import com.sayedhesham.travelorch.travel_service.dto.PagedResponse;
import com.sayedhesham.travelorch.travel_service.dto.TravelRankingResponse;

import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private TravelRepository travelRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private TravelFeedbackRepository travelFeedbackRepository;

    @Mock
    private ManagerReportRepository managerReportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private AnalyticsService analyticsService;

    private User manager1;
    private User manager2;
    private Travel travel1;
    private Travel travel2;

    @BeforeEach
    void setUp() {
        Role managerRole = new Role();
        managerRole.setId(2L);
        managerRole.setName("travel_manager");
        managerRole.setPermissions(new HashSet<>());

        manager1 = new User();
        manager1.setId(1L);
        manager1.setUsername("manager1");
        manager1.setEmail("manager1@test.com");
        manager1.setFirstName("Alice");
        manager1.setLastName("Smith");
        manager1.setRole(managerRole);

        manager2 = new User();
        manager2.setId(2L);
        manager2.setUsername("manager2");
        manager2.setEmail("manager2@test.com");
        manager2.setFirstName("Bob");
        manager2.setLastName("Jones");
        manager2.setRole(managerRole);

        travel1 = new Travel();
        travel1.setId(10L);
        travel1.setManager(manager1);
        travel1.setTitle("Safari Adventure");
        travel1.setStartDate(LocalDate.of(2026, 7, 1));
        travel1.setEndDate(LocalDate.of(2026, 7, 15));
        travel1.setTotalPrice(new BigDecimal("3000.00"));
        travel1.setStatus(TravelStatus.draft);
        travel1.setCreatedAt(LocalDateTime.now());

        travel2 = new Travel();
        travel2.setId(20L);
        travel2.setManager(manager2);
        travel2.setTitle("City Tour");
        travel2.setStartDate(LocalDate.of(2026, 8, 1));
        travel2.setEndDate(LocalDate.of(2026, 8, 10));
        travel2.setTotalPrice(new BigDecimal("1500.00"));
        travel2.setStatus(TravelStatus.draft);
        travel2.setCreatedAt(LocalDateTime.now());
    }

    private void setupTransactionTemplateInvocation() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    void getOverview_WithData_ReturnsCorrectAggregates() {
        setupTransactionTemplateInvocation();
        when(paymentTransactionRepository.sumAmountByStatus(PaymentStatus.completed))
                .thenReturn(new BigDecimal("12500.50"));
        when(paymentTransactionRepository.countByStatus(PaymentStatus.completed)).thenReturn(42L);
        when(userRepository.findByRole_Name("travel_manager")).thenReturn(List.of(manager1, manager2));
        when(travelRepository.count()).thenReturn(15L);
        Object[] overviewCurrentMonth = new Object[]{2026, 5, new BigDecimal("3200.00"), 10L};
        List<Object[]> overviewMonthlyData = new ArrayList<>();
        overviewMonthlyData.add(overviewCurrentMonth);
        when(paymentTransactionRepository.findMonthlyIncomeSince(any(PaymentStatus.class), any(LocalDateTime.class)))
                .thenReturn(overviewMonthlyData);

        StepVerifier.create(analyticsService.getOverview())
                .expectNextMatches(overview -> {
                    assertEquals(new BigDecimal("12500.50"), overview.getTotalIncome());
                    assertEquals(42L, overview.getTotalBookings());
                    assertEquals(2L, overview.getTotalManagers());
                    assertEquals(15L, overview.getTotalOrganizedTravels());
                    assertEquals(new BigDecimal("3200.00"), overview.getLastMonthIncome());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getOverview_NoPayments_ReturnsZeros() {
        setupTransactionTemplateInvocation();
        when(paymentTransactionRepository.sumAmountByStatus(PaymentStatus.completed))
                .thenReturn(BigDecimal.ZERO);
        when(paymentTransactionRepository.countByStatus(PaymentStatus.completed)).thenReturn(0L);
        when(userRepository.findByRole_Name("travel_manager")).thenReturn(List.of());
        when(travelRepository.count()).thenReturn(0L);
        when(paymentTransactionRepository.findMonthlyIncomeSince(any(PaymentStatus.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        StepVerifier.create(analyticsService.getOverview())
                .expectNextMatches(overview -> {
                    assertEquals(BigDecimal.ZERO, overview.getTotalIncome());
                    assertEquals(0L, overview.getTotalBookings());
                    assertEquals(0L, overview.getTotalManagers());
                    assertEquals(0L, overview.getTotalOrganizedTravels());
                    assertEquals(BigDecimal.ZERO, overview.getLastMonthIncome());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getIncome_WithMonths_ReturnsFilledBuckets() {
        setupTransactionTemplateInvocation();
        Object[] row1 = new Object[]{2026, 3, new BigDecimal("1000.00"), 5L};
        Object[] row2 = new Object[]{2026, 5, new BigDecimal("2000.00"), 8L};

        when(paymentTransactionRepository.findMonthlyIncomeSince(any(PaymentStatus.class), any(LocalDateTime.class)))
                .thenReturn(List.of(row1, row2));

        StepVerifier.create(analyticsService.getIncome(3))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void getIncome_WithZeroFill_FillsMissingMonths() {
        setupTransactionTemplateInvocation();
        when(paymentTransactionRepository.findMonthlyIncomeSince(any(PaymentStatus.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        StepVerifier.create(analyticsService.getIncome(2))
                .expectNextMatches(m -> {
                    assertEquals(BigDecimal.ZERO, m.getTotalIncome());
                    assertEquals(0L, m.getTransactionCount());
                    assertNotNull(m.getLabel());
                    return true;
                })
                .expectNextMatches(m -> {
                    assertEquals(BigDecimal.ZERO, m.getTotalIncome());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getIncome_SingleMonth_ReturnsOneBucket() {
        setupTransactionTemplateInvocation();
        Object[] row = new Object[]{2026, 5, new BigDecimal("500.00"), 3L};
        when(paymentTransactionRepository.findMonthlyIncomeSince(any(PaymentStatus.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(row));

        StepVerifier.create(analyticsService.getIncome(1))
                .expectNextMatches(m -> {
                    assertEquals(new BigDecimal("500.00"), m.getTotalIncome());
                    assertEquals(3L, m.getTransactionCount());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getTopManagers_RankingOrder_MatchesScoreFormula() {
        setupTransactionTemplateInvocation();
        when(userRepository.findByRole_Name("travel_manager")).thenReturn(List.of(manager1, manager2));

        when(paymentTransactionRepository.aggregateRevenueByManager(PaymentStatus.completed))
                .thenReturn(List.of(
                        new Object[]{1L, new BigDecimal("5000.00"), 15L},
                        new Object[]{2L, new BigDecimal("2000.00"), 8L}
                ));
        when(travelRepository.countByManagerId(1L)).thenReturn(5L);
        when(travelRepository.countByManagerId(2L)).thenReturn(3L);

        TravelFeedback f1a = new TravelFeedback();
        f1a.setRating(5);
        f1a.setManagerId(1L);
        TravelFeedback f1b = new TravelFeedback();
        f1b.setRating(4);
        f1b.setManagerId(1L);
        TravelFeedback f2a = new TravelFeedback();
        f2a.setRating(3);
        f2a.setManagerId(2L);
        TravelFeedback f2b = new TravelFeedback();
        f2b.setRating(2);
        f2b.setManagerId(2L);

        when(travelFeedbackRepository.findAll()).thenReturn(List.of(f1a, f1b, f2a, f2b));

        ManagerReport report = new ManagerReport();
        report.setManagerId(2L);
        when(managerReportRepository.findAll()).thenReturn(List.of(report));

        StepVerifier.create(analyticsService.getTopManagers(0, 5))
                .expectNextMatches(paged -> {
                    assertEquals(2, paged.getContent().size());
                    assertEquals(2L, paged.getTotalElements());
                    assertEquals(1, paged.getTotalPages());

                    ManagerRankingResponse first = paged.getContent().get(0);
                    ManagerRankingResponse second = paged.getContent().get(1);

                    assertEquals("Alice Smith", first.getName());
                    assertTrue(first.getPerformanceScore() > second.getPerformanceScore(),
                            "Manager1 should rank higher: " + first.getPerformanceScore() + " vs " + second.getPerformanceScore());
                    assertTrue(first.getPerformanceScore() > 0);
                    assertTrue(second.getPerformanceScore() >= 0);

                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getTopManagers_EmptyList_ReturnsEmptyPage() {
        setupTransactionTemplateInvocation();
        when(userRepository.findByRole_Name("travel_manager")).thenReturn(List.of());
        when(paymentTransactionRepository.aggregateRevenueByManager(PaymentStatus.completed)).thenReturn(List.of());
        when(travelFeedbackRepository.findAll()).thenReturn(List.of());

        StepVerifier.create(analyticsService.getTopManagers(0, 5))
                .expectNextMatches(paged -> {
                    assertEquals(0, paged.getContent().size());
                    assertEquals(0L, paged.getTotalElements());
                    assertEquals(0, paged.getTotalPages());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getTopManagers_SecondPage_ReturnsCorrectSlice() {
        setupTransactionTemplateInvocation();
        when(userRepository.findByRole_Name("travel_manager")).thenReturn(List.of(manager1, manager2));

        when(paymentTransactionRepository.aggregateRevenueByManager(PaymentStatus.completed))
                .thenReturn(List.of());
        when(travelRepository.countByManagerId(1L)).thenReturn(0L);
        when(travelRepository.countByManagerId(2L)).thenReturn(0L);
        when(travelFeedbackRepository.findAll()).thenReturn(List.of());
        when(managerReportRepository.findAll()).thenReturn(List.of());

        StepVerifier.create(analyticsService.getTopManagers(1, 2))
                .expectNextMatches(paged -> {
                    assertEquals(0, paged.getContent().size());
                    assertEquals(2L, paged.getTotalElements());
                    assertEquals(1, paged.getPage());
                    assertEquals(1, paged.getTotalPages());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getTopManagers_PageSlicing_TotalPagesCorrect() {
        setupTransactionTemplateInvocation();
        when(userRepository.findByRole_Name("travel_manager")).thenReturn(List.of(manager1, manager2));

        when(paymentTransactionRepository.aggregateRevenueByManager(PaymentStatus.completed))
                .thenReturn(List.of());
        when(travelRepository.countByManagerId(any())).thenReturn(0L);
        when(travelFeedbackRepository.findAll()).thenReturn(List.of());
        when(managerReportRepository.findAll()).thenReturn(List.of());

        StepVerifier.create(analyticsService.getTopManagers(0, 1))
                .expectNextMatches(paged -> {
                    assertEquals(1, paged.getContent().size());
                    assertEquals(2L, paged.getTotalElements());
                    assertEquals(0, paged.getPage());
                    assertEquals(2, paged.getTotalPages());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getTopManagers_ScoreClampedToZero_WhenReportsExceed() {
        setupTransactionTemplateInvocation();
        when(userRepository.findByRole_Name("travel_manager")).thenReturn(List.of(manager2));
        when(paymentTransactionRepository.aggregateRevenueByManager(PaymentStatus.completed))
                .thenReturn(List.of());
        when(travelRepository.countByManagerId(2L)).thenReturn(0L);
        when(travelFeedbackRepository.findAll()).thenReturn(List.of());

        ManagerReport r1 = new ManagerReport();
        r1.setManagerId(2L);
        ManagerReport r2 = new ManagerReport();
        r2.setManagerId(2L);
        ManagerReport r3 = new ManagerReport();
        r3.setManagerId(2L);
        when(managerReportRepository.findAll()).thenReturn(List.of(r1, r2, r3));

        StepVerifier.create(analyticsService.getTopManagers(0, 5))
                .expectNextMatches(paged -> {
                    assertEquals(1, paged.getContent().size());
                    assertEquals(0.0, paged.getContent().get(0).getPerformanceScore(), 0.001);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getTopTravels_RankingOrder_MatchesScoreFormula() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findAll()).thenReturn(List.of(travel1, travel2));

        when(paymentTransactionRepository.aggregateRevenueByTravel(PaymentStatus.completed))
                .thenReturn(List.of(
                        new Object[]{10L, new BigDecimal("6000.00"), 20L},
                        new Object[]{20L, new BigDecimal("1500.00"), 5L}
                ));

        TravelFeedback ft1a = new TravelFeedback();
        ft1a.setRating(5);
        ft1a.setTravelId(10L);
        TravelFeedback ft1b = new TravelFeedback();
        ft1b.setRating(4);
        ft1b.setTravelId(10L);
        TravelFeedback ft2a = new TravelFeedback();
        ft2a.setRating(3);
        ft2a.setTravelId(20L);

        when(travelFeedbackRepository.findAll()).thenReturn(List.of(ft1a, ft1b, ft2a));

        StepVerifier.create(analyticsService.getTopTravels(0, 5))
                .expectNextMatches(paged -> {
                    assertEquals(2, paged.getContent().size());
                    assertEquals(2L, paged.getTotalElements());

                    TravelRankingResponse first = paged.getContent().get(0);
                    TravelRankingResponse second = paged.getContent().get(1);

                    assertEquals("Safari Adventure", first.getTitle());
                    assertEquals("Alice Smith", first.getManagerName());
                    assertEquals(TravelStatus.draft, first.getStatus());
                    assertTrue(first.getPerformanceScore() > second.getPerformanceScore());

                    assertEquals(20L, first.getBookings());
                    assertEquals(new BigDecimal("6000.00"), first.getRevenue());
                    assertEquals(4.5, first.getAverageRating(), 0.001);
                    assertEquals(2L, first.getTotalReviews());

                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getTopTravels_EmptyList_ReturnsEmptyPage() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findAll()).thenReturn(List.of());
        when(paymentTransactionRepository.aggregateRevenueByTravel(PaymentStatus.completed)).thenReturn(List.of());
        when(travelFeedbackRepository.findAll()).thenReturn(List.of());

        StepVerifier.create(analyticsService.getTopTravels(0, 5))
                .expectNextMatches(paged -> {
                    assertEquals(0, paged.getContent().size());
                    assertEquals(0L, paged.getTotalElements());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getTopTravels_PageSlicing_CorrectTotalPages() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findAll()).thenReturn(List.of(travel1, travel2));
        when(paymentTransactionRepository.aggregateRevenueByTravel(PaymentStatus.completed))
                .thenReturn(List.of());
        when(travelFeedbackRepository.findAll()).thenReturn(List.of());

        StepVerifier.create(analyticsService.getTopTravels(0, 1))
                .expectNextMatches(paged -> {
                    assertEquals(1, paged.getContent().size());
                    assertEquals(2L, paged.getTotalElements());
                    assertEquals(2, paged.getTotalPages());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getTopTravels_ScoreDetail_VerifiesFormula() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findAll()).thenReturn(List.of(travel1));
        when(paymentTransactionRepository.aggregateRevenueByTravel(PaymentStatus.completed))
                .thenReturn(List.<Object[]>of(new Object[]{10L, new BigDecimal("3000.00"), 10L}));

        TravelFeedback ft = new TravelFeedback();
        ft.setRating(4);
        ft.setTravelId(10L);
        when(travelFeedbackRepository.findAll()).thenReturn(List.of(ft));

        StepVerifier.create(analyticsService.getTopTravels(0, 5))
                .expectNextMatches(paged -> {
                    assertEquals(1, paged.getContent().size());
                    TravelRankingResponse t = paged.getContent().get(0);

                    assertEquals(4.0, t.getAverageRating(), 0.001);
                    assertEquals(1L, t.getTotalReviews());
                    assertEquals(new BigDecimal("3000.00"), t.getRevenue());
                    assertEquals(10L, t.getBookings());

                    double expectedScore = 100.0 * (0.50 * (4.0 / 5.0) + 0.20 * 0.0 + 0.30 * 0.0);
                    assertEquals(Math.round(expectedScore * 10.0) / 10.0, t.getPerformanceScore(), 0.001);

                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getTopManagers_AllEqualStats_SameScores() {
        setupTransactionTemplateInvocation();
        when(userRepository.findByRole_Name("travel_manager")).thenReturn(List.of(manager1, manager2));
        when(paymentTransactionRepository.aggregateRevenueByManager(PaymentStatus.completed))
                .thenReturn(List.of(
                        new Object[]{1L, new BigDecimal("1000.00"), 5L},
                        new Object[]{2L, new BigDecimal("1000.00"), 5L}
                ));
        when(travelRepository.countByManagerId(any())).thenReturn(3L);

        TravelFeedback fa = new TravelFeedback();
        fa.setRating(4);
        fa.setManagerId(1L);
        TravelFeedback fb = new TravelFeedback();
        fb.setRating(4);
        fb.setManagerId(2L);
        when(travelFeedbackRepository.findAll()).thenReturn(List.of(fa, fb));
        when(managerReportRepository.findAll()).thenReturn(List.of());

        StepVerifier.create(analyticsService.getTopManagers(0, 5))
                .expectNextMatches(paged -> {
                    ManagerRankingResponse m1 = paged.getContent().get(0);
                    ManagerRankingResponse m2 = paged.getContent().get(1);

                    assertEquals(m1.getPerformanceScore(), m2.getPerformanceScore(), 0.001);

                    double expected = 100.0 * (0.45 * (4.0 / 5.0) + 0.0 + 0.0 + 0.0);
                    assertEquals(Math.round(expected * 10.0) / 10.0, m1.getPerformanceScore(), 0.001);

                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getTopTravels_NoFeedback_ZeroRatingNoPenalty() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findAll()).thenReturn(List.of(travel1));
        when(paymentTransactionRepository.aggregateRevenueByTravel(PaymentStatus.completed))
                .thenReturn(List.<Object[]>of(new Object[]{10L, new BigDecimal("500.00"), 2L}));
        when(travelFeedbackRepository.findAll()).thenReturn(List.of());

        StepVerifier.create(analyticsService.getTopTravels(0, 5))
                .expectNextMatches(paged -> {
                    assertEquals(1, paged.getContent().size());
                    TravelRankingResponse t = paged.getContent().get(0);
                    assertEquals(0.0, t.getAverageRating(), 0.001);
                    assertEquals(0L, t.getTotalReviews());
                    assertTrue(t.getPerformanceScore() >= 0);
                    return true;
                })
                .verifyComplete();
    }
}
