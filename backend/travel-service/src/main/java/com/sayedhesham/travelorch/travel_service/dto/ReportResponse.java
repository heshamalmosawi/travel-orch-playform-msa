package com.sayedhesham.travelorch.travel_service.dto;

import com.sayedhesham.travelorch.common.entity.report.ManagerReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    private Long id;
    private Long managerId;
    private Long reporterId;
    private String reporterUsername;
    private String reason;
    private LocalDateTime createdAt;

    public static ReportResponse fromEntity(ManagerReport report) {
        return ReportResponse.builder()
                .id(report.getId())
                .managerId(report.getManagerId())
                .reporterId(report.getReporter() != null ? report.getReporter().getId() : null)
                .reporterUsername(report.getReporter() != null ? report.getReporter().getUsername() : null)
                .reason(report.getReason())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
