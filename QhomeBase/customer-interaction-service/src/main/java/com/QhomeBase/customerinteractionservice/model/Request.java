package com.QhomeBase.customerinteractionservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema= "cs_service", name="requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Request {
    @Id @GeneratedValue
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "resident_id", nullable = false)
    private UUID residentId;
    @Column(name = "resident_name", nullable = false)
    private String resident_name;
    @Column(name = "image_path", nullable = true)
    private String image_path;
    @Column(name = "title", nullable = false)
    private String title;
    @Column(name = "content", nullable = false)
    private String content;
    @Column(name = "status", nullable = false)
    private String status;
    @Column(name = "priority", nullable = false)
    private String priority;
    @Column(name = "created_at", nullable = false)
    private Instant created_at;
    @Column(name = "updated_at", nullable = true)
    private Instant updated_at;
}
