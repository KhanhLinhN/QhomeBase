package com.qhomebaseapp.service.elevatorcard;

import com.qhomebaseapp.config.VnpayProperties;
import com.qhomebaseapp.dto.elevatorcard.ElevatorCardRegistrationDto;
import com.qhomebaseapp.dto.elevatorcard.ElevatorCardRegistrationResponseDto;
import com.qhomebaseapp.mapper.ElevatorCardRegistrationMapper;
import com.qhomebaseapp.mapper.ElevatorCardRegistrationResponseMapper;
import com.qhomebaseapp.model.ElevatorCardRegistration;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.repository.UserRepository;
import com.qhomebaseapp.repository.elevatorcard.ElevatorCardRegistrationRepository;
import com.qhomebaseapp.service.user.EmailService;
import com.qhomebaseapp.service.vnpay.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElevatorCardRegistrationServiceImpl implements ElevatorCardRegistrationService {

    private final ElevatorCardRegistrationRepository repository;
    private final UserRepository userRepository;
    private final ElevatorCardRegistrationMapper mapper;
    private final ElevatorCardRegistrationResponseMapper responseMapper;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;
    private final EmailService emailService;
    private static final BigDecimal REGISTRATION_FEE = new BigDecimal("30000"); // 30,000 VNĐ

    @Override
    public ElevatorCardRegistrationResponseDto registerElevatorCard(ElevatorCardRegistrationDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        ElevatorCardRegistration entity = mapper.toEntity(dto);
        entity.setUser(user);
        entity.setStatus("PENDING");
        entity.setPaymentStatus("UNPAID");
        entity.setPaymentAmount(REGISTRATION_FEE);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        ElevatorCardRegistration saved = repository.save(entity);
        return responseMapper.toDto(saved);
    }

    @Override
    public Map<String, Object> createVnpayPaymentUrlWithData(ElevatorCardRegistrationDto dto, Long userId, HttpServletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        ElevatorCardRegistration entity = mapper.toEntity(dto);
        entity.setUser(user);
        entity.setStatus("PENDING");
        entity.setPaymentStatus("UNPAID");
        entity.setPaymentAmount(REGISTRATION_FEE);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        ElevatorCardRegistration saved = repository.save(entity);
        Long registrationId = saved.getId();

        log.info("💳 [ElevatorCard] Tạo registration {} để thanh toán VNPAY", registrationId);

        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }

        String orderInfo = "Thanh toán phí đăng ký thẻ thang máy #" + registrationId;
        String baseUrl = vnpayProperties.getReturnUrl().replace("/api/invoices/vnpay/redirect", "");
        String returnUrl = baseUrl + "/api/elevator-card/vnpay/redirect";
        
        String paymentUrl = vnpayService.createPaymentUrl(registrationId, orderInfo, REGISTRATION_FEE, clientIp, returnUrl);
        
        log.info("💳 [ElevatorCard] Tạo VNPAY URL cho registration: {}, userId: {}", registrationId, userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("registrationId", registrationId);
        result.put("paymentUrl", paymentUrl);
        
        return result;
    }

    @Override
    public void cancelRegistration(Long registrationId, Long userId) {
        ElevatorCardRegistration registration = repository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        if (!registration.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot cancel others' registration");
        }

        if ("PAID".equalsIgnoreCase(registration.getPaymentStatus())) {
            log.warn("⚠️ [ElevatorCard] Không thể hủy registration {} - đã thanh toán thành công", registrationId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể hủy đăng ký đã thanh toán thành công");
        }
        
        registration.setPaymentStatus("UNPAID");
        registration.setStatus("PENDING");
        registration.setUpdatedAt(OffsetDateTime.now());
        repository.save(registration);
        
        log.info("🔄 [ElevatorCard] Đã update registration {} thành payment_status UNPAID", registrationId);
    }

    @Override
    public List<ElevatorCardRegistrationResponseDto> getByUserId(Long userId) {
        List<ElevatorCardRegistration> list = repository.findByUser_Id(userId);
        return list.stream()
                .map(responseMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ElevatorCardRegistrationResponseDto getById(Long id, Long userId) {
        ElevatorCardRegistration entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        if (!entity.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot access others' registration");
        }

        return responseMapper.toDto(entity);
    }

    @Override
    public Page<ElevatorCardRegistrationResponseDto> getByUserIdPaginated(Long userId, int page, int size) {
        Page<ElevatorCardRegistration> pageResult =
                repository.findByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));

        return pageResult.map(responseMapper::toDto);
    }

    @Override
    public String createVnpayPaymentUrl(Long registrationId, Long userId, HttpServletRequest request) {
        ElevatorCardRegistration registration = repository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        if (!registration.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot pay for others' registration");
        }

        if ("PAID".equalsIgnoreCase(registration.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đã thanh toán rồi");
        }

        if (!"UNPAID".equalsIgnoreCase(registration.getPaymentStatus())) {
            registration.setPaymentStatus("PENDING");
            repository.save(registration);
        }

        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }

        String orderInfo = "Thanh toán phí đăng ký thẻ thang máy #" + registrationId;
        BigDecimal amount = registration.getPaymentAmount() != null 
                ? registration.getPaymentAmount() 
                : REGISTRATION_FEE;

        String baseUrl = vnpayProperties.getReturnUrl().replace("/api/invoices/vnpay/redirect", "");
        String returnUrl = baseUrl + "/api/elevator-card/vnpay/redirect";
        
        String paymentUrl = vnpayService.createPaymentUrl(registrationId, orderInfo, amount, clientIp, returnUrl);
        log.info("💳 [ElevatorCard] Tạo VNPAY URL cho registration: {}, userId: {}", registrationId, userId);
        
        return paymentUrl;
    }

    @Override
    public void handleVnpayCallback(Long registrationId, Map<String, String> vnpParams) {
        ElevatorCardRegistration registration = repository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        boolean valid = vnpayService.validateReturn(new HashMap<>(vnpParams));
        String responseCode = vnpParams.get("vnp_ResponseCode");
        String transactionStatus = vnpParams.get("vnp_TransactionStatus");
        String txnRef = vnpParams.get("vnp_TxnRef");

        log.info("💳 [ElevatorCard] VNPAY callback cho registration: {}", registrationId);
        log.info("💳 [ElevatorCard] Valid: {}, ResponseCode: {}, TransactionStatus: {}", valid, responseCode, transactionStatus);

        if (valid && "00".equals(responseCode) && "00".equals(transactionStatus)) {
            OffsetDateTime paymentDateNow = OffsetDateTime.now();
            registration.setPaymentStatus("PAID");
            registration.setStatus("PENDING");
            registration.setPaymentDate(paymentDateNow);
            registration.setPaymentGateway("VNPAY");
            registration.setVnpayTransactionRef(txnRef);
            registration.setUpdatedAt(OffsetDateTime.now());
            
            repository.saveAndFlush(registration);
            
            log.info("✅ [ElevatorCard] Đã cập nhật registration {} với payment_status PAID", registrationId);
            
            // Gửi email thông báo thanh toán thành công
            try {
                User user = registration.getUser();
                if (user != null && user.getEmail() != null) {
                    String emailSubject = "Thanh toán thành công - Đăng ký thẻ thang máy";
                    String paymentDateStr = paymentDateNow.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                    NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
                    String amountStr = currencyFormat.format(REGISTRATION_FEE) + " VNĐ";
                    
                    String requestTypeName = "NEW_CARD".equalsIgnoreCase(registration.getRequestType()) 
                            ? "Làm thẻ mới" 
                            : "Cấp lại thẻ bị mất";
                    
                    StringBuilder serviceDetails = new StringBuilder();
                    serviceDetails.append("- Họ và tên: ").append(registration.getFullName()).append("\n");
                    if (registration.getApartmentNumber() != null && registration.getBuildingName() != null) {
                        serviceDetails.append("- Địa chỉ: ").append(registration.getApartmentNumber())
                                .append(", ").append(registration.getBuildingName()).append("\n");
                    }
                    serviceDetails.append("- Loại yêu cầu: ").append(requestTypeName).append("\n");
                    if (registration.getCitizenId() != null) {
                        serviceDetails.append("- Căn cước công dân: ").append(registration.getCitizenId()).append("\n");
                    }
                    if (registration.getPhoneNumber() != null) {
                        serviceDetails.append("- Số điện thoại: ").append(registration.getPhoneNumber()).append("\n");
                    }
                    
                    String emailBody = String.format(
                        "Xin chào %s,\n\n" +
                        "Thanh toán đăng ký thẻ thang máy của bạn đã được xử lý thành công!\n\n" +
                        "Thông tin đăng ký:\n" +
                        "%s" +
                        "Thông tin thanh toán:\n" +
                        "- Tổng số tiền: %s\n" +
                        "- Ngày giờ thanh toán: %s\n" +
                        "- Phương thức thanh toán: VNPAY\n" +
                        "- Mã giao dịch: %s\n\n" +
                        "Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!\n\n" +
                        "Trân trọng,\n" +
                        "Hệ thống QHomeBase",
                        user.getEmail().split("@")[0],
                        serviceDetails.toString(),
                        amountStr,
                        paymentDateStr,
                        txnRef != null ? txnRef : "N/A"
                    );
                    
                    emailService.sendEmail(user.getEmail(), emailSubject, emailBody);
                    log.info("✅ [ElevatorCard] Đã gửi email thông báo thanh toán thành công cho user: {}", user.getEmail());
                }
            } catch (Exception e) {
                log.error("❌ [ElevatorCard] Lỗi khi gửi email thông báo thanh toán: {}", e.getMessage(), e);
            }
        } else {
            registration.setPaymentStatus("UNPAID");
            registration.setStatus("PENDING");
            registration.setUpdatedAt(OffsetDateTime.now());
            
            repository.save(registration);
            
            log.warn("❌ [ElevatorCard] Thanh toán thất bại cho registration {} - giữ lại với payment_status UNPAID", registrationId);
            throw new RuntimeException("Thanh toán thất bại hoặc chữ ký không hợp lệ");
        }
    }
}

