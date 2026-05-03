package com.sayedhesham.travelorch.payment_service.dto;

import com.sayedhesham.travelorch.common.entity.payment.PaymentTransaction;
import com.sayedhesham.travelorch.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionResponse {

    private Long id;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String providerTransactionId;
    private String paymentIntentId;
    private Long travelId;
    private LocalDateTime createdAt;

    public static PaymentTransactionResponse fromEntity(PaymentTransaction transaction) {
        return PaymentTransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .providerTransactionId(transaction.getProviderTransactionId())
                .paymentIntentId(transaction.getPaymentIntentId())
                .travelId(transaction.getTravel() != null ? transaction.getTravel().getId() : null)
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
