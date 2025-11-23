package com.QhomeBase.servicescardservice.jobs;

import com.QhomeBase.servicescardservice.model.ElevatorCardRegistration;
import com.QhomeBase.servicescardservice.model.RegisterServiceRequest;
import com.QhomeBase.servicescardservice.model.ResidentCardRegistration;
import com.QhomeBase.servicescardservice.repository.ElevatorCardRegistrationRepository;
import com.QhomeBase.servicescardservice.repository.RegisterServiceRequestRepository;
import com.QhomeBase.servicescardservice.repository.ResidentCardRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled job để tự động cập nhật trạng thái thẻ dựa trên thời gian:
 * - Sau 30 ngày từ lúc admin approve: Chuyển sang "NEEDS_RENEWAL" (cần gia hạn)
 * - Sau 36 ngày từ lúc admin approve: Chuyển sang "SUSPENDED" (tạm ngưng)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CardStatusUpdateScheduler {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_NEEDS_RENEWAL = "NEEDS_RENEWAL";
    private static final String STATUS_SUSPENDED = "SUSPENDED";
    private static final String PAYMENT_STATUS_PAID = "PAID";

    private final ResidentCardRegistrationRepository residentCardRepository;
    private final ElevatorCardRegistrationRepository elevatorCardRepository;
    private final RegisterServiceRequestRepository vehicleCardRepository;

    @Value("${card.fee.cycle-days:30}")
    private int cycleDays;

    @Value("${card.fee.reminder.grace-days:5}")
    private int graceDays;

    @Value("${card.status.update.enabled:true}")
    private boolean statusUpdateEnabled;

    /**
     * Scheduled job chạy mỗi ngày lúc 08:00 để cập nhật trạng thái thẻ.
     */
    @Scheduled(cron = "${card.status.update.cron:0 0 8 * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void updateCardStatuses() {
        if (!statusUpdateEnabled) {
            log.debug("ℹ️ [CardStatusUpdate] Status update is disabled via configuration");
            return;
        }

        try {
            OffsetDateTime now = OffsetDateTime.now(ZONE);
            boolean isTestMode = cycleDays == 0;
            int updatedNeedsRenewal = 0;
            int updatedSuspended = 0;

            // Update Resident Cards
            List<ResidentCardRegistration> residentCards = residentCardRepository
                    .findByStatusAndPaymentStatus(STATUS_APPROVED, PAYMENT_STATUS_PAID);
            for (ResidentCardRegistration card : residentCards) {
                if (card.getApprovedAt() == null) continue;

                long timeSinceApproval;
                String timeUnit;
                long needsRenewalThreshold;
                long suspendedThreshold;

                if (isTestMode) {
                    // Test mode: check minutes
                    // Timeline: approve -> 2 phút -> reminder #1 -> mỗi 1 phút -> reminder #6 (8 phút) -> NEEDS_RENEWAL -> SUSPENDED
                    timeSinceApproval = ChronoUnit.MINUTES.between(card.getApprovedAt(), now);
                    timeUnit = "phút";
                    needsRenewalThreshold = 8; // Sau 8 phút (2 phút delay + 6 lần reminder)
                    suspendedThreshold = 9; // Sau 9 phút
                } else {
                    // Production mode: check days
                    LocalDate approvedDate = card.getApprovedAt().atZoneSameInstant(ZONE).toLocalDate();
                    LocalDate today = LocalDate.now(ZONE);
                    timeSinceApproval = ChronoUnit.DAYS.between(approvedDate, today);
                    timeUnit = "ngày";
                    needsRenewalThreshold = cycleDays; // Sau 30 ngày
                    suspendedThreshold = cycleDays + graceDays; // Sau 36 ngày
                }

                if (timeSinceApproval >= suspendedThreshold) {
                    // SUSPENDED
                    if (!STATUS_SUSPENDED.equals(card.getStatus())) {
                        card.setStatus(STATUS_SUSPENDED);
                        updatedSuspended++;
                        log.info("🔄 [CardStatusUpdate] Resident card {} chuyển sang SUSPENDED ({} {} từ khi approve)",
                                card.getId(), timeSinceApproval, timeUnit);
                    }
                } else if (timeSinceApproval >= needsRenewalThreshold) {
                    // NEEDS_RENEWAL
                    if (!STATUS_NEEDS_RENEWAL.equals(card.getStatus())) {
                        card.setStatus(STATUS_NEEDS_RENEWAL);
                        updatedNeedsRenewal++;
                        log.info("🔄 [CardStatusUpdate] Resident card {} chuyển sang NEEDS_RENEWAL ({} {} từ khi approve)",
                                card.getId(), timeSinceApproval, timeUnit);
                    }
                }
            }

            // Update Elevator Cards
            List<ElevatorCardRegistration> elevatorCards = elevatorCardRepository
                    .findByStatusAndPaymentStatus(STATUS_APPROVED, PAYMENT_STATUS_PAID);
            for (ElevatorCardRegistration card : elevatorCards) {
                if (card.getApprovedAt() == null) continue;

                long timeSinceApproval;
                String timeUnit;
                long needsRenewalThreshold;
                long suspendedThreshold;

                if (isTestMode) {
                    // Test mode: check minutes
                    // Timeline: approve -> 2 phút -> reminder #1 -> mỗi 1 phút -> reminder #6 (8 phút) -> NEEDS_RENEWAL -> SUSPENDED
                    timeSinceApproval = ChronoUnit.MINUTES.between(card.getApprovedAt(), now);
                    timeUnit = "phút";
                    needsRenewalThreshold = 8; // Sau 8 phút (2 phút delay + 6 lần reminder)
                    suspendedThreshold = 9; // Sau 9 phút
                } else {
                    // Production mode: check days
                    LocalDate approvedDate = card.getApprovedAt().atZoneSameInstant(ZONE).toLocalDate();
                    LocalDate today = LocalDate.now(ZONE);
                    timeSinceApproval = ChronoUnit.DAYS.between(approvedDate, today);
                    timeUnit = "ngày";
                    needsRenewalThreshold = cycleDays; // Sau 30 ngày
                    suspendedThreshold = cycleDays + graceDays; // Sau 36 ngày
                }

                if (timeSinceApproval >= suspendedThreshold) {
                    // SUSPENDED
                    if (!STATUS_SUSPENDED.equals(card.getStatus())) {
                        card.setStatus(STATUS_SUSPENDED);
                        updatedSuspended++;
                        log.info("🔄 [CardStatusUpdate] Elevator card {} chuyển sang SUSPENDED ({} {} từ khi approve)",
                                card.getId(), timeSinceApproval, timeUnit);
                    }
                } else if (timeSinceApproval >= needsRenewalThreshold) {
                    // NEEDS_RENEWAL
                    if (!STATUS_NEEDS_RENEWAL.equals(card.getStatus())) {
                        card.setStatus(STATUS_NEEDS_RENEWAL);
                        updatedNeedsRenewal++;
                        log.info("🔄 [CardStatusUpdate] Elevator card {} chuyển sang NEEDS_RENEWAL ({} {} từ khi approve)",
                                card.getId(), timeSinceApproval, timeUnit);
                    }
                }
            }

            // Update Vehicle Cards
            List<RegisterServiceRequest> vehicleCards = vehicleCardRepository
                    .findByStatusAndPaymentStatus(STATUS_APPROVED, PAYMENT_STATUS_PAID);
            for (RegisterServiceRequest card : vehicleCards) {
                if (card.getApprovedAt() == null) continue;

                long timeSinceApproval;
                String timeUnit;
                long needsRenewalThreshold;
                long suspendedThreshold;

                if (isTestMode) {
                    // Test mode: check minutes
                    // Timeline: approve -> 2 phút -> reminder #1 -> mỗi 1 phút -> reminder #6 (8 phút) -> NEEDS_RENEWAL -> SUSPENDED
                    timeSinceApproval = ChronoUnit.MINUTES.between(card.getApprovedAt(), now);
                    timeUnit = "phút";
                    needsRenewalThreshold = 8; // Sau 8 phút (2 phút delay + 6 lần reminder)
                    suspendedThreshold = 9; // Sau 9 phút
                } else {
                    // Production mode: check days
                    LocalDate approvedDate = card.getApprovedAt().atZoneSameInstant(ZONE).toLocalDate();
                    LocalDate today = LocalDate.now(ZONE);
                    timeSinceApproval = ChronoUnit.DAYS.between(approvedDate, today);
                    timeUnit = "ngày";
                    needsRenewalThreshold = cycleDays; // Sau 30 ngày
                    suspendedThreshold = cycleDays + graceDays; // Sau 36 ngày
                }

                if (timeSinceApproval >= suspendedThreshold) {
                    // SUSPENDED
                    if (!STATUS_SUSPENDED.equals(card.getStatus())) {
                        card.setStatus(STATUS_SUSPENDED);
                        updatedSuspended++;
                        log.info("🔄 [CardStatusUpdate] Vehicle card {} chuyển sang SUSPENDED ({} {} từ khi approve)",
                                card.getId(), timeSinceApproval, timeUnit);
                    }
                } else if (timeSinceApproval >= needsRenewalThreshold) {
                    // NEEDS_RENEWAL
                    if (!STATUS_NEEDS_RENEWAL.equals(card.getStatus())) {
                        card.setStatus(STATUS_NEEDS_RENEWAL);
                        updatedNeedsRenewal++;
                        log.info("🔄 [CardStatusUpdate] Vehicle card {} chuyển sang NEEDS_RENEWAL ({} {} từ khi approve)",
                                card.getId(), timeSinceApproval, timeUnit);
                    }
                }
            }

            if (updatedNeedsRenewal > 0 || updatedSuspended > 0) {
                log.info("✅ [CardStatusUpdate] Đã cập nhật {} thẻ sang NEEDS_RENEWAL, {} thẻ sang SUSPENDED",
                        updatedNeedsRenewal, updatedSuspended);
            } else {
                log.debug("ℹ️ [CardStatusUpdate] Không có thẻ nào cần cập nhật trạng thái");
            }
        } catch (Exception ex) {
            log.error("❌ [CardStatusUpdate] Lỗi khi cập nhật trạng thái thẻ", ex);
        }
    }
}

