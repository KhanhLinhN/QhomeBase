package com.QhomeBase.iamservice.repository;

import com.QhomeBase.iamservice.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

}
