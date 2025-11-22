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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"NullAway", "DataFlowIssue"})
public class ElevatorCardRegistrationService {

    private static final BigDecimal REGISTRATION_FEE = BigDecimal.valueOf(30000);
    private static final String STATUS_PENDING_REVIEW = "PENDING";
    private static final String STATUS_READY_FOR_PAYMENT = "READY_FOR_PAYMENT";
    private static final String STATUS_PAYMENT_PENDING = "PAYMENT_PENDING";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String PAYMENT_VNPAY = "VNPAY";

    private final ElevatorCardRegistrationRepository repository;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;
    private final BillingClient billingClient;
    private final ResidentUnitLookupService residentUnitLookupService;
    private final NotificationClient notificationClient;
    private final NamedParameterJdbcTemplate jdbcTemplate;
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
                .fullName(null) // Sẽ được lấy từ user context
                .apartmentNumber(normalize(dto.apartmentNumber()))
                .buildingName(normalize(dto.buildingName()))
                .citizenId(null) // Không lưu CCCD cho thẻ thang máy, validate theo số người trong căn hộ
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
            // Tự động lấy thông tin từ user context (fullName, apartmentNumber, buildingName)
            applyResolvedAddress(registration, dto.residentId(), dto.unitId(), null, dto.apartmentNumber(), dto.buildingName());
            // Đảm bảo fullName luôn được set từ user context
            if (!StringUtils.hasText(registration.getFullName())) {
                log.warn("⚠️ [ElevatorCard] Không thể lấy fullName từ user context cho residentId: {}", dto.residentId());
                throw new IllegalStateException("Không thể lấy thông tin người dùng. Vui lòng thử lại sau.");
            }
        } catch (IllegalStateException e) {
            throw e; // Re-throw IllegalStateException
        } catch (Exception e) {
            log.warn("⚠️ [ElevatorCard] Không thể resolve địa chỉ từ database, sử dụng giá trị từ form: {}", e.getMessage());
            // Fallback to form values if lookup fails
            if (!StringUtils.hasText(registration.getApartmentNumber())) {
                registration.setApartmentNumber(normalize(dto.apartmentNumber()));
            }
            if (!StringUtils.hasText(registration.getBuildingName())) {
                registration.setBuildingName(normalize(dto.buildingName()));
            }
            // Nếu không lấy được fullName từ user context, throw error
            if (!StringUtils.hasText(registration.getFullName())) {
                throw new IllegalStateException("Không thể lấy thông tin người dùng. Vui lòng thử lại sau.");
            }
        }

        @SuppressWarnings("NullAway")
        ElevatorCardRegistration saved = repository.save(registration);
        return toDto(saved);
    }

    @Transactional
    public ElevatorCardPaymentResponse createAndInitiatePayment(UUID userId,
                                                                ElevatorCardRegistrationCreateDto dto,
                                                                HttpServletRequest request) {
        ElevatorCardRegistrationDto created = createRegistration(userId, dto);
        return initiatePayment(userId, created.id(), request);
    }

    /**
     * Lấy số lượng thẻ thang máy tối đa có thể đăng ký cho một căn hộ
     * (bằng số người đang ở trong căn hộ đó)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMaxCardsForUnit(UUID unitId) {
        if (unitId == null) {
            log.warn("⚠️ [ElevatorCard] getMaxCardsForUnit called with null unitId");
            throw new IllegalArgumentException("unitId không được để trống");
        }
        
        log.debug("🔍 [ElevatorCard] getMaxCardsForUnit được gọi với unitId: {}", unitId);
        
        UnitCapacityInfo capacityInfo = resolveUnitCapacity(unitId);
        long maxCards = capacityInfo.maxResidents();
        long registeredCards = repository.countElevatorCardsByUnitId(unitId);
        long remainingSlots = Math.max(0, maxCards - registeredCards);
        
        log.info("📊 [ElevatorCard] Unit {} ({}): maxCards={}, registeredCards={}, remainingSlots={}", 
                capacityInfo.unitCode(), capacityInfo.buildingName(), maxCards, registeredCards, remainingSlots);
        
        Map<String, Object> result = new HashMap<>();
        result.put("unitId", unitId.toString());
        result.put("unitCode", capacityInfo.unitCode());
        result.put("buildingName", capacityInfo.buildingName());
        result.put("maxCards", maxCards);
        result.put("registeredCards", registeredCards);
        result.put("remainingSlots", remainingSlots);
        result.put("canRegisterMore", remainingSlots > 0);
        
        return result;
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký thẻ thang máy"));

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

            ElevatorCardRegistration saved = repository.save(registration);

            // Send notification to resident
            sendElevatorCardApprovalNotification(saved, request.issueMessage());

            log.info("✅ [ElevatorCard] Admin {} đã approve đăng ký {}", adminId, registrationId);
            return toDto(saved);
        } else if ("REJECT".equalsIgnoreCase(decision) || "REJECTED".equalsIgnoreCase(decision)) {
            // Reject logic
            if (STATUS_REJECTED.equalsIgnoreCase(registration.getStatus())) {
                throw new IllegalStateException("Đăng ký đã bị từ chối");
            }

            registration.setStatus(STATUS_REJECTED);
            registration.setAdminNote(request.note());
            registration.setRejectionReason(request.note());
            registration.setUpdatedAt(now);

            ElevatorCardRegistration saved = repository.save(registration);

            log.info("✅ [ElevatorCard] Admin {} đã reject đăng ký {}", adminId, registrationId);
            return toDto(saved);
        } else {
            throw new IllegalArgumentException("Invalid decision: " + decision + ". Must be APPROVE or REJECT");
        }
    }

    private void sendElevatorCardApprovalNotification(ElevatorCardRegistration registration, String issueMessage) {
        try {
            UUID residentId = registration.getResidentId();
            if (residentId == null) {
                log.warn("⚠️ [ElevatorCard] residentId là null, không thể gửi notification cho registrationId: {}", 
                        registration.getId());
                return;
            }

            // Resolve buildingId from unitId if needed
            UUID buildingId = null;
            if (registration.getUnitId() != null) {
                // Note: AddressInfo doesn't have buildingId, so we pass null and let the notification service handle it
                buildingId = null;
            }

            String title = "Thẻ thang máy đã được duyệt";
            String message = issueMessage != null && !issueMessage.isBlank() 
                    ? issueMessage 
                    : String.format("Thẻ thang máy của bạn đã được duyệt. Vui lòng đến nhận thẻ theo thông tin đã cung cấp.", 
                            registration.getApartmentNumber() != null ? registration.getApartmentNumber() : "");

            Map<String, String> data = new HashMap<>();
            data.put("cardType", "ELEVATOR_CARD");
            data.put("registrationId", registration.getId().toString());
            if (registration.getApartmentNumber() != null) {
                data.put("apartmentNumber", registration.getApartmentNumber());
            }
            if (registration.getFullName() != null) {
                data.put("fullName", registration.getFullName());
            }

            notificationClient.sendResidentNotification(
                    residentId,
                    buildingId,
                    "CARD_APPROVED",
                    title,
                    message,
                    registration.getId(),
                    "ELEVATOR_CARD_REGISTRATION",
                    data
            );

            log.info("✅ [ElevatorCard] Đã gửi notification approval cho residentId: {}", residentId);
        } catch (Exception e) {
            log.error("❌ [ElevatorCard] Không thể gửi notification approval cho registrationId: {}", 
                    registration.getId(), e);
        }
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
        if ("CANCELLED".equalsIgnoreCase(registration.getStatus())) {
            throw new IllegalStateException("Đăng ký này đã bị hủy do không thanh toán. Vui lòng tạo đăng ký mới.");
        }
        // Cho phép tiếp tục thanh toán nếu payment_status là UNPAID hoặc PAYMENT_PENDING
        // (PAYMENT_PENDING có thể xảy ra khi user chưa hoàn tất thanh toán trong 10 phút)
        String paymentStatus = registration.getPaymentStatus();
        if (!Objects.equals(paymentStatus, "UNPAID") && !Objects.equals(paymentStatus, "PAYMENT_PENDING")) {
            throw new IllegalStateException("Đăng ký đã thanh toán hoặc không thể tiếp tục thanh toán");
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
        var paymentResult = vnpayService.createPaymentUrlWithRef(orderId, orderInfo, REGISTRATION_FEE, clientIp, returnUrl);
        
        // Save transaction reference to database for fallback lookup
        saved.setVnpayTransactionRef(paymentResult.transactionRef());
        repository.save(saved);

        return new ElevatorCardPaymentResponse(saved.getId(), paymentResult.paymentUrl());
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
        if (STATUS_CANCELLED.equalsIgnoreCase(registration.getStatus())) {
            return;
        }
        registration.setStatus(STATUS_CANCELLED);
        registration.setUpdatedAt(OffsetDateTime.now());
        repository.save(registration);
        log.info("✅ [ElevatorCard] User {} đã hủy đăng ký {}", userId, registrationId);
    }

    @Transactional
    public ElevatorCardPaymentResult handleVnpayCallback(Map<String, String> params) {
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
            log.error("❌ [ElevatorCard] Cannot parse orderId from txnRef: {}", txnRef);
            throw new IllegalArgumentException("Invalid transaction reference format");
        }

        UUID registrationId = orderIdToRegistrationId.get(orderId);
        ElevatorCardRegistration registration = null;

        // Try to find registration by orderId map first
        if (registrationId != null) {
            var optional = repository.findById(registrationId);
            if (optional.isPresent()) {
                registration = optional.get();
                log.info("✅ [ElevatorCard] Found registration by orderId map: registrationId={}, orderId={}", 
                        registrationId, orderId);
            }
        }

        // Fallback: try to find by transaction reference
        if (registration == null) {
            var optionalByTxnRef = repository.findByVnpayTransactionRef(txnRef);
            if (optionalByTxnRef.isPresent()) {
                registration = optionalByTxnRef.get();
                log.info("✅ [ElevatorCard] Found registration by txnRef: registrationId={}, txnRef={}", 
                        registration.getId(), txnRef);
            }
        }

        // If still not found, throw exception with orderId for debugging
        if (registration == null) {
            log.error("❌ [ElevatorCard] Cannot find registration: orderId={}, txnRef={}, mapSize={}", 
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
                applyResolvedAddress(
                        registration,
                        registration.getResidentId(),
                        registration.getUnitId(),
                        registration.getFullName(),
                        registration.getApartmentNumber(),
                        registration.getBuildingName()
                );
            } catch (Exception e) {
                log.warn("⚠️ [ElevatorCard] Không thể resolve địa chỉ sau thanh toán, giữ nguyên giá trị hiện tại: {}", e.getMessage());
            }
            registration.setStatus(STATUS_PENDING_REVIEW);
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

    private void applyResolvedAddress(ElevatorCardRegistration registration,
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

    private void validatePayload(ElevatorCardRegistrationCreateDto dto) {
        if (dto.unitId() == null) {
            throw new IllegalArgumentException("Căn hộ là bắt buộc");
        }
        if (dto.residentId() == null) {
            throw new IllegalArgumentException("Cư dân là bắt buộc");
        }
        // fullName sẽ được tự động lấy từ user context, không cần validate
        
        // Validate resident thuộc unit (căn hộ) đó
        validateResidentBelongsToUnit(dto.residentId(), dto.unitId());
        
        // Validate số thẻ thang máy không vượt quá số người trong căn hộ
        validateElevatorCardLimitByUnit(dto.unitId());
    }
    
    /**
     * Kiểm tra resident có thuộc unit (căn hộ) đó không
     */
    private void validateResidentBelongsToUnit(UUID residentId, UUID unitId) {
        Optional<ResidentUnitLookupService.AddressInfo> info = 
                residentUnitLookupService.resolveByResident(residentId, unitId);
        
        if (info.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Cư dân không thuộc căn hộ này. Vui lòng kiểm tra lại thông tin căn hộ và cư dân.")
            );
        }
        
        // Kiểm tra thêm: resident phải có trong household của unit đó
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("residentId", residentId)
                    .addValue("unitId", unitId);
            
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM data.household_members hm
                    JOIN data.households h ON h.id = hm.household_id
                    WHERE hm.resident_id = :residentId
                      AND h.unit_id = :unitId
                      AND (hm.left_at IS NULL OR hm.left_at >= CURRENT_DATE)
                      AND (h.end_date IS NULL OR h.end_date >= CURRENT_DATE)
                    """, params, Long.class);
            
            if (count == null || count == 0) {
                throw new IllegalArgumentException(
                    String.format("Cư dân không thuộc căn hộ này hoặc đã rời khỏi căn hộ. Vui lòng kiểm tra lại.")
                );
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("⚠️ [ElevatorCard] Không thể validate resident-unit relationship: {}", e.getMessage());
            // Nếu có lỗi khi query, vẫn cho phép tiếp tục (fallback)
        }
    }

    /**
     * Kiểm tra số thẻ thang máy đã đăng ký không vượt quá số người trong căn hộ
     */
    private void validateElevatorCardLimitByUnit(UUID unitId) {
        UnitCapacityInfo capacityInfo = resolveUnitCapacity(unitId);
        long numberOfResidents = capacityInfo.maxResidents();
        
        // Đếm số thẻ đã thanh toán (bao gồm cả đang chờ duyệt) hoặc đã được duyệt
        long registeredCards = repository.countElevatorCardsByUnitId(unitId);
        
        if (registeredCards >= numberOfResidents) {
            throw new IllegalStateException(
                String.format("Căn hộ này chỉ được phép đăng ký tối đa %d thẻ thang máy (theo số người trong căn hộ). " +
                            "Hiện tại đã có %d thẻ đã thanh toán (bao gồm thẻ chờ duyệt và đã duyệt). " +
                            "Vui lòng thanh toán hoặc hủy các thẻ đã đăng ký trước khi đăng ký thẻ mới.",
                            numberOfResidents, registeredCards)
            );
        }
        
        log.debug("✅ [ElevatorCard] Unit {} ({}): capacity={} residents, {} registered cards (including unpaid)", 
                capacityInfo.unitCode(), capacityInfo.buildingName(), numberOfResidents, registeredCards);
    }

    private UnitCapacityInfo resolveUnitCapacity(UUID unitId) {
        if (unitId == null) {
            throw new IllegalArgumentException("unitId không được để trống");
        }
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("unitId", unitId);

            return jdbcTemplate.queryForObject("""
                    SELECT 
                        u.id   AS unit_id,
                        u.code AS unit_code,
                        u.bedrooms,
                        b.id   AS building_id,
                        b.code AS building_code,
                        b.name AS building_name
                    FROM data.units u
                    JOIN data.buildings b ON b.id = u.building_id
                    WHERE u.id = :unitId
                    """, params, (rs, rowNum) -> {
                Integer bedrooms = rs.getObject("bedrooms") != null ? rs.getInt("bedrooms") : null;
                int maxResidents = computeMaxResidents(bedrooms);
                return new UnitCapacityInfo(
                        rs.getObject("unit_id", UUID.class),
                        rs.getString("unit_code"),
                        rs.getObject("building_id", UUID.class),
                        rs.getString("building_code"),
                        rs.getString("building_name"),
                        bedrooms,
                        maxResidents
                );
            });
        } catch (Exception e) {
            log.error("❌ [ElevatorCard] Không thể lấy thông tin căn hộ unitId: {}", unitId, e);
            throw new IllegalStateException("Không thể xác định sức chứa căn hộ. Vui lòng thử lại sau.", e);
        }
    }

    private int computeMaxResidents(Integer bedrooms) {
        if (bedrooms != null && bedrooms > 0) {
            return Math.max(bedrooms * 2, 1);
        }
        return 4;
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

    private record UnitCapacityInfo(
            UUID unitId,
            String unitCode,
            UUID buildingId,
            String buildingCode,
            String buildingName,
            Integer bedrooms,
            int maxResidents
    ) {}

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


