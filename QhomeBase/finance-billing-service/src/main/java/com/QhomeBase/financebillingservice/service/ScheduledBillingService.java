package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.model.BillingCycle;
import com.QhomeBase.financebillingservice.repository.BillingCycleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledBillingService {
    
    private final BillingCycleRepository billingCycleRepository;
    
    /**
     * Scheduled job to create billing cycles automatically
     * Runs on the 1st day of every month at 00:00 AM
     * Cron: "0 0 0 1 * ?" = second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void createMonthlyBillingCycles() {
        log.info("🔄 Starting scheduled monthly billing cycle creation...");
        
        LocalDate today = LocalDate.now();
        LocalDate periodFrom = today.withDayOfMonth(1);
        LocalDate periodTo = today.withDayOfMonth(today.lengthOfMonth());
        
        String cycleName = "Tháng " + today.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        
        log.info("📅 Creating billing cycle: {} (From: {} - To: {})", 
                cycleName, periodFrom, periodTo);
        
        // TODO: Implement logic to:
        // 1. Get all active tenants from base-service
        // 2. For each tenant, create billing cycle
        // 3. Get all active vehicles for each tenant
        // 4. Group by unit/resident
        // 5. Create invoices with invoice lines
        
        log.warn("⚠️ Scheduled billing cycle creation is not fully implemented yet.");
        log.info("✅ Scheduled billing cycle creation completed");
    }
    
    /**
     * Scheduled job to calculate late payment fees
     * Runs every day at 01:00 AM
     * Cron: "0 0 1 * * ?" = second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void calculateLateFees() {
        log.info("🔄 Starting scheduled late fee calculation...");
        
        LocalDate today = LocalDate.now();
        
        log.info("📅 Calculating late fees for invoices overdue as of: {}", today);
        
        // TODO: Implement logic to:
        // 1. Find all unpaid invoices (status = PUBLISHED)
        // 2. Check if due_date < today
        // 3. Calculate days overdue
        // 4. Apply late_payment_config rules
        // 5. Create late_payment_charges records
        
        log.warn("⚠️ Late fee calculation is not fully implemented yet.");
        log.info("✅ Late fee calculation completed");
    }
    
    /**
     * Scheduled job to send payment reminders
     * Runs every day at 08:00 AM
     * Cron: "0 0 8 * * ?" = second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void sendPaymentReminders() {
        log.info("🔄 Starting scheduled payment reminder sending...");
        
        LocalDate today = LocalDate.now();
        LocalDate reminderDate = today.plusDays(5); // 5 days before due
        
        log.info("📧 Sending reminders for invoices due on: {}", reminderDate);
        
        // TODO: Implement logic to:
        // 1. Find invoices with due_date = today + 5 days
        // 2. Check if reminder already sent
        // 3. Create payment_reminders records
        // 4. Send email/SMS/push notification
        
        log.warn("⚠️ Payment reminder sending is not fully implemented yet.");
        log.info("✅ Payment reminder sending completed");
    }
}




