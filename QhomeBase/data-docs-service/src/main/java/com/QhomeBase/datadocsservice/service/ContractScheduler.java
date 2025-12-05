package com.QhomeBase.datadocsservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to automatically activate contracts with status INACTIVE
 * when their start date equals today.
 * Runs daily at 00:00:00 (midnight).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContractScheduler {

    private final ContractService contractService;

    /**
     * Activate inactive contracts daily at midnight.
     * Cron expression: "0 0 0 * * ?" means:
     * - 0 seconds
     * - 0 minutes
     * - 0 hours (midnight)
     * - Every day of month
     * - Every month
     * - Every day of week
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void activateInactiveContractsDaily() {
        try {
            log.info("Starting scheduled task: Activate inactive contracts");
            int activatedCount = contractService.activateInactiveContracts();
            log.info("Scheduled task completed: Activated {} contract(s)", activatedCount);
        } catch (Exception e) {
            log.error("Error in scheduled task to activate inactive contracts", e);
        }
    }
}

