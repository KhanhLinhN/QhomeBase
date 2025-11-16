package com.QhomeBase.servicescardservice.service;

import com.QhomeBase.servicescardservice.config.VnpayProperties;
import com.QhomeBase.servicescardservice.dto.CardRegistrationAdminDecisionRequest;
import com.QhomeBase.servicescardservice.dto.ResidentCardRegistrationCreateDto;
import com.QhomeBase.servicescardservice.dto.ResidentCardRegistrationDto;
import com.QhomeBase.servicescardservice.model.ResidentCardRegistration;
import com.QhomeBase.servicescardservice.repository.ResidentCardRegistrationRepository;
import com.QhomeBase.servicescardservice.service.vnpay.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"NullAway", "DataFlowIssue"})
public class ResidentCardRegistrationService {

    private static final BigDecimal REGISTRATION_FEE = BigDecimal.valueOf(30000);
    private static final String STATUS_READY_FOR_PAYMENT = "READY_FOR_PAYMENT";
    private static final String STATUS_PAYMENT_PENDING = "PAYMENT_PENDING";
    private static final String STATUS_PENDING_REVIEW = "PENDING";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String PAYMENT_VNPAY = "VNPAY";

    private final ResidentCardRegistrationRepository repository;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;
    private final BillingClient billingClient;
    private final ResidentUnitLookupService residentUnitLookupService;
    private final NotificationClient notificationClient;
    private final ConcurrentMap<Long, UUID> orderIdToRegistrationId = new ConcurrentHashMap<>();

    @Transactional
    public ResidentCardRegistrationDto createRegistration(UUID userId, ResidentCardRegistrationCreateDto dto) {
        validatePayload(dto);

        ResidentCardRegistration registration = ResidentCardRegistration.builder()
                .userId(userId)
                .unitId(dto.unitId())
                .requestType(resolveRequestType(dto.requestType()))
                .residentId(dto.residentId())
                .fullName(normalize(dto.fullName()))
                .apartmentNumber(normalize(dto.apartmentNumber()))
                .buildingName(normalize(dto.buildingName()))
                .citizenId(normalize(dto.citizenId()))
                .phoneNumber(normalize(dto.phoneNumber()))
                .note(dto.note())
                .status(STATUS_READY_FOR_PAYMENT)
                .paymentStatus("UNPAID")
                .paymentAmount(REGISTRATION_FEE)
                .paymentGateway(null)
                .vnpayTransactionRef(null)
                .adminNote(null)
                .rejectionReason(null)
                .approvedAt(null)
                .approvedBy(null)
                .build();

        try {
            applyResolvedAddressForResident(
                    registration,
                    dto.residentId(),
                    dto.unitId(),
                    dto.fullName(),
                    dto.apartmentNumber(),
                    dto.buildingName()
            );
        } catch (Exception e) {
            log.warn("⚠️ [ResidentCard] Không thể resolve địa chỉ từ database, sử dụng giá trị từ form: {}", e.getMessage());
            // Fallback to form values if lookup fails
            if (!StringUtils.hasText(registration.getFullName())) {
                registration.setFullName(normalize(dto.fullName()));
            }
            if (!StringUtils.hasText(registration.getApartmentNumber())) {
                registration.setApartmentNumber(normalize(dto.apartmentNumber()));
            }
            if (!StringUtils.hasText(registration.getBuildingName())) {
                registration.setBuildingName(normalize(dto.buildingName()));
            }
        }

        ResidentCardRegistration saved = repository.save(registration);
        return toDto(saved);
    }

    @Transactional
    public ResidentCardPaymentResponse createAndInitiatePayment(UUID userId,
                                                               ResidentCardRegistrationCreateDto dto,
                                                               HttpServletRequest request) {
        ResidentCardRegistrationDto created = createRegistration(userId, dto);
        return initiatePayment(userId, created.id(), request);
    }

    @Transactional(readOnly = true)
    public List<ResidentCardRegistrationDto> getRegistrationsForAdmin(String status, String paymentStatus) {
        List<ResidentCardRegistration> registrations = repository.findAllByOrderByCreatedAtDesc();
        return registrations.stream()
                .filter(reg -> !StringUtils.hasText(status) || status.equalsIgnoreCase(reg.getStatus()))
                .filter(reg -> !StringUtils.hasText(paymentStatus) || paymentStatus.equalsIgnoreCase(reg.getPaymentStatus()))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResidentCardRegistrationDto getRegistrationForAdmin(UUID registrationId) {
        ResidentCardRegistration registration = repository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký thẻ cư dân"));
        return toDto(registration);
    }

    @Transactional
    public ResidentCardRegistrationDto processAdminDecision(UUID adminId,
                                                            UUID registrationId,
                                                            CardRegistrationAdminDecisionRequest request) {
        ResidentCardRegistration registration = repository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký thẻ cư dân"));

        String decision = request.decision();
        if (decision == null || decision.isBlank()) {
            throw new IllegalArgumentException("Decision is required");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));

        if ("APPROVE".equalsIgnoreCase(decision) || "APPROVED".equalsIgnoreCase(decision)) {
            // Approve logic
            if (!STATUS_PENDING_REVIEW.equalsIgnoreCase(registration.getStatus()) 
                    && !STATUS_READY_FOR_PAYMENT.equalsIgnoreCase(registration.getStatus())) {
                throw new IllegalStateException("Đăng ký không ở trạng thái chờ duyệt. Trạng thái hiện tại: " + registration.getStatus());
            }

            registration.setStatus("APPROVED");
            registration.setApprovedBy(adminId);
            registration.setApprovedAt(now);
            registration.setAdminNote(request.note());
            registration.setUpdatedAt(now);

            ResidentCardRegistration saved = repository.save(registration);

            // Send notification to resident
            sendCardApprovalNotification(saved, request.issueMessage());

            log.info("✅ [ResidentCard] Admin {} đã approve đăng ký {}", adminId, registrationId);
            return toDto(saved);
        } else if ("REJECT".equalsIgnoreCase(decision) || "REJECTED".equalsIgnoreCase(decision)) {
            // Reject logic
            if (STATUS_REJECTED.equalsIgnoreCase(registration.getStatus())) {
                throw new IllegalStateException("Đăng ký đã bị từ chối");
            }

            registration.setStatus(STATUS_REJECTED);
            registration.setAdminNote(request.note());
            registration.setUpdatedAt(now);

            ResidentCardRegistration saved = repository.save(registration);

            log.info("✅ [ResidentCard] Admin {} đã reject đăng ký {}", adminId, registrationId);
            return toDto(saved);
        } else {
            throw new IllegalArgumentException("Invalid decision: " + decision + ". Must be APPROVE or REJECT");
        }
    }

    private void sendCardApprovalNotification(ResidentCardRegistration registration, String issueMessage) {
        try {
            String title = "Thẻ cư dân đã được duyệt";
            String message = issueMessage != null && !issueMessage.isBlank() 
                    ? issueMessage 
                    : String.format("Thẻ cư dân của bạn đã được duyệt. Vui lòng đến nhận thẻ theo thông tin đã cung cấp.", registration.getApartmentNumber());

            Map<String, String> data = new HashMap<>();
            data.put("cardType", "RESIDENT_CARD");
            data.put("registrationId", registration.getId().toString());
            if (registration.getApartmentNumber() != null) {
                data.put("apartmentNumber", registration.getApartmentNumber());
            }

            notificationClient.sendResidentNotification(
                    registration.getResidentId(),
                    null, // buildingId - có thể null vì gửi theo residentId
                    "CARD_APPROVED",
                    title,
                    message,
                    registration.getId(),
                    "RESIDENT_CARD_REGISTRATION",
                    data
            );

            log.info("✅ [ResidentCard] Đã gửi notification approval cho residentId: {}", registration.getResidentId());
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Không thể gửi notification approval cho residentId: {}", registration.getResidentId(), e);
        }
    }

    @Transactional
    public ResidentCardPaymentResponse initiatePayment(UUID userId,
                                                       UUID registrationId,
                                                       HttpServletRequest request) {
        ResidentCardRegistration registration = repository.findByIdAndUserId(registrationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký thẻ cư dân"));

        if (STATUS_REJECTED.equalsIgnoreCase(registration.getStatus())) {
            throw new IllegalStateException("Đăng ký đã bị từ chối");
        }
        if (!Objects.equals(registration.getPaymentStatus(), "UNPAID")) {
            throw new IllegalStateException("Đăng ký đã thanh toán hoặc đang xử lý");
        }

        registration.setStatus(STATUS_PAYMENT_PENDING);
        registration.setPaymentStatus("PAYMENT_PENDING");
        registration.setPaymentGateway(PAYMENT_VNPAY);
        ResidentCardRegistration saved = repository.save(registration);

        long orderId = Math.abs(saved.getId().hashCode());
        if (orderId == 0) {
            orderId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        }
        orderIdToRegistrationId.put(orderId, saved.getId());

        String clientIp = resolveClientIp(request);
        String orderInfo = "Thanh toán thẻ cư dân " +
                (saved.getApartmentNumber() != null ? saved.getApartmentNumber() : saved.getId());
        String returnUrl = StringUtils.hasText(vnpayProperties.getResidentReturnUrl())
                ? vnpayProperties.getResidentReturnUrl()
                : vnpayProperties.getReturnUrl();
        var paymentResult = vnpayService.createPaymentUrlWithRef(orderId, orderInfo, REGISTRATION_FEE, clientIp, returnUrl);
        
        // Save transaction reference to database for fallback lookup
        saved.setVnpayTransactionRef(paymentResult.transactionRef());
        repository.save(saved);

        return new ResidentCardPaymentResponse(saved.getId(), paymentResult.paymentUrl());
    }

    @Transactional(readOnly = true)
    public ResidentCardRegistrationDto getRegistration(UUID userId, UUID registrationId) {
        ResidentCardRegistration registration = repository.findByIdAndUserId(registrationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký thẻ cư dân"));
        return toDto(registration);
    }

    @Transactional
    public void cancelRegistration(UUID userId, UUID registrationId) {
        ResidentCardRegistration registration = repository.findByIdAndUserId(registrationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký thẻ cư dân"));
        if ("PAID".equalsIgnoreCase(registration.getPaymentStatus())) {
            throw new IllegalStateException("Không thể hủy đăng ký đã thanh toán");
        }
        repository.delete(registration);
    }

    @Transactional
    public ResidentCardPaymentResult handleVnpayCallback(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("Missing callback data from VNPAY");
        }

        String txnRef = params.get("vnp_TxnRef");
        if (txnRef == null || !txnRef.contains("_")) {
            throw new IllegalArgumentException("Invalid transaction reference");
        }

        Long orderId;
        try {
            orderId = Long.parseLong(txnRef.split("_")[0]);
        } catch (NumberFormatException e) {
            log.error("❌ [ResidentCard] Cannot parse orderId from txnRef: {}", txnRef);
            throw new IllegalArgumentException("Invalid transaction reference format");
        }

        UUID registrationId = orderIdToRegistrationId.get(orderId);
        ResidentCardRegistration registration = null;

        // Try to find registration by orderId map first
        if (registrationId != null) {
            registration = repository.findById(registrationId).orElse(null);
            if (registration != null) {
                log.info("✅ [ResidentCard] Found registration by orderId map: registrationId={}, orderId={}", 
                        registrationId, orderId);
            }
        }

        // Fallback: try to find by transaction reference
        if (registration == null) {
            var optionalByTxnRef = repository.findByVnpayTransactionRef(txnRef);
            if (optionalByTxnRef.isPresent()) {
                registration = optionalByTxnRef.get();
                log.info("✅ [ResidentCard] Found registration by txnRef: registrationId={}, txnRef={}", 
                        registration.getId(), txnRef);
            }
        }

        // If still not found, throw exception with orderId for debugging
        if (registration == null) {
            log.error("❌ [ResidentCard] Cannot find registration: orderId={}, txnRef={}, mapSize={}", 
                    orderId, txnRef, orderIdToRegistrationId.size());
            throw new IllegalArgumentException(
                    String.format("Registration not found for orderId: %d, txnRef: %s", orderId, txnRef)
            );
        }

        boolean signatureValid = vnpayService.validateReturn(params);
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");

        registration.setVnpayTransactionRef(txnRef);

        if (signatureValid && "00".equals(responseCode) && "00".equals(transactionStatus)) {
            registration.setPaymentStatus("PAID");
            try {
                applyResolvedAddressForResident(
                        registration,
                        registration.getResidentId(),
                        registration.getUnitId(),
                        registration.getFullName(),
                        registration.getApartmentNumber(),
                        registration.getBuildingName()
                );
            } catch (Exception e) {
                log.warn("⚠️ [ResidentCard] Không thể resolve địa chỉ sau thanh toán, giữ nguyên giá trị hiện tại: {}", e.getMessage());
            }
            registration.setStatus(STATUS_PENDING_REVIEW);
            registration.setPaymentGateway(PAYMENT_VNPAY);
            OffsetDateTime payDate = parsePayDate(params.get("vnp_PayDate"));
            registration.setPaymentDate(payDate);
            repository.save(registration);

            log.info("✅ [ResidentCard] Thanh toán thành công cho đăng ký {}", registration.getId());
            billingClient.recordResidentCardPayment(
                    registration.getId(),
                    registration.getUserId(),
                    registration.getUnitId(),
                    registration.getFullName(),
                    registration.getApartmentNumber(),
                    registration.getBuildingName(),
                    registration.getRequestType(),
                    registration.getNote(),
                    registration.getPaymentAmount(),
                    payDate,
                    txnRef,
                    params.get("vnp_TransactionNo"),
                    params.get("vnp_BankCode"),
                    params.get("vnp_CardType"),
                    responseCode
            );

            orderIdToRegistrationId.remove(orderId);
            return new ResidentCardPaymentResult(registration.getId(), true, responseCode, true);
        }

        registration.setStatus(STATUS_READY_FOR_PAYMENT);
        registration.setPaymentStatus("UNPAID");
        repository.save(registration);
        orderIdToRegistrationId.remove(orderId);
        return new ResidentCardPaymentResult(registration.getId(), false, responseCode, signatureValid);
    }

    private void applyResolvedAddressForResident(ResidentCardRegistration registration,
                                                 UUID residentId,
                                                 UUID unitId,
                                                 String fallbackFullName,
                                                 String fallbackApartment,
                                                 String fallbackBuilding) {
        residentUnitLookupService.resolveByResident(residentId, unitId).ifPresentOrElse(info -> {
            if (StringUtils.hasText(info.residentFullName())) {
                registration.setFullName(normalize(info.residentFullName()));
            } else {
                registration.setFullName(normalize(fallbackFullName));
            }

            String apartment = info.apartmentNumber() != null ? info.apartmentNumber() : fallbackApartment;
            String building = info.buildingName() != null ? info.buildingName() : fallbackBuilding;
            registration.setApartmentNumber(normalize(apartment));
            registration.setBuildingName(normalize(building));
        }, () -> {
            registration.setFullName(normalize(fallbackFullName));
            registration.setApartmentNumber(normalize(fallbackApartment));
            registration.setBuildingName(normalize(fallbackBuilding));
        });
    }

    private OffsetDateTime parsePayDate(String payDate) {
        if (!StringUtils.hasText(payDate)) {
            return OffsetDateTime.now();
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            LocalDateTime localDateTime = LocalDateTime.parse(payDate, formatter);
            return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
        } catch (Exception e) {
            return OffsetDateTime.now();
        }
    }

    private void validatePayload(ResidentCardRegistrationCreateDto dto) {
        if (dto.unitId() == null) {
            throw new IllegalArgumentException("Căn hộ là bắt buộc");
        }
        if (dto.residentId() == null) {
            throw new IllegalArgumentException("Cư dân là bắt buộc");
        }
    }

    private String resolveRequestType(String requestType) {
        if (!StringUtils.hasText(requestType)) {
            return "NEW_CARD";
        }
        String normalized = requestType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "REPLACE_CARD", "NEW_CARD" -> normalized;
            default -> "NEW_CARD";
        };
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String header = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(header)) {
            return header.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private ResidentCardRegistrationDto toDto(ResidentCardRegistration entity) {
        return new ResidentCardRegistrationDto(
                entity.getId(),
                entity.getUserId(),
                entity.getUnitId(),
                entity.getRequestType(),
                entity.getResidentId(),
                entity.getFullName(),
                entity.getApartmentNumber(),
                entity.getBuildingName(),
                entity.getCitizenId(),
                entity.getPhoneNumber(),
                entity.getNote(),
                entity.getStatus(),
                entity.getPaymentStatus(),
                entity.getPaymentAmount(),
                entity.getPaymentDate(),
                entity.getPaymentGateway(),
                entity.getVnpayTransactionRef(),
                entity.getAdminNote(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getRejectionReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public record ResidentCardPaymentResponse(UUID registrationId, String paymentUrl) {}

    public record ResidentCardPaymentResult(UUID registrationId, boolean success, String responseCode, boolean signatureValid) {}
}


