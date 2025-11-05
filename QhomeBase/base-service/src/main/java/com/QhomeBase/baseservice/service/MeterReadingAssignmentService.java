package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.MeterReadingAssignmentCreateReq;
import com.QhomeBase.baseservice.dto.MeterReadingAssignmentDto;
import com.QhomeBase.baseservice.dto.AssignmentProgressDto;
import com.QhomeBase.baseservice.model.*;
import com.QhomeBase.baseservice.repository.*;
import com.QhomeBase.baseservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeterReadingAssignmentService {
    
    private final MeterReadingAssignmentRepository meterReadingAssignmentRepository;
    private final ReadingCycleRepository readingCycleRepository;
    private final BuildingRepository buildingRepository;
    private final ServiceRepository serviceRepository;
    private final MeterRepository meterRepository;
    private final MeterReadingRepository meterReadingRepository;


    @Transactional
    public MeterReadingAssignmentDto create(MeterReadingAssignmentCreateReq req, UserPrincipal principal) {
        ReadingCycle cycle = readingCycleRepository.findById(req.cycleId())
                .orElseThrow(() -> new RuntimeException("Reading cycle not found"));
        
        Building building = req.buildingId() != null 
            ? buildingRepository.findById(req.buildingId())
                .orElseThrow(() -> new RuntimeException("Building not found"))
            : null;
        
        com.QhomeBase.baseservice.model.Service service =
                serviceRepository.findById(req.serviceId())
                        .orElseThrow(() -> new RuntimeException("Service not found"));
        
        if (!service.getRequiresMeter()) {
            throw new RuntimeException("Service does not require meter reading");
        }
        
        LocalDate startDate = req.startDate() != null ? req.startDate() : cycle.getStartDate();
        LocalDate endDate = req.endDate() != null ? req.endDate() : cycle.getEndDate();
        
        validateTimeRange(cycle, startDate, endDate);
        
        if (building != null) {
            validateNoOverlap(
                req.cycleId(), 
                building.getId(), 
                req.serviceId(),
                startDate, 
                endDate, 
                req.floor()
            );
        }
        
        MeterReadingAssignmentStatus initialStatus = determineInitialStatus(startDate, endDate);
        
        MeterReadingAssignment assignment = MeterReadingAssignment.builder()
                .cycle(cycle)
                .building(building)
                .service(service)
                .assignedTo(req.assignedTo())
                .assignedBy(principal.uid())
                .assignedAt(OffsetDateTime.now())
                .startDate(startDate)
                .endDate(endDate)
                .note(req.note())
                .floor(req.floor())
                .unitIds(req.unitIds())
                .status(initialStatus)
                .build();
        
        assignment = meterReadingAssignmentRepository.save(assignment);
        return toDto(assignment);
    }

    private void validateTimeRange(ReadingCycle cycle, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("startDate must be <= endDate");
        }

        if (startDate.isBefore(cycle.getStartDate()) || startDate.isAfter(cycle.getEndDate())) {
            throw new RuntimeException(
                String.format("startDate (%s) must be within cycle period (%s to %s)",
                    startDate, cycle.getStartDate(), cycle.getEndDate())
            );
        }

        if (endDate.isBefore(cycle.getStartDate()) || endDate.isAfter(cycle.getEndDate())) {
            throw new RuntimeException(
                String.format("endDate (%s) must be within cycle period (%s to %s)",
                    endDate, cycle.getStartDate(), cycle.getEndDate())
            );
        }
    }




    private void validateNoOverlap(UUID cycleId, UUID buildingId, UUID serviceId,
                                    LocalDate startDate, LocalDate endDate,
                                    Integer floor) {
        
        List<MeterReadingAssignment> assignments = meterReadingAssignmentRepository
                .findByCycleId(cycleId)
                .stream()
                .filter(a -> a.getBuilding() != null && a.getBuilding().getId().equals(buildingId))
                .filter(a -> a.getService().getId().equals(serviceId))
                .filter(a -> a.getStatus() != MeterReadingAssignmentStatus.COMPLETED 
                          && a.getStatus() != MeterReadingAssignmentStatus.CANCELLED)
                .collect(Collectors.toList());
        
        for (MeterReadingAssignment existing : assignments) {
            boolean timeOverlap = !startDate.isAfter(existing.getEndDate()) 
                               && !endDate.isBefore(existing.getStartDate());
            
            if (!timeOverlap) {
                continue;
            }
            
            boolean floorOverlap = checkFloorOverlap(floor, existing.getFloor());
            
            if (floorOverlap) {
                throw new RuntimeException(
                    String.format(
                        "Assignment overlap detected! " +
                        "Existing: %s to %s, Floor %s | " +
                        "New: %s to %s, Floor %s",
                        existing.getStartDate(), existing.getEndDate(),
                        formatFloor(existing.getFloor()),
                        startDate, endDate,
                        formatFloor(floor)
                    )
                );
            }
        }
    }

    private boolean checkFloorOverlap(Integer floor1, Integer floor2) {
        if (floor1 == null || floor2 == null) {
            return true;
        }
        return floor1.equals(floor2);
    }

    private String formatFloor(Integer floor) {
        if (floor == null) {
            return "ALL FLOORS";
        }
        return String.format("Floor %d", floor);
    }

    public MeterReadingAssignmentDto getById(UUID id) {
        MeterReadingAssignment assignment = meterReadingAssignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        return toDto(assignment);
    }

    public List<MeterReadingAssignmentDto> getByCycleId(UUID cycleId) {
        return meterReadingAssignmentRepository.findByCycleId(cycleId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<MeterReadingAssignmentDto> getByAssignedTo(UUID staffId) {
        return meterReadingAssignmentRepository.findByAssignedTo(staffId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<MeterReadingAssignmentDto> getActiveByStaff(UUID staffId) {
        List<MeterReadingAssignmentStatus> activeStatuses = List.of(
            MeterReadingAssignmentStatus.PENDING,
            MeterReadingAssignmentStatus.IN_PROGRESS,
            MeterReadingAssignmentStatus.OVERDUE
        );
        
        return meterReadingAssignmentRepository.findByAssignedToAndStatusIn(staffId, activeStatuses)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MeterReadingAssignmentDto markAsCompleted(UUID id, UserPrincipal principal) {
        MeterReadingAssignment assignment = meterReadingAssignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        if (assignment.getCompletedAt() != null) {
            throw new RuntimeException("Assignment already completed");
        }

        if (!assignment.getAssignedTo().equals(principal.uid())) {
            throw new RuntimeException("Only assigned staff can complete this assignment");
        }

        if (assignment.getStatus() == MeterReadingAssignmentStatus.CANCELLED) {
            throw new RuntimeException("Cannot complete a cancelled assignment");
        }

        validateCompletedMeterReading(assignment);

        assignment.setCompletedAt(OffsetDateTime.now());
        assignment.setStatus(MeterReadingAssignmentStatus.COMPLETED);
        assignment = meterReadingAssignmentRepository.save(assignment);

        return toDto(assignment);
    }
    public void validateCompletedMeterReading(MeterReadingAssignment assignment) {
        if (assignment.getBuilding() == null) {
            throw new IllegalArgumentException("Assignment must have a building");
        }
        
        UUID buildingId = assignment.getBuilding().getId();
        UUID serviceId = assignment.getService().getId();
        Integer floor = assignment.getFloor();
        List<UUID> exclusiveUnitIds = assignment.getUnitIds();
        
        List<Meter> allMeters;
        
        if (floor != null) {
            allMeters = meterRepository.findByBuildingServiceAndFloor(buildingId, serviceId, floor);
        } else {
            allMeters = meterRepository.findByBuildingAndService(buildingId, serviceId);
        }
        
        List<Meter> metersInScope;
        if (exclusiveUnitIds != null && !exclusiveUnitIds.isEmpty()) {
            metersInScope = allMeters.stream()
                .filter(m -> m.getUnit() != null && !exclusiveUnitIds.contains(m.getUnit().getId()))
                .toList();
        } else {
            metersInScope = allMeters;
        }
        
        int totalMetersNeeded = metersInScope.size();
        List<MeterReading> readings = meterReadingRepository.findByAssignmentId(assignment.getId());
        int readingsCount = readings.size();
        
        if (readingsCount < totalMetersNeeded) {
            throw new IllegalArgumentException(
                String.format("Not enough meter readings to complete assignment. Required: %d, Current: %d", 
                    totalMetersNeeded, readingsCount)
            );
        }
    }

    @Transactional
    public void delete(UUID id) {
        MeterReadingAssignment assignment = meterReadingAssignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        if (assignment.getStatus() == MeterReadingAssignmentStatus.COMPLETED) {
            throw new RuntimeException("Cannot delete completed assignment");
        }

        meterReadingAssignmentRepository.delete(assignment);
    }

    @Transactional
    public MeterReadingAssignmentDto cancelAssignment(UUID id, UserPrincipal principal) {
        MeterReadingAssignment assignment = meterReadingAssignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        if (assignment.getStatus() == MeterReadingAssignmentStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel completed assignment");
        }

        if (assignment.getStatus() == MeterReadingAssignmentStatus.CANCELLED) {
            throw new RuntimeException("Assignment already cancelled");
        }

        assignment.setStatus(MeterReadingAssignmentStatus.CANCELLED);
        assignment = meterReadingAssignmentRepository.save(assignment);

        return toDto(assignment);
    }

    public AssignmentProgressDto getProgress(UUID assignmentId) {
        var assignment = meterReadingAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Not found assignment"));

        UUID buildingId = assignment.getBuilding().getId();
        UUID serviceId  = assignment.getService().getId();
        Integer floor = assignment.getFloor();


        int total = floor != null 
            ? meterRepository.findByBuildingServiceAndFloor(buildingId, serviceId, floor).size()
            : meterRepository.findByBuildingAndService(buildingId, serviceId).size();

        int done = meterReadingRepository.findByAssignmentId(assignmentId).size();
        int remain = Math.max(0, total-done);
        double percent = total > 0 ? Math.round((done * 10000.0 / total))/100.0 : 0;
        boolean completed = assignment.getCompletedAt() != null || (total > 0 && done >= total);

        return new AssignmentProgressDto(
            total,
            done,
            remain,
            percent,
            completed,
            assignment.getCompletedAt()
        );
    }

    public MeterReadingAssignmentDto toDto(MeterReadingAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment cannot be null");
        }
        
        return new MeterReadingAssignmentDto(
                assignment.getId(),
                assignment.getCycle().getId(),
                assignment.getCycle().getName(),
                assignment.getBuilding() != null ? assignment.getBuilding().getId() : null,
                assignment.getBuilding() != null ? assignment.getBuilding().getCode() : null,
                assignment.getBuilding() != null ? assignment.getBuilding().getName() : null,
                assignment.getService().getId(),
                assignment.getService().getCode(),
                assignment.getService().getName(),
                assignment.getAssignedTo(),
                assignment.getAssignedBy(),
                assignment.getAssignedAt(),
                assignment.getStartDate(),
                assignment.getEndDate(),
                assignment.getCompletedAt(),
                assignment.getNote(),
                assignment.getFloor(),
                assignment.getUnitIds(),
                assignment.getStatus() != null ? assignment.getStatus().name() : null,
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }

    private MeterReadingAssignmentStatus determineInitialStatus(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return MeterReadingAssignmentStatus.PENDING;
        }
        
        LocalDate today = LocalDate.now();
        
        if (endDate.isBefore(today)) {
            return MeterReadingAssignmentStatus.OVERDUE;
        } else if (startDate.compareTo(today) <= 0 && endDate.compareTo(today) >= 0) {
            return MeterReadingAssignmentStatus.IN_PROGRESS;
        } else {
            return MeterReadingAssignmentStatus.PENDING;
        }
    }
}
