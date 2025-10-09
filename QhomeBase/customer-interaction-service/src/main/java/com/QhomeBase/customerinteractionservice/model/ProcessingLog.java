package com.QhomeBase.customerinteractionservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema= "cs_service", name="processing_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProcessingLog {
    @Id @GeneratedValue
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "record_type", nullable = false)
    private String record_type;
    @Column(name = "record_id", nullable = false)
    private UUID record_id;
    @Column(name = "staff_in_charge", nullable = true)
    private UUID staff_in_charge;
    @Column(name = "content", nullable = false)
    private String content;
    @Column(name = "request_status", nullable = true)
    private String request_status;
    @Column(name = "log_type", nullable = true)
    private String log_type;
    @Column(name = "staff_in_charge_name", nullable = true)
    private String staff_in_charge_name;
    @Column(name = "created_at", nullable = false)
    private Instant created_at;
}
