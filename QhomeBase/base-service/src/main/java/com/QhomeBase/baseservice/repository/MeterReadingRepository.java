package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MeterReadingRepository extends JpaRepository<MeterReading,Long> {
    public List<MeterReading> findByMeterId(UUID meterId);
    
    @Query("SELECT mr FROM MeterReading mr WHERE mr.assignment.id = :assignmentId")
    List<MeterReading> findByAssignmentId(@Param("assignmentId") UUID assignmentId);
}
