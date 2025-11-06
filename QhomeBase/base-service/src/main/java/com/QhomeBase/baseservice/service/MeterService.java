package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.MeterCreateReq;
import com.QhomeBase.baseservice.dto.MeterDto;
import com.QhomeBase.baseservice.dto.MeterUpdateReq;
import com.QhomeBase.baseservice.model.Meter;
import com.QhomeBase.baseservice.model.MeterReading;
import com.QhomeBase.baseservice.model.MeterReadingAssignment;
import com.QhomeBase.baseservice.repository.MeterReadingAssignmentRepository;
import com.QhomeBase.baseservice.repository.MeterReadingRepository;
import com.QhomeBase.baseservice.repository.MeterRepository;
import com.QhomeBase.baseservice.repository.ServiceRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeterService {

    private final MeterRepository meterRepository;
    private final MeterReadingAssignmentRepository assignmentRepository;
    private final UnitRepository unitRepository;
    private final ServiceRepository serviceRepository;
    private final MeterReadingRepository meterReadingRepository;

    @Transactional(readOnly = true)
    public List<MeterDto> getMetersByAssignment(UUID assignmentId) {
        MeterReadingAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        List<Meter> meters;
        
        UUID buildingId = assignment.getBuilding() != null ? assignment.getBuilding().getId() : null;
        UUID serviceId = assignment.getService().getId();
        Integer floor = assignment.getFloor();
        List<UUID> unitIds = assignment.getUnitIds();
        
        if (buildingId == null) {
            throw new IllegalArgumentException("Assignment must have a building");
        }
        
        if (unitIds != null && !unitIds.isEmpty()) {
            List<Meter> allMeters = floor != null 
                ? meterRepository.findByBuildingServiceAndFloor(buildingId, serviceId, floor)
                : meterRepository.findByBuildingAndService(buildingId, serviceId);
            
            meters = allMeters.stream()
                .filter(m -> m.getUnit() != null && unitIds.contains(m.getUnit().getId()))
                .toList();
        } else if (floor != null) {
            meters = meterRepository.findByBuildingServiceAndFloor(buildingId, serviceId, floor);
        } else {
            meters = meterRepository.findByBuildingAndService(buildingId, serviceId);
        }

        return meters.stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeterDto> getMetersByStaffAndCycle(UUID staffId, UUID cycleId) {
        List<MeterReadingAssignment> assignments = assignmentRepository.findByAssignedToAndCycleId(staffId, cycleId);
        
        if (assignments.isEmpty()) {
            return List.of();
        }
        
        java.util.Set<UUID> uniqueMeterIds = new java.util.HashSet<>();
        List<Meter> allMeters = new java.util.ArrayList<>();
        
        for (MeterReadingAssignment assignment : assignments) {
            UUID buildingId = assignment.getBuilding() != null ? assignment.getBuilding().getId() : null;
            UUID serviceId = assignment.getService().getId();
            Integer floor = assignment.getFloor();
            List<UUID> unitIds = assignment.getUnitIds();
            
            if (buildingId == null) {
                continue;
            }
            
            List<Meter> meters;
            if (unitIds != null && !unitIds.isEmpty()) {
                List<Meter> allMetersForAssignment = floor != null 
                    ? meterRepository.findByBuildingServiceAndFloor(buildingId, serviceId, floor)
                    : meterRepository.findByBuildingAndService(buildingId, serviceId);
                
                meters = allMetersForAssignment.stream()
                    .filter(m -> m.getUnit() != null && unitIds.contains(m.getUnit().getId()))
                    .toList();
            } else if (floor != null) {
                meters = meterRepository.findByBuildingServiceAndFloor(buildingId, serviceId, floor);
            } else {
                meters = meterRepository.findByBuildingAndService(buildingId, serviceId);
            }
            
            for (Meter meter : meters) {
                if (!uniqueMeterIds.contains(meter.getId())) {
                    uniqueMeterIds.add(meter.getId());
                    allMeters.add(meter);
                }
            }
        }
        
        return allMeters.stream()
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

    @Transactional(readOnly = true)
    public List<MeterDto> getAll() {
        return meterRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeterDto> getByBuildingId(UUID buildingId) {
        return meterRepository.findByBuildingId(buildingId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeterDto> getByActive(Boolean active) {
        return meterRepository.findByActive(active).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public MeterDto create(MeterCreateReq req) {
        var unit = unitRepository.findById(req.unitId())
                .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + req.unitId()));

        var service = serviceRepository.findById(req.serviceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + req.serviceId()));

        if (meterRepository.findByMeterCode(req.meterCode()).isPresent()) {
            throw new IllegalStateException("Meter code already exists: " + req.meterCode());
        }

        var existingMeter = meterRepository.findByUnitAndService(req.unitId(), req.serviceId());
        if (existingMeter.isPresent() && existingMeter.get().getActive()) {
            throw new IllegalStateException(
                    "Active meter already exists for unit " + req.unitId() + " and service " + req.serviceId());
        }

        var meter = Meter.builder()
                .unit(unit)
                .service(service)
                .meterCode(req.meterCode())
                .active(true)
                .installedAt(req.installedAt() != null ? req.installedAt() : LocalDate.now())
                .build();

        var savedMeter = meterRepository.save(meter);
        log.info("Created meter: {} for unit: {} service: {}", savedMeter.getId(), req.unitId(), req.serviceId());

        return toDto(savedMeter);
    }

    @Transactional
    public MeterDto update(UUID id, MeterUpdateReq req) {
        var meter = meterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meter not found: " + id));

        if (req.meterCode() != null && !req.meterCode().isBlank()) {
            var existingMeter = meterRepository.findByMeterCode(req.meterCode());
            if (existingMeter.isPresent() && !existingMeter.get().getId().equals(id)) {
                throw new IllegalStateException("Meter code already exists: " + req.meterCode());
            }
            meter.setMeterCode(req.meterCode());
        }

        if (req.active() != null) {
            meter.setActive(req.active());
            
            if (!req.active() && meter.getRemovedAt() == null) {
                meter.setRemovedAt(LocalDate.now());
            }
            if (req.active() && meter.getRemovedAt() != null) {
                meter.setRemovedAt(null);
            }
        }

        if (req.removedAt() != null) {
            meter.setRemovedAt(req.removedAt());
            if (meter.getActive()) {
                meter.setActive(false);
            }
        }

        var updatedMeter = meterRepository.save(meter);
        log.info("Updated meter: {}", id);

        return toDto(updatedMeter);
    }

    @Transactional
    public void delete(UUID id) {
        var meter = meterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meter not found: " + id));

        var readings = meterReadingRepository.findByMeterId(id);
        if (!readings.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete meter with existing readings. Please deactivate instead. Meter has " 
                    + readings.size() + " reading(s)");
        }

        meterRepository.delete(meter);
        log.info("Deleted meter: {}", id);
    }

    @Transactional
    public void deactivate(UUID id) {
        var meter = meterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meter not found: " + id));

        meter.setActive(false);
        if (meter.getRemovedAt() == null) {
            meter.setRemovedAt(LocalDate.now());
        }
        meterRepository.save(meter);
        log.info("Deactivated meter: {}", id);
    }

    private MeterDto toDto(Meter meter) {
        var readings = meterReadingRepository.findByMeterId(meter.getId());
        Double lastReading = null;
        LocalDate lastReadingDate = null;

        if (!readings.isEmpty()) {
            var latestReading = readings.stream()
                    .max(Comparator.comparing(MeterReading::getReadingDate)
                            .thenComparing((MeterReading mr) -> 
                                mr.getCreatedAt() != null ? mr.getCreatedAt() : OffsetDateTime.MIN))
                    .orElse(null);

            if (latestReading != null) {
                lastReading = latestReading.getCurrIndex() != null 
                        ? latestReading.getCurrIndex().doubleValue() 
                        : null;
                lastReadingDate = latestReading.getReadingDate();
            }
        }

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
                lastReading,
                lastReadingDate,
                meter.getCreatedAt(),
                meter.getUpdatedAt()
        );
    }
}

