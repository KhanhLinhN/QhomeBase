package com.QhomeBase.datadocsservice.service;

import com.QhomeBase.datadocsservice.client.BaseServiceClient;
import com.QhomeBase.datadocsservice.client.NotificationClient;
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
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContractScheduler {

    private final ContractService contractService;
    private final NotificationClient notificationClient;
    private final BaseServiceClient baseServiceClient;
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready - Running initial contract status checks...");
        contractService.markExpiredContracts();
        sendRenewalReminders();
        markRenewalDeclined();
        log.info("Initial contract status checks completed");
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
            
            // Get all active RENTAL contracts that need reminders
            List<Contract> allContracts = contractService.findContractsNeedingRenewalReminder();
            log.info("Found {} contract(s) that may need renewal reminders", allContracts.size());
            
            int firstReminderCount = 0;
            int secondReminderCount = 0;
            int thirdReminderCount = 0;
            
            for (Contract contract : allContracts) {
                if (contract.getEndDate() == null || !"RENTAL".equals(contract.getContractType()) 
                        || !"ACTIVE".equals(contract.getStatus())) {
                    continue;
                }
                
                LocalDate endDate = contract.getEndDate();
                
                // Calculate days until end date
                long daysUntilEndDate = ChronoUnit.DAYS.between(today, endDate);
                
                log.debug("Checking contract {}: endDate={}, today={}, daysUntilEndDate={}, renewalStatus={}, reminderSentAt={}", 
                        contract.getContractNumber(), endDate, today, daysUntilEndDate,
                        contract.getRenewalStatus(), contract.getRenewalReminderSentAt());
                
                try {
                    // Lần 1: Trước 30 ngày hết hạn hợp đồng
                    // Gửi khi còn 28-32 ngày (buffer để đảm bảo không bỏ sót do scheduler chạy 1 lần/ngày)
                    if (daysUntilEndDate >= 28 && daysUntilEndDate <= 32 
                            && contract.getRenewalReminderSentAt() == null) {
                        contractService.sendRenewalReminder(contract.getId());
                        sendReminderNotification(contract, 1, false);
                        firstReminderCount++;
                        log.info("✅ Sent FIRST renewal reminder for contract {} (expires on {}, {} days until end date)", 
                                contract.getContractNumber(), endDate, daysUntilEndDate);
                    }
                    // Lần 2: Đúng ngày 8 của tháng endDate
                    // Chỉ gửi nếu:
                    // - Đã gửi lần 1 (renewalReminderSentAt != null)
                    // - Hôm nay là ngày 8 của tháng endDate
                    // - Contract vẫn trong tháng cuối (daysUntilEndDate > 0 và < 30)
                    // - Lần 1 đã được gửi trước hôm nay (đảm bảo không gửi lần 2 trước lần 1)
                    else if (contract.getRenewalReminderSentAt() != null
                            && "REMINDED".equals(contract.getRenewalStatus())
                            && today.getYear() == endDate.getYear()
                            && today.getMonth() == endDate.getMonth()
                            && today.getDayOfMonth() == 8
                            && daysUntilEndDate > 0 && daysUntilEndDate < 30) {
                        // Check if we already sent reminder 2 (by checking if reminder was sent before today)
                        LocalDate firstReminderDate = contract.getRenewalReminderSentAt().toLocalDate();
                        // Lần 2 chỉ gửi 1 lần vào ngày 8, và chỉ gửi nếu lần 1 đã được gửi trước đó
                        // Kiểm tra: lần 1 phải được gửi trước ngày 8 (không phải cùng ngày 8)
                        if (firstReminderDate.isBefore(today) && firstReminderDate.getDayOfMonth() != 8) {
                            contractService.sendRenewalReminder(contract.getId());
                            sendReminderNotification(contract, 2, false);
                            secondReminderCount++;
                            log.info("✅ Sent SECOND renewal reminder for contract {} (expires on {}, today is day 8 of endDate month)", 
                                    contract.getContractNumber(), endDate);
                        } else {
                            log.debug("⏭️ Skipping reminder 2 for contract {}: firstReminderDate={}, today={}", 
                                    contract.getContractNumber(), firstReminderDate, today);
                }
            }
                    // Lần 3: Đúng ngày 20 của tháng endDate - BẮT BUỘC
                    // Chỉ gửi nếu:
                    // - Đã gửi lần 1 (renewalReminderSentAt != null)
                    // - Hôm nay là ngày 20 của tháng endDate
                    // - Contract vẫn trong tháng cuối (daysUntilEndDate > 0 và < 30)
                    // - Lần 1 đã được gửi trước hôm nay (đảm bảo không gửi lần 3 trước lần 1)
                    else if (contract.getRenewalReminderSentAt() != null
                            && "REMINDED".equals(contract.getRenewalStatus())
                            && today.getYear() == endDate.getYear()
                            && today.getMonth() == endDate.getMonth()
                            && today.getDayOfMonth() == 20
                            && daysUntilEndDate > 0 && daysUntilEndDate < 30) {
                        // Check if we already sent reminder 3 (by checking if reminder was sent before today)
                        LocalDate firstReminderDate = contract.getRenewalReminderSentAt().toLocalDate();
                        // Lần 3 chỉ gửi 1 lần vào ngày 20, và chỉ gửi nếu lần 1 đã được gửi trước đó
                        // Kiểm tra: lần 1 phải được gửi trước ngày 20 (không phải cùng ngày 20)
                        if (firstReminderDate.isBefore(today) && firstReminderDate.getDayOfMonth() != 20) {
                            contractService.sendRenewalReminder(contract.getId());
                            sendReminderNotification(contract, 3, true);
                            thirdReminderCount++;
                            log.info("✅ Sent THIRD (FINAL) renewal reminder for contract {} (expires on {}, today is day 20 of endDate month - BẮT BUỘC HỦY HOẶC GIA HẠN)", 
                                    contract.getContractNumber(), endDate);
                        } else {
                            log.debug("⏭️ Skipping reminder 3 for contract {}: firstReminderDate={}, today={}", 
                                    contract.getContractNumber(), firstReminderDate, today);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error sending renewal reminder for contract {}", contract.getId(), e);
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

    /**
     * Send notification for contract renewal reminder
     */
    private void sendReminderNotification(Contract contract, int reminderNumber, boolean isFinalReminder) {
        try {
            Optional<UUID> residentIdOpt = baseServiceClient.getPrimaryResidentIdByUnitId(contract.getUnitId());
            Optional<UUID> buildingIdOpt = baseServiceClient.getBuildingIdByUnitId(contract.getUnitId());
            
            if (residentIdOpt.isPresent()) {
                UUID residentId = residentIdOpt.get();
                UUID buildingId = buildingIdOpt.orElse(null);
                
                notificationClient.sendContractRenewalReminderNotification(
                        residentId,
                        buildingId,
                        contract.getId(),
                        contract.getContractNumber(),
                        reminderNumber,
                        isFinalReminder
                );
                log.info("✅ Sent notification for contract {} reminder #{} to resident {}", 
                        contract.getContractNumber(), reminderNumber, residentId);
            } else {
                log.warn("⚠️ Could not find primary resident for unitId: {}", contract.getUnitId());
            }
        } catch (Exception e) {
            log.error("❌ Error sending notification for contract {} reminder #{}", 
                    contract.getContractNumber(), reminderNumber, e);
        }
    }
}

