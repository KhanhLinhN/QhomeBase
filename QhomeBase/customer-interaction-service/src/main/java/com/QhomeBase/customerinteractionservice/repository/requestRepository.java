package com.QhomeBase.customerinteractionservice.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.QhomeBase.customerinteractionservice.model.Request;

@Repository
public interface requestRepository extends JpaRepository<Request, UUID>, JpaSpecificationExecutor<Request> {
    
}
