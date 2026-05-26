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
    private Long buyerId;
    private String buyerUsername;
    private String buyerName;
    private String buyerEmail;
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
                .buyerId(transaction.getBuyer() != null ? transaction.getBuyer().getId() : null)
                .buyerUsername(transaction.getBuyer() != null ? transaction.getBuyer().getUsername() : null)
                .buyerName(buildBuyerName(transaction))
                .buyerEmail(transaction.getBuyer() != null ? transaction.getBuyer().getEmail() : null)
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private static String buildBuyerName(PaymentTransaction transaction) {
        if (transaction.getBuyer() == null) {
            return null;
        }
        String firstName = transaction.getBuyer().getFirstName();
        String lastName = transaction.getBuyer().getLastName();
        String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
        return fullName.isEmpty() ? transaction.getBuyer().getUsername() : fullName;
    }
}
