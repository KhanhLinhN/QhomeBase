package com.QhomeBase.baseservice.scheduler;

import com.QhomeBase.baseservice.service.ReadingCycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.YearMonth;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReadingCycleScheduler {

    private final ReadingCycleService readingCycleService;
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

        readingCycleService.ensureMonthlyCycle(currentMonth);
        readingCycleService.ensureMonthlyCycle(nextMonth);
        log.debug("Ensured reading cycles exist for {} and {}", currentMonth, nextMonth);
    }
}





