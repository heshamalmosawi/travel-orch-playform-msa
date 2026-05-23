package com.sayedhesham.travelorch.common.entity.report;

import com.sayedhesham.travelorch.common.entity.base.BaseEntity;
import com.sayedhesham.travelorch.common.entity.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "manager_reports",
    indexes = {
        @Index(name = "idx_report_manager", columnList = "manager_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_report_manager_reporter", columnNames = {"manager_id", "reporter_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ManagerReport extends BaseEntity {

    // Stored as a plain column — no FK — so reports survive manager deletion
    @NotNull
    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @Column(columnDefinition = "TEXT")
    private String reason;
}
