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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ReadingCycleService {
    private final ReadingCycleRepository readingCycleRepository;

    public ReadingCycleDto createCycle(ReadingCycleCreateReq req, Authentication authentication) {
        var principal = (UserPrincipal) authentication.getPrincipal();
        UUID createdBy = principal.uid();

        if (req.periodFrom().isAfter(req.periodTo())) {
            throw new IllegalArgumentException("Period from must be before period to");
        }

        ReadingCycle cycle = ReadingCycle.builder()
                .name(req.name())
                .periodFrom(req.periodFrom())
                .periodTo(req.periodTo())
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
        var principal = (UserPrincipal) authentication.getPrincipal();

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
        ReadingCycle existing = readingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Reading cycle not found with id: " + cycleId));

        validateStatusTransition(existing.getStatus(), newStatus);
        existing.setStatus(newStatus);

        ReadingCycle saved = readingCycleRepository.save(existing);
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

    private void validateStatusTransition(ReadingCycleStatus current, ReadingCycleStatus next) {
        if (current == ReadingCycleStatus.CLOSED) {
            throw new IllegalStateException("Cannot change status of a closed cycle");
        }

        switch (current) {
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
}
