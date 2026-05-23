package com.sayedhesham.travelorch.common.entity.feedback;

import com.sayedhesham.travelorch.common.entity.base.BaseEntity;
import com.sayedhesham.travelorch.common.entity.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "travel_feedbacks",
    indexes = {
        @Index(name = "idx_feedback_travel",   columnList = "travel_id"),
        @Index(name = "idx_feedback_manager",  columnList = "manager_id"),
        @Index(name = "idx_feedback_reviewer", columnList = "reviewer_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "idx_feedback_travel_reviewer", columnNames = {"travel_id", "reviewer_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
public class TravelFeedback extends BaseEntity {

    // Stored as plain columns — no FK — so reviews survive travel/manager deletion
    @NotNull
    @Column(name = "travel_id", nullable = false)
    private Long travelId;

    @NotNull
    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @NotNull
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
