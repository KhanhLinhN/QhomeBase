package com.qhomebaseapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "service", schema = "qhomebaseapp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceCategory category;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "service_type")
    private String serviceType; 

    @Column(name = "type_display_name")
    private String typeDisplayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "location")
    private String location;

    @Column(name = "map_url")
    private String mapUrl;

    @Column(name = "price_per_hour", precision = 15, scale = 2)
    private BigDecimal pricePerHour;

    @Column(name = "price_per_session", precision = 15, scale = 2)
    private BigDecimal pricePerSession;

    @Column(name = "pricing_type")
    private String pricingType = "HOURLY";

    @Column(name = "booking_type")
    private String bookingType; 

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(name = "min_duration_hours")
    private Integer minDurationHours = 1;

    @Column(name = "max_duration_hours")
    private Integer maxDurationHours;

    @Column(name = "advance_booking_days")
    private Integer advanceBookingDays = 30;

    @Column(name = "rules", columnDefinition = "TEXT")
    private String rules;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

