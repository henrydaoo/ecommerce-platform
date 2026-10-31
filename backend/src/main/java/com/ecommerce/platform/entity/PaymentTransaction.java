package com.ecommerce.platform.entity;

import com.ecommerce.platform.common.PaymentMethod;
import com.ecommerce.platform.common.PaymentTransactionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

   
                                                                           
                                                                           
                                                                            
   
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod provider;

    @Column(name = "provider_txn_ref", unique = true)
    private String providerTxnRef;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentTransactionStatus status = PaymentTransactionStatus.INITIATED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_callback_payload")
    private String rawCallbackPayload;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
