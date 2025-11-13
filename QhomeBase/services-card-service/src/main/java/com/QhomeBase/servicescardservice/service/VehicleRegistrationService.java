package com.QhomeBase.servicescardservice.service;

import com.QhomeBase.servicescardservice.dto.RegisterServiceImageDto;
import com.QhomeBase.servicescardservice.dto.RegisterServiceRequestCreateDto;
import com.QhomeBase.servicescardservice.dto.RegisterServiceRequestDto;
import com.QhomeBase.servicescardservice.model.RegisterServiceImage;
import com.QhomeBase.servicescardservice.model.RegisterServiceRequest;
import com.QhomeBase.servicescardservice.repository.RegisterServiceImageRepository;
import com.QhomeBase.servicescardservice.repository.RegisterServiceRequestRepository;
import com.QhomeBase.servicescardservice.config.VnpayProperties;
import com.QhomeBase.servicescardservice.service.vnpay.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleRegistrationService {

    private static final BigDecimal REGISTRATION_FEE = BigDecimal.valueOf(30000);
    private static final int MAX_IMAGES = 6;
    private static final String SERVICE_TYPE = "VEHICLE_REGISTRATION";
    private static final String STATUS_READY_FOR_PAYMENT = "READY_FOR_PAYMENT";
    private static final String STATUS_PAYMENT_PENDING = "PAYMENT_PENDING";
    private static final String STATUS_PENDING_REVIEW = "PENDING";
    private static final String PAYMENT_VNPAY = "VNPAY";

    private final RegisterServiceRequestRepository requestRepository;
    private final RegisterServiceImageRepository imageRepository;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;
    private final BillingClient billingClient;
    private final ResidentUnitLookupService residentUnitLookupService;
    private final ConcurrentMap<Long, UUID> orderIdToRegistrationId = new ConcurrentHashMap<>();

    private Path ensureUploadDir() throws IOException {
        Path uploadDir = Paths.get("uploads", "vehicle");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        return uploadDir;
    }

    public List<String> storeImages(List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        if (files.size() > MAX_IMAGES) {
            throw new IllegalArgumentException("Chỉ được tải tối đa " + MAX_IMAGES + " ảnh");
        }
        Path uploadDir = ensureUploadDir();
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String extension = "";
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0) {
                extension = originalFilename.substring(dot);
            }
            String filename = UUID.randomUUID() + extension;
            Path target = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), target);
            urls.add("/uploads/vehicle/" + filename);
        }
        return urls;
    }

    @Transactional
    @SuppressWarnings({"NullAway", "DataFlowIssue"})
    public RegisterServiceRequestDto createRegistration(UUID userId, RegisterServiceRequestCreateDto dto) {
        validatePayload(dto);

        RegisterServiceRequest request = RegisterServiceRequest.builder()
                .userId(userId)
                .serviceType(Optional.ofNullable(dto.serviceType()).orElse(SERVICE_TYPE))
                .requestType(resolveRequestType(dto.requestType()))
                .note(dto.note())
                .unitId(dto.unitId())
                .vehicleType(resolveVehicleType(dto.vehicleType()))
                .licensePlate(normalize(dto.licensePlate()))
                .vehicleBrand(normalize(dto.vehicleBrand()))
                .vehicleColor(normalize(dto.vehicleColor()))
                .apartmentNumber(normalize(dto.apartmentNumber()))
                .buildingName(normalize(dto.buildingName()))
                .status(STATUS_READY_FOR_PAYMENT)
                .paymentStatus("UNPAID")
                .paymentAmount(REGISTRATION_FEE)
                .build();

        applyResolvedAddressForUser(
                request,
                userId,
                dto.unitId(),
                dto.apartmentNumber() != null ? dto.apartmentNumber() : request.getApartmentNumber(),
                dto.buildingName() != null ? dto.buildingName() : request.getBuildingName()
        );

        if (dto.imageUrls() != null) {
            dto.imageUrls().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .limit(MAX_IMAGES)
                    .map(url -> RegisterServiceImage.builder().imageUrl(url).registerServiceRequest(request).build())
                    .forEach(request::addImage);
        }

        RegisterServiceRequest saved = requestRepository.save(request);
        return toDto(saved);
    }

    @Transactional
    public RegisterServiceRequestDto updateRegistration(UUID userId, UUID registrationId, RegisterServiceRequestCreateDto dto) {
        RegisterServiceRequest request = requestRepository.findByIdAndUserId(registrationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký xe"));

        if (!Objects.equals(request.getPaymentStatus(), "UNPAID")) {
            throw new IllegalStateException("Đăng ký đã thanh toán, không thể chỉnh sửa");
        }

        validatePayload(dto);

        request.setServiceType(Optional.ofNullable(dto.serviceType()).orElse(SERVICE_TYPE));
        request.setRequestType(resolveRequestType(dto.requestType()));
        request.setNote(dto.note());
        request.setUnitId(dto.unitId());
        request.setVehicleType(resolveVehicleType(dto.vehicleType()));
        request.setLicensePlate(normalize(dto.licensePlate()));
        request.setVehicleBrand(normalize(dto.vehicleBrand()));
        request.setVehicleColor(normalize(dto.vehicleColor()));
        request.setStatus(STATUS_READY_FOR_PAYMENT);
        request.setAdminNote(null);
        request.setApprovedAt(null);
        request.setApprovedBy(null);
        request.setRejectionReason(null);

        applyResolvedAddressForUser(
                request,
                userId,
                dto.unitId(),
                dto.apartmentNumber(),
                dto.buildingName()
        );

        imageRepository.deleteByRegisterServiceRequestId(request.getId());
        request.getImages().clear();
        if (dto.imageUrls() != null) {
            dto.imageUrls().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .limit(MAX_IMAGES)
                    .map(url -> RegisterServiceImage.builder().imageUrl(url).registerServiceRequest(request).build())
                    .forEach(request::addImage);
        }

        RegisterServiceRequest saved = Objects.requireNonNull(requestRepository.save(request));
        return toDto(saved);
    }

    @Transactional
    public VehicleRegistrationPaymentResponse initiatePayment(UUID userId, UUID registrationId, HttpServletRequest request) {
        RegisterServiceRequest registration = requestRepository.findByIdAndUserId(registrationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký xe"));

        if (!Objects.equals(registration.getPaymentStatus(), "UNPAID")) {
            throw new IllegalStateException("Đăng ký đã thanh toán hoặc đang xử lý");
        }
        registration.setStatus(STATUS_PAYMENT_PENDING);
        registration.setPaymentStatus("PAYMENT_APPROVAL");
        registration.setPaymentGateway(PAYMENT_VNPAY);
        RegisterServiceRequest saved = requestRepository.save(registration);

        long orderId = Math.abs(saved.getId().hashCode());
        if (orderId == 0) {
            orderId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        }
        orderIdToRegistrationId.put(orderId, saved.getId());

        String clientIp = resolveClientIp(request);
        String orderInfo = "Thanh toán đăng ký xe " + (saved.getLicensePlate() != null ? saved.getLicensePlate() : saved.getId());
        String returnUrl = vnpayProperties.getReturnUrl();
        var paymentResult = vnpayService.createPaymentUrlWithRef(orderId, orderInfo, REGISTRATION_FEE, clientIp, returnUrl);
        
        // Save transaction reference to database for fallback lookup
        saved.setVnpayTransactionRef(paymentResult.transactionRef());
        requestRepository.save(saved);

        return new VehicleRegistrationPaymentResponse(saved.getId(), paymentResult.paymentUrl());
    }

    @Transactional
    public VehicleRegistrationPaymentResponse createAndInitiatePayment(UUID userId, RegisterServiceRequestCreateDto dto, HttpServletRequest request) {
        RegisterServiceRequestDto created = createRegistration(userId, dto);
        return initiatePayment(userId, created.id(), request);
    }

    @Transactional(readOnly = true)
    public RegisterServiceRequestDto getRegistration(UUID userId, UUID registrationId) {
        RegisterServiceRequest registration = requestRepository.findByIdAndUserIdWithImages(registrationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký xe"));
        return toDto(registration);
    }

    @Transactional(readOnly = true)
    public List<RegisterServiceRequestDto> getRegistrationsForAdmin(String status, String paymentStatus) {
        List<RegisterServiceRequest> registrations =
                requestRepository.findAllByServiceTypeWithImages(SERVICE_TYPE);
        return registrations.stream()
                .filter(reg -> status == null || status.equalsIgnoreCase(reg.getStatus()))
                .filter(reg -> paymentStatus == null || paymentStatus.equalsIgnoreCase(reg.getPaymentStatus()))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RegisterServiceRequestDto getRegistrationForAdmin(UUID registrationId) {
        RegisterServiceRequest registration = requestRepository.findByIdWithImages(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký xe"));
        return toDto(registration);
    }

    @Transactional
    public void cancelRegistration(UUID userId, UUID registrationId) {
        RegisterServiceRequest registration = requestRepository.findByIdAndUserId(registrationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký xe"));
        if (Objects.equals(registration.getPaymentStatus(), "PAID")) {
            throw new IllegalStateException("Không thể hủy đăng ký đã thanh toán");
        }
        requestRepository.delete(registration);
    }

    @Transactional
    public RegisterServiceRequestDto approveRegistration(UUID registrationId, UUID adminId, String adminNote) {
        throw new IllegalStateException("Nghiệp vụ phê duyệt đã bị vô hiệu hóa");
    }

    @Transactional
    public RegisterServiceRequestDto rejectRegistration(UUID registrationId, UUID adminId, String reason) {
        throw new IllegalStateException("Nghiệp vụ phê duyệt đã bị vô hiệu hóa");
    }

    @Transactional
    public VehicleRegistrationPaymentResult handleVnpayCallback(Map<String, String> params) {
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
            log.error("❌ [VehicleRegistration] Cannot parse orderId from txnRef: {}", txnRef);
            throw new IllegalArgumentException("Invalid transaction reference format");
        }

        UUID registrationId = orderIdToRegistrationId.get(orderId);
        RegisterServiceRequest registration = null;

        // Try to find registration by orderId map first
        if (registrationId != null) {
            var optional = requestRepository.findById(registrationId);
            if (optional.isPresent()) {
                registration = optional.get();
                log.info("✅ [VehicleRegistration] Found registration by orderId map: registrationId={}, orderId={}", 
                        registrationId, orderId);
            }
        }

        // Fallback: try to find by transaction reference
        if (registration == null) {
            var optionalByTxnRef = requestRepository.findByVnpayTransactionRef(txnRef);
            if (optionalByTxnRef.isPresent()) {
                registration = optionalByTxnRef.get();
                log.info("✅ [VehicleRegistration] Found registration by txnRef: registrationId={}, txnRef={}", 
                        registration.getId(), txnRef);
            }
        }

        // If still not found, throw exception with orderId for debugging
        if (registration == null) {
            log.error("❌ [VehicleRegistration] Cannot find registration: orderId={}, txnRef={}, mapSize={}", 
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
            applyResolvedAddressForUser(
                    registration,
                    registration.getUserId(),
                    registration.getUnitId(),
                    registration.getApartmentNumber(),
                    registration.getBuildingName()
            );
            registration.setStatus(STATUS_PENDING_REVIEW);
            registration.setPaymentGateway(PAYMENT_VNPAY);
            OffsetDateTime payDate = parsePayDate(params.get("vnp_PayDate"));
            registration.setPaymentDate(payDate);
            requestRepository.save(registration);

            // Email placeholder – actual implementation depends on user info lookup
            log.info("✅ [VehicleRegistration] Thanh toán thành công cho đăng ký {}", registrationId);
            java.math.BigDecimal amount = registration.getPaymentAmount();
            billingClient.recordVehicleRegistrationPayment(
                    registrationId,
                    registration.getUserId(),
                    registration.getUnitId(),
                    registration.getVehicleType(),
                    registration.getLicensePlate(),
                    registration.getRequestType(),
                    registration.getNote(),
                    amount,
                    payDate,
                    txnRef,
                    params.get("vnp_TransactionNo"),
                    params.get("vnp_BankCode"),
                    params.get("vnp_CardType"),
                    responseCode
            );
            orderIdToRegistrationId.remove(orderId);
            return new VehicleRegistrationPaymentResult(registrationId, true, responseCode, signatureValid);
        }

        registration.setStatus(STATUS_READY_FOR_PAYMENT);
        registration.setPaymentStatus("UNPAID");
        requestRepository.save(registration);
        orderIdToRegistrationId.remove(orderId);
        return new VehicleRegistrationPaymentResult(registrationId, false, responseCode, signatureValid);
    }

    private void applyResolvedAddressForUser(RegisterServiceRequest request,
                                             UUID userId,
                                             UUID unitId,
                                             String fallbackApartment,
                                             String fallbackBuilding) {
        residentUnitLookupService.resolveByUser(userId, unitId).ifPresentOrElse(info -> {
            String resolvedApartment = info.apartmentNumber();
            String resolvedBuilding = info.buildingName();
            request.setApartmentNumber(normalize(resolvedApartment != null ? resolvedApartment : fallbackApartment));
            request.setBuildingName(normalize(resolvedBuilding != null ? resolvedBuilding : fallbackBuilding));
        }, () -> {
            request.setApartmentNumber(normalize(fallbackApartment));
            request.setBuildingName(normalize(fallbackBuilding));
        });
    }

    private OffsetDateTime parsePayDate(String payDate) {
        if (payDate == null || payDate.isBlank()) {
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

    private void validatePayload(RegisterServiceRequestCreateDto dto) {
        if (dto.unitId() == null) {
            throw new IllegalArgumentException("Căn hộ là bắt buộc");
        }
        if (dto.imageUrls() != null && dto.imageUrls().size() > MAX_IMAGES) {
            throw new IllegalArgumentException("Chỉ được chọn tối đa " + MAX_IMAGES + " ảnh");
        }
        if (dto.licensePlate() == null || dto.licensePlate().isBlank()) {
            throw new IllegalArgumentException("Biển số xe là bắt buộc");
        }
        if (dto.vehicleType() == null || dto.vehicleType().isBlank()) {
            throw new IllegalArgumentException("Loại phương tiện là bắt buộc");
        }
    }

    private String resolveRequestType(String requestType) {
        if (requestType == null) {
            return "NEW_CARD";
        }
        return switch (requestType.toUpperCase(Locale.ROOT)) {
            case "REPLACE_CARD", "NEW_CARD" -> requestType.toUpperCase(Locale.ROOT);
            default -> "NEW_CARD";
        };
    }

    private String resolveVehicleType(String vehicleType) {
        if (vehicleType == null) {
            return null;
        }
        String normalized = vehicleType.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("CAR") || normalized.contains("Ô TÔ")) {
            return "CAR";
        }
        if (normalized.contains("MOTOR") || normalized.contains("XE MÁY")) {
            return "MOTORBIKE";
        }
        return vehicleType;
    }

    private String normalize(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isBlank()) {
            return header.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private RegisterServiceRequestDto toDto(RegisterServiceRequest entity) {
        List<RegisterServiceImageDto> images = entity.getImages().stream()
                .map(img -> new RegisterServiceImageDto(img.getId(), entity.getId(), img.getImageUrl(), img.getCreatedAt()))
                .toList();

        return new RegisterServiceRequestDto(
                entity.getId(),
                entity.getUserId(),
                entity.getServiceType(),
                entity.getRequestType(),
                entity.getNote(),
                entity.getStatus(),
                entity.getVehicleType(),
                entity.getLicensePlate(),
                entity.getVehicleBrand(),
                entity.getVehicleColor(),
                entity.getApartmentNumber(),
                entity.getBuildingName(),
                entity.getUnitId(),
                entity.getPaymentStatus(),
                entity.getPaymentAmount(),
                entity.getPaymentDate(),
                entity.getPaymentGateway(),
                entity.getVnpayTransactionRef(),
                entity.getAdminNote(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getRejectionReason(),
                images,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public record VehicleRegistrationPaymentResponse(UUID registrationId, String paymentUrl) {}

    public record VehicleRegistrationPaymentResult(UUID registrationId, boolean success, String responseCode, boolean signatureValid) {}
}


