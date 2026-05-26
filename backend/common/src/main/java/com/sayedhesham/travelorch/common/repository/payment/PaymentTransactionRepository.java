package com.sayedhesham.travelorch.common.repository.payment;

import com.sayedhesham.travelorch.common.entity.payment.PaymentTransaction;
import com.sayedhesham.travelorch.common.entity.travel.Travel;
import com.sayedhesham.travelorch.common.enums.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    
    List<PaymentTransaction> findByTravel(Travel travel);

    @EntityGraph(attributePaths = {"buyer", "travel"})
    List<PaymentTransaction> findByTravelId(Long travelId);

    List<PaymentTransaction> findByStatus(PaymentStatus status);
    
    Optional<PaymentTransaction> findByProviderTransactionId(String providerTransactionId);
    
    Optional<PaymentTransaction> findByPaymentIntentId(String paymentIntentId);

    List<PaymentTransaction> findByTravelManagerId(Long managerId);

    List<PaymentTransaction> findByBuyerId(Long buyerId);

    List<PaymentTransaction> findByBuyerIdAndTravelId(Long buyerId, Long travelId);

    @Query("SELECT COALESCE(SUM(pt.amount),0) FROM PaymentTransaction pt WHERE pt.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);

    long countByStatus(PaymentStatus status);

    @Query("SELECT EXTRACT(YEAR FROM pt.createdAt), EXTRACT(MONTH FROM pt.createdAt), " +
           "COALESCE(SUM(pt.amount),0), COUNT(pt) FROM PaymentTransaction pt " +
           "WHERE pt.status = :status AND pt.createdAt >= :since " +
           "GROUP BY EXTRACT(YEAR FROM pt.createdAt), EXTRACT(MONTH FROM pt.createdAt)")
    List<Object[]> findMonthlyIncomeSince(@Param("status") PaymentStatus status, @Param("since") LocalDateTime since);

    @Query("SELECT pt.travel.manager.id, COALESCE(SUM(pt.amount),0), COUNT(pt) FROM PaymentTransaction pt " +
           "WHERE pt.status = :status GROUP BY pt.travel.manager.id")
    List<Object[]> aggregateRevenueByManager(@Param("status") PaymentStatus status);

    @Query("SELECT pt.travel.id, COALESCE(SUM(pt.amount),0), COUNT(pt) FROM PaymentTransaction pt " +
           "WHERE pt.status = :status GROUP BY pt.travel.id")
    List<Object[]> aggregateRevenueByTravel(@Param("status") PaymentStatus status);
}
