package com.QhomeBase.servicescardservice.service;

import com.QhomeBase.servicescardservice.dto.BatchCardPaymentRequest;
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
@SuppressWarnings({"NullAway", "DataFlowIssue"})
public class ElevatorCardRegistrationService {

    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_PENDING_REVIEW = "PENDING";
    private static final String STATUS_READY_FOR_PAYMENT = "READY_FOR_PAYMENT";
    private static final String STATUS_PAYMENT_PENDING = "PAYMENT_PENDING";
    
    private final CardPricingService cardPricingService;
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String PAYMENT_VNPAY = "VNPAY";

    private final ElevatorCardRegistrationRepository repository;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;
    private final BillingClient billingClient;
    private final ResidentUnitLookupService residentUnitLookupService;
    private final NotificationClient notificationClient;
    private final CardFeeReminderService cardFeeReminderService;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final BaseServiceClient baseServiceClient;
    private final ConcurrentMap<Long, UUID> orderIdToRegistrationId = new ConcurrentHashMap<>();

    @Transactional
    @SuppressWarnings({"NullAway", "DataFlowIssue"})
    public ElevatorCardRegistrationDto createRegistration(UUID userId, ElevatorCardRegistrationCreateDto dto) {
        return createRegistration(userId, dto, null);
    }

    @Transactional
    @SuppressWarnings({"NullAway", "DataFlowIssue"})
    public ElevatorCardRegistrationDto createRegistration(UUID userId, ElevatorCardRegistrationCreateDto dto, String accessToken) {
        validatePayload(dto);

        // Kiểm tra xem cư dân đã được duyệt thành thành viên chưa
        if (dto.residentId() != null) {
            boolean isApproved = baseServiceClient.isResidentMemberApproved(dto.residentId(), accessToken);
            if (!isApproved) {
                throw new IllegalStateException(
                    "Cư dân chưa được duyệt thành thành viên. Vui lòng đợi admin duyệt yêu cầu tạo tài khoản trước khi đăng ký thẻ thang máy."
                );
            }
        }

        ElevatorCardRegistration registration = ElevatorCardRegistration.builder()
                .userId(userId)
                .unitId(dto.unitId())
                .residentId(dto.residentId())
                .requestType(resolveRequestType(dto.requestType()))
                .fullName(normalize(dto.fullName())) // Sử dụng fullName từ DTO nếu có
                .apartmentNumber(normalize(dto.apartmentNumber()))
                .buildingName(normalize(dto.buildingName()))
                .citizenId(normalize(dto.citizenId())) // Sử dụng citizenId từ DTO nếu có
                .phoneNumber(normalize(dto.phoneNumber()))
                .note(dto.note())
                .status(STATUS_READY_FOR_PAYMENT)
                .paymentStatus("UNPAID")
                .paymentAmount(cardPricingService.getPrice("ELEVATOR"))
                .paymentGateway(null)
                .vnpayTransactionRef(null)
                .adminNote(null)
                .rejectionReason(null)
                .approvedAt(null)
                .approvedBy(null)
                .build();

        try {
            // Nếu fullName không có từ DTO, tự động lấy từ user context
            if (!StringUtils.hasText(registration.getFullName())) {
                log.debug("🔍 [ElevatorCard] fullName không có trong DTO, đang lấy từ user context cho residentId: {}, userId: {}, unitId: {}", 
                        dto.residentId(), userId, dto.unitId());
                applyResolvedAddress(registration, dto.residentId(), dto.unitId(), null, dto.apartmentNumber(), dto.buildingName());
                // Đảm bảo fullName luôn được set từ user context
                if (!StringUtils.hasText(registration.getFullName())) {
                    log.warn("⚠️ [ElevatorCard] Không thể lấy fullName từ user context cho residentId: {}, userId: {}", 
                            dto.residentId(), userId);
                    // Thử lấy trực tiếp từ DB một lần nữa với logging chi tiết
                    String fullNameFromDb = getResidentFullNameFromDb(dto.residentId());
                    String fullNameFromUser = getResidentFullNameByUserId(userId);
                    log.warn("⚠️ [ElevatorCard] Debug - fullNameFromDb: {}, fullNameFromUser: {}", fullNameFromDb, fullNameFromUser);
                    if (StringUtils.hasText(fullNameFromDb)) {
                        registration.setFullName(normalize(fullNameFromDb));
                        log.info("✅ [ElevatorCard] Đã lấy fullName từ DB sau khi retry: {}", fullNameFromDb);
                    } else if (StringUtils.hasText(fullNameFromUser)) {
                        registration.setFullName(normalize(fullNameFromUser));
                        log.info("✅ [ElevatorCard] Đã lấy fullName từ userId sau khi retry: {}", fullNameFromUser);
                    } else {
                        throw new IllegalStateException("Không thể lấy thông tin người dùng. Vui lòng thử lại sau.");
                    }
                } else {
                    log.debug("✅ [ElevatorCard] Đã lấy fullName từ user context: {}", registration.getFullName());
                }
            } else {
                log.debug("✅ [ElevatorCard] Đã sử dụng fullName từ DTO: {}", registration.getFullName());
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
            // Nếu không lấy được fullName từ user context, thử lại
            if (!StringUtils.hasText(registration.getFullName())) {
                String fullNameFromDb = getResidentFullNameFromDb(dto.residentId());
                String fullNameFromUser = getResidentFullNameByUserId(userId);
                if (StringUtils.hasText(fullNameFromDb)) {
                    registration.setFullName(normalize(fullNameFromDb));
                    log.info("✅ [ElevatorCard] Đã lấy fullName từ DB trong catch block: {}", fullNameFromDb);
                } else if (StringUtils.hasText(fullNameFromUser)) {
                    registration.setFullName(normalize(fullNameFromUser));
                    log.info("✅ [ElevatorCard] Đã lấy fullName từ userId trong catch block: {}", fullNameFromUser);
                } else {
                    throw new IllegalStateException("Không thể lấy thông tin người dùng. Vui lòng thử lại sau.");
                }
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
        return createAndInitiatePayment(userId, dto, request, null);
    }

    @Transactional
    public ElevatorCardPaymentResponse createAndInitiatePayment(UUID userId,
                                                                ElevatorCardRegistrationCreateDto dto,
                                                                HttpServletRequest request,
                                                                String accessToken) {
        ElevatorCardRegistrationDto created = createRegistration(userId, dto, accessToken);
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

            // Check payment status - must be PAID before approval
            if (!"PAID".equalsIgnoreCase(registration.getPaymentStatus())) {
                throw new IllegalStateException(
                    String.format("Không thể duyệt thẻ. Thẻ phải đã thanh toán trước khi được duyệt. Trạng thái thanh toán hiện tại: %s", 
                        registration.getPaymentStatus())
                );
            }

            registration.setStatus("APPROVED");
            registration.setApprovedBy(adminId);
            registration.setApprovedAt(now);
            registration.setAdminNote(request.note());
            registration.setUpdatedAt(now);

            ElevatorCardRegistration saved = repository.save(registration);

            // Create reminder state if card is already paid (for test mode)
            // In production, reminder state will be created after payment callback
            if ("PAID".equalsIgnoreCase(saved.getPaymentStatus())) {
                try {
                    cardFeeReminderService.resetReminderAfterPayment(
                            CardFeeReminderService.CardFeeType.ELEVATOR,
                            saved.getId(),
                            saved.getUnitId(),
                            saved.getResidentId(),
                            saved.getUserId(),
                            saved.getApartmentNumber(),
                            saved.getBuildingName(),
                            saved.getPaymentDate() != null ? saved.getPaymentDate() : now
                    );
                    log.info("✅ [ElevatorCard] Đã tạo reminder state cho thẻ {} sau khi approve", saved.getId());
                } catch (Exception e) {
                    log.warn("⚠️ [ElevatorCard] Không thể tạo reminder state sau khi approve: {}", e.getMessage());
                }
            }

            // Send notification to resident
            sendElevatorCardApprovalNotification(saved, request.issueMessage(), request.issueTime());

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

            // Send notification to resident
            sendElevatorCardRejectionNotification(saved, request.note());

            log.info("✅ [ElevatorCard] Admin {} đã reject đăng ký {}", adminId, registrationId);
            return toDto(saved);
        } else if ("CANCEL".equalsIgnoreCase(decision) || "CANCELLED".equalsIgnoreCase(decision)) {
            // Admin cancel logic - set status to REJECTED (bị từ chối)
            // Note: Cư dân hủy sẽ set status = CANCELLED, admin hủy sẽ set status = REJECTED
            if (STATUS_REJECTED.equalsIgnoreCase(registration.getStatus())) {
                throw new IllegalStateException("Đăng ký đã bị từ chối");
            }

            registration.setStatus(STATUS_REJECTED);
            registration.setAdminNote(request.note());
            registration.setRejectionReason(request.note());
            registration.setUpdatedAt(now);

            ElevatorCardRegistration saved = repository.save(registration);

            // Send notification to resident (admin cancel = reject)
            sendElevatorCardRejectionNotification(saved, request.note());

            log.info("✅ [ElevatorCard] Admin {} đã cancel (reject) đăng ký {}", adminId, registrationId);
            return toDto(saved);
        } else {
            throw new IllegalArgumentException("Invalid decision: " + decision + ". Must be APPROVE, REJECT, or CANCEL");
        }
    }

    private void sendElevatorCardApprovalNotification(ElevatorCardRegistration registration, String issueMessage, OffsetDateTime issueTime) {
        try {
            // CARD_APPROVED is PRIVATE - only resident who created the request can see
            // Get residentId from userId (người tạo request) instead of residentId (người được đăng ký thẻ)
            UUID requesterResidentId = residentUnitLookupService.resolveByUser(
                    registration.getUserId(), 
                    registration.getUnitId()
            ).map(ResidentUnitLookupService.AddressInfo::residentId).orElse(null);
            
            if (requesterResidentId == null) {
                log.warn("⚠️ [ElevatorCard] Không thể tìm thấy residentId cho userId={}, không thể gửi notification cho registrationId: {}", 
                        registration.getUserId(), registration.getId());
                return;
            }

            // Get payment amount (use actual payment amount if available, otherwise use current price)
            BigDecimal paymentAmount = registration.getPaymentAmount();
            if (paymentAmount == null) {
                paymentAmount = cardPricingService.getPrice("ELEVATOR");
            }
            String formattedPrice = formatVnd(paymentAmount);

            // Get resident full name (người được đăng ký thẻ - từ CCCD mà cư dân đăng ký chọn)
            String residentFullName = registration.getFullName();
            if (residentFullName == null || residentFullName.isBlank()) {
                residentFullName = "cư dân";
            }

            String title = "Thẻ thang máy đã được duyệt";
            
            // Format thời gian nhận thẻ (từ issueTime nếu có, nếu không thì dùng approvedAt)
            String issueTimeFormatted = "";
            OffsetDateTime timeToUse = issueTime != null ? issueTime : registration.getApprovedAt();
            if (timeToUse != null) {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN"));
                issueTimeFormatted = timeToUse.atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"))
                        .format(dateFormatter);
            }
            
            String message;
            if (issueMessage != null && !issueMessage.isBlank()) {
                message = issueMessage;
            } else {
                // Tự động tạo message: "Thẻ cư dân của (họ tên cư dân) tạo thành công sẽ được gửi vào (ngày giờ)"
                if (issueTimeFormatted.isEmpty()) {
                    message = String.format("Thẻ cư dân của %s tạo thành công.", residentFullName);
                } else {
                    message = String.format("Thẻ cư dân của %s tạo thành công sẽ được gửi vào %s.", 
                            residentFullName, issueTimeFormatted);
                }
            }

            Map<String, String> data = new HashMap<>();
            data.put("cardType", "ELEVATOR_CARD");
            data.put("registrationId", registration.getId().toString());
            data.put("price", paymentAmount.toString());
            data.put("formattedPrice", formattedPrice);
            if (registration.getApartmentNumber() != null) {
                data.put("apartmentNumber", registration.getApartmentNumber());
            }
            if (residentFullName != null) {
                data.put("fullName", residentFullName);
            }
            if (!issueTimeFormatted.isEmpty()) {
                data.put("issueTime", issueTimeFormatted);
            }
            if (timeToUse != null) {
                data.put("issueTimeTimestamp", timeToUse.toString());
            }

            // Send PRIVATE notification to requester (người tạo request) only
            // buildingId = null for private notification
            notificationClient.sendResidentNotification(
                    requesterResidentId, // residentId của người tạo request
                    null, // buildingId = null for private notification
                    "CARD_APPROVED",
                    title,
                    message,
                    registration.getId(),
                    "ELEVATOR_CARD_REGISTRATION",
                    data
            );

            log.info("✅ [ElevatorCard] Đã gửi notification approval riêng tư cho requester residentId: {} (userId: {})", 
                    requesterResidentId, registration.getUserId());
        } catch (Exception e) {
            log.error("❌ [ElevatorCard] Không thể gửi notification approval cho registrationId: {}", 
                    registration.getId(), e);
        }
    }

    private void sendElevatorCardRejectionNotification(ElevatorCardRegistration registration, String rejectionReason) {
        try {
            // CARD_REJECTED is PRIVATE - only resident who created the request can see
            // Get residentId from userId (người tạo request) instead of residentId (người được đăng ký thẻ)
            UUID requesterResidentId = residentUnitLookupService.resolveByUser(
                    registration.getUserId(), 
                    registration.getUnitId()
            ).map(ResidentUnitLookupService.AddressInfo::residentId).orElse(null);
            
            if (requesterResidentId == null) {
                log.warn("⚠️ [ElevatorCard] Không thể tìm thấy residentId cho userId={}, không thể gửi notification cho registrationId: {}", 
                        registration.getUserId(), registration.getId());
                return;
            }

            // Get payment amount (use actual payment amount if available, otherwise use current price)
            BigDecimal paymentAmount = registration.getPaymentAmount();
            if (paymentAmount == null) {
                paymentAmount = cardPricingService.getPrice("ELEVATOR");
            }
            String formattedPrice = formatVnd(paymentAmount);

            // Get resident full name (người được đăng ký thẻ)
            String residentFullName = registration.getFullName();
            if (residentFullName == null || residentFullName.isBlank()) {
                residentFullName = "cư dân";
            }

            String title = "Thẻ thang máy bị từ chối";
            String message;
            if (rejectionReason != null && !rejectionReason.isBlank()) {
                message = String.format("Yêu cầu đăng ký thẻ thang máy của %s đã bị từ chối. Phí đăng ký: %s. Lý do: %s", 
                        residentFullName, formattedPrice, rejectionReason);
            } else {
                message = String.format("Yêu cầu đăng ký thẻ thang máy của %s đã bị từ chối. Phí đăng ký: %s. Vui lòng liên hệ quản trị viên để biết thêm chi tiết.", 
                        residentFullName, formattedPrice);
            }

            Map<String, String> data = new HashMap<>();
            data.put("cardType", "ELEVATOR_CARD");
            data.put("registrationId", registration.getId().toString());
            data.put("status", "REJECTED");
            data.put("price", paymentAmount.toString());
            data.put("formattedPrice", formattedPrice);
            if (registration.getApartmentNumber() != null) {
                data.put("apartmentNumber", registration.getApartmentNumber());
            }
            if (residentFullName != null) {
                data.put("fullName", residentFullName);
            }
            if (rejectionReason != null) {
                data.put("rejectionReason", rejectionReason);
            }

            // Send PRIVATE notification to requester (người tạo request) only
            // buildingId = null for private notification
            notificationClient.sendResidentNotification(
                    requesterResidentId, // residentId của người tạo request
                    null, // buildingId = null for private notification
                    "CARD_REJECTED",
                    title,
                    message,
                    registration.getId(),
                    "ELEVATOR_CARD_REGISTRATION",
                    data
            );

            log.info("✅ [ElevatorCard] Đã gửi notification rejection riêng tư cho requester residentId: {} (userId: {})", 
                    requesterResidentId, registration.getUserId());
        } catch (Exception e) {
            log.error("❌ [ElevatorCard] Không thể gửi notification rejection cho registrationId: {}", 
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
        BigDecimal registrationFee = cardPricingService.getPrice("ELEVATOR");
        var paymentResult = vnpayService.createPaymentUrlWithRef(orderId, orderInfo, registrationFee, clientIp, returnUrl);
        
        // Save transaction reference to database for fallback lookup
        saved.setVnpayTransactionRef(paymentResult.transactionRef());
        repository.save(saved);

        return new ElevatorCardPaymentResponse(saved.getId(), paymentResult.paymentUrl());
    }

    @Transactional
    public ElevatorCardPaymentResponse batchInitiatePayment(UUID userId,
                                                           BatchCardPaymentRequest request,
                                                           HttpServletRequest httpRequest) {
        if (request.registrationIds() == null || request.registrationIds().isEmpty()) {
            throw new IllegalArgumentException("Danh sách đăng ký không được để trống");
        }

        // Validate all registrations belong to user and are in valid state
        List<ElevatorCardRegistration> registrations = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (UUID registrationId : request.registrationIds()) {
            ElevatorCardRegistration registration = repository.findByIdAndUserId(registrationId, userId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("Không tìm thấy đăng ký thẻ thang máy: %s", registrationId)));

            // Validate unitId matches (if provided)
            if (request.unitId() != null && registration.getUnitId() != null && 
                !registration.getUnitId().equals(request.unitId())) {
                throw new IllegalArgumentException(
                        String.format("Đăng ký %s không thuộc căn hộ %s", registrationId, request.unitId()));
            }

            // Validate status
            if (STATUS_REJECTED.equalsIgnoreCase(registration.getStatus())) {
                throw new IllegalStateException(
                        String.format("Đăng ký %s đã bị từ chối", registrationId));
            }
            if ("CANCELLED".equalsIgnoreCase(registration.getStatus())) {
                throw new IllegalStateException(
                        String.format("Đăng ký %s đã bị hủy. Vui lòng tạo đăng ký mới.", registrationId));
            }

            String currentStatus = registration.getStatus();
            String paymentStatus = registration.getPaymentStatus();
            
            if (!"NEEDS_RENEWAL".equalsIgnoreCase(currentStatus) && 
                !"SUSPENDED".equalsIgnoreCase(currentStatus)) {
                if (!Objects.equals(paymentStatus, "UNPAID") && 
                    !Objects.equals(paymentStatus, "PAYMENT_PENDING")) {
                    throw new IllegalStateException(
                            String.format("Đăng ký %s đã thanh toán hoặc không thể tiếp tục thanh toán", registrationId));
                }
            }

            // Use paymentAmount from registration, fallback to pricing service if null
            BigDecimal registrationAmount = registration.getPaymentAmount();
            if (registrationAmount == null || registrationAmount.compareTo(BigDecimal.ZERO) <= 0) {
                registrationAmount = cardPricingService.getPrice("ELEVATOR");
                log.warn("⚠️ [ElevatorCard] Registration {} has no paymentAmount, using default price: {}", 
                        registrationId, registrationAmount);
            }

            registrations.add(registration);
            totalAmount = totalAmount.add(registrationAmount);
        }

        // Update all registrations to PAYMENT_PENDING
        String apartmentNumber = registrations.get(0).getApartmentNumber();
        for (ElevatorCardRegistration registration : registrations) {
            registration.setStatus(STATUS_PAYMENT_PENDING);
            registration.setPaymentStatus("PAYMENT_PENDING");
            registration.setPaymentGateway(PAYMENT_VNPAY);
            repository.save(registration);
        }

        // Create single payment URL for all cards
        UUID firstRegistrationId = registrations.get(0).getId();
        long orderId = Math.abs(firstRegistrationId.hashCode());
        if (orderId == 0) {
            orderId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        }
        orderIdToRegistrationId.put(orderId, firstRegistrationId);
        
        String clientIp = resolveClientIp(httpRequest);
        int cardCount = registrations.size();
        String orderInfo = String.format("Thanh toán %d thẻ thang máy %s", 
                cardCount, 
                apartmentNumber != null ? apartmentNumber : firstRegistrationId.toString().substring(0, 8));
        
        String returnUrl = StringUtils.hasText(vnpayProperties.getElevatorReturnUrl())
                ? vnpayProperties.getElevatorReturnUrl()
                : vnpayProperties.getReturnUrl();
        
        log.info("💰 [ElevatorCard] Batch payment calculation: {} cards, totalAmount={} VND", 
                cardCount, totalAmount);
        
        var paymentResult = vnpayService.createPaymentUrlWithRef(orderId, orderInfo, totalAmount, clientIp, returnUrl);
        
        // Save transaction reference to all registrations and set payment status
        String txnRef = paymentResult.transactionRef();
        OffsetDateTime now = OffsetDateTime.now();
        for (ElevatorCardRegistration registration : registrations) {
            registration.setVnpayTransactionRef(txnRef);
            registration.setPaymentStatus("PAYMENT_IN_PROGRESS");
            registration.setVnpayInitiatedAt(now);
            repository.save(registration);
        }

        log.info("✅ [ElevatorCard] Batch payment initiated: {} cards, total amount: {} VND, txnRef: {}", 
                cardCount, totalAmount, txnRef);
        
        return new ElevatorCardPaymentResponse(firstRegistrationId, paymentResult.paymentUrl());
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
            // Handle batch payment: find all registrations with the same txnRef
            List<ElevatorCardRegistration> allRegistrations = repository.findAllByVnpayTransactionRef(txnRef);
            if (allRegistrations.isEmpty()) {
                allRegistrations = List.of(registration);
            }
            
            log.info("✅ [ElevatorCard] Processing payment for {} registration(s) with txnRef: {}", 
                    allRegistrations.size(), txnRef);
            
            // Use current time for payment date to ensure accurate timestamp
            OffsetDateTime payDate = OffsetDateTime.now();
            
            for (ElevatorCardRegistration reg : allRegistrations) {
                reg.setPaymentStatus("PAID");
                reg.setPaymentGateway(PAYMENT_VNPAY);
                reg.setPaymentDate(payDate);
                reg.setVnpayTransactionRef(txnRef);
                
                // Không cần gọi applyResolvedAddress lại vì đã có đầy đủ thông tin khi tạo registration
                // Chỉ cần đảm bảo fullName không null
                if (!StringUtils.hasText(reg.getFullName())) {
                    log.warn("⚠️ [ElevatorCard] fullName is null trong callback, thử lấy lại từ DB");
                    try {
                        String fullNameFromDb = getResidentFullNameFromDb(reg.getResidentId());
                        if (StringUtils.hasText(fullNameFromDb)) {
                            reg.setFullName(fullNameFromDb);
                            log.info("✅ [ElevatorCard] Đã lấy lại fullName từ DB: {}", fullNameFromDb);
                        } else {
                            String fullNameFromUser = getResidentFullNameByUserId(reg.getUserId());
                            if (StringUtils.hasText(fullNameFromUser)) {
                                reg.setFullName(fullNameFromUser);
                                log.info("✅ [ElevatorCard] Đã lấy lại fullName từ userId: {}", fullNameFromUser);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("⚠️ [ElevatorCard] Không thể lấy lại fullName trong callback: {}", e.getMessage());
                    }
                }
                
                // Nếu là gia hạn (status = NEEDS_RENEWAL hoặc SUSPENDED), sau khi thanh toán thành công → set status = APPROVED
                // Nếu là đăng ký mới, sau khi thanh toán → set status = PENDING_REVIEW (chờ admin duyệt)
                String currentStatus = reg.getStatus();
                if ("NEEDS_RENEWAL".equals(currentStatus) || "SUSPENDED".equals(currentStatus)) {
                    reg.setStatus(STATUS_APPROVED);
                    reg.setApprovedAt(OffsetDateTime.now()); // Cập nhật lại approved_at khi gia hạn
                    log.info("✅ [ElevatorCard] Gia hạn thành công, thẻ {} đã được set lại status = APPROVED", reg.getId());
                    
                    // Reset reminder cycle sau khi gia hạn (approved_at đã được set ở trên)
                    try {
                        cardFeeReminderService.resetReminderAfterPayment(
                                CardFeeReminderService.CardFeeType.ELEVATOR,
                                reg.getId(),
                                reg.getUnitId(),
                                reg.getResidentId(),
                                reg.getUserId(),
                                reg.getApartmentNumber(),
                                reg.getBuildingName(),
                                payDate // payment_date mới (approved_at sẽ được lấy từ registration.getApprovedAt())
                        );
                    } catch (Exception e) {
                        log.error("❌ [ElevatorCard] Lỗi khi reset reminder sau gia hạn: {}", e.getMessage(), e);
                        // Không throw exception, chỉ log error để không làm gián đoạn quá trình thanh toán
                    }
                } else {
                    reg.setStatus(STATUS_PENDING_REVIEW);
                }
                repository.save(reg);

                log.info("✅ [ElevatorCard] Thanh toán thành công cho đăng ký {}", reg.getId());
                
                // Ghi nhận thanh toán vào billing service (có thể fail nhưng không nên làm gián đoạn callback)
                try {
                    billingClient.recordElevatorCardPayment(
                            reg.getId(),
                            reg.getUserId(),
                            reg.getUnitId(),
                            reg.getFullName(),
                            reg.getApartmentNumber(),
                            reg.getBuildingName(),
                            reg.getRequestType(),
                            reg.getNote(),
                            reg.getPaymentAmount(),
                            payDate,
                            txnRef,
                            params.get("vnp_TransactionNo"),
                            params.get("vnp_BankCode"),
                            params.get("vnp_CardType"),
                            responseCode
                    );
                    log.info("✅ [ElevatorCard] Đã ghi nhận thanh toán vào billing service cho registration {}", reg.getId());
                } catch (Exception e) {
                    log.error("❌ [ElevatorCard] Lỗi khi ghi nhận thanh toán vào billing service: {}", e.getMessage(), e);
                    // Không throw exception, chỉ log error để không làm gián đoạn quá trình thanh toán
                }

                // Reset reminder cycle sau khi thanh toán
                try {
                    cardFeeReminderService.resetReminderAfterPayment(
                            CardFeeReminderService.CardFeeType.ELEVATOR,
                            reg.getId(),
                            reg.getUnitId(),
                            reg.getResidentId(),
                            reg.getUserId(),
                            reg.getApartmentNumber(),
                            reg.getBuildingName(),
                            payDate
                    );
                    log.info("✅ [ElevatorCard] Đã reset reminder cycle cho registration {}", reg.getId());
                } catch (Exception e) {
                    log.error("❌ [ElevatorCard] Lỗi khi reset reminder cycle: {}", e.getMessage(), e);
                    // Không throw exception, chỉ log error để không làm gián đoạn quá trình thanh toán
                }
            }

            orderIdToRegistrationId.remove(orderId);
            
            // Tạo thông báo thành công dựa trên loại yêu cầu
            String requestType = registration.getRequestType();
            String successMessage;
            if ("RENEWAL".equals(requestType)) {
                successMessage = "Gia hạn thẻ thang máy thành công";
            } else {
                successMessage = "Đăng ký thẻ thang máy thành công";
            }
            
            return new ElevatorCardPaymentResult(
                registration.getId(), 
                true, 
                responseCode, 
                true,
                requestType,
                successMessage
            );
        }

        registration.setStatus(STATUS_READY_FOR_PAYMENT);
        registration.setPaymentStatus("UNPAID");
        repository.save(registration);
        orderIdToRegistrationId.remove(orderId);
        
        String errorMessage = "Thanh toán không thành công. Vui lòng thử lại.";
        return new ElevatorCardPaymentResult(
            registration.getId(), 
            false, 
            responseCode, 
            signatureValid,
            registration.getRequestType(),
            errorMessage
        );
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
            // Nếu không tìm thấy qua resolveByResident, thử lấy fullName trực tiếp từ residents table
            String fullNameFromDb = getResidentFullNameFromDb(residentId);
            if (StringUtils.hasText(fullNameFromDb)) {
                registration.setFullName(normalize(fullNameFromDb));
                log.debug("✅ [ElevatorCard] Lấy fullName từ residents table cho residentId: {}", residentId);
            } else {
                // Nếu không lấy được từ residents table, thử lấy từ userId nếu có
                if (registration.getUserId() != null) {
                    String fullNameFromUser = getResidentFullNameByUserId(registration.getUserId());
                    if (StringUtils.hasText(fullNameFromUser)) {
                        registration.setFullName(normalize(fullNameFromUser));
                        log.debug("✅ [ElevatorCard] Lấy fullName từ userId cho residentId: {}", residentId);
                    } else if (StringUtils.hasText(fallbackFullName)) {
                        registration.setFullName(normalize(fallbackFullName));
                        log.debug("✅ [ElevatorCard] Sử dụng fallback fullName cho residentId: {}", residentId);
                    } else {
                        log.warn("⚠️ [ElevatorCard] Không thể lấy fullName từ database, userId, hoặc fallback cho residentId: {}", residentId);
                    }
                } else if (StringUtils.hasText(fallbackFullName)) {
                    registration.setFullName(normalize(fallbackFullName));
                    log.debug("✅ [ElevatorCard] Sử dụng fallback fullName cho residentId: {}", residentId);
                } else {
                    log.warn("⚠️ [ElevatorCard] Không thể lấy fullName từ database hoặc fallback cho residentId: {}", residentId);
                }
            }
            registration.setApartmentNumber(normalize(fallbackApartment));
            registration.setBuildingName(normalize(fallbackBuilding));
        });
    }
    
    /**
     * Lấy fullName trực tiếp từ bảng residents
     */
    private String getResidentFullNameFromDb(UUID residentId) {
        if (residentId == null) {
            return null;
        }
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("residentId", residentId);
            
            List<String> results = jdbcTemplate.queryForList("""
                    SELECT full_name
                    FROM data.residents
                    WHERE id = :residentId
                    LIMIT 1
                    """, params, String.class);
            
            if (results != null && !results.isEmpty()) {
                String fullName = results.get(0);
                log.debug("✅ [ElevatorCard] Tìm thấy fullName trong residents table: {} cho residentId: {}", fullName, residentId);
                return fullName;
            }
            log.debug("⚠️ [ElevatorCard] Không tìm thấy fullName trong residents table cho residentId: {}", residentId);
            return null;
        } catch (Exception e) {
            log.warn("⚠️ [ElevatorCard] Lỗi khi lấy fullName từ residents table cho residentId {}: {}", 
                    residentId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Lấy fullName từ residents table thông qua userId
     */
    private String getResidentFullNameByUserId(UUID userId) {
        if (userId == null) {
            return null;
        }
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId);
            
            List<String> results = jdbcTemplate.queryForList("""
                    SELECT full_name
                    FROM data.residents
                    WHERE user_id = :userId
                    LIMIT 1
                    """, params, String.class);
            
            if (results != null && !results.isEmpty()) {
                String fullName = results.get(0);
                log.debug("✅ [ElevatorCard] Tìm thấy fullName qua userId: {} cho userId: {}", fullName, userId);
                return fullName;
            }
            log.debug("⚠️ [ElevatorCard] Không tìm thấy fullName qua userId: {}", userId);
            return null;
        } catch (Exception e) {
            log.warn("⚠️ [ElevatorCard] Lỗi khi lấy fullName qua userId {}: {}", userId, e.getMessage());
            return null;
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
        
        // Nếu resolveByResident tìm thấy, đã OK
        if (info.isPresent()) {
            log.debug("✅ [ElevatorCard] Resident {} validated qua resolveByResident cho unit {}", residentId, unitId);
            return;
        }
        
        // Nếu không tìm thấy qua resolveByResident, kiểm tra xem có phải primary resident không
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("residentId", residentId)
                    .addValue("unitId", unitId);
            
            // Kiểm tra xem resident có phải là primaryResidentId của unit không
            Long primaryResidentCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM data.households h
                    WHERE h.unit_id = :unitId
                      AND h.primary_resident_id = :residentId
                      AND (h.end_date IS NULL OR h.end_date >= CURRENT_DATE)
                    """, params, Long.class);
            
            if (primaryResidentCount != null && primaryResidentCount > 0) {
                log.debug("✅ [ElevatorCard] Resident {} là primaryResidentId của unit {}, cho phép tiếp tục", residentId, unitId);
                return; // Primary resident được phép, không cần có trong household_members
            }
            
            // Kiểm tra chi tiết trong household_members
            List<Map<String, Object>> details = jdbcTemplate.queryForList("""
                    SELECT 
                        hm.id as member_id,
                        hm.resident_id,
                        hm.left_at,
                        h.id as household_id,
                        h.unit_id,
                        h.end_date,
                        CASE 
                            WHEN hm.left_at IS NOT NULL AND hm.left_at < CURRENT_DATE THEN 'RESIDENT_LEFT'
                            WHEN h.end_date IS NOT NULL AND h.end_date < CURRENT_DATE THEN 'HOUSEHOLD_ENDED'
                            WHEN hm.id IS NULL THEN 'NOT_IN_HOUSEHOLD'
                            ELSE 'ACTIVE'
                        END as status
                    FROM data.household_members hm
                    JOIN data.households h ON h.id = hm.household_id
                    WHERE hm.resident_id = :residentId
                      AND h.unit_id = :unitId
                    """, params);
            
            if (details.isEmpty()) {
                log.warn("⚠️ [ElevatorCard] Resident {} không có trong bất kỳ household nào của unit {} và không phải primaryResidentId", residentId, unitId);
                throw new IllegalArgumentException(
                    String.format("Cư dân không thuộc căn hộ này. Vui lòng kiểm tra lại thông tin căn hộ và cư dân.")
                );
            }
            
            // Kiểm tra xem có record nào active không
            Long activeCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM data.household_members hm
                    JOIN data.households h ON h.id = hm.household_id
                    WHERE hm.resident_id = :residentId
                      AND h.unit_id = :unitId
                      AND (hm.left_at IS NULL OR hm.left_at >= CURRENT_DATE)
                      AND (h.end_date IS NULL OR h.end_date >= CURRENT_DATE)
                    """, params, Long.class);
            
            if (activeCount == null || activeCount == 0) {
                // Tìm lý do cụ thể
                String reason = "không xác định";
                for (Map<String, Object> detail : details) {
                    String status = (String) detail.get("status");
                    if ("RESIDENT_LEFT".equals(status)) {
                        Object leftAt = detail.get("left_at");
                        reason = String.format("cư dân đã rời khỏi căn hộ vào ngày %s", leftAt);
                        break;
                    } else if ("HOUSEHOLD_ENDED".equals(status)) {
                        Object endDate = detail.get("end_date");
                        reason = String.format("hộ gia đình đã kết thúc vào ngày %s", endDate);
                        break;
                    }
                }
                
                log.warn("⚠️ [ElevatorCard] Resident {} không active trong unit {} - Lý do: {}", residentId, unitId, reason);
                throw new IllegalArgumentException(
                    String.format("Cư dân không thuộc căn hộ này hoặc đã rời khỏi căn hộ (%s). Vui lòng kiểm tra lại.", reason)
                );
            }
            
            log.debug("✅ [ElevatorCard] Resident {} validated cho unit {}", residentId, unitId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ [ElevatorCard] Lỗi khi validate resident-unit relationship: {}", e.getMessage(), e);
            // Nếu có lỗi khi query, vẫn cho phép tiếp tục (fallback) nhưng log warning
            log.warn("⚠️ [ElevatorCard] Fallback: cho phép tiếp tục do lỗi query, nhưng nên kiểm tra lại dữ liệu");
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

    public record ElevatorCardPaymentResult(UUID registrationId, boolean success, String responseCode, boolean signatureValid, String requestType, String message) {
        public ElevatorCardPaymentResult(UUID registrationId, boolean success, String responseCode, boolean signatureValid) {
            this(registrationId, success, responseCode, signatureValid, null, null);
        }
    }

    /**
     * Lấy danh sách thành viên trong căn hộ (bao gồm chủ căn hộ và household members)
     * Tương tự như ResidentCard nhưng check thẻ thang máy thay vì thẻ cư dân
     */
    public List<Map<String, Object>> getHouseholdMembersByUnit(UUID unitId) {
        if (unitId == null) {
            log.warn("⚠️ [ElevatorCard] getHouseholdMembersByUnit called with null unitId");
            return List.of();
        }
        
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("unitId", unitId);
            
            log.debug("🔍 [ElevatorCard] Đang lấy danh sách thành viên trong căn hộ unitId: {}", unitId);
            
            // Query để lấy danh sách thành viên và check xem họ đã có thẻ thang máy được approve chưa
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
                                SELECT 1 FROM card.elevator_card_registration ecr
                                WHERE ecr.resident_id = r.id
                                  AND ecr.status IN ('APPROVED', 'ACTIVE', 'ISSUED', 'COMPLETED')
                            ) THEN true
                            ELSE false
                        END AS has_approved_card,
                        CASE
                            WHEN EXISTS (
                                SELECT 1 FROM card.elevator_card_registration ecr
                                WHERE ecr.resident_id = r.id
                                  AND ecr.status IN ('PENDING', 'REVIEW_PENDING', 'PROCESSING', 'IN_PROGRESS', 'READY_FOR_PAYMENT')
                                  AND ecr.payment_status = 'PAID'
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
            
            log.info("✅ [ElevatorCard] Căn hộ {} có {} thành viên", unitId, members.size());
            return members;
        } catch (Exception e) {
            log.error("❌ [ElevatorCard] Không thể lấy danh sách thành viên trong căn hộ unitId: {}", unitId, e);
            return List.of();
        }
    }

    /**
     * Format BigDecimal price to VND string (e.g., 30000 -> "30.000 VND")
     */
    private String formatVnd(BigDecimal amount) {
        if (amount == null) {
            return "0 VND";
        }
        String digits = amount.toBigInteger().toString();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            buffer.append(digits.charAt(i));
            int remaining = digits.length() - i - 1;
            if (remaining % 3 == 0 && remaining != 0) {
                buffer.append(".");
            }
        }
        buffer.append(" VND");
        return buffer.toString();
    }
}


