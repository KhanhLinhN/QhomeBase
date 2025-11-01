package com.QhomeBase.datadocsservice.repository;

import com.QhomeBase.datadocsservice.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {

    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.files WHERE c.id = :id")
    Optional<Contract> findByIdWithFiles(@Param("id") UUID id);

    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.files WHERE c.unitId = :unitId")
    List<Contract> findByUnitId(@Param("unitId") UUID unitId);

    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.files WHERE c.unitId = :unitId AND c.status = :status")
    List<Contract> findByUnitIdAndStatus(@Param("unitId") UUID unitId, @Param("status") String status);

    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.files WHERE c.contractNumber = :contractNumber")
    Optional<Contract> findByContractNumber(@Param("contractNumber") String contractNumber);

    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.files " +
           "WHERE c.status = 'ACTIVE' " +
           "AND (c.endDate IS NULL OR c.endDate >= :currentDate)")
    List<Contract> findActiveContracts(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.files " +
           "WHERE c.unitId = :unitId " +
           "AND c.status = 'ACTIVE' " +
           "AND (c.endDate IS NULL OR c.endDate >= :currentDate)")
    List<Contract> findActiveContractsByUnit(@Param("unitId") UUID unitId, @Param("currentDate") LocalDate currentDate);
}

