package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.ReadingCycleCreateReq;
import com.QhomeBase.baseservice.dto.ReadingCycleDto;
import com.QhomeBase.baseservice.dto.ReadingCycleUpdateReq;
import com.QhomeBase.baseservice.model.ReadingCycle;
import com.QhomeBase.baseservice.model.ReadingCycleStatus;
import com.QhomeBase.baseservice.repository.ReadingCycleRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReadingCycleService {
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ReadingCycleRepository readingCycleRepository;

    public ReadingCycleDto createCycle(ReadingCycleCreateReq req, Authentication authentication) {
        var principal = (UserPrincipal) authentication.getPrincipal();
        UUID createdBy = principal.uid();

        YearMonth targetMonth = validateMonthlyWindow(req.periodFrom(), req.periodTo());
        ensureCycleDoesNotExist(targetMonth);

        ReadingCycle cycle = ReadingCycle.builder()
                .name(buildCycleName(targetMonth))
                .periodFrom(targetMonth.atDay(1))
                .periodTo(targetMonth.atEndOfMonth())
                .status(ReadingCycleStatus.OPEN)
                .description(req.description())
                .createdBy(createdBy)
                .build();

        ReadingCycle saved = readingCycleRepository.save(cycle);
        return toDto(saved);
    }

    public List<ReadingCycleDto> getAllCycles() {
        return readingCycleRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ReadingCycleDto getCycleById(UUID cycleId) {
        ReadingCycle cycle = readingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Reading cycle not found with id: " + cycleId));
        return toDto(cycle);



    }

    public List<ReadingCycleDto> getCyclesByStatus(ReadingCycleStatus status) {
        return readingCycleRepository.findAll().stream()
                .filter(cycle -> cycle.getStatus() == status)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ReadingCycleDto> getCyclesByPeriod(LocalDate from, LocalDate to) {
        return readingCycleRepository.findAll().stream()
                .filter(cycle -> !cycle.getPeriodFrom().isAfter(to) 
                        && !cycle.getPeriodTo().isBefore(from))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ReadingCycleDto updateCycle(UUID cycleId, ReadingCycleUpdateReq req, Authentication authentication) {
        ReadingCycle existing = readingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Reading cycle not found with id: " + cycleId));

        if (req.description() != null) {
            existing.setDescription(req.description());
        }
        if (req.status() != null) {
            validateStatusTransition(existing.getStatus(), req.status());
            existing.setStatus(req.status());
        }

        ReadingCycle saved = readingCycleRepository.save(existing);
        return toDto(saved);
    }

    public ReadingCycleDto changeCycleStatus(UUID cycleId, ReadingCycleStatus newStatus) {
        log.debug("Changing cycle {} status to {}", cycleId, newStatus);
        
        ReadingCycle existing = readingCycleRepository.findById(cycleId)
                .orElseThrow(() -> {
                    log.warn("Reading cycle not found with id: {}", cycleId);
                    return new IllegalArgumentException("Reading cycle not found with id: " + cycleId);
                });

        log.debug("Current cycle status: {}, requested status: {}", existing.getStatus(), newStatus);
        
        try {
            validateStatusTransition(existing.getStatus(), newStatus);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            log.warn("Status transition validation failed for cycle {}: {} -> {}: {}", 
                    cycleId, existing.getStatus(), newStatus, ex.getMessage());
            throw ex;
        }
        
        ReadingCycleStatus oldStatus = existing.getStatus();
        existing.setStatus(newStatus);
        ReadingCycle saved = readingCycleRepository.save(existing);
        
        log.info("Successfully changed cycle {} status from {} to {}", 
                cycleId, oldStatus, newStatus);
        
        return toDto(saved);
    }

    public void deleteCycle(UUID cycleId) {
        ReadingCycle cycle = readingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Reading cycle not found with id: " + cycleId));

        if (cycle.getStatus() != ReadingCycleStatus.OPEN) {
            throw new IllegalStateException("Can only delete cycles with status OPEN");
        }

        readingCycleRepository.delete(cycle);
    }

    public ReadingCycle ensureMonthlyCycle(YearMonth month) {
        String cycleName = buildCycleName(month);
        return readingCycleRepository.findByName(cycleName)
                .orElseGet(() -> {
                    ReadingCycle cycle = ReadingCycle.builder()
                            .name(cycleName)
                            .periodFrom(month.atDay(1))
                            .periodTo(month.atEndOfMonth())
                            .status(ReadingCycleStatus.OPEN)
                            .description("Auto-generated cycle for " + cycleName)
                            .build();
                    ReadingCycle saved = readingCycleRepository.save(cycle);
                    log.info("Auto-created reading cycle {}", cycleName);
                    return saved;
                });
    }

    private void validateStatusTransition(ReadingCycleStatus current, ReadingCycleStatus next) {
        if (current == null || next == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        
        switch (current) {
            case CLOSED:
                throw new IllegalStateException("Cannot change status of a closed cycle");
            case OPEN:
                if (next != ReadingCycleStatus.IN_PROGRESS && next != ReadingCycleStatus.CLOSED) {
                    throw new IllegalStateException("OPEN cycle can only transition to IN_PROGRESS or CLOSED");
                }
                break;
            case IN_PROGRESS:
                if (next != ReadingCycleStatus.COMPLETED && next != ReadingCycleStatus.CLOSED) {
                    throw new IllegalStateException("IN_PROGRESS cycle can only transition to COMPLETED or CLOSED");
                }
                break;
            case COMPLETED:
                if (next != ReadingCycleStatus.CLOSED) {
                    throw new IllegalStateException("COMPLETED cycle can only transition to CLOSED");
                }
                break;
            default:
                throw new IllegalStateException("Unknown current status: " + current);
        }
    }

    private ReadingCycleDto toDto(ReadingCycle cycle) {
        return new ReadingCycleDto(
                cycle.getId(),
                cycle.getName(),
                cycle.getPeriodFrom(),
                cycle.getPeriodTo(),
                cycle.getStatus(),
                cycle.getDescription(),
                cycle.getCreatedBy(),
                cycle.getCreatedAt(),
                cycle.getUpdatedAt()
        );
    }

    private YearMonth validateMonthlyWindow(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Period from/to cannot be null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Period from must be before period to");
        }

        YearMonth yearMonth = YearMonth.from(from);
        if (!from.equals(yearMonth.atDay(1)) || !to.equals(yearMonth.atEndOfMonth())) {
            throw new IllegalArgumentException("Reading cycles must align to a full calendar month");
        }
        return yearMonth;
    }

    private void ensureCycleDoesNotExist(YearMonth month) {
        String cycleName = buildCycleName(month);
        readingCycleRepository.findByName(cycleName).ifPresent(existing -> {
            throw new IllegalStateException("Reading cycle already exists for " + cycleName);
        });
        List<ReadingCycle> overlaps = readingCycleRepository.findOverlappingCycles(
                month.atDay(1), month.atEndOfMonth());
        if (!overlaps.isEmpty()) {
            throw new IllegalStateException("Reading cycle overlaps with an existing period");
        }
    }

    private String buildCycleName(YearMonth month) {
        return month.format(MONTH_FORMATTER);
    }
}
