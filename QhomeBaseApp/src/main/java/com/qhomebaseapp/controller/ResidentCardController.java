package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.residentcard.ResidentCardRegistrationDto;
import com.qhomebaseapp.dto.residentcard.ResidentCardRegistrationResponseDto;
import com.qhomebaseapp.security.CustomUserDetails;
import com.qhomebaseapp.service.residentcard.ResidentCardRegistrationService;
import com.qhomebaseapp.service.vnpay.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@Slf4j
@RestController
@RequestMapping("/api/resident-card")
@RequiredArgsConstructor
public class ResidentCardController {

    private final ResidentCardRegistrationService service;
    private final VnpayService vnpayService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResidentCardRegistrationResponseDto> register(
            @RequestBody ResidentCardRegistrationDto dto,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        ResidentCardRegistrationResponseDto result = service.registerResidentCard(dto, userId);

        log.info("User {} registered resident card", userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ResidentCardRegistrationResponseDto>> getByUser(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<ResidentCardRegistrationResponseDto> list = service.getByUserId(userId);
        log.info("User {} fetched their resident card registrations, count={}", userId, list.size());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResidentCardRegistrationResponseDto> getById(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        ResidentCardRegistrationResponseDto result = service.getById(id, userId);
        log.info("User {} fetched resident card registration {}", userId, id);
        return ResponseEntity.ok(result);
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

        Page<ResidentCardRegistrationResponseDto> result = service.getByUserIdPaginated(userId, pageIndex, size);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Danh sách đăng ký thẻ cư dân",
                "data", result.getContent(),
                "totalPages", result.getTotalPages(),
                "totalElements", result.getTotalElements(),
                "currentPage", page
        ));
    }

    @PostMapping("/vnpay-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createVnpayUrlWithData(
            @RequestBody ResidentCardRegistrationDto dto,
            Authentication authentication,
            HttpServletRequest request
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        
        try {
            log.info("💳 [ResidentCardController] Tạo VNPAY URL với data cho userId: {}", userId);
            
            Map<String, Object> result = service.createVnpayPaymentUrlWithData(dto, userId, request);
            Long registrationId = ((Number) result.get("registrationId")).longValue();
            String paymentUrl = (String) result.get("paymentUrl");
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tạo URL thanh toán thành công",
                    "registrationId", registrationId,
                    "paymentUrl", paymentUrl
            ));
        } catch (Exception ex) {
            log.error("❌ [ResidentCardController] Lỗi tạo VNPAY URL với data: {}", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Lỗi hệ thống: " + ex.getMessage()
            ));
        }
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<?> handleVnpayReturn(HttpServletRequest request) {
        Map<String, String> params = vnpayService.getVnpayParams(request);
        log.info("[VNPAY RETURN] ✅ Callback nhận được cho resident card: {}", params);

        try {
            boolean valid = vnpayService.validateReturn(new java.util.HashMap<>(params));
            log.info("[VNPAY RETURN] ✅ Chữ ký hợp lệ: {}", valid);

            String txnRef = params.get("vnp_TxnRef");
            if (txnRef == null || !txnRef.contains("_")) {
                log.warn("[VNPAY RETURN] ❌ Thiếu hoặc sai định dạng vnp_TxnRef: {}", txnRef);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Thiếu hoặc sai định dạng mã giao dịch (vnp_TxnRef)"
                ));
            }

            Long registrationId = Long.parseLong(txnRef.split("_")[0]);
            log.info("[VNPAY RETURN] 🔍 Resident Card Registration ID trích xuất được: {}", registrationId);

            String responseCode = params.get("vnp_ResponseCode");
            String transactionStatus = params.get("vnp_TransactionStatus");
            log.info("[VNPAY RETURN] ↩️ ResponseCode={}, TransactionStatus={}", responseCode, transactionStatus);

            if (valid && "00".equals(responseCode) && "00".equals(transactionStatus)) {
                service.handleVnpayCallback(registrationId, params);
                log.info("[VNPAY RETURN] ✅ Resident Card Registration {} đã được cập nhật sang PAID", registrationId);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Thanh toán thành công!",
                        "registrationId", registrationId
                ));
            } else {
                log.warn("[VNPAY RETURN] ❌ Thanh toán thất bại - ResponseCode={}, Valid={}", responseCode, valid);
                
                try {
                    service.handleVnpayCallback(registrationId, params);
                } catch (Exception e) {
                    log.error("[VNPAY RETURN] Lỗi khi cập nhật registration thất bại: {}", e.getMessage(), e);
                }
                
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

    @GetMapping("/vnpay/redirect")
    public ResponseEntity<?> redirectAfterPayment(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        Map<String, String> params = vnpayService.getVnpayParams(request);
        log.info("[VNPAY REDIRECT] 🔁 Người dùng được redirect về với params: {}", params);

        handleVnpayReturn(request);

        String txnRef = params.getOrDefault("vnp_TxnRef", "");
        Long registrationId = 0L;
        try {
            if (txnRef.contains("_")) {
                registrationId = Long.parseLong(txnRef.split("_")[0]);
            }
        } catch (Exception e) {
            log.warn("[VNPAY REDIRECT] Không thể lấy registrationId: {}", e.getMessage());
        }

        String responseCode = params.getOrDefault("vnp_ResponseCode", "99");
        String deepLinkUrl = "qhomeapp://vnpay-resident-card-result?registrationId=" + registrationId + "&responseCode=" + responseCode;
        log.info("[VNPAY REDIRECT] 🔁 Tạo deep link để mở app: {}", deepLinkUrl);

        String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Đang chuyển hướng...</title>\n" +
                "    <meta http-equiv=\"refresh\" content=\"0;url=" + deepLinkUrl + "\">\n" +
                "    <script>\n" +
                "        window.location.href = \"" + deepLinkUrl + "\";\n" +
                "        setTimeout(function() {\n" +
                "            document.body.innerHTML = '<div style=\"text-align:center;padding:50px;font-family:Arial;\"><h2>Thanh toán thành công!</h2><p>Đang chuyển hướng về ứng dụng...</p><p>Nếu ứng dụng không tự động mở, vui lòng quay lại ứng dụng thủ công.</p></div>';\n" +
                "        }, 3000);\n" +
                "    </script>\n" +
                "</head>\n" +
                "<body style=\"margin:0;padding:0;background:#f5f5f5;\">\n" +
                "    <div style=\"text-align:center;padding:50px;font-family:Arial;\">\n" +
                "        <h2>Thanh toán thành công!</h2>\n" +
                "        <p>Đang chuyển hướng về ứng dụng...</p>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";

        response.setHeader("ngrok-skip-browser-warning", "true");
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(html);
        response.getWriter().flush();
        
        log.info("[VNPAY REDIRECT] ✅ Đã trả về HTML page với auto-redirect");
        return null;
    }

    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelRegistration(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        
        try {
            log.info("🗑️ [ResidentCardController] Hủy registration: {}, userId: {}", id, userId);
            
            service.cancelRegistration(id, userId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã hủy đăng ký thành công"
            ));
        } catch (ResponseStatusException ex) {
            log.error("❌ [ResidentCardController] Lỗi hủy registration: {}", id, ex);
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                    "success", false,
                    "message", ex.getReason()
            ));
        } catch (Exception ex) {
            log.error("❌ [ResidentCardController] Lỗi hệ thống khi hủy registration: {}", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Lỗi hệ thống"
            ));
        }
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
}

