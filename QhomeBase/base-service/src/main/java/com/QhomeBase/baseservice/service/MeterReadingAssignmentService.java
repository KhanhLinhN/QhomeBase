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
        
        if (req.floorFrom() != null && req.floorTo() != null) {
            validateFloorRange(req.floorFrom(), req.floorTo());
        }
        
        if (building != null) {
            validateNoOverlap(
                req.cycleId(), 
                building.getId(), 
                req.serviceId(),
                startDate, 
                endDate, 
                req.floorFrom(), 
                req.floorTo()
            );
        }
        
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
                .floorFrom(req.floorFrom())
                .floorTo(req.floorTo())
                .build();
        
        assignment = meterReadingAssignmentRepository.save(assignment);
        return toDto(assignment);
    }

    private void validateFloorRange(Integer floorFrom, Integer floorTo) {
        if (floorFrom > floorTo) {
            throw new RuntimeException(
                String.format("floorFrom (%d) must be <= floorTo (%d)", floorFrom, floorTo)
            );
        }
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
                                    Integer floorFrom, Integer floorTo) {
        
        List<MeterReadingAssignment> assignments = meterReadingAssignmentRepository
                .findByCycleId(cycleId)
                .stream()
                .filter(a -> a.getBuilding() != null && a.getBuilding().getId().equals(buildingId))
                .filter(a -> a.getService().getId().equals(serviceId))
                .filter(a -> a.getCompletedAt() == null)
                .collect(Collectors.toList());
        
        for (MeterReadingAssignment existing : assignments) {
            boolean timeOverlap = !startDate.isAfter(existing.getEndDate()) 
                               && !endDate.isBefore(existing.getStartDate());
            
            if (!timeOverlap) {
                continue;
            }
            
            boolean floorOverlap = checkFloorOverlap(
                floorFrom, floorTo,
                existing.getFloorFrom(), existing.getFloorTo()
            );
            
            if (floorOverlap) {
                throw new RuntimeException(
                    String.format(
                        "Assignment overlap detected! " +
                        "Existing: %s to %s, Floors %s | " +
                        "New: %s to %s, Floors %s",
                        existing.getStartDate(), existing.getEndDate(),
                        formatFloorRange(existing.getFloorFrom(), existing.getFloorTo()),
                        startDate, endDate,
                        formatFloorRange(floorFrom, floorTo)
                    )
                );
            }
        }
    }

    private boolean checkFloorOverlap(Integer from1, Integer to1, Integer from2, Integer to2) {
        if (from1 == null || to1 == null || from2 == null || to2 == null) {
            return true;
        }
        return from1 <= to2 && to1 >= from2;
    }

    private String formatFloorRange(Integer from, Integer to) {
        if (from == null || to == null) {
            return "ALL FLOORS";
        }
        return String.format("Floors %d-%d", from, to);
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
        return meterReadingAssignmentRepository.findByAssignedToAndCompletedAtIsNull(staffId)
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

        assignment.setCompletedAt(OffsetDateTime.now());
        assignment = meterReadingAssignmentRepository.save(assignment);

        return toDto(assignment);
    }

    @Transactional
    public void delete(UUID id) {
        MeterReadingAssignment assignment = meterReadingAssignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        if (assignment.getCompletedAt() != null) {
            throw new RuntimeException("Cannot delete completed assignment");
        }

        meterReadingAssignmentRepository.delete(assignment);
    }

    public AssignmentProgressDto getProgress(UUID assignmentId) {
        var assignment = meterReadingAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Not found assignment"));

        UUID buildingId = assignment.getBuilding().getId();
        UUID serviceId  = assignment.getService().getId();
        Integer floorFrom   = assignment.getFloorFrom();
        Integer floorTo     = assignment.getFloorTo();

        // Tổng meter cần đọc
        int total = meterRepository.findByBuildingServiceAndFloorRange(buildingId, serviceId, floorFrom, floorTo).size();
        // Đã đọc (phiếu hợp lệ, gắn đúng assignment)
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
                assignment.getFloorFrom(),
                assignment.getFloorTo(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }
}
