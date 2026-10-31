package com.ecommerce.platform.repository;

import com.ecommerce.platform.entity.PaymentTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select paymentTxn from PaymentTransaction paymentTxn "
                        + "where paymentTxn.providerTxnRef = :providerTxnRef")
        Optional<PaymentTransaction> findByProviderTxnRefForUpdate(
                        @Param("providerTxnRef") String providerTxnRef);

        Optional<PaymentTransaction> findFirstByOrderIdAndProviderOrderByCreatedAtDesc(Long orderId,
                        com.ecommerce.platform.common.PaymentMethod provider);
}
