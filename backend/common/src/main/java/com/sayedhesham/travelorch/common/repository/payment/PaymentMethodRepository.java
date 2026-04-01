package com.sayedhesham.travelorch.common.repository.payment;

import com.sayedhesham.travelorch.common.entity.payment.PaymentMethod;
import com.sayedhesham.travelorch.common.enums.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    
    List<PaymentMethod> findByProvider(PaymentProvider provider);
    
    List<PaymentMethod> findByIsTestMode(Boolean isTestMode);
    
    Optional<PaymentMethod> findByProvider(PaymentProvider provider, Boolean isTestMode);
}
