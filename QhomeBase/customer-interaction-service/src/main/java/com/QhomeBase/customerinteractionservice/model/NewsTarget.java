package com.QhomeBase.customerinteractionservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "news_targets", schema = "content")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "target_type", nullable = false, columnDefinition = "target_type")
    private TargetType targetType;

    @Column(name = "building_id")
    private UUID buildingId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();


    @PrePersist
    @PreUpdate
    private void validate() {
        if (targetType == TargetType.ALL && buildingId != null) {
            throw new IllegalStateException("Target type ALL must have null buildingId");
        }
        if (targetType == TargetType.BUILDING && buildingId == null) {
            throw new IllegalStateException("Target type BUILDING must have buildingId");
        }
    }


    public boolean appliesToBuilding(UUID checkBuildingId) {
        if (targetType == TargetType.ALL) {
            return true;
        }
        return buildingId != null && buildingId.equals(checkBuildingId);
    }
}


