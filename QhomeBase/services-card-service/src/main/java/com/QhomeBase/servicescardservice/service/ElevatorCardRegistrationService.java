package com.QhomeBase.servicescardservice.service;

import com.QhomeBase.servicescardservice.dto.CardRegistrationAdminDecisionRequest;
import com.QhomeBase.servicescardservice.dto.ElevatorCardRegistrationCreateDto;
import com.QhomeBase.servicescardservice.dto.ElevatorCardRegistrationDto;
import com.QhomeBase.servicescardservice.model.ElevatorCardRegistration;
import com.QhomeBase.servicescardservice.repository.ElevatorCardRegistrationRepository;
import com.QhomeBase.servicescardservice.service.vnpay.VnpayService;
import com.QhomeBase.servicescardservice.config.VnpayProperties;
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
public class ElevatorCardRegistrationService {

    private static final BigDecimal REGISTRATION_FEE = BigDecimal.valueOf(30000);
    private static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL"; // waiting for admin after successful payment
    private static final String STATUS_READY_FOR_PAYMENT = "READY_FOR_PAYMENT";
    private static final String STATUS_PAYMENT_PENDING = "PAYMENT_PENDING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String PAYMENT_VNPAY = "VNPAY";

    private final ElevatorCardRegistrationRepository repository;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;
    private final BillingClient billingClient;
    private final NotificationClient notificationClient;
    private final ConcurrentMap<Long, UUID> orderIdToRegistrationId = new ConcurrentHashMap<>();

    @Transactional
    @SuppressWarnings({"NullAway", "DataFlowIssue"})
    public ElevatorCardRegistrationDto createRegistration(UUID userId, ElevatorCardRegistrationCreateDto dto) {
        validatePayload(dto);

        ElevatorCardRegistration registration = ElevatorCardRegistration.builder()
                .userId(userId)
                .unitId(dto.unitId())
                .residentId(dto.residentId())
                .requestType(resolveRequestType(dto.requestType()))
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

        @SuppressWarnings("NullAway")
        ElevatorCardRegistration saved = repository.save(registration);
        if (saved == null) {
            throw new IllegalStateException("Không thể tạo đăng ký thang máy mới");
        }
        return toDto(saved);
    }

    @Transactional
    public ElevatorCardPaymentResponse createAndInitiatePayment(UUID userId,
                                                                ElevatorCardRegistrationCreateDto dto,
                                                                HttpServletRequest request) {
        ElevatorCardRegistrationDto created = createRegistration(userId, dto);
        return initiatePayment(userId, created.id(), request);
    }

    @Transactional(readOnly = true)
    public List<ElevatorCardRegistrationDto> getRegistrationsForAdmin(String status, String paymentStatus) {
        List<ElevatorCardRegistration> registrations = repository.findAllByOrderByCreatedAtDesc();
        return registrations.stream()
                .filter(reg -> !StringUtils.hasText(status) || status.equalsIgnoreCase(reg.getStatus()))
                .filter(reg -> !StringUtils.hasText(paymentStatus) || paymentStatus.equalsIgnoreCase(reg.getPaymentStatus()))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ElevatorCardRegistrationDto getRegistrationForAdmin(UUID registrationId) {
        ElevatorCardRegistration registration = repository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký thang máy"));
        return toDto(registration);
    }

    @Transactional
    public ElevatorCardRegistrationDto processAdminDecision(UUID adminId,
                                                            UUID registrationId,
                                                            CardRegistrationAdminDecisionRequest request) {
        ElevatorCardRegistration registration = repository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký thang máy"));

        if (STATUS_COMPLETED.equalsIgnoreCase(registration.getStatus())) {
            throw new IllegalStateException("Đăng ký đã hoàn tất");
        }
        if (STATUS_PAYMENT_PENDING.equalsIgnoreCase(registration.getStatus())) {
            throw new IllegalStateException("Đăng ký đang trong quá trình thanh toán");
        }
        if (STATUS_REJECTED.equalsIgnoreCase(registration.getStatus())) {
            throw new IllegalStateException("Đăng ký đã bị từ chối");
        }

        String decision = request.decision() != null ? request.decision().trim().toUpperCase(Locale.ROOT) : "";
        OffsetDateTime decisionTime = OffsetDateTime.now();
        boolean approved = "APPROVE".equals(decision);
        if (approved) {
            if (!STATUS_PENDING_APPROVAL.equalsIgnoreCase(registration.getStatus())) {
                throw new IllegalStateException("Đăng ký không ở trạng thái chờ duyệt");
            }
            if (!"PAID".equalsIgnoreCase(registration.getPaymentStatus())) {
                throw new IllegalStateException("Đăng ký chưa hoàn tất thanh toán");
            }
            registration.setStatus(STATUS_COMPLETED);
            registration.setAdminNote(normalize(request.note()));
            registration.setRejectionReason(null);
            registration.setApprovedBy(adminId);
            registration.setApprovedAt(decisionTime);
        } else if ("REJECT".equals(decision)) {
            if (!STATUS_PENDING_APPROVAL.equalsIgnoreCase(registration.getStatus())) {
                throw new IllegalStateException("Đăng ký không ở trạng thái chờ duyệt");
            }
            if (!StringUtils.hasText(request.note())) {
                throw new IllegalArgumentException("Lý do từ chối là bắt buộc");
            }
            String reason = normalize(request.note());
            registration.setStatus(STATUS_REJECTED);
            // Giữ nguyên thông tin thanh toán để phục vụ đối soát/refund (nếu cần)
            registration.setAdminNote(reason);
            registration.setRejectionReason(reason);
            registration.setApprovedBy(adminId);
            registration.setApprovedAt(decisionTime);
        } else {
            throw new IllegalArgumentException("Quyết định không hợp lệ: " + request.decision());
        }

        ElevatorCardRegistration saved = repository.save(registration);
        notifyResidentDecision(saved, approved, request.note());
        return toDto(saved);
    }

    @Transactional
    @SuppressWarnings({"NullAway", "DataFlowIssue"})
    public ElevatorCardPaymentResponse initiatePayment(UUID userId,
                                                       UUID registrationId,
                                                       HttpServletRequest request) {
        ElevatorCardRegistration registration = repository.findByIdAndUserId(registrationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký thang máy"));

        if (STATUS_REJECTED.equalsIgnoreCase(registration.getStatus())) {
            throw new IllegalStateException("Đăng ký đã bị từ chối");
        }
        if (STATUS_PENDING_APPROVAL.equalsIgnoreCase(registration.getStatus())
                && "UNPAID".equalsIgnoreCase(registration.getPaymentStatus())) {
            registration.setStatus(STATUS_READY_FOR_PAYMENT);
        }
        if (!Objects.equals(registration.getPaymentStatus(), "UNPAID")) {
            throw new IllegalStateException("Đăng ký đã thanh toán hoặc đang xử lý");
        }

        registration.setStatus(STATUS_PAYMENT_PENDING);
        registration.setPaymentStatus("PAYMENT_PENDING");
        registration.setPaymentGateway(PAYMENT_VNPAY);
        ElevatorCardRegistration saved = repository.save(registration);

        long orderId = Math.abs(saved.getId().hashCode());
        if (orderId == 0) {
            orderId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        }
        orderIdToRegistrationId.put(orderId, saved.getId());

        String clientIp = resolveClientIp(request);
        String orderInfo = "Thanh toán thẻ thang máy " +
                (saved.getApartmentNumber() != null ? saved.getApartmentNumber() : saved.getId());
        String returnUrl = StringUtils.hasText(vnpayProperties.getElevatorReturnUrl())
                ? vnpayProperties.getElevatorReturnUrl()
                : vnpayProperties.getReturnUrl();
        String paymentUrl = vnpayService.createPaymentUrl(orderId, orderInfo, REGISTRATION_FEE, clientIp, returnUrl);

        return new ElevatorCardPaymentResponse(saved.getId(), paymentUrl);
    }

    @Transactional(readOnly = true)
    public ElevatorCardRegistrationDto getRegistration(UUID userId, UUID registrationId) {
        ElevatorCardRegistration registration = repository.findByIdAndUserId(registrationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký thang máy"));
        return toDto(registration);
    }

    @Transactional
    public void cancelRegistration(UUID userId, UUID registrationId) {
        ElevatorCardRegistration registration = repository.findByIdAndUserId(registrationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký thang máy"));
        if ("PAID".equalsIgnoreCase(registration.getPaymentStatus())) {
            throw new IllegalStateException("Không thể hủy đăng ký đã thanh toán");
        }
        repository.delete(registration);
    }

    @Transactional
    public ElevatorCardPaymentResult handleVnpayCallback(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("Thiếu dữ liệu callback từ VNPAY");
        }

        String txnRef = params.get("vnp_TxnRef");
        if (txnRef == null || !txnRef.contains("_")) {
            throw new IllegalArgumentException("Mã giao dịch không hợp lệ");
        }

        Long orderId = Long.parseLong(txnRef.split("_")[0]);
        UUID registrationId = orderIdToRegistrationId.get(orderId);

        ElevatorCardRegistration registration = null;
        if (registrationId != null) {
            var optional = repository.findById(registrationId);
            if (optional.isPresent()) {
                registration = optional.get();
            }
        }
        if (registration == null) {
            registration = repository.findByVnpayTransactionRef(txnRef)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký tương ứng với giao dịch"));
        }

        boolean signatureValid = vnpayService.validateReturn(params);
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");

        registration.setVnpayTransactionRef(txnRef);

        if (signatureValid && "00".equals(responseCode) && "00".equals(transactionStatus)) {
            registration.setPaymentStatus("PAID");
            registration.setStatus(STATUS_PENDING_APPROVAL);
            registration.setPaymentGateway(PAYMENT_VNPAY);
            OffsetDateTime payDate = parsePayDate(params.get("vnp_PayDate"));
            registration.setPaymentDate(payDate);
            repository.save(registration);

            log.info("✅ [ElevatorCard] Thanh toán thành công cho đăng ký {}", registration.getId());
            billingClient.recordElevatorCardPayment(
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
            return new ElevatorCardPaymentResult(registration.getId(), true, responseCode, true);
        }

        registration.setStatus(STATUS_READY_FOR_PAYMENT);
        registration.setPaymentStatus("UNPAID");
        repository.save(registration);
        orderIdToRegistrationId.remove(orderId);
        return new ElevatorCardPaymentResult(registration.getId(), false, responseCode, signatureValid);
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

    private void notifyResidentDecision(ElevatorCardRegistration registration, boolean approved, String note) {
        if (registration.getUserId() == null) {
            return;
        }
        String title = approved ? "Thẻ thang máy đã được duyệt" : "Thẻ thang máy bị từ chối";
        String body = approved
                ? "Yêu cầu đăng ký thẻ thang máy của bạn đã được phê duyệt."
                : ("Yêu cầu đăng ký thẻ thang máy bị từ chối"
                + (StringUtils.hasText(note) ? (": " + note.trim()) : "."));

        Map<String, Object> data = new HashMap<>();
        data.put("type", approved ? "ELEVATOR_CARD_APPROVED" : "ELEVATOR_CARD_REJECTED");
        data.put("registrationId", registration.getId().toString());
        data.put("status", registration.getStatus());
        if (!approved && StringUtils.hasText(note)) {
            data.put("reason", note.trim());
        }

        try {
            notificationClient.sendResidentNotification(registration.getUserId(), title, body, data);
        } catch (Exception ex) {
            log.warn("⚠️ [ElevatorCard] Không thể gửi thông báo cho cư dân {}: {}", registration.getUserId(), ex.getMessage());
        }
    }

    private void validatePayload(ElevatorCardRegistrationCreateDto dto) {
        if (!StringUtils.hasText(dto.fullName())) {
            throw new IllegalArgumentException("Họ và tên là bắt buộc");
        }
        if (!StringUtils.hasText(dto.apartmentNumber())) {
            throw new IllegalArgumentException("Số căn hộ là bắt buộc");
        }
        if (!StringUtils.hasText(dto.buildingName())) {
            throw new IllegalArgumentException("Tòa nhà là bắt buộc");
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

    private ElevatorCardRegistrationDto toDto(ElevatorCardRegistration entity) {
        return new ElevatorCardRegistrationDto(
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

    public record ElevatorCardPaymentResponse(UUID registrationId, String paymentUrl) {}

    public record ElevatorCardPaymentResult(UUID registrationId, boolean success, String responseCode, boolean signatureValid) {}
}


