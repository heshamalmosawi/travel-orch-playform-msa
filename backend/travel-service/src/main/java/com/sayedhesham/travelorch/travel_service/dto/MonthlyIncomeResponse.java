package com.sayedhesham.travelorch.travel_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyIncomeResponse {

    private int year;
    private int month;
    private String label;
    private BigDecimal totalIncome;
    private long transactionCount;
}
