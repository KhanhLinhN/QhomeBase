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
        
        if (readings.isEmpty()) {
            log.warn("No readings found for cycle: {}. Checking all readings...", cycleId);
            
            List<MeterReading> allReadings = meterReadingRepository.findAll();
            log.debug("Total readings in database: {}", allReadings.size());
            
            for (MeterReading r : allReadings) {
                if (r.getSession() != null && r.getSession().getCycle() != null) {
                    log.debug("Reading {} has session.cycle.id = {}", r.getId(), r.getSession().getCycle().getId());
                } else if (r.getAssignment() != null && r.getAssignment().getCycle() != null) {
                    log.debug("Reading {} has assignment.cycle.id = {}", r.getId(), r.getAssignment().getCycle().getId());
                } else {
                    log.debug("Reading {} has no session/cycle or assignment/cycle", r.getId());
                }
            }
            
            return MeterReadingImportResponse.builder()
                    .totalReadings(0)
                    .invoicesCreated(0)
                    .message("No readings found for cycle: " + cycleId)
                    .build();
        }
        
        log.info("Found {} readings for cycle: {}", readings.size(), cycleId);

        List<BillingImportedReadingDto> billingReadings = convertToBillingReadings(readings);
        MeterReadingImportResponse response = financeBillingClient.importMeterReadingsSync(billingReadings);
        
        log.info("Exported {} readings from cycle {} to finance-billing. Invoices created: {}", 
                readings.size(), cycleId, response != null ? response.getInvoicesCreated() : 0);
        return response;
    }

    private List<BillingImportedReadingDto> convertToBillingReadings(List<MeterReading> readings) {
        List<BillingImportedReadingDto> result = new ArrayList<>();
        
        for (MeterReading reading : readings) {
            if (reading.getMeter() == null || reading.getMeter().getUnit() == null) {
                log.warn("Skipping reading {} - missing meter or unit", reading.getId());
                continue;
            }

            UUID unitId = reading.getMeter().getUnit().getId();
            UUID residentId = getResidentId(unitId);
            
            if (residentId == null) {
                log.warn("No active resident found for unit {}, but proceeding with null residentId for reading {}", unitId, reading.getId());
            }

            UUID cycleId = null;
            if (reading.getSession() != null && reading.getSession().getCycle() != null) {
                cycleId = reading.getSession().getCycle().getId();
            } else if (reading.getAssignment() != null && reading.getAssignment().getCycle() != null) {
                cycleId = reading.getAssignment().getCycle().getId();
            } else {
                log.warn("Skipping reading {} - missing session/cycle or assignment/cycle", reading.getId());
                continue;
            }
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
