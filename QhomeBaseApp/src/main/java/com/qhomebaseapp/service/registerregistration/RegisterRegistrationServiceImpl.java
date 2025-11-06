package com.qhomebaseapp.service.registerregistration;

import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestDto;
import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestResponseDto;
import com.qhomebaseapp.mapper.RegisterServiceRequestMapper;
import com.qhomebaseapp.mapper.RegisterServiceRequestResponseMapper;
import com.qhomebaseapp.model.RegisterServiceImage;
import com.qhomebaseapp.model.RegisterServiceRequest;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.repository.UserRepository;
import com.qhomebaseapp.repository.registerregistration.RegisterRegistrationRepository;
import com.qhomebaseapp.config.VnpayProperties;
import com.qhomebaseapp.service.vnpay.VnpayService;
import com.qhomebaseapp.service.user.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
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
public class RegisterRegistrationServiceImpl implements RegisterRegistrationService {

    private final RegisterRegistrationRepository repository;
    private final UserRepository userRepository;
    private final RegisterServiceRequestMapper registerServiceRequestMapper;
    private final RegisterServiceRequestResponseMapper registerServiceRequestResponseMapper;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;
    private final EmailService emailService;
    private static final java.math.BigDecimal REGISTRATION_FEE = new java.math.BigDecimal("30000"); // 30,000 VNĐ

    @Override
    public RegisterServiceRequestResponseDto registerService(RegisterServiceRequestDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        RegisterServiceRequest entity = registerServiceRequestMapper.toEntity(dto);
        entity.setUser(user);
        entity.setStatus("PENDING"); // PENDING - mặc định (admin chưa xử lý)
        entity.setPaymentStatus("UNPAID");
        entity.setPaymentAmount(REGISTRATION_FEE); // 30,000 VNĐ
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            dto.getImageUrls().forEach(url -> {
                RegisterServiceImage image = RegisterServiceImage.builder()
                        .registerServiceRequest(entity)
                        .imageUrl(url)
                        .createdAt(OffsetDateTime.now())
                        .build();
                entity.getImages().add(image);
            });
        }

        RegisterServiceRequest saved = repository.save(entity);
        return registerServiceRequestResponseMapper.toDto(saved);
    }

    /**
     * Tạo VNPAY payment URL với data, tạo temporary registration với status DRAFT
     * Chỉ chuyển sang PENDING khi thanh toán thành công
     * Trả về Map chứa registrationId và paymentUrl
     */
    @Override
    public Map<String, Object> createVnpayPaymentUrlWithData(RegisterServiceRequestDto dto, Long userId, HttpServletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Tạo registration với status PENDING (mặc định), payment_status UNPAID
        // Sẽ được lưu vào DB ngay cả khi hủy hoặc crash
        RegisterServiceRequest entity = registerServiceRequestMapper.toEntity(dto);
        entity.setUser(user);
        entity.setStatus("PENDING"); // PENDING - mặc định (admin chưa xử lý)
        entity.setPaymentStatus("UNPAID"); // UNPAID - chưa thanh toán
        entity.setPaymentAmount(REGISTRATION_FEE); // 30,000 VNĐ
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            dto.getImageUrls().forEach(url -> {
                RegisterServiceImage image = RegisterServiceImage.builder()
                        .registerServiceRequest(entity)
                        .imageUrl(url)
                        .createdAt(OffsetDateTime.now())
                        .build();
                entity.getImages().add(image);
            });
        }

        // Lưu registration vào DB (sẽ giữ lại ngay cả khi hủy hoặc crash)
        RegisterServiceRequest saved = repository.save(entity);
        Long registrationId = saved.getId();

        log.info("💳 [RegisterService] Tạo registration {} để thanh toán VNPAY", registrationId);

        // Tạo VNPAY payment URL
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }

        String orderInfo = "Thanh toán phí đăng ký thẻ xe #" + registrationId;
        String baseUrl = vnpayProperties.getReturnUrl().replace("/api/invoices/vnpay/redirect", "");
        String registerReturnUrl = baseUrl + "/api/register-service/vnpay/redirect";
        
        String paymentUrl = vnpayService.createPaymentUrl(registrationId, orderInfo, REGISTRATION_FEE, clientIp, registerReturnUrl);
        
        log.info("💳 [RegisterService] Tạo VNPAY URL cho registration: {}, userId: {}", registrationId, userId);
        
        // Trả về Map chứa registrationId và paymentUrl
        Map<String, Object> result = new HashMap<>();
        result.put("registrationId", Long.valueOf(registrationId));
        result.put("paymentUrl", paymentUrl);
        
        return result;
    }

    /**
     * Xóa temporary registration nếu thanh toán bị hủy
     */
    @Override
    public void cancelRegistration(Long registrationId, Long userId) {
        RegisterServiceRequest registration = repository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        if (!registration.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot cancel others' registration");
        }

        // Trường hợp 2: Hủy thanh toán - update payment_status thành UNPAID, giữ lại registration
        if ("PAID".equalsIgnoreCase(registration.getPaymentStatus())) {
            log.warn("⚠️ [RegisterService] Không thể hủy registration {} - đã thanh toán thành công", registrationId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể hủy đăng ký đã thanh toán thành công");
        }
        
        // Update payment_status thành UNPAID để có thể thanh toán sau
        registration.setPaymentStatus("UNPAID");
        registration.setStatus("PENDING"); // Giữ status PENDING
        registration.setUpdatedAt(OffsetDateTime.now());
        repository.save(registration);
        
        log.info("🔄 [RegisterService] Đã update registration {} thành payment_status UNPAID (hủy thanh toán)", registrationId);
    }


    @Override
    public List<RegisterServiceRequestResponseDto> getByUserId(Long userId) {
        List<RegisterServiceRequest> list = repository.findByUser_Id(userId);
        return list.stream()
                .map(registerServiceRequestResponseMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public RegisterServiceRequestResponseDto getById(Long id, Long userId) {
        RegisterServiceRequest entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        if (!entity.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot access others' registration");
        }

        return registerServiceRequestResponseMapper.toDto(entity);
    }

    @Override
    public RegisterServiceRequestResponseDto updateRegistration(Long id, RegisterServiceRequestDto dto, Long userId) {
        RegisterServiceRequest entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        if (!entity.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify others’ registration");
        }

        if (dto.getNote() != null) entity.setNote(dto.getNote());
        if (dto.getVehicleBrand() != null) entity.setVehicleBrand(dto.getVehicleBrand());
        if (dto.getVehicleColor() != null) entity.setVehicleColor(dto.getVehicleColor());
        if (dto.getLicensePlate() != null) entity.setLicensePlate(dto.getLicensePlate());
        if (dto.getVehicleType() != null) entity.setVehicleType(dto.getVehicleType());

        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {

            entity.getImages().clear();

            dto.getImageUrls().forEach(url -> {
                RegisterServiceImage image = RegisterServiceImage.builder()
                        .registerServiceRequest(entity)
                        .imageUrl(url)
                        .createdAt(OffsetDateTime.now())
                        .build();
                entity.getImages().add(image);
            });
        }

        entity.setUpdatedAt(OffsetDateTime.now());
        RegisterServiceRequest updated = repository.save(entity);

        return registerServiceRequestResponseMapper.toDto(updated);
    }


    @Override
    public List<String> uploadVehicleImages(List<MultipartFile> files, Long userId) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files uploaded");
        }

        try {
            String uploadDir = "uploads/vehicles/";
            Files.createDirectories(Path.of(uploadDir));

            List<String> urls = files.stream().map(file -> {
                String fileName = "vehicle_" + userId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path filePath = Path.of(uploadDir + fileName);
                try {
                    file.transferTo(filePath);
                } catch (IOException e) {
                    throw new RuntimeException("Error saving file: " + fileName);
                }
                return "/uploads/vehicles/" + fileName;
            }).collect(Collectors.toList());

            log.info("User {} uploaded {} vehicle images", userId, urls.size());
            return urls;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error uploading images: " + e.getMessage());
        }
    }

    @Override
    public Page<RegisterServiceRequestResponseDto> getByUserIdPaginated(Long userId, int page, int size) {
        Page<RegisterServiceRequest> pageResult =
                repository.findByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));

        return pageResult.map(registerServiceRequestResponseMapper::toDto);
    }

    @Override
    public String createVnpayPaymentUrl(Long registrationId, Long userId, HttpServletRequest request) {
        RegisterServiceRequest registration = repository.findById(registrationId)
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

        String orderInfo = "Thanh toán phí đăng ký thẻ xe #" + registrationId;
        BigDecimal amount = registration.getPaymentAmount() != null 
                ? registration.getPaymentAmount() 
                : REGISTRATION_FEE;

        // Sử dụng returnUrl riêng cho register-service
        // Lấy ngrok base URL từ returnUrl hiện tại và thay đổi endpoint
        String baseUrl = vnpayProperties.getReturnUrl().replace("/api/invoices/vnpay/redirect", "");
        String registerReturnUrl = baseUrl + "/api/register-service/vnpay/redirect";
        
        String paymentUrl = vnpayService.createPaymentUrl(registrationId, orderInfo, amount, clientIp, registerReturnUrl);
        log.info("💳 [RegisterService] Tạo VNPAY URL cho registration: {}, userId: {}", registrationId, userId);
        
        return paymentUrl;
    }

    @Override
    public void handleVnpayCallback(Long registrationId, Map<String, String> vnpParams) {
        RegisterServiceRequest registration = repository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        boolean valid = vnpayService.validateReturn(new HashMap<>(vnpParams));
        String responseCode = vnpParams.get("vnp_ResponseCode");
        String transactionStatus = vnpParams.get("vnp_TransactionStatus");
        String txnRef = vnpParams.get("vnp_TxnRef");

        log.info("💳 [RegisterService] VNPAY callback cho registration: {}", registrationId);
        log.info("💳 [RegisterService] Valid: {}, ResponseCode: {}, TransactionStatus: {}", valid, responseCode, transactionStatus);
        log.info("💳 [RegisterService] Current registration - Status: {}, PaymentStatus: {}, PaymentDate: {}, Gateway: {}", 
                registration.getStatus(), registration.getPaymentStatus(), registration.getPaymentDate(), registration.getPaymentGateway());

        if (valid && "00".equals(responseCode) && "00".equals(transactionStatus)) {
            // Trường hợp 1: Thanh toán thành công → update payment_status PAID, giữ status PENDING
            log.info("✅ [RegisterService] Bắt đầu cập nhật registration {} - Thanh toán thành công!", registrationId);
            log.info("✅ [RegisterService] Before update - PaymentStatus: {}, PaymentDate: {}, Gateway: {}, TxnRef: {}", 
                    registration.getPaymentStatus(), registration.getPaymentDate(), registration.getPaymentGateway(), registration.getVnpayTransactionRef());
            
            OffsetDateTime paymentDateNow = OffsetDateTime.now();
            registration.setPaymentStatus("PAID");
            registration.setStatus("PENDING"); // Giữ status PENDING
            registration.setPaymentDate(paymentDateNow);
            registration.setPaymentGateway("VNPAY");
            registration.setVnpayTransactionRef(txnRef);
            registration.setUpdatedAt(OffsetDateTime.now());
            
            log.info("✅ [RegisterService] Set values - PaymentStatus: PAID, PaymentDate: {}, Gateway: VNPAY, TxnRef: {}", 
                    paymentDateNow, txnRef);
            
            RegisterServiceRequest saved = repository.saveAndFlush(registration); // Use saveAndFlush để đảm bảo persist ngay lập tức
            
            log.info("✅ [RegisterService] Saved registration - ID: {}, PaymentStatus: {}, PaymentDate: {}, Gateway: {}, TxnRef: {}", 
                    saved.getId(), saved.getPaymentStatus(), saved.getPaymentDate(), saved.getPaymentGateway(), saved.getVnpayTransactionRef());
            
            // Verify sau khi save
            RegisterServiceRequest verified = repository.findById(registrationId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy registration sau khi save"));
            
            log.info("✅ [RegisterService] Đã cập nhật registration {} với payment_status PAID (thanh toán thành công)", registrationId);
            log.info("✅ [RegisterService] Verified Payment details - Status: {}, PaymentStatus: {}, Date: {}, Gateway: {}, TxnRef: {}", 
                    verified.getStatus(), verified.getPaymentStatus(), verified.getPaymentDate(), 
                    verified.getPaymentGateway(), verified.getVnpayTransactionRef());
            
            // Gửi email thông báo thanh toán thành công
            try {
                User user = registration.getUser();
                if (user != null && user.getEmail() != null) {
                    String emailSubject = "Thanh toán thành công - Đăng ký thẻ xe";
                    String paymentDateStr = paymentDateNow.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                    NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
                    String amountStr = currencyFormat.format(REGISTRATION_FEE) + " VNĐ";
                    
                    StringBuilder serviceDetails = new StringBuilder();
                    if (registration.getLicensePlate() != null) {
                        serviceDetails.append("- Biển số xe: ").append(registration.getLicensePlate()).append("\n");
                    }
                    if (registration.getVehicleType() != null) {
                        serviceDetails.append("- Loại xe: ").append(registration.getVehicleType()).append("\n");
                    }
                    if (registration.getRequestType() != null) {
                        String requestTypeName = "NEW_CARD".equalsIgnoreCase(registration.getRequestType())
                                ? "Làm thẻ mới"
                                : "Cấp lại thẻ bị mất";
                        serviceDetails.append("- Loại yêu cầu: ").append(requestTypeName).append("\n");
                    }
                    
                    String emailBody = String.format(
                        "Xin chào %s,\n\n" +
                        "Thanh toán đăng ký thẻ xe của bạn đã được xử lý thành công!\n\n" +
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
                    log.info("✅ [RegisterService] Đã gửi email thông báo thanh toán thành công cho user: {}", user.getEmail());
                }
            } catch (Exception e) {
                log.error("❌ [RegisterService] Lỗi khi gửi email thông báo thanh toán: {}", e.getMessage(), e);
                // Không throw exception để không ảnh hưởng đến flow thanh toán
            }
        } else {
            // Trường hợp 2: Thanh toán thất bại - giữ lại registration với payment_status UNPAID
            registration.setPaymentStatus("UNPAID");
            registration.setStatus("PENDING"); // Giữ status PENDING
            registration.setUpdatedAt(OffsetDateTime.now());
            
            repository.save(registration);
            
            log.warn("❌ [RegisterService] Thanh toán thất bại cho registration {} - giữ lại với payment_status UNPAID để thanh toán sau", registrationId);
            throw new RuntimeException("Thanh toán thất bại hoặc chữ ký không hợp lệ");
        }
    }
}
