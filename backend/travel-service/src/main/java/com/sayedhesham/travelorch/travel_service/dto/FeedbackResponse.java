package com.sayedhesham.travelorch.travel_service.dto;

import com.sayedhesham.travelorch.common.entity.feedback.TravelFeedback;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {

    private Long id;
    private Long travelId;
    private Long managerId;
    private Long reviewerId;
    private String reviewerUsername;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FeedbackResponse fromEntity(TravelFeedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .travelId(feedback.getTravelId())
                .managerId(feedback.getManagerId())
                .reviewerId(feedback.getReviewer() != null ? feedback.getReviewer().getId() : null)
                .reviewerUsername(feedback.getReviewer() != null ? feedback.getReviewer().getUsername() : null)
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }
}
