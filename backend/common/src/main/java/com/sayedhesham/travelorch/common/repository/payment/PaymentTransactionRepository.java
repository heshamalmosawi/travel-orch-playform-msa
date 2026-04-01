package com.sayedhesham.travelorch.common.repository.payment;

import com.sayedhesham.travelorch.common.entity.payment.PaymentTransaction;
import com.sayedhesham.travelorch.common.entity.travel.Travel;
import com.sayedhesham.travelorch.common.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    
    List<PaymentTransaction> findByTravel(Travel travel);
    
    List<PaymentTransaction> findByStatus(PaymentStatus status);
    
    Optional<PaymentTransaction> findByProviderTransactionId(String providerTransactionId);
    
    Optional<PaymentTransaction> findByPaymentIntentId(String paymentIntentId);
}
