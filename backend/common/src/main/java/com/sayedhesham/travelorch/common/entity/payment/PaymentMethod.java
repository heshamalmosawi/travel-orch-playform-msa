package com.sayedhesham.travelorch.common.entity.payment;

import com.sayedhesham.travelorch.common.entity.base.BaseEntity;
import com.sayedhesham.travelorch.common.enums.PaymentProvider;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment_methods", indexes = {
    @Index(name = "idx_payment_methods_provider", columnList = "provider")
})
@Getter
@Setter
@NoArgsConstructor
public class PaymentMethod extends BaseEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentProvider provider;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    @Column(name = "is_test_mode")
    private Boolean isTestMode = true;

    @Column(columnDefinition = "TEXT")
    private String config; // JSON string for non-sensitive config
}
