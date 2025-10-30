package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.MeterDto;
import com.QhomeBase.baseservice.model.Meter;
import com.QhomeBase.baseservice.model.MeterReadingAssignment;
import com.QhomeBase.baseservice.repository.MeterReadingAssignmentRepository;
import com.QhomeBase.baseservice.repository.MeterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeterService {

    private final MeterRepository meterRepository;
    private final MeterReadingAssignmentRepository assignmentRepository;

    @Transactional(readOnly = true)
    public List<MeterDto> getMetersByAssignment(UUID assignmentId) {
        MeterReadingAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        List<Meter> meters;
        
        if (assignment.getFloorFrom() != null && assignment.getFloorTo() != null) {
            meters = meterRepository.findByBuildingServiceAndFloorRange(
                    assignment.getBuilding().getId(),
                    assignment.getService().getId(),
                    assignment.getFloorFrom(),
                    assignment.getFloorTo()
            );
        } else {
            meters = meterRepository.findByBuildingAndService(
                    assignment.getBuilding().getId(),
                    assignment.getService().getId()
            );
        }

        return meters.stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MeterDto getById(UUID id) {
        return meterRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Meter not found"));
    }

    @Transactional(readOnly = true)
    public List<MeterDto> getByUnitId(UUID unitId) {
        return meterRepository.findByUnitId(unitId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeterDto> getByServiceId(UUID serviceId) {
        return meterRepository.findByServiceId(serviceId).stream()
                .map(this::toDto)
                .toList();
    }

    private MeterDto toDto(Meter meter) {
        return new MeterDto(
                meter.getId(),
                meter.getUnit() != null ? meter.getUnit().getId() : null,
                meter.getUnit() != null ? meter.getUnit().getCode() : null,
                meter.getUnit() != null ? meter.getUnit().getFloor() : null,
                meter.getService() != null ? meter.getService().getId() : null,
                meter.getService() != null ? meter.getService().getCode() : null,
                meter.getService() != null ? meter.getService().getName() : null,
                meter.getMeterCode(),
                meter.getActive(),
                meter.getInstalledAt(),
                meter.getRemovedAt(),
                null, // lastReading - TODO: query from meter_readings
                null, // lastReadingDate - TODO: query from meter_readings
                meter.getCreatedAt(),
                meter.getUpdatedAt()
        );
    }
}

