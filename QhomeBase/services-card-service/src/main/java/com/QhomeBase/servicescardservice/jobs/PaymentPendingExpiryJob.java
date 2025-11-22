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
    private static final String STATUS_CANCELLED = "CANCELLED";

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
            final String expiryNote = "Auto-cancelled pending payment after " + pendingTtlMinutes + " minutes";

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

            // Auto-cancel READY_FOR_PAYMENT that stayed unpaid beyond TTL
            cancelExpiredReadyForPayment(residentRepo.findByStatusAndUpdatedAtBefore(STATUS_READY_FOR_PAYMENT, threshold),
                    expiryNote, "resident-card");
            cancelExpiredReadyForPayment(elevatorRepo.findByStatusAndUpdatedAtBefore(STATUS_READY_FOR_PAYMENT, threshold),
                    expiryNote, "elevator-card");
            cancelExpiredReadyForPayment(vehicleRepo.findByStatusAndUpdatedAtBefore(STATUS_READY_FOR_PAYMENT, threshold),
                    expiryNote, "vehicle");

        } catch (Exception e) {
            log.error("❌ [ExpireJob] Error sweeping pending payments", e);
        }
    }

    private void cancelExpiredReadyForPayment(List<?> registrations, String note, String tag) {
        if (registrations.isEmpty()) {
            return;
        }
        int cancelled = 0;
        for (Object obj : registrations) {
            if (obj instanceof ResidentCardRegistration resident) {
                resident.setStatus(STATUS_CANCELLED);
                resident.setPaymentStatus("UNPAID");
                if (resident.getAdminNote() == null || resident.getAdminNote().isBlank()) {
                    resident.setAdminNote(note);
                }
                residentRepo.save(resident);
                cancelled++;
            } else if (obj instanceof ElevatorCardRegistration elevator) {
                elevator.setStatus(STATUS_CANCELLED);
                elevator.setPaymentStatus("UNPAID");
                if (elevator.getAdminNote() == null || elevator.getAdminNote().isBlank()) {
                    elevator.setAdminNote(note);
                }
                elevatorRepo.save(elevator);
                cancelled++;
            } else if (obj instanceof RegisterServiceRequest vehicle) {
                vehicle.setStatus(STATUS_CANCELLED);
                vehicle.setPaymentStatus("UNPAID");
                if (vehicle.getAdminNote() == null || vehicle.getAdminNote().isBlank()) {
                    vehicle.setAdminNote(note);
                }
                vehicleRepo.save(vehicle);
                cancelled++;
            }
        }
        log.info("🧹 [ExpireJob] Auto-cancelled {} {} registrations stuck at READY_FOR_PAYMENT", cancelled, tag);
    }
}

