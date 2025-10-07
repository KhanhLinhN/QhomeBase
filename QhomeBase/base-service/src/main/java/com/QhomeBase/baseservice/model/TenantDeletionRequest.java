package com.QhomeBase.baseservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_deletion_requests", schema = "base")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class TenantDeletionRequest {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = "requested_by", nullable = false, columnDefinition = "uuid")
    private UUID requestedBy;

    @Column(name = "approved_by_1", columnDefinition = "uuid")
    private UUID approvedBy1;

    @Column(name = "approved_by_2", columnDefinition = "uuid")
    private UUID approvedBy2;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(columnDefinition = "text")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    @Builder.Default
    private TenantDeletionStatus status = TenantDeletionStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;
}