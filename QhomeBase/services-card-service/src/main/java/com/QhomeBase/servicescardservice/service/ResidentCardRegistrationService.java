package com.QhomeBase.servicescardservice.service;

import com.QhomeBase.servicescardservice.config.VnpayProperties;
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
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String PAYMENT_VNPAY = "VNPAY";

    private final ResidentCardRegistrationRepository repository;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;
    private final BillingClient billingClient;
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
                .build();

        ResidentCardRegistration saved = Objects.requireNonNull(repository.save(registration),
                "Không thể tạo đăng ký thẻ cư dân mới");
        return toDto(saved);
    }

    @Transactional
    public ResidentCardPaymentResponse createAndInitiatePayment(UUID userId,
                                                               ResidentCardRegistrationCreateDto dto,
                                                               HttpServletRequest request) {
        ResidentCardRegistrationDto created = createRegistration(userId, dto);
        return initiatePayment(userId, created.id(), request);
    }

    @Transactional
    public ResidentCardPaymentResponse initiatePayment(UUID userId,
                                                       UUID registrationId,
                                                       HttpServletRequest request) {
        ResidentCardRegistration registration = repository.findByIdAndUserId(registrationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký thẻ cư dân"));

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
        String paymentUrl = vnpayService.createPaymentUrl(orderId, orderInfo, REGISTRATION_FEE, clientIp, returnUrl);

        return new ResidentCardPaymentResponse(saved.getId(), paymentUrl);
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
            throw new IllegalArgumentException("Thiếu dữ liệu callback từ VNPAY");
        }

        String txnRef = params.get("vnp_TxnRef");
        if (txnRef == null || !txnRef.contains("_")) {
            throw new IllegalArgumentException("Mã giao dịch không hợp lệ");
        }

        Long orderId = Long.parseLong(txnRef.split("_")[0]);
        UUID registrationId = orderIdToRegistrationId.get(orderId);

        ResidentCardRegistration registration = null;
        if (registrationId != null) {
            registration = repository.findById(registrationId).orElse(null);
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
            registration.setStatus(STATUS_COMPLETED);
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
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public record ResidentCardPaymentResponse(UUID registrationId, String paymentUrl) {}

    public record ResidentCardPaymentResult(UUID registrationId, boolean success, String responseCode, boolean signatureValid) {}
}


