package com.QhomeBase.datadocsservice.service;

import com.QhomeBase.datadocsservice.model.Contract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContractScheduler {

    private final ContractService contractService;
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("🚀 Application ready - Running initial contract status checks...");
        contractService.markExpiredContracts();
        sendRenewalReminders();
        markRenewalDeclined();
        log.info("✅ Initial contract status checks completed");
    }

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

    @Scheduled(cron = "0 0 1 * * ?")
    public void markExpiredContractsDaily() {
        try {
            log.info("Starting scheduled task: Mark expired contracts");
            int expiredCount = contractService.markExpiredContracts();
            log.info("Scheduled task completed: Marked {} contract(s) as expired", expiredCount);
        } catch (Exception e) {
            log.error("Error in scheduled task to mark expired contracts", e);
        }
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void sendRenewalReminders() {
        try {
            log.info("Starting scheduled task: Send renewal reminders");
            LocalDate today = LocalDate.now();
            LocalDate thirtyDaysFromToday = today.plusDays(30);
            
            log.info("Today: {}, Looking for contracts expiring within next 30 days (until {})", today, thirtyDaysFromToday);
            
            int firstReminderCount = 0;
            List<Contract> firstReminderContracts = contractService.findContractsNeedingRenewalReminder();
            log.info("Found {} contract(s) with endDate in range [today, oneMonthLater]", firstReminderContracts.size());
            
            for (Contract contract : firstReminderContracts) {
                log.debug("Checking contract {}: endDate={}, renewalStatus={}, reminderSentAt={}", 
                        contract.getContractNumber(), 
                        contract.getEndDate(), 
                        contract.getRenewalStatus(),
                        contract.getRenewalReminderSentAt());
                
                try {
                    if (contract.getEndDate() != null 
                            && contract.getRenewalReminderSentAt() == null
                            && !contract.getEndDate().isBefore(today)
                            && !contract.getEndDate().isAfter(thirtyDaysFromToday)) {
                        contractService.sendRenewalReminder(contract.getId());
                        firstReminderCount++;
                        log.info("Sent first renewal reminder for contract {} (expires on {}, within 30 days from today)", 
                                contract.getContractNumber(), contract.getEndDate());
                    } else {
                        if (contract.getEndDate() == null) {
                            log.debug("Contract {} skipped: endDate is null", contract.getContractNumber());
                        } else if (contract.getRenewalReminderSentAt() != null) {
                            log.debug("Contract {} skipped: reminder already sent at {}", 
                                    contract.getContractNumber(), contract.getRenewalReminderSentAt());
                        } else if (contract.getEndDate().isBefore(today)) {
                            log.debug("Contract {} skipped: endDate {} is in the past", 
                                    contract.getContractNumber(), contract.getEndDate());
                        } else if (contract.getEndDate().isAfter(thirtyDaysFromToday)) {
                            log.debug("Contract {} skipped: endDate {} is more than 30 days away", 
                                    contract.getContractNumber(), contract.getEndDate());
                        }
                    }
                } catch (Exception e) {
                    log.error("Error sending first renewal reminder for contract {}", contract.getId(), e);
                }
            }
            
            int secondReminderCount = 0;
            List<Contract> secondReminderContracts = contractService.findContractsNeedingSecondReminder();
            for (Contract contract : secondReminderContracts) {
                try {
                    if (contract.getEndDate() != null 
                            && "REMINDED".equals(contract.getRenewalStatus())
                            && contract.getRenewalReminderSentAt() != null) {
                        long daysSinceFirstReminder = ChronoUnit.DAYS.between(
                            contract.getRenewalReminderSentAt().toLocalDate(),
                            today
                        );
                        
                        if (daysSinceFirstReminder >= 7 && daysSinceFirstReminder < 20) {
                            contractService.sendRenewalReminder(contract.getId());
                            secondReminderCount++;
                            log.info("Sent second renewal reminder for contract {} (expires on {}, {} days since first reminder)", 
                                    contract.getContractNumber(), contract.getEndDate(), daysSinceFirstReminder);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error sending second renewal reminder for contract {}", contract.getId(), e);
                }
            }
            
            int thirdReminderCount = 0;
            List<Contract> thirdReminderContracts = contractService.findContractsNeedingThirdReminder();
            for (Contract contract : thirdReminderContracts) {
                try {
                    if (contract.getEndDate() != null 
                            && "REMINDED".equals(contract.getRenewalStatus())
                            && contract.getRenewalReminderSentAt() != null) {
                        long daysSinceFirstReminder = ChronoUnit.DAYS.between(
                            contract.getRenewalReminderSentAt().toLocalDate(),
                            today
                        );
                        
                        if (daysSinceFirstReminder >= 20) {
                            contractService.sendRenewalReminder(contract.getId());
                            thirdReminderCount++;
                            log.info("Sent third (FINAL) renewal reminder for contract {} (expires on {}, {} days since first reminder - THIS IS THE DEADLINE)", 
                                    contract.getContractNumber(), contract.getEndDate(), daysSinceFirstReminder);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error sending third renewal reminder for contract {}", contract.getId(), e);
                }
            }
            
            log.info("Scheduled task completed: Sent {} first reminder(s), {} second reminder(s), {} third reminder(s)", 
                    firstReminderCount, secondReminderCount, thirdReminderCount);
        } catch (Exception e) {
            log.error("Error in scheduled task to send renewal reminders", e);
        }
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void markRenewalDeclined() {
        try {
            log.info("Starting scheduled task: Mark renewal declined");
            OffsetDateTime deadlineDate = OffsetDateTime.now().minusDays(20);
            
            List<Contract> contracts = contractService.findContractsWithRenewalDeclined(deadlineDate);
            log.info("Found {} contract(s) with reminder sent >= 20 days ago", contracts.size());
            
            int declinedCount = 0;
            for (Contract contract : contracts) {
                try {
                    if ("REMINDED".equals(contract.getRenewalStatus()) && contract.getRenewalReminderSentAt() != null) {
                        long daysSinceFirstReminder = ChronoUnit.DAYS.between(
                            contract.getRenewalReminderSentAt().toLocalDate(),
                            LocalDate.now()
                        );
                        
                        log.debug("Checking contract {}: daysSinceFirstReminder = {}", 
                                contract.getContractNumber(), daysSinceFirstReminder);
                        
                        if (daysSinceFirstReminder > 20) {
                            contractService.markRenewalDeclined(contract.getId());
                            declinedCount++;
                            log.info("Marked contract {} as renewal declined (first reminder sent on {}, {} days ago - deadline passed)", 
                                    contract.getContractNumber(), contract.getRenewalReminderSentAt(), daysSinceFirstReminder);
                        } else {
                            log.debug("Contract {} skipped: daysSinceFirstReminder ({}) is not > 20", 
                                    contract.getContractNumber(), daysSinceFirstReminder);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error marking contract {} as renewal declined", contract.getId(), e);
                }
            }
            
            log.info("Scheduled task completed: Marked {} contract(s) as renewal declined", declinedCount);
        } catch (Exception e) {
            log.error("Error in scheduled task to mark renewal declined", e);
        }
    }
}

