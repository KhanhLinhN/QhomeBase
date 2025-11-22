package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.client.BaseServiceClient;
import com.QhomeBase.financebillingservice.dto.BillingCycleDto;
import com.QhomeBase.financebillingservice.dto.CreateBillingCycleRequest;
import com.QhomeBase.financebillingservice.dto.ReadingCycleDto;
import com.QhomeBase.financebillingservice.model.BillingCycle;
import com.QhomeBase.financebillingservice.repository.BillingCycleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingCycleService {

    private final BillingCycleRepository billingCycleRepository;
    private final BaseServiceClient baseService;

    private BillingCycleDto mapDto(BillingCycle b) {
        return BillingCycleDto.builder()
                .id(b.getId())
                .name(b.getName())
                .periodFrom(b.getPeriodFrom())
                .periodTo(b.getPeriodTo())
                .status(b.getStatus())
                .externalCycleId(b.getExternalCycleId())
                .serviceId(resolveReadingCycle(b).map(ReadingCycleDto::serviceId).orElse(null))
                .serviceCode(resolveReadingCycle(b).map(ReadingCycleDto::serviceCode).orElse(null))
                .serviceName(resolveReadingCycle(b).map(ReadingCycleDto::serviceName).orElse(null))
                .build();
    }

    private Optional<ReadingCycleDto> resolveReadingCycle(BillingCycle billingCycle) {
        if (billingCycle.getExternalCycleId() == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(baseService.getReadingCycleById(billingCycle.getExternalCycleId()));
        } catch (Exception e) {
            log.debug("Unable to fetch reading cycle {} for billing cycle {}: {}", billingCycle.getExternalCycleId(), billingCycle.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    public List<BillingCycleDto> loadPeriod(Integer year) {
        int y = (year != null) ? year : LocalDate.now().getYear();
        return billingCycleRepository.loadPeriod(y)
                .stream()
                .map(this::mapDto)
                .toList();
    }

    public List<BillingCycleDto> getListByTime(LocalDate periodFrom, LocalDate periodTo) {
        return billingCycleRepository.findListByTime(periodFrom, periodTo)
                .stream()
                .map(this::mapDto)
                .toList();
    }

    @Transactional
    public BillingCycleDto createBillingCycle(CreateBillingCycleRequest request) {
        log.info("Creating billing cycle period: {} to {}",
                request.getPeriodFrom(), request.getPeriodTo());

        if (request.getPeriodFrom().isAfter(request.getPeriodTo())) {
            throw new IllegalArgumentException("Period From must be before Period To");
        }

        BillingCycle billingCycle = BillingCycle.builder()
                .name(request.getName())
                .periodFrom(request.getPeriodFrom())
                .periodTo(request.getPeriodTo())
                .status(request.getStatus() != null ? request.getStatus() : "OPEN")
                .externalCycleId(request.getExternalCycleId())
                .build();

        BillingCycle saved = billingCycleRepository.save(billingCycle);
        log.info("Billing cycle created with ID: {}", saved.getId());

        return mapDto(saved);
    }

    @Transactional
    public BillingCycleDto updateBillingCycleStatus(UUID cycleId, String status) {
        log.info("Updating billing cycle status: {} to {}", cycleId, status);

        BillingCycle cycle = billingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Billing cycle not found: " + cycleId));

        String oldStatus = cycle.getStatus();
        cycle.setStatus(status);

        BillingCycle updated = billingCycleRepository.save(cycle);
        log.info("Billing cycle {} status updated from {} to {}", cycleId, oldStatus, status);

        return mapDto(updated);
    }

    public List<BillingCycleDto> findByExternalCycleId(UUID externalCycleId) {
        return billingCycleRepository.findByExternalCycleId(externalCycleId)
                .stream()
                .map(this::mapDto)
                .toList();
    }
    public List<UUID> findUnmatchBillingCycle() {
        List<ReadingCycleDto> readingCycleDtoList = baseService.getAllReadingCycles();
        Set<UUID> readingCycleId = readingCycleDtoList.stream().map(ReadingCycleDto::id).collect(Collectors.toSet());
        List<BillingCycle> billingCycleDtoList = billingCycleRepository.findAll();
        Set<UUID> billingCycleId = billingCycleDtoList.stream()
                .map(BillingCycle::getExternalCycleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<UUID> missingBillingCycle = readingCycleId.stream()
                .filter(id -> !billingCycleId.contains(id))
                .collect(Collectors.toSet());
        return new ArrayList<>(missingBillingCycle);
    }

    public List<ReadingCycleDto> getMissingReadingCyclesInfo() {
        List<ReadingCycleDto> readingCycles = baseService.getAllReadingCycles();
        Set<UUID> billingExternalIds = billingCycleRepository.findAll()
                .stream()
                .map(BillingCycle::getExternalCycleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return readingCycles.stream()
                .filter(rc -> !billingExternalIds.contains(rc.id()))
                .toList();
    }

    public List<BillingCycleDto> createBillingCyclesForMissingReadingCycles() {
        List<UUID> missingReadingCycles = findUnmatchBillingCycle();
        List<BillingCycleDto> createdCycles = new ArrayList<>();

        for (UUID readingCycleId : missingReadingCycles) {
            try {
                ReadingCycleDto readingCycle = baseService.getReadingCycleById(readingCycleId);
                if (readingCycle == null) {
                    continue;
                }

                LocalDate periodFrom = readingCycle.periodFrom();
                LocalDate periodTo = periodFrom.withDayOfMonth(24);

                CreateBillingCycleRequest request = CreateBillingCycleRequest.builder()
                        .name(readingCycle.name())
                        .periodFrom(periodFrom)
                        .periodTo(periodTo)
                        .status(readingCycle.status() != null ? readingCycle.status() : "OPEN")
                        .externalCycleId(readingCycle.id())
                        .build();

                BillingCycleDto created = createBillingCycle(request);
                createdCycles.add(created);
            } catch (Exception e) {
                log.error("Failed to create billing cycle for reading cycle {}: {}", readingCycleId, e.getMessage(), e);
            }
        }

        return createdCycles;
    }

}

