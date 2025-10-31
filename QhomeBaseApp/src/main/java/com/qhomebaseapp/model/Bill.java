package com.qhomebaseapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bills", schema = "qhomebaseapp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bill_type", nullable = false)
    private String billType; // ELECTRICITY, WATER, INTERNET, etc.

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "billing_month", nullable = false)
    private LocalDate billingMonth;

    @Column(nullable = false)
    private String status;

    private String description;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "vnp_transaction_no", length = 50)
    private String vnpTransactionNo;

    @Column(name = "vnp_bank_code", length = 50)
    private String vnpBankCode;

    @Column(name = "vnp_card_type", length = 50)
    private String vnpCardType;

    @Column(name = "vnp_pay_date")
    private LocalDateTime vnpPayDate;

    @Column(name = "payment_gateway", length = 30)
    private String paymentGateway;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "vnpay_status", length = 20)
    private String vnpayStatus;


    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
