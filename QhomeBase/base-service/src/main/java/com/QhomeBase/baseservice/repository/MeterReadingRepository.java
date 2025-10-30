package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MeterReadingRepository extends JpaRepository<MeterReading,Long> {
    public List<MeterReading> findByMeterId(UUID meterId);

}
