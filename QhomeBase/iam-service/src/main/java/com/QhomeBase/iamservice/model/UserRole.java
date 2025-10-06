package com.QhomeBase.iamservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_roles", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @Column(name = "granted_at", insertable = false, updatable = false)
    private java.time.Instant grantedAt;

    @Transient
    public UUID getUserId() { return id != null ? id.getUserId() : null; }

    @Transient
    public String getRole() { return id != null ? id.getRole() : null; }
}
