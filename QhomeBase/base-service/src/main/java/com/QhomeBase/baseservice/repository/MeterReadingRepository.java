package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID> {
    public List<MeterReading> findByMeterId(UUID meterId);
    
    @Query("SELECT mr FROM MeterReading mr WHERE mr.assignment.id = :assignmentId")
    List<MeterReading> findByAssignmentId(@Param("assignmentId") UUID assignmentId);
    
    @Query("SELECT mr FROM MeterReading mr WHERE mr.session.id = :sessionId")
    List<MeterReading> findBySessionId(@Param("sessionId") UUID sessionId);
    
    @Query("SELECT mr FROM MeterReading mr " +
           "WHERE mr.session.cycle.id = :cycleId " +
           "AND mr.session.completedAt IS NOT NULL")
    List<MeterReading> findByCycleIdWhereSessionCompleted(@Param("cycleId") UUID cycleId);
    
    @Query("SELECT DISTINCT mr FROM MeterReading mr " +
           "LEFT JOIN FETCH mr.session s " +
           "LEFT JOIN FETCH mr.assignment a " +
           "WHERE (s.cycle.id = :cycleId OR a.cycle.id = :cycleId)")
    List<MeterReading> findByCycleId(@Param("cycleId") UUID cycleId);
}
