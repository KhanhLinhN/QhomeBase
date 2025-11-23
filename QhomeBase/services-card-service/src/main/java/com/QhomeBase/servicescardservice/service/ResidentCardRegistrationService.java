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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"NullAway", "DataFlowIssue"})
public class ResidentCardRegistrationService {

    private static final BigDecimal REGISTRATION_FEE = BigDecimal.valueOf(30000);
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_READY_FOR_PAYMENT = "READY_FOR_PAYMENT";
    private static final String STATUS_PAYMENT_PENDING = "PAYMENT_PENDING";
    private static final String STATUS_PENDING_REVIEW = "PENDING";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String PAYMENT_VNPAY = "VNPAY";

    private final ResidentCardRegistrationRepository repository;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;
    private final BillingClient billingClient;
    private final ResidentUnitLookupService residentUnitLookupService;
    private final NotificationClient notificationClient;
    private final CardFeeReminderService cardFeeReminderService;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ConcurrentMap<Long, UUID> orderIdToRegistrationId = new ConcurrentHashMap<>();

    @Transactional
    public ResidentCardRegistrationDto createRegistration(UUID userId, ResidentCardRegistrationCreateDto dto) {
        validatePayload(dto);

        // Normalize citizenId: loại bỏ tất cả ký tự không phải số
        String normalizedCitizenId = dto.citizenId() != null 
                ? dto.citizenId().replaceAll("[^0-9]", "") 
                : null;
        
        ResidentCardRegistration registration = ResidentCardRegistration.builder()
                .userId(userId)
                .unitId(dto.unitId())
                .requestType(resolveRequestType(dto.requestType()))
                .residentId(dto.residentId())
                .fullName(normalize(dto.fullName()))
                .apartmentNumber(normalize(dto.apartmentNumber()))
                .buildingName(normalize(dto.buildingName()))
                .citizenId(normalizedCitizenId)
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

            // Create reminder state if card is already paid (for test mode)
            // In production, reminder state will be created after payment callback
            if ("PAID".equalsIgnoreCase(saved.getPaymentStatus())) {
                try {
                    cardFeeReminderService.resetReminderAfterPayment(
                            CardFeeReminderService.CardFeeType.RESIDENT,
                            saved.getId(),
                            saved.getUnitId(),
                            saved.getResidentId(),
                            saved.getUserId(),
                            saved.getApartmentNumber(),
                            saved.getBuildingName(),
                            saved.getPaymentDate() != null ? saved.getPaymentDate() : now
                    );
                    log.info("✅ [ResidentCard] Đã tạo reminder state cho thẻ {} sau khi approve", saved.getId());
                } catch (Exception e) {
                    log.warn("⚠️ [ResidentCard] Không thể tạo reminder state sau khi approve: {}", e.getMessage());
                }
            }

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
            // Cho phép tiếp tục thanh toán nếu payment_status là UNPAID hoặc PAYMENT_PENDING
            // (PAYMENT_PENDING có thể xảy ra khi user chưa hoàn tất thanh toán trong 10 phút)
            if (!Objects.equals(paymentStatus, "UNPAID") && !Objects.equals(paymentStatus, "PAYMENT_PENDING")) {
                throw new IllegalStateException("Đăng ký đã thanh toán hoặc không thể tiếp tục thanh toán");
            }
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
        if (STATUS_CANCELLED.equalsIgnoreCase(registration.getStatus())) {
            return;
        }
        registration.setStatus(STATUS_CANCELLED);
        registration.setUpdatedAt(OffsetDateTime.now());
        repository.save(registration);
        log.info("✅ [ResidentCard] User {} đã hủy đăng ký {}", userId, registrationId);
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
            
            // Nếu là gia hạn (status = NEEDS_RENEWAL hoặc SUSPENDED), sau khi thanh toán thành công → set status = APPROVED
            // Nếu là đăng ký mới, sau khi thanh toán → set status = PENDING_REVIEW (chờ admin duyệt)
            registration.setPaymentGateway(PAYMENT_VNPAY);
            OffsetDateTime payDate = parsePayDate(params.get("vnp_PayDate"));
            registration.setPaymentDate(payDate);
            
            String currentStatus = registration.getStatus();
            if ("NEEDS_RENEWAL".equals(currentStatus) || "SUSPENDED".equals(currentStatus)) {
                registration.setStatus(STATUS_APPROVED);
                registration.setApprovedAt(OffsetDateTime.now()); // Cập nhật lại approved_at khi gia hạn
                log.info("✅ [ResidentCard] Gia hạn thành công, thẻ {} đã được set lại status = APPROVED", registration.getId());
                
                // Reset reminder cycle sau khi gia hạn (approved_at đã được set ở trên)
                cardFeeReminderService.resetReminderAfterPayment(
                        CardFeeReminderService.CardFeeType.RESIDENT,
                        registration.getId(),
                        registration.getUnitId(),
                        registration.getResidentId(),
                        registration.getUserId(),
                        registration.getApartmentNumber(),
                        registration.getBuildingName(),
                        payDate // payment_date mới (approved_at sẽ được lấy từ registration.getApprovedAt())
                );
            } else {
                registration.setStatus(STATUS_PENDING_REVIEW);
            }
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

            cardFeeReminderService.resetReminderAfterPayment(
                    CardFeeReminderService.CardFeeType.RESIDENT,
                    registration.getId(),
                    registration.getUnitId(),
                    registration.getResidentId(),
                    registration.getUserId(),
                    registration.getApartmentNumber(),
                    registration.getBuildingName(),
                    payDate
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
        
        // Validate số lượng thẻ cư dân không vượt quá số người trong căn hộ
        validateResidentCardLimitByUnit(dto.unitId());
        
        // Validate CCCD phải là 13 số
        if (!StringUtils.hasText(dto.citizenId())) {
            throw new IllegalArgumentException("CCCD/CMND là bắt buộc");
        }
        
        // Normalize CCCD: loại bỏ tất cả khoảng trắng và ký tự không phải số
        String normalizedCitizenId = dto.citizenId().replaceAll("[^0-9]", "");
        
        // Validate format: phải đúng 13 số
        if (normalizedCitizenId.length() != 13) {
            throw new IllegalArgumentException("CCCD/CMND phải là 13 số");
        }
        
        // Kiểm tra CCCD có thuộc căn hộ không
        validateCitizenIdBelongsToUnit(normalizedCitizenId, dto.unitId());
        
        // Kiểm tra CCCD đã được sử dụng chưa
        if (repository.existsByCitizenId(normalizedCitizenId)) {
            throw new IllegalStateException(
                String.format("CCCD/CMND %s đã được sử dụng để đăng ký thẻ cư dân. " +
                            "Mỗi CCCD/CMND chỉ được phép đăng ký 1 thẻ cư dân.",
                            normalizedCitizenId)
            );
        }
        log.debug("✅ [ResidentCard] CCCD {} chưa được sử dụng và thuộc căn hộ", normalizedCitizenId);
    }

    /**
     * Kiểm tra số thẻ cư dân đã đăng ký không vượt quá số người trong căn hộ
     */
    private void validateResidentCardLimitByUnit(UUID unitId) {
        // Đếm số household members (số người) trong căn hộ
        long numberOfResidents = countHouseholdMembersByUnit(unitId);
        
        // Đếm số thẻ cư dân đã đăng ký cho căn hộ này (bao gồm cả chưa thanh toán)
        // Đếm TẤT CẢ các registration trừ REJECTED và CANCELLED
        // Logic: Nếu đã đăng ký đủ số lượng thẻ (kể cả chưa thanh toán), không cho phép đăng ký thêm
        // Chỉ khi một thẻ bị hủy (CANCELLED) hoặc từ chối (REJECTED) thì mới có thể đăng ký thêm
        long registeredCards = repository.countAllResidentCardsByUnitId(unitId, List.of("REJECTED", "CANCELLED"));
        
        if (registeredCards >= numberOfResidents) {
            throw new IllegalStateException(
                String.format("Căn hộ này chỉ được phép đăng ký tối đa %d thẻ cư dân (theo số người trong căn hộ). " +
                            "Hiện tại đã đăng ký %d thẻ (bao gồm cả các thẻ chưa thanh toán). " +
                            "Vui lòng thanh toán hoặc hủy các thẻ đã đăng ký trước khi đăng ký thẻ mới.",
                            numberOfResidents, registeredCards)
            );
        }
        
        log.debug("✅ [ResidentCard] Unit {}: {} residents, {} registered cards (including unpaid)", 
                unitId, numberOfResidents, registeredCards);
    }

    /**
     * Kiểm tra CCCD có thuộc căn hộ đó không
     */
    private void validateCitizenIdBelongsToUnit(String citizenId, UUID unitId) {
        if (!StringUtils.hasText(citizenId) || unitId == null) {
            return;
        }
        
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("citizenId", citizenId)
                    .addValue("unitId", unitId);
            
            log.debug("🔍 [ResidentCard] Đang kiểm tra CCCD {} có thuộc căn hộ {} không", citizenId, unitId);
            
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(DISTINCT hm.resident_id)
                    FROM data.household_members hm
                    JOIN data.households h ON h.id = hm.household_id
                    JOIN data.residents r ON r.id = hm.resident_id
                    WHERE h.unit_id = :unitId
                      AND r.national_id = :citizenId
                      AND (hm.left_at IS NULL OR hm.left_at >= CURRENT_DATE)
                      AND (h.end_date IS NULL OR h.end_date >= CURRENT_DATE)
                    """, params, Long.class);
            
            if (count == null || count == 0) {
                throw new IllegalStateException(
                    String.format("CCCD/CMND %s không thuộc căn hộ này. " +
                                "Vui lòng kiểm tra lại thông tin CCCD/CMND và căn hộ.",
                                citizenId)
                );
            }
            
            log.debug("✅ [ResidentCard] CCCD {} thuộc căn hộ {}", citizenId, unitId);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Không thể kiểm tra CCCD {} có thuộc căn hộ {} không", citizenId, unitId, e);
            throw new IllegalStateException(
                String.format("Không thể xác thực CCCD/CMND. Vui lòng thử lại sau."), e);
        }
    }

    /**
     * Lấy danh sách thành viên trong căn hộ (bao gồm citizenId và fullName)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHouseholdMembersByUnit(UUID unitId) {
        if (unitId == null) {
            log.warn("⚠️ [ResidentCard] getHouseholdMembersByUnit called with null unitId");
            return List.of();
        }
        
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("unitId", unitId);
            
            log.debug("🔍 [ResidentCard] Đang lấy danh sách thành viên trong căn hộ unitId: {}", unitId);
            
            // Query để lấy danh sách thành viên và check xem họ đã có thẻ được approve chưa
            List<Map<String, Object>> members = jdbcTemplate.query("""
                    SELECT DISTINCT
                        r.id AS resident_id,
                        r.full_name AS full_name,
                        r.national_id AS citizen_id,
                        r.phone AS phone_number,
                        r.email AS email,
                        r.dob AS date_of_birth,
                        CASE 
                            WHEN EXISTS (
                                SELECT 1 FROM card.resident_card_registration rcr
                                WHERE rcr.citizen_id = r.national_id
                                  AND rcr.status IN ('APPROVED', 'ACTIVE', 'ISSUED', 'COMPLETED')
                            ) THEN true
                            ELSE false
                        END AS has_approved_card,
                        CASE
                            WHEN EXISTS (
                                SELECT 1 FROM card.resident_card_registration rcr
                                WHERE rcr.citizen_id = r.national_id
                                  AND rcr.status IN ('PENDING', 'REVIEW_PENDING', 'PROCESSING', 'IN_PROGRESS')
                                  AND rcr.payment_status = 'PAID'
                            ) THEN true
                            ELSE false
                        END AS waiting_for_approval
                    FROM data.household_members hm
                    JOIN data.households h ON h.id = hm.household_id
                    JOIN data.residents r ON r.id = hm.resident_id
                    WHERE h.unit_id = :unitId
                      AND (hm.left_at IS NULL OR hm.left_at >= CURRENT_DATE)
                      AND (h.end_date IS NULL OR h.end_date >= CURRENT_DATE)
                    ORDER BY r.full_name
                    """, params, (rs, rowNum) -> {
                Map<String, Object> member = new HashMap<>();
                member.put("residentId", rs.getObject("resident_id", UUID.class).toString());
                member.put("fullName", rs.getString("full_name"));
                member.put("citizenId", rs.getString("citizen_id"));
                member.put("phoneNumber", rs.getString("phone_number"));
                member.put("email", rs.getString("email"));
                member.put("dateOfBirth", rs.getDate("date_of_birth") != null 
                    ? rs.getDate("date_of_birth").toString() : null);
                member.put("hasApprovedCard", rs.getBoolean("has_approved_card"));
                member.put("waitingForApproval", rs.getBoolean("waiting_for_approval"));
                return member;
            });
            
            log.info("✅ [ResidentCard] Căn hộ {} có {} thành viên", unitId, members.size());
            return members;
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Không thể lấy danh sách thành viên trong căn hộ unitId: {}", unitId, e);
            return List.of();
        }
    }

    /**
     * Đếm số household members (số người) đang ở trong căn hộ
     */
    private long countHouseholdMembersByUnit(UUID unitId) {
        if (unitId == null) {
            log.warn("⚠️ [ResidentCard] unitId is null, returning 0");
            return 0;
        }
        
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("unitId", unitId);
            
            log.debug("🔍 [ResidentCard] Đang đếm số người trong căn hộ unitId: {}", unitId);
            
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(DISTINCT hm.resident_id)
                    FROM data.household_members hm
                    JOIN data.households h ON h.id = hm.household_id
                    WHERE h.unit_id = :unitId
                      AND (hm.left_at IS NULL OR hm.left_at >= CURRENT_DATE)
                      AND (h.end_date IS NULL OR h.end_date >= CURRENT_DATE)
                    """, params, Long.class);
            
            long result = count != null ? count : 0;
            log.info("✅ [ResidentCard] Căn hộ {} có {} người đang ở", unitId, result);
            return result;
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Không thể đếm số người trong căn hộ unitId: {}", unitId, e);
            throw new IllegalStateException(
                String.format("Không thể đếm số người trong căn hộ. Vui lòng thử lại sau. UnitId: %s", unitId), e);
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


