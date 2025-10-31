package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestDto;
import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestResponseDto;
import com.qhomebaseapp.security.CustomUserDetails;
import com.qhomebaseapp.service.registerregistration.RegisterRegistrationService;
import com.qhomebaseapp.service.vnpay.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@Slf4j
@RestController
@RequestMapping("/api/register-service")
@RequiredArgsConstructor
public class RegisterRegistrationController {

    private final RegisterRegistrationService service;
    private final VnpayService vnpayService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RegisterServiceRequestResponseDto> register(
            @RequestBody RegisterServiceRequestDto dto,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        RegisterServiceRequestResponseDto result = service.registerService(dto, userId);

        log.info("User {} registered service {}", userId, dto.getServiceType());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RegisterServiceRequestResponseDto>> getByUser(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<RegisterServiceRequestResponseDto> list = service.getByUserId(userId);
        log.info("User {} fetched their registered services, count={}", userId, list.size());
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RegisterServiceRequestResponseDto> updateRegistration(
            @PathVariable Long id,
            @RequestBody RegisterServiceRequestDto dto,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        RegisterServiceRequestResponseDto result = service.updateRegistration(id, dto, userId);

        log.info("User {} updated registration {}", userId, id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/upload-images")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadImages(
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        List<String> imageUrls = service.uploadVehicleImages(files, userId);
        return ResponseEntity.ok(Map.of("imageUrls", imageUrls));
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails customUser) {
            return customUser.getUserId();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found in authentication");
    }

    @GetMapping("/me/paginated")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getByUserPaginated(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        int pageIndex = page > 0 ? page - 1 : 0;

        Page<RegisterServiceRequestResponseDto> result = service.getByUserIdPaginated(userId, pageIndex, size);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Danh sách thẻ xe đã đăng ký",
                "data", result.getContent(),
                "totalPages", result.getTotalPages(),
                "totalElements", result.getTotalElements(),
                "currentPage", page
        ));
    }

    /**
     * Tạo VNPAY payment URL cho đăng ký xe
     * POST /api/register-service/{id}/vnpay-url
     */
    @PostMapping("/{id}/vnpay-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createVnpayUrl(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        
        try {
            log.info("💳 [RegisterController] Tạo VNPAY URL cho registration: {}, userId: {}", id, userId);
            
            String paymentUrl = service.createVnpayPaymentUrl(id, userId, request);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tạo URL thanh toán thành công",
                    "paymentUrl", paymentUrl
            ));
        } catch (ResponseStatusException ex) {
            log.error("❌ [RegisterController] Lỗi tạo VNPAY URL cho registration: {}", id, ex);
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                    "success", false,
                    "message", ex.getReason()
            ));
        } catch (Exception ex) {
            log.error("❌ [RegisterController] Lỗi hệ thống khi tạo VNPAY URL: {}", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Lỗi hệ thống"
            ));
        }
    }

    /**
     * Xử lý VNPAY callback
     * GET /api/register-service/vnpay/return
     */
    @GetMapping("/vnpay/return")
    public ResponseEntity<?> handleVnpayReturn(HttpServletRequest request) {
        Map<String, String> params = vnpayService.getVnpayParams(request);
        log.info("[VNPAY RETURN] Callback nhận được cho registration: {}", params);

        try {
            boolean valid = vnpayService.validateReturn(new java.util.HashMap<>(params));
            log.info("[VNPAY RETURN] Chữ ký hợp lệ: {}", valid);

            String txnRef = params.get("vnp_TxnRef");
            if (txnRef == null || !txnRef.contains("_")) {
                log.warn("[VNPAY RETURN] Thiếu hoặc sai định dạng vnp_TxnRef: {}", txnRef);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Thiếu hoặc sai định dạng mã giao dịch (vnp_TxnRef)"
                ));
            }

            // Lấy registrationId từ txnRef (format: registrationId_timestamp)
            Long registrationId = Long.parseLong(txnRef.split("_")[0]);
            log.info("[VNPAY RETURN] Registration ID trích xuất được: {}", registrationId);

            String responseCode = params.get("vnp_ResponseCode");
            String transactionStatus = params.get("vnp_TransactionStatus");
            log.info("[VNPAY RETURN] ResponseCode={}, TransactionStatus={}", responseCode, transactionStatus);

            if (valid && "00".equals(responseCode) && "00".equals(transactionStatus)) {
                service.handleVnpayCallback(registrationId, params);
                log.info("[VNPAY RETURN] Registration {} đã được cập nhật sang PAID", registrationId);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Thanh toán thành công!",
                        "registrationId", registrationId
                ));
            } else {
                log.warn("[VNPAY RETURN] Thanh toán thất bại - ResponseCode={}, Valid={}", responseCode, valid);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Thanh toán thất bại hoặc chữ ký không hợp lệ",
                        "registrationId", registrationId,
                        "responseCode", responseCode,
                        "valid", valid
                ));
            }
        } catch (Exception ex) {
            log.error("[VNPAY RETURN ERROR]", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Lỗi hệ thống khi xử lý kết quả thanh toán"
            ));
        }
    }

    /**
     * Redirect sau khi thanh toán VNPAY
     * GET /api/register-service/vnpay/redirect
     */
    @GetMapping("/vnpay/redirect")
    public void redirectAfterPayment(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        Map<String, String> params = vnpayService.getVnpayParams(request);
        log.info("[VNPAY REDIRECT] Người dùng được redirect về với params: {}", params);

        String txnRef = params.getOrDefault("vnp_TxnRef", "");
        Long registrationId = 0L;
        try {
            if (txnRef.contains("_")) {
                registrationId = Long.parseLong(txnRef.split("_")[0]);
            }
        } catch (Exception e) {
            log.warn("[VNPAY REDIRECT] Không thể lấy registrationId: {}", e.getMessage());
        }

        // Xử lý callback
        handleVnpayReturn(request);

        String responseCode = params.get("vnp_ResponseCode");
        String redirectUrl = "qhomeapp://vnpay-registration-result?registrationId=" + registrationId + "&responseCode=" + responseCode;
        log.info("[VNPAY REDIRECT] Điều hướng người dùng về app URL: {}", redirectUrl);

        response.sendRedirect(redirectUrl);
    }

}
