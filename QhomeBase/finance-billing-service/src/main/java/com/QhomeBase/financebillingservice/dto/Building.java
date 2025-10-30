package com.QhomeBase.financebillingservice.dto;

import java.time.Instant;
import java.util.UUID;

public class Building {
    UUID id;
    UUID tenantId;
    String codeName;
    String name;
    String address;
    Instant createdAt;
    Instant updatedAt;
}
