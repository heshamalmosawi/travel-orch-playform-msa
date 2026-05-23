package com.sayedhesham.travelorch.travel_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackUpdateRequest {

    @Min(1)
    @Max(5)
    private Integer rating;

    private String comment;
}
