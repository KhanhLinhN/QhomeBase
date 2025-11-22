package com.QhomeBase.baseservice.scheduler;

import com.QhomeBase.baseservice.model.ReadingCycle;
import com.QhomeBase.baseservice.repository.ServiceRepository;
import com.QhomeBase.baseservice.service.ReadingCycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReadingCycleScheduler {

    private final ReadingCycleService readingCycleService;
    private final ServiceRepository serviceRepository;
    private final ZoneId zoneId = ZoneId.systemDefault();

    @PostConstruct
    public void initializeCycles() {
        ensureCurrentAndNextCycles();
    }

    @Scheduled(cron = "${meter-reading.cycle.cron:0 0 1 * * *}")
    public void scheduledCycleGeneration() {
        ensureCurrentAndNextCycles();
    }

    private void ensureCurrentAndNextCycles() {
        YearMonth currentMonth = YearMonth.now(zoneId);
        YearMonth nextMonth = currentMonth.plusMonths(1);

        List<com.QhomeBase.baseservice.model.Service> servicesRequiringMeter = 
                serviceRepository.findByActiveAndRequiresMeter(true, true);

        if (servicesRequiringMeter.isEmpty()) {
            log.warn("No active services requiring meter reading found. Skipping cycle creation.");
            return;
        }

        for (com.QhomeBase.baseservice.model.Service service : servicesRequiringMeter) {
            try {
                ReadingCycle currentCycle = readingCycleService.ensureMonthlyCycle(currentMonth, service.getId());
                ReadingCycle nextCycle = readingCycleService.ensureMonthlyCycle(nextMonth, service.getId());
                readingCycleService.ensureBillingCycleFor(currentCycle);
                readingCycleService.ensureBillingCycleFor(nextCycle);
                log.debug("Ensured reading cycles for service {} ({} and {})", 
                        service.getCode(), currentMonth, nextMonth);
            } catch (Exception e) {
                log.error("Failed to ensure cycles for service {}: {}", service.getCode(), e.getMessage(), e);
            }
        }

        log.debug("Ensured reading cycles exist for {} services, months {} and {}", 
                servicesRequiringMeter.size(), currentMonth, nextMonth);
    }
}


