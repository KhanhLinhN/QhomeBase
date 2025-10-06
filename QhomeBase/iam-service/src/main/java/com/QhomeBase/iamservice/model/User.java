package com.QhomeBase.iamservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;
@Entity
@Table(name="users", schema="iam")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @Column(columnDefinition="uuid") private UUID id;
    @Column(nullable=false, unique=true, columnDefinition = "citext") private String username;
    @Column(nullable=false, unique=true, columnDefinition = "citext") private String email;
    @Column(name="password_hash", nullable=false) private String passwordHash;
    @Column(nullable=false) private boolean active = true;
}
