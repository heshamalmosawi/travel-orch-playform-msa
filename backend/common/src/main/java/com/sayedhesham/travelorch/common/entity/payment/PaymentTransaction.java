package com.sayedhesham.travelorch.common.entity.payment;

import com.sayedhesham.travelorch.common.entity.base.BaseEntity;
import com.sayedhesham.travelorch.common.entity.travel.Travel;
import com.sayedhesham.travelorch.common.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_payment_transactions_travel", columnList = "travel_id"),
    @Index(name = "idx_payment_transactions_status", columnList = "status"),
    @Index(name = "idx_payment_transactions_provider", columnList = "provider_transaction_id")
})
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id", nullable = false)
    private Travel travel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Size(max = 3)
    @Column(length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PaymentStatus status = PaymentStatus.pending;

    @Column(name = "provider_transaction_id")
    private String providerTransactionId;

    @Column(name = "payment_intent_id")
    private String paymentIntentId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON string
}
