package com.QhomeBase.iamservice.model;

import jakarta.persistence.*;
import lombok.*;

import javax.management.relation.Role;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@ToString(exclude = "baseRole")
@Entity
@Table(name = "tenant_roles", schema = "iam")
public class TenantRole {

    @EmbeddedId
    private TenantRoleId id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_role", referencedColumnName = "role")
    private Roles baseRole;

    @Column(name = "description")
    private String description;



    @Transient public UUID getTenantId() { return id != null ? id.getTenantId() : null; }
    @Transient public String getRoleName() { return id != null ? id.getRole() : null; }
}
