package com.qhomebaseapp.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "register_service_request", schema = "qhomebaseapp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterServiceRequest {
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
}
