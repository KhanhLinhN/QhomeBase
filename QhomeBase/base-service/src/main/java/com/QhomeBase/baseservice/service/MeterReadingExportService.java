package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.client.FinanceBillingClient;
import com.QhomeBase.baseservice.dto.BillingImportedReadingDto;
import com.QhomeBase.baseservice.dto.MeterReadingImportResponse;
import com.QhomeBase.baseservice.model.MeterReading;
import com.QhomeBase.baseservice.repository.HouseholdRepository;
import com.QhomeBase.baseservice.repository.MeterReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeterReadingExportService {

    private final MeterReadingRepository meterReadingRepository;
    private final HouseholdRepository householdRepository;
    private final FinanceBillingClient financeBillingClient;

    @Transactional(readOnly = true)
    public MeterReadingImportResponse exportReadingsByCycle(UUID cycleId) {
        log.info("Exporting readings for cycle: {}", cycleId);
        List<MeterReading> readings = meterReadingRepository.findByCycleId(cycleId);
        log.info("Found {} readings linked to cycle {} via column or assignment", readings.size(), cycleId);
        
        if (readings.isEmpty()) {
            log.warn("No readings found for cycle: {}. Checking all readings...", cycleId);
            
            List<MeterReading> allReadings = meterReadingRepository.findAll();
            log.debug("Total readings in database: {}", allReadings.size());
            
            for (MeterReading r : allReadings) {
                if (r.getAssignment() != null && r.getAssignment().getCycle() != null) {
                    log.debug("Reading {} has assignment.cycle.id = {}", r.getId(), r.getAssignment().getCycle().getId());
                } else {
                    log.debug("Reading {} has no assignment/cycle", r.getId());
                }
            }
            
            return MeterReadingImportResponse.builder()
                    .totalReadings(0)
                    .invoicesCreated(0)
                    .message("No readings found for cycle: " + cycleId)
                    .build();
        }
        
        long mismatchedCycleCount = readings.stream()
                .filter(r -> r.getAssignment() != null && r.getAssignment().getCycle() != null)
                .filter(r -> !cycleId.equals(r.getAssignment().getCycle().getId()))
                .count();
        if (mismatchedCycleCount > 0) {
            log.warn("Detected {} readings whose assignment.cycle != requested cycle {}", mismatchedCycleCount, cycleId);
            readings.stream()
                    .filter(r -> r.getAssignment() != null && r.getAssignment().getCycle() != null)
                    .filter(r -> !cycleId.equals(r.getAssignment().getCycle().getId()))
                    .limit(20)
                    .forEach(r -> log.warn("Reading {} -> assignment {} cycle {}", r.getId(),
                            r.getAssignment().getId(), r.getAssignment().getCycle().getId()));
        }
        log.info("Proceeding with {} readings for cycle {}", readings.size(), cycleId);

        List<BillingImportedReadingDto> billingReadings = convertToBillingReadings(readings);
        MeterReadingImportResponse response = financeBillingClient.importMeterReadingsSync(billingReadings);
        
        log.info("Exported {} readings from cycle {} to finance-billing. Invoices created: {}", 
                readings.size(), cycleId, response != null ? response.getInvoicesCreated() : 0);
        return response;
    }

    private List<BillingImportedReadingDto> convertToBillingReadings(List<MeterReading> readings) {
        List<BillingImportedReadingDto> result = new ArrayList<>();
        
        for (MeterReading reading : readings) {
            if (reading.getMeter() == null || reading.getUnit() == null) {
                log.warn("Skipping reading {} - missing meter or unit", reading.getId());
                continue;
            }

            UUID unitId = reading.getUnit().getId();
            UUID residentId = getResidentId(unitId);
            
            if (residentId == null) {
                log.warn("No active resident found for unit {}, but proceeding with null residentId for reading {}", unitId, reading.getId());
            }

            UUID cycleId = null;
            if (reading.getAssignment() != null && reading.getAssignment().getCycle() != null) {
                cycleId = reading.getAssignment().getCycle().getId();
            } else {
                log.warn("Skipping reading {} - missing assignment/cycle", reading.getId());
                continue;
            }
            if (!cycleId.equals(reading.getCycleId())) {
                log.warn("Reading {} cycle mismatch: column={} assignment.cycle={}", reading.getId(),
                        reading.getCycleId(), cycleId);
            }
            log.debug("Preparing billing reading {} unit {} assignment {} cycle {}", reading.getId(),
                    unitId, reading.getAssignment() != null ? reading.getAssignment().getId() : null, cycleId);
            String serviceCode = reading.getMeter().getService() != null 
                    ? reading.getMeter().getService().getCode() 
                    : null;

            if (serviceCode == null) {
                log.warn("Skipping reading {} - missing service code", reading.getId());
                continue;
            }

            BigDecimal usageKwh = reading.getCurrIndex() != null && reading.getPrevIndex() != null
                    ? reading.getCurrIndex().subtract(reading.getPrevIndex())
                    : null;

            if (usageKwh == null || usageKwh.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("Skipping reading {} - invalid usage: {}", reading.getId(), usageKwh);
                continue;
            }

            BillingImportedReadingDto billingReading = BillingImportedReadingDto.builder()
                    .unitId(unitId)
                    .residentId(residentId)
                    .cycleId(cycleId)
                    .readingDate(reading.getReadingDate())
                    .usageKwh(usageKwh)
                    .serviceCode(serviceCode)
                    .description(buildDescription(reading))
                    .externalReadingId(reading.getId())
                    .build();

            result.add(billingReading);
        }

        return result;
    }

    private UUID getResidentId(UUID unitId) {
        return householdRepository.findCurrentHouseholdByUnitId(unitId)
                .map(household -> household.getPrimaryResidentId())
                .orElse(null);
    }

    private String buildDescription(MeterReading reading) {
        StringBuilder desc = new StringBuilder();
        if (reading.getMeter() != null && reading.getMeter().getMeterCode() != null) {
            desc.append("Meter: ").append(reading.getMeter().getMeterCode());
        }
        if (reading.getNote() != null && !reading.getNote().trim().isEmpty()) {
            if (desc.length() > 0) desc.append(" - ");
            desc.append(reading.getNote());
        }
        return desc.length() > 0 ? desc.toString() : null;
    }
}
