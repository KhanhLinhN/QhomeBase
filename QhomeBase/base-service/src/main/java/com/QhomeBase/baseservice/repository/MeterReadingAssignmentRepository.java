package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.MeterReadingAssignment;
import com.QhomeBase.baseservice.model.MeterReadingAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MeterReadingAssignmentRepository extends JpaRepository<MeterReadingAssignment, UUID> {
    
    List<MeterReadingAssignment> findByCycleId(UUID cycleId);
    
    List<MeterReadingAssignment> findByAssignedTo(UUID assignedTo);
    
    List<MeterReadingAssignment> findByBuildingId(UUID buildingId);
    
    List<MeterReadingAssignment> findByServiceId(UUID serviceId);
    
    List<MeterReadingAssignment> findByCycleIdAndCompletedAtIsNull(UUID cycleId);
    
    List<MeterReadingAssignment> findByAssignedToAndCompletedAtIsNull(UUID assignedTo);
    
    List<MeterReadingAssignment> findByBuildingIdAndServiceId(UUID buildingId, UUID serviceId);
    
    List<MeterReadingAssignment> findByBuildingIdAndServiceIdAndCompletedAtIsNull(UUID buildingId, UUID serviceId);
    
    @Query("SELECT a FROM MeterReadingAssignment a WHERE a.assignedTo = :staffId " +
           "AND a.status NOT IN :excludedStatuses")
    List<MeterReadingAssignment> findActiveByAssignedTo(
        @Param("staffId") UUID staffId,
        @Param("excludedStatuses") List<MeterReadingAssignmentStatus> excludedStatuses
    );
    
    List<MeterReadingAssignment> findByAssignedToAndStatusIn(
        UUID assignedTo,
        List<MeterReadingAssignmentStatus> statuses
    );


}
