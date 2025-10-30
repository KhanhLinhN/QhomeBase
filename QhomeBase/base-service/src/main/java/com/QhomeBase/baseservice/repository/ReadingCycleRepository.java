package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.ReadingCycle;
import com.QhomeBase.baseservice.model.ReadingCycleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReadingCycleRepository extends JpaRepository<ReadingCycle, UUID> {
    
    List<ReadingCycle> findByStatus(ReadingCycleStatus status);
    
    List<ReadingCycle> findByStatusIn(List<ReadingCycleStatus> statuses);
    
    Optional<ReadingCycle> findByName(String name);
    
    @Query("SELECT rc FROM ReadingCycle rc WHERE rc.periodFrom <= :endDate AND rc.periodTo >= :startDate")
    List<ReadingCycle> findOverlappingCycles(@Param("startDate") LocalDate startDate, 
                                              @Param("endDate") LocalDate endDate);
    
    @Query("SELECT rc FROM ReadingCycle rc WHERE rc.status = 'ACTIVE' AND rc.periodTo < :date")
    List<ReadingCycle> findActiveExpiredCycles(@Param("date") LocalDate date);
}
