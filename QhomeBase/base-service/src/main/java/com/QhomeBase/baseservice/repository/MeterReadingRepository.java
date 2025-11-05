package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID> {
    public List<MeterReading> findByMeterId(UUID meterId);
    
    List<MeterReading> findByUnitId(UUID unitId);
    
    @Query("SELECT mr FROM MeterReading mr WHERE mr.assignment.id = :assignmentId")
    List<MeterReading> findByAssignmentId(@Param("assignmentId") UUID assignmentId);
    
    @Query("SELECT DISTINCT mr FROM MeterReading mr " +
           "LEFT JOIN FETCH mr.assignment a " +
           "WHERE a.cycle.id = :cycleId")
    List<MeterReading> findByCycleId(@Param("cycleId") UUID cycleId);
    
    @Query("SELECT mr FROM MeterReading mr " +
           "WHERE mr.meter.id = :meterId " +
           "AND mr.assignment.cycle.id = :cycleId")
    List<MeterReading> findByMeterIdAndCycleId(
        @Param("meterId") UUID meterId,
        @Param("cycleId") UUID cycleId
    );
    
    @Query("SELECT mr FROM MeterReading mr " +
           "WHERE mr.meter.id = :meterId " +
           "AND mr.assignment.id = :assignmentId")
    Optional<MeterReading> findByMeterIdAndAssignmentId(
        @Param("meterId") UUID meterId,
        @Param("assignmentId") UUID assignmentId
    );
}
