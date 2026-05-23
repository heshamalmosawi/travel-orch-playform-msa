package com.sayedhesham.travelorch.travel_service.service;

import com.sayedhesham.travelorch.common.entity.report.ManagerReport;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.repository.report.ManagerReportRepository;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.travel_service.dto.ReportCreateRequest;
import com.sayedhesham.travelorch.travel_service.dto.ReportResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ManagerReportRepository reportRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    public Mono<ReportResponse> createReport(String username, ReportCreateRequest request) {
        Long managerId = request.getManagerId();
        log.info("createReport - managerId: {} by user: {}", managerId, username);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User reporter = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

            if (!reporter.hasRole("user")) {
                log.warn("createReport - User {} (role {}) is not a regular user", username,
                        reporter.getRole() != null ? reporter.getRole().getName() : null);
                throw new SecurityException("Only regular users can report travel managers");
            }

            User manager = userRepository.findById(managerId)
                    .orElseThrow(() -> new IllegalArgumentException("Manager not found with id: " + managerId));

            if (!manager.hasRole("travel_manager")) {
                log.warn("createReport - Target {} is not a travel manager", managerId);
                throw new IllegalArgumentException("User is not a travel manager: " + managerId);
            }

            if (reportRepository.existsByManagerIdAndReporterId(managerId, reporter.getId())) {
                log.warn("createReport - User {} already reported manager {}", username, managerId);
                throw new IllegalStateException("You have already reported this manager");
            }

            ManagerReport report = new ManagerReport();
            report.setManagerId(managerId);
            report.setReporter(reporter);
            report.setReason(request.getReason());

            ManagerReport saved = reportRepository.save(report);
            log.info("createReport - Created report id: {}", saved.getId());
            return ReportResponse.fromEntity(saved, displayName(manager));
        }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PreAuthorize("hasPermission('admin', 'all')")
    public Flux<ReportResponse> getAllReports() {
        log.info("getAllReports");
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            List<ManagerReport> reports = reportRepository.findAll();
            List<Long> managerIds = reports.stream().map(ManagerReport::getManagerId).distinct().toList();
            Map<Long, User> managers = userRepository.findAllById(managerIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
            return reports.stream()
                    .map(r -> ReportResponse.fromEntity(r, displayName(managers.get(r.getManagerId()))))
                    .toList();
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getAllReports - Found {} reports", list.size()))
                .flatMapMany(Flux::fromIterable);
    }

    // Reported managers may be deleted; managerId has no FK, so the user can be absent
    private String displayName(User user) {
        if (user == null) {
            return null;
        }
        String full = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
        return full.isEmpty() ? user.getUsername() : full;
    }
}
