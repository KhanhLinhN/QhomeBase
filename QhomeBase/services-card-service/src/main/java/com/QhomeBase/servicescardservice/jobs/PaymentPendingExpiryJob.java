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

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentPendingExpiryJob {

    private static final String STATUS_READY_FOR_PAYMENT = "READY_FOR_PAYMENT";

    private final ResidentCardRegistrationRepository residentRepo;
    private final ElevatorCardRegistrationRepository elevatorRepo;
    private final RegisterServiceRequestRepository vehicleRepo;

    @Value("${payments.pending.ttl-minutes:10}")
    private int pendingTtlMinutes;

    // Chạy mỗi phút để dọn dẹp các bản ghi PAYMENT_PENDING quá thời gian TTL
    @Scheduled(fixedDelayString = "${payments.pending.sweep-interval-ms:60000}")
    public void sweepPendingPayments() {
        try {
            final OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(pendingTtlMinutes);

            // Resident
            List<ResidentCardRegistration> residentPendings =
                    residentRepo.findByPaymentStatusAndUpdatedAtBefore("PAYMENT_PENDING", threshold);
            for (ResidentCardRegistration r : residentPendings) {
                r.setPaymentStatus("UNPAID");
                r.setStatus(STATUS_READY_FOR_PAYMENT);
                if (r.getAdminNote() == null || r.getAdminNote().isBlank()) {
                    r.setAdminNote("Auto-expired payment after " + pendingTtlMinutes + " minutes");
                }
                residentRepo.save(r);
            }
            if (!residentPendings.isEmpty()) {
                log.info("🧹 [ExpireJob] Reset {} resident-card registrations from PAYMENT_PENDING -> UNPAID",
                        residentPendings.size());
            }

            // Elevator
            List<ElevatorCardRegistration> elevatorPendings =
                    elevatorRepo.findByPaymentStatusAndUpdatedAtBefore("PAYMENT_PENDING", threshold);
            for (ElevatorCardRegistration e : elevatorPendings) {
                e.setPaymentStatus("UNPAID");
                e.setStatus(STATUS_READY_FOR_PAYMENT);
                elevatorRepo.save(e);
            }
            if (!elevatorPendings.isEmpty()) {
                log.info("🧹 [ExpireJob] Reset {} elevator-card registrations from PAYMENT_PENDING -> UNPAID",
                        elevatorPendings.size());
            }

            // Vehicle
            List<RegisterServiceRequest> vehiclePendings =
                    vehicleRepo.findByPaymentStatusAndUpdatedAtBefore("PAYMENT_PENDING", threshold);
            for (RegisterServiceRequest v : vehiclePendings) {
                v.setPaymentStatus("UNPAID");
                v.setStatus(STATUS_READY_FOR_PAYMENT);
                vehicleRepo.save(v);
            }
            if (!vehiclePendings.isEmpty()) {
                log.info("🧹 [ExpireJob] Reset {} vehicle registrations from PAYMENT_PENDING -> UNPAID",
                        vehiclePendings.size());
            }

        } catch (Exception e) {
            log.error("❌ [ExpireJob] Error sweeping pending payments", e);
        }
    }
}

