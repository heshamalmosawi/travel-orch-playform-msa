package com.sayedhesham.travelorch.travel_service.service;

import com.sayedhesham.travelorch.common.entity.rbac.Role;
import com.sayedhesham.travelorch.common.entity.report.ManagerReport;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.repository.report.ManagerReportRepository;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.travel_service.dto.ReportCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ManagerReportRepository reportRepository;
    @Mock private UserRepository userRepository;
    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks
    private ReportService reportService;

    private User reporter;
    private User manager;
    private ManagerReport report;

    @BeforeEach
    void setUp() {
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName("user");

        Role managerRole = new Role();
        managerRole.setId(2L);
        managerRole.setName("travel_manager");

        reporter = new User();
        reporter.setId(10L);
        reporter.setUsername("reporter");
        reporter.setEmail("reporter@example.com");
        reporter.setRole(userRole);

        manager = new User();
        manager.setId(20L);
        manager.setUsername("manager");
        manager.setEmail("manager@example.com");
        manager.setRole(managerRole);

        report = new ManagerReport();
        report.setId(1L);
        report.setManagerId(20L);
        report.setReporter(reporter);
        report.setReason("Unprofessional conduct");
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
    }

    private void setupTransaction() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    // -------------------------------------------------------------------------
    // createReport
    // -------------------------------------------------------------------------

    @Test
    void createReport_Success() {
        setupTransaction();
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporter));
        when(userRepository.findById(20L)).thenReturn(Optional.of(manager));
        when(reportRepository.existsByManagerIdAndReporterId(20L, 10L)).thenReturn(false);
        when(reportRepository.save(any(ManagerReport.class))).thenAnswer(inv -> {
            ManagerReport saved = inv.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        ReportCreateRequest request = new ReportCreateRequest();
        request.setManagerId(20L);
        request.setReason("Unprofessional conduct");

        StepVerifier.create(reportService.createReport("reporter", request))
                .expectNextMatches(response -> {
                    assertEquals(20L, response.getManagerId());
                    assertEquals(10L, response.getReporterId());
                    assertEquals("reporter", response.getReporterUsername());
                    assertEquals("Unprofessional conduct", response.getReason());
                    return true;
                })
                .verifyComplete();

        verify(reportRepository).save(any(ManagerReport.class));
    }

    @Test
    void createReport_ReporterNotFound_ThrowsIllegalArgument() {
        setupTransaction();
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        ReportCreateRequest request = new ReportCreateRequest();
        request.setManagerId(20L);

        StepVerifier.create(reportService.createReport("ghost", request))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().equals("User not found: ghost"))
                .verify();

        verify(reportRepository, never()).save(any());
    }

    @Test
    void createReport_ReporterNotRegularUser_ThrowsSecurity() {
        setupTransaction();
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));

        ReportCreateRequest request = new ReportCreateRequest();
        request.setManagerId(20L);

        StepVerifier.create(reportService.createReport("manager", request))
                .expectErrorMatches(ex -> ex instanceof SecurityException
                        && ex.getMessage().contains("regular users"))
                .verify();

        verify(reportRepository, never()).save(any());
    }

    @Test
    void createReport_ManagerNotFound_ThrowsIllegalArgument() {
        setupTransaction();
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporter));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ReportCreateRequest request = new ReportCreateRequest();
        request.setManagerId(999L);

        StepVerifier.create(reportService.createReport("reporter", request))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().equals("Manager not found with id: 999"))
                .verify();

        verify(reportRepository, never()).save(any());
    }

    @Test
    void createReport_TargetNotManager_ThrowsIllegalArgument() {
        setupTransaction();
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporter));
        when(userRepository.findById(10L)).thenReturn(Optional.of(reporter));

        ReportCreateRequest request = new ReportCreateRequest();
        request.setManagerId(10L);

        StepVerifier.create(reportService.createReport("reporter", request))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().equals("User is not a travel manager: 10"))
                .verify();

        verify(reportRepository, never()).save(any());
    }

    @Test
    void createReport_AlreadyReported_ThrowsIllegalState() {
        setupTransaction();
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporter));
        when(userRepository.findById(20L)).thenReturn(Optional.of(manager));
        when(reportRepository.existsByManagerIdAndReporterId(20L, 10L)).thenReturn(true);

        ReportCreateRequest request = new ReportCreateRequest();
        request.setManagerId(20L);

        StepVerifier.create(reportService.createReport("reporter", request))
                .expectErrorMatches(ex -> ex instanceof IllegalStateException
                        && ex.getMessage().contains("already reported"))
                .verify();

        verify(reportRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getAllReports
    // -------------------------------------------------------------------------

    @Test
    void getAllReports_ReturnsList() {
        setupTransaction();
        ManagerReport second = new ManagerReport();
        second.setId(2L);
        second.setManagerId(21L);
        second.setReporter(reporter);
        second.setReason("Late refund");
        second.setCreatedAt(LocalDateTime.now());
        second.setUpdatedAt(LocalDateTime.now());

        when(reportRepository.findAll()).thenReturn(List.of(report, second));

        StepVerifier.create(reportService.getAllReports())
                .expectNextMatches(r -> r.getId().equals(1L) && r.getManagerId().equals(20L))
                .expectNextMatches(r -> r.getId().equals(2L) && r.getManagerId().equals(21L))
                .verifyComplete();
    }

    @Test
    void getAllReports_EmptyList() {
        setupTransaction();
        when(reportRepository.findAll()).thenReturn(List.of());

        StepVerifier.create(reportService.getAllReports())
                .verifyComplete();
    }
}
