package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.MeterReadingAssignment;
import com.QhomeBase.baseservice.model.MeterReadingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeterReadingSessionRepository extends JpaRepository<MeterReadingSession, UUID> {
    
    List<MeterReadingSession> findByAssignmentId(UUID assignmentId);
    
    List<MeterReadingSession> findByReaderId(UUID readerId);
    
    @Query("SELECT s FROM MeterReadingSession s WHERE s.readerId = :readerId AND s.completedAt IS NULL")
    Optional<MeterReadingSession> findActiveSessionByReader(@Param("readerId") UUID readerId);
    
    @Query("SELECT s FROM MeterReadingSession s WHERE s.readerId = :readerId AND s.completedAt IS NOT NULL")
    List<MeterReadingSession> findCompletedSessionsByReader(@Param("readerId") UUID readerId);
    
    @Query("SELECT s FROM MeterReadingSession s WHERE s.assignment.id = :assignmentId AND s.completedAt IS NULL")
    List<MeterReadingSession> findActiveSessionsByAssignment(@Param("assignmentId") UUID assignmentId);

    @Query("SELECT s FROM MeterReadingSession s WHERE s.cycle.id = :cycleId")
    List<MeterReadingSession> findByCycleId(@Param("cycleId") UUID cycleId);

    @Query("SELECT s FROM MeterReadingSession s WHERE s.cycle.id = :cycleId")
    MeterReadingAssignment findMeterReadingAssignmentById(@Param("sessionId") UUID assignmentId);
}

