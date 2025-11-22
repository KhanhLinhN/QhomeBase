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
import java.util.HashMap;
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
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_READY_FOR_PAYMENT = "READY_FOR_PAYMENT";
    private static final String STATUS_PAYMENT_PENDING = "PAYMENT_PENDING";
    private static final String STATUS_PENDING_REVIEW = "PENDING";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String PAYMENT_VNPAY = "VNPAY";

    private final RegisterServiceRequestRepository requestRepository;
    private final RegisterServiceImageRepository imageRepository;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;
    private final BillingClient billingClient;
    private final ResidentUnitLookupService residentUnitLookupService;
    private final NotificationClient notificationClient;
    private final CardFeeReminderService cardFeeReminderService;
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
            log.warn("⚠️ [VehicleRegistration] storeImages: Danh sách file rỗng");
            return List.of();
        }
        if (files.size() > MAX_IMAGES) {
            throw new IllegalArgumentException("Chỉ được tải tối đa " + MAX_IMAGES + " ảnh");
        }
        
        log.info("📤 [VehicleRegistration] storeImages: Bắt đầu lưu {} file", files.size());
        Path uploadDir = ensureUploadDir();
        log.debug("📁 [VehicleRegistration] Upload directory: {}", uploadDir.toAbsolutePath());
        
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            try {
                String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
                log.debug("📄 [VehicleRegistration] Đang xử lý file {}/{}: {} ({} bytes)", 
                    i + 1, files.size(), originalFilename, file.getSize());
                
                String extension = "";
                int dot = originalFilename.lastIndexOf('.');
                if (dot >= 0) {
                    extension = originalFilename.substring(dot);
                }
                String filename = UUID.randomUUID() + extension;
                Path target = uploadDir.resolve(filename);
                
                long startTime = System.currentTimeMillis();
                Files.copy(file.getInputStream(), target);
                long duration = System.currentTimeMillis() - startTime;
                log.debug("✅ [VehicleRegistration] Đã lưu file {} trong {}ms: {}", 
                    i + 1, duration, filename);
                
                urls.add("/uploads/vehicle/" + filename);
            } catch (IOException e) {
                log.error("❌ [VehicleRegistration] Lỗi khi lưu file {}/{}: {}", 
                    i + 1, files.size(), file.getOriginalFilename(), e);
                throw new IOException("Không thể lưu file \"" + file.getOriginalFilename() + "\": " + e.getMessage(), e);
            }
        }
        
        log.info("✅ [VehicleRegistration] storeImages: Đã lưu thành công {} file", urls.size());
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

        if ("CANCELLED".equalsIgnoreCase(registration.getStatus())) {
            throw new IllegalStateException("Đăng ký này đã bị hủy do không thanh toán. Vui lòng tạo đăng ký mới.");
        }

        String currentStatus = registration.getStatus();
        String paymentStatus = registration.getPaymentStatus();
        
        // Cho phép gia hạn nếu status = NEEDS_RENEWAL hoặc SUSPENDED (đã thanh toán trước đó)
        if ("NEEDS_RENEWAL".equalsIgnoreCase(currentStatus) || "SUSPENDED".equalsIgnoreCase(currentStatus)) {
            if (!"PAID".equalsIgnoreCase(paymentStatus)) {
                throw new IllegalStateException("Thẻ chưa thanh toán, không thể gia hạn");
            }
            // Cho phép thanh toán để gia hạn
        } else {
            // Cho phép tiếp tục thanh toán nếu payment_status là UNPAID hoặc PAYMENT_PENDING/PAYMENT_APPROVAL
            // (PAYMENT_PENDING/PAYMENT_APPROVAL có thể xảy ra khi user chưa hoàn tất thanh toán trong 10 phút)
            if (!Objects.equals(paymentStatus, "UNPAID") && 
                !Objects.equals(paymentStatus, "PAYMENT_PENDING") && 
                !Objects.equals(paymentStatus, "PAYMENT_APPROVAL")) {
                throw new IllegalStateException("Đăng ký đã thanh toán hoặc không thể tiếp tục thanh toán");
            }
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
        if (STATUS_CANCELLED.equalsIgnoreCase(registration.getStatus())) {
            return;
        }
        registration.setStatus(STATUS_CANCELLED);
        registration.setUpdatedAt(OffsetDateTime.now());
        requestRepository.save(registration);
        log.info("✅ [VehicleRegistration] User {} đã hủy đăng ký {}", userId, registrationId);
    }

    @Transactional
    public RegisterServiceRequestDto approveRegistration(UUID registrationId, UUID adminId, String adminNote, String issueMessage) {
        RegisterServiceRequest registration = requestRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký xe"));

        if (!STATUS_PENDING_REVIEW.equalsIgnoreCase(registration.getStatus()) 
                && !STATUS_READY_FOR_PAYMENT.equalsIgnoreCase(registration.getStatus())) {
            throw new IllegalStateException("Đăng ký không ở trạng thái chờ duyệt. Trạng thái hiện tại: " + registration.getStatus());
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
        registration.setStatus("APPROVED");
        registration.setApprovedBy(adminId);
        registration.setApprovedAt(now);
        registration.setAdminNote(adminNote);
        registration.setUpdatedAt(now);

        RegisterServiceRequest saved = requestRepository.save(registration);

        // Send notification to resident
        sendVehicleCardApprovalNotification(saved, issueMessage);

        log.info("✅ [VehicleRegistration] Admin {} đã approve đăng ký {}", adminId, registrationId);
        return toDto(saved);
    }

    private void sendVehicleCardApprovalNotification(RegisterServiceRequest registration, String issueMessage) {
        try {
            // Resolve residentId from userId and unitId
            UUID residentId = residentUnitLookupService.resolveByUser(registration.getUserId(), registration.getUnitId())
                    .map(ResidentUnitLookupService.AddressInfo::residentId)
                    .orElse(null);

            if (residentId == null) {
                log.warn("⚠️ [VehicleRegistration] Không thể tìm thấy residentId cho userId={}, unitId={}, bỏ qua notification", 
                        registration.getUserId(), registration.getUnitId());
                return;
            }

            // Resolve buildingId from unitId if needed
            // Note: AddressInfo doesn't have buildingId, so we pass null and let the notification service handle it
            UUID buildingId = null;

            String title = "Thẻ xe đã được duyệt";
            String message = issueMessage != null && !issueMessage.isBlank() 
                    ? issueMessage 
                    : String.format("Thẻ xe %s của bạn đã được duyệt. Vui lòng đến nhận thẻ theo thông tin đã cung cấp.", 
                            registration.getLicensePlate() != null ? registration.getLicensePlate() : "");

            Map<String, String> data = new HashMap<>();
            data.put("cardType", "VEHICLE_CARD");
            data.put("registrationId", registration.getId().toString());
            if (registration.getLicensePlate() != null) {
                data.put("licensePlate", registration.getLicensePlate());
            }
            if (registration.getApartmentNumber() != null) {
                data.put("apartmentNumber", registration.getApartmentNumber());
            }

            notificationClient.sendResidentNotification(
                    residentId,
                    buildingId,
                    "CARD_APPROVED",
                    title,
                    message,
                    registration.getId(),
                    "VEHICLE_CARD_REGISTRATION",
                    data
            );

            log.info("✅ [VehicleRegistration] Đã gửi notification approval cho residentId: {}", residentId);
        } catch (Exception e) {
            log.error("❌ [VehicleRegistration] Không thể gửi notification approval cho registrationId: {}", 
                    registration.getId(), e);
        }
    }

    @Transactional
    public RegisterServiceRequestDto rejectRegistration(UUID registrationId, UUID adminId, String reason) {
        RegisterServiceRequest registration = requestRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký xe"));

        if ("REJECTED".equalsIgnoreCase(registration.getStatus())) {
            throw new IllegalStateException("Đăng ký đã bị từ chối");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
        registration.setStatus("REJECTED");
        registration.setAdminNote(reason);
        registration.setRejectionReason(reason);
        registration.setUpdatedAt(now);

        RegisterServiceRequest saved = requestRepository.save(registration);

        log.info("✅ [VehicleRegistration] Admin {} đã reject đăng ký {}", adminId, registrationId);
        return toDto(saved);
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
            
            registration.setPaymentGateway(PAYMENT_VNPAY);
            OffsetDateTime payDate = parsePayDate(params.get("vnp_PayDate"));
            registration.setPaymentDate(payDate);
            
            // Nếu là gia hạn (status = NEEDS_RENEWAL hoặc SUSPENDED), sau khi thanh toán thành công → set status = APPROVED
            // Nếu là đăng ký mới, sau khi thanh toán → set status = PENDING_REVIEW (chờ admin duyệt)
            String currentStatus = registration.getStatus();
            if ("NEEDS_RENEWAL".equals(currentStatus) || "SUSPENDED".equals(currentStatus)) {
                registration.setStatus(STATUS_APPROVED);
                registration.setApprovedAt(OffsetDateTime.now()); // Cập nhật lại approved_at khi gia hạn
                log.info("✅ [VehicleRegistration] Gia hạn thành công, thẻ {} đã được set lại status = APPROVED", registration.getId());
                
                // Reset reminder cycle sau khi gia hạn (approved_at đã được set ở trên)
                cardFeeReminderService.resetReminderAfterPayment(
                        CardFeeReminderService.CardFeeType.VEHICLE,
                        registration.getId(),
                        registration.getUnitId(),
                        null, // Vehicle card không có residentId
                        registration.getUserId(),
                        registration.getApartmentNumber(),
                        registration.getBuildingName(),
                        payDate // payment_date mới (approved_at sẽ được lấy từ registration.getApprovedAt())
                );
            } else {
                registration.setStatus(STATUS_PENDING_REVIEW);
            }
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

            UUID residentId = residentUnitLookupService.resolveByUser(registration.getUserId(), registration.getUnitId())
                    .map(ResidentUnitLookupService.AddressInfo::residentId)
                    .orElse(null);

            cardFeeReminderService.resetReminderAfterPayment(
                    CardFeeReminderService.CardFeeType.VEHICLE,
                    registration.getId(),
                    registration.getUnitId(),
                    residentId,
                    registration.getUserId(),
                    registration.getApartmentNumber(),
                    registration.getBuildingName(),
                    payDate
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


