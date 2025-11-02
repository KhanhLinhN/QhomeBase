package com.qhomebaseapp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "register_vehicle_image", schema = "qhomebaseapp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterVehicleImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "register_request_id", nullable = false)
    private RegisterVehicleRequest registerVehicleRequest;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}

