package com.sayedhesham.travelorch.travel_service.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.sayedhesham.travelorch.common.entity.feedback.TravelFeedback;
import com.sayedhesham.travelorch.common.entity.travel.Travel;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.enums.PaymentStatus;
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

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private static final double MANAGER_WEIGHT_RATING = 0.45;
    private static final double MANAGER_WEIGHT_REVIEWS = 0.20;
    private static final double MANAGER_WEIGHT_REVENUE = 0.25;
    private static final double MANAGER_WEIGHT_BOOKINGS = 0.10;
    private static final double MANAGER_REPORT_PENALTY = 5.0;

    private static final double TRAVEL_WEIGHT_RATING = 0.50;
    private static final double TRAVEL_WEIGHT_REVIEWS = 0.20;
    private static final double TRAVEL_WEIGHT_REVENUE = 0.30;

    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

    private final TravelRepository travelRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final TravelFeedbackRepository travelFeedbackRepository;
    private final ManagerReportRepository managerReportRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    @PreAuthorize("hasPermission('admin', 'all')")
    public Mono<AnalyticsOverviewResponse> getOverview() {
        log.info("getOverview");
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            BigDecimal totalIncome = paymentTransactionRepository.sumAmountByStatus(PaymentStatus.completed);
            long totalBookings = paymentTransactionRepository.countByStatus(PaymentStatus.completed);
            long totalManagers = userRepository.findByRole_Name("travel_manager").size();
            long totalOrganizedTravels = travelRepository.count();

            LocalDateTime startOfCurrentMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            List<Object[]> currentMonthData = paymentTransactionRepository.findMonthlyIncomeSince(
                    PaymentStatus.completed, startOfCurrentMonth);
            BigDecimal lastMonthIncome = currentMonthData.isEmpty()
                    ? BigDecimal.ZERO
                    : toBigDecimal(currentMonthData.get(0)[2]);

            log.info("getOverview - income={} bookings={} managers={} travels={} lastMonthIncome={}",
                    totalIncome, totalBookings, totalManagers, totalOrganizedTravels, lastMonthIncome);

            return AnalyticsOverviewResponse.builder()
                    .totalIncome(totalIncome)
                    .totalBookings(totalBookings)
                    .totalManagers(totalManagers)
                    .totalOrganizedTravels(totalOrganizedTravels)
                    .lastMonthIncome(lastMonthIncome)
                    .build();
        }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PreAuthorize("hasPermission('admin', 'all')")
    public Flux<MonthlyIncomeResponse> getIncome(int months) {
        log.info("getIncome - months={}", months);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            YearMonth current = YearMonth.now();
            YearMonth startMonth = current.minusMonths(months - 1);
            LocalDateTime since = startMonth.atDay(1).atStartOfDay();

            List<Object[]> raw = paymentTransactionRepository.findMonthlyIncomeSince(PaymentStatus.completed, since);

            Map<String, Object[]> dataMap = new LinkedHashMap<>();
            for (Object[] row : raw) {
                int year = ((Number) row[0]).intValue();
                int month = ((Number) row[1]).intValue();
                BigDecimal total = toBigDecimal(row[2]);
                long count = ((Number) row[3]).longValue();
                dataMap.put(year + "-" + month, new Object[]{year, month, total, count});
            }

            List<MonthlyIncomeResponse> result = new ArrayList<>();
            for (int i = months - 1; i >= 0; i--) {
                YearMonth ym = current.minusMonths(i);
                String key = ym.getYear() + "-" + ym.getMonthValue();
                if (dataMap.containsKey(key)) {
                    Object[] row = dataMap.get(key);
                    result.add(MonthlyIncomeResponse.builder()
                            .year((int) row[0])
                            .month((int) row[1])
                            .label(ym.format(LABEL_FORMATTER))
                            .totalIncome((BigDecimal) row[2])
                            .transactionCount((long) row[3])
                            .build());
                } else {
                    result.add(MonthlyIncomeResponse.builder()
                            .year(ym.getYear())
                            .month(ym.getMonthValue())
                            .label(ym.format(LABEL_FORMATTER))
                            .totalIncome(BigDecimal.ZERO)
                            .transactionCount(0)
                            .build());
                }
            }
            return result;
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getIncome - Returning {} monthly buckets", list.size()))
                .flatMapMany(Flux::fromIterable);
    }

    @PreAuthorize("hasPermission('admin', 'all')")
    public Mono<PagedResponse<ManagerRankingResponse>> getTopManagers(int page, int size) {
        log.info("getTopManagers - page={} size={}", page, size);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            List<User> managers = userRepository.findByRole_Name("travel_manager");

            List<Object[]> managerRevenue = paymentTransactionRepository.aggregateRevenueByManager(PaymentStatus.completed);
            Map<Long, BigDecimal> revenueByManager = new HashMap<>();
            Map<Long, Long> bookingsByManager = new HashMap<>();
            for (Object[] row : managerRevenue) {
                Long managerId = ((Number) row[0]).longValue();
                revenueByManager.put(managerId, toBigDecimal(row[1]));
                bookingsByManager.put(managerId, ((Number) row[2]).longValue());
            }

            List<TravelFeedback> allFeedbacks = travelFeedbackRepository.findAll();
            Map<Long, List<TravelFeedback>> feedbacksByManager = allFeedbacks.stream()
                    .collect(Collectors.groupingBy(TravelFeedback::getManagerId));

            List<ManagerReportCount> allReports = managerReportRepository.findAll().stream()
                    .collect(Collectors.groupingBy(r -> r.getManagerId(), Collectors.counting()))
                    .entrySet().stream()
                    .map(e -> new ManagerReportCount(e.getKey(), e.getValue()))
                    .toList();
            Map<Long, Long> reportCountMap = allReports.stream()
                    .collect(Collectors.toMap(ManagerReportCount::managerId, ManagerReportCount::count));

            List<ManagerRankingResponse> allRanked = new ArrayList<>();
            for (User manager : managers) {
                long organizedTravels = travelRepository.countByManagerId(manager.getId());
                BigDecimal revenue = revenueByManager.getOrDefault(manager.getId(), BigDecimal.ZERO);
                long bookings = bookingsByManager.getOrDefault(manager.getId(), 0L);
                List<TravelFeedback> feedbacks = feedbacksByManager.getOrDefault(manager.getId(), List.of());
                double avgRating = feedbacks.stream().mapToInt(TravelFeedback::getRating).average().orElse(0.0);
                long totalReviews = feedbacks.size();
                long totalReports = reportCountMap.getOrDefault(manager.getId(), 0L);

                allRanked.add(ManagerRankingResponse.builder()
                        .managerId(manager.getId())
                        .name(displayName(manager))
                        .email(manager.getEmail())
                        .organizedTravels(organizedTravels)
                        .totalBookings(bookings)
                        .totalRevenue(revenue)
                        .averageRating(avgRating)
                        .totalReviews(totalReviews)
                        .totalReports(totalReports)
                        .build());
            }

            computeManagerScores(allRanked);
            allRanked.sort((a, b) -> Double.compare(b.getPerformanceScore(), a.getPerformanceScore()));

            return paged(allRanked, page, size);
        }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PreAuthorize("hasPermission('admin', 'all')")
    public Mono<PagedResponse<TravelRankingResponse>> getTopTravels(int page, int size) {
        log.info("getTopTravels - page={} size={}", page, size);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            List<Travel> travels = travelRepository.findAll();

            List<Object[]> travelRevenue = paymentTransactionRepository.aggregateRevenueByTravel(PaymentStatus.completed);
            Map<Long, BigDecimal> revenueByTravel = new HashMap<>();
            Map<Long, Long> bookingsByTravel = new HashMap<>();
            for (Object[] row : travelRevenue) {
                Long travelId = ((Number) row[0]).longValue();
                revenueByTravel.put(travelId, toBigDecimal(row[1]));
                bookingsByTravel.put(travelId, ((Number) row[2]).longValue());
            }

            List<TravelFeedback> allFeedbacks = travelFeedbackRepository.findAll();
            Map<Long, List<TravelFeedback>> feedbacksByTravel = allFeedbacks.stream()
                    .collect(Collectors.groupingBy(TravelFeedback::getTravelId));

            List<TravelRankingResponse> allRanked = new ArrayList<>();
            for (Travel travel : travels) {
                BigDecimal revenue = revenueByTravel.getOrDefault(travel.getId(), BigDecimal.ZERO);
                long bookings = bookingsByTravel.getOrDefault(travel.getId(), 0L);
                List<TravelFeedback> feedbacks = feedbacksByTravel.getOrDefault(travel.getId(), List.of());
                double avgRating = feedbacks.stream().mapToInt(TravelFeedback::getRating).average().orElse(0.0);
                long totalReviews = feedbacks.size();

                allRanked.add(TravelRankingResponse.builder()
                        .travelId(travel.getId())
                        .title(travel.getTitle())
                        .managerName(displayName(travel.getManager()))
                        .bookings(bookings)
                        .revenue(revenue)
                        .averageRating(avgRating)
                        .totalReviews(totalReviews)
                        .status(travel.getStatus())
                        .build());
            }

            computeTravelScores(allRanked);
            allRanked.sort((a, b) -> Double.compare(b.getPerformanceScore(), a.getPerformanceScore()));

            return paged(allRanked, page, size);
        }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private <T> PagedResponse<T> paged(List<T> all, int page, int size) {
        int totalElements = all.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<T> content = fromIndex < totalElements ? all.subList(fromIndex, toIndex) : List.of();
        return PagedResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    private void computeManagerScores(List<ManagerRankingResponse> managers) {
        if (managers.isEmpty()) return;

        long minReviews = managers.stream().mapToLong(ManagerRankingResponse::getTotalReviews).min().orElse(0);
        long maxReviews = managers.stream().mapToLong(ManagerRankingResponse::getTotalReviews).max().orElse(0);
        double minRevenue = managers.stream().mapToDouble(m -> m.getTotalRevenue().doubleValue()).min().orElse(0);
        double maxRevenue = managers.stream().mapToDouble(m -> m.getTotalRevenue().doubleValue()).max().orElse(0);
        long minBookings = managers.stream().mapToLong(ManagerRankingResponse::getTotalBookings).min().orElse(0);
        long maxBookings = managers.stream().mapToLong(ManagerRankingResponse::getTotalBookings).max().orElse(0);

        double reviewRange = maxReviews - minReviews;
        double revenueRange = maxRevenue - minRevenue;
        double bookingRange = maxBookings - minBookings;

        for (ManagerRankingResponse m : managers) {
            double nReviews = reviewRange > 0 ? (m.getTotalReviews() - minReviews) / reviewRange : 0;
            double nRevenue = revenueRange > 0 ? (m.getTotalRevenue().doubleValue() - minRevenue) / revenueRange : 0;
            double nBookings = bookingRange > 0 ? (m.getTotalBookings() - minBookings) / bookingRange : 0;

            double score = 100.0 * (MANAGER_WEIGHT_RATING * (m.getAverageRating() / 5.0)
                    + MANAGER_WEIGHT_REVIEWS * nReviews
                    + MANAGER_WEIGHT_REVENUE * nRevenue
                    + MANAGER_WEIGHT_BOOKINGS * nBookings)
                    - MANAGER_REPORT_PENALTY * m.getTotalReports();

            m.setPerformanceScore(Math.round(Math.max(0, score) * 10.0) / 10.0);
        }
    }

    private void computeTravelScores(List<TravelRankingResponse> travels) {
        if (travels.isEmpty()) return;

        long minReviews = travels.stream().mapToLong(TravelRankingResponse::getTotalReviews).min().orElse(0);
        long maxReviews = travels.stream().mapToLong(TravelRankingResponse::getTotalReviews).max().orElse(0);
        double minRevenue = travels.stream().mapToDouble(t -> t.getRevenue().doubleValue()).min().orElse(0);
        double maxRevenue = travels.stream().mapToDouble(t -> t.getRevenue().doubleValue()).max().orElse(0);

        double reviewRange = maxReviews - minReviews;
        double revenueRange = maxRevenue - minRevenue;

        for (TravelRankingResponse t : travels) {
            double nReviews = reviewRange > 0 ? (t.getTotalReviews() - minReviews) / reviewRange : 0;
            double nRevenue = revenueRange > 0 ? (t.getRevenue().doubleValue() - minRevenue) / revenueRange : 0;

            double score = 100.0 * (TRAVEL_WEIGHT_RATING * (t.getAverageRating() / 5.0)
                    + TRAVEL_WEIGHT_REVIEWS * nReviews
                    + TRAVEL_WEIGHT_REVENUE * nRevenue);

            t.setPerformanceScore(Math.round(Math.max(0, score) * 10.0) / 10.0);
        }
    }

    private String displayName(User user) {
        if (user == null) return null;
        String full = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
        return full.isEmpty() ? user.getUsername() : full;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    private record ManagerReportCount(Long managerId, Long count) {}
}
