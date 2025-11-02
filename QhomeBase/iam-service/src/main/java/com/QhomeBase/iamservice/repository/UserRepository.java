package com.QhomeBase.iamservice.repository;

import com.QhomeBase.iamservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);

    @Query(value = """
        SELECT DISTINCT u.* 
        FROM iam.users u
        JOIN iam.user_roles ur ON u.id = ur.user_id
        WHERE ur.role IN ('technician', 'supporter', 'accountant') 
        AND u.active = true
        """, nativeQuery = true)
    List<User> findAvailableStaff();
}