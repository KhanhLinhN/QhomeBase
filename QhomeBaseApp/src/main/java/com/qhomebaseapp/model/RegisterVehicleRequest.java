package com.qhomebaseapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "register_vehicle", schema = "qhomebaseapp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterVehicleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "note")
    private String note;

    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "vehicle_brand")
    private String vehicleBrand;

    @Column(name = "vehicle_color")
    private String vehicleColor;

    @Column(name = "payment_status")
    private String paymentStatus = "UNPAID"; // UNPAID, PAID, PENDING

    @Column(name = "payment_amount")
    private java.math.BigDecimal paymentAmount;

    @Column(name = "payment_date")
    private OffsetDateTime paymentDate;

    @Column(name = "payment_gateway")
    private String paymentGateway; // VNPAY

    @Column(name = "vnpay_transaction_ref")
    private String vnpayTransactionRef;

    @OneToMany(mappedBy = "registerVehicleRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RegisterVehicleImage> images = new ArrayList<>();

    public void addImage(RegisterVehicleImage image) {
        images.add(image);
        image.setRegisterVehicleRequest(this);
    }

    public void removeImage(RegisterVehicleImage image) {
        images.remove(image);
        image.setRegisterVehicleRequest(null);
    }
}

