package com.qhomebaseapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "resident_card_registration", schema = "qhomebaseapp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResidentCardRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "resident_name", nullable = false)
    private String residentName;

    @Column(name = "apartment_number", nullable = false)
    private String apartmentNumber;

    @Column(name = "building_name", nullable = false)
    private String buildingName;

    @Column(name = "request_type", nullable = false)
    @Builder.Default
    private String requestType = "NEW_CARD"; // NEW_CARD, REPLACE_CARD

    @Column(name = "citizen_id", nullable = false)
    private String citizenId;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "note")
    private String note;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private String paymentStatus = "UNPAID"; // UNPAID, PAID, PENDING

    @Column(name = "payment_amount")
    private java.math.BigDecimal paymentAmount;

    @Column(name = "payment_date")
    private OffsetDateTime paymentDate;

    @Column(name = "payment_gateway")
    private String paymentGateway; // VNPAY

    @Column(name = "vnpay_transaction_ref")
    private String vnpayTransactionRef;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}

