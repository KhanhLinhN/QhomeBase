package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.invoice.InvoiceLineResponseDto;
import com.qhomebaseapp.dto.invoice.UnifiedPaidInvoiceDto;
import com.qhomebaseapp.dto.invoice.ElectricityMonthlyDto;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.repository.UserRepository;
import com.qhomebaseapp.security.CustomUserDetails;
import com.qhomebaseapp.service.invoice.InvoiceService;
import com.qhomebaseapp.service.vnpay.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final VnpayService vnpayService;
    private final UserRepository userRepository;

    private Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return null;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) return null;
        return userRepository.findById(userId).orElse(null);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyInvoices(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
            ));
        }

        String unitId = user.getUnitId();
        log.info("🔍 [InvoiceController] User {} có unitId: {}", user.getId(), unitId);
        
        if (unitId == null || unitId.isBlank()) {
            log.warn("⚠️ [InvoiceController] User {} không có unitId", user.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Bạn chưa được gán vào căn hộ nào",
                    "data", List.of()
            ));
        }

        try {
            log.info("📋 [InvoiceController] Lấy danh sách invoice lines cho userId: {}, unitId: {}", user.getId(), unitId);
            
            List<InvoiceLineResponseDto> invoiceLines = invoiceService.getInvoiceLinesForFlutter(unitId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Lấy danh sách hóa đơn thành công",
                    "data", invoiceLines
            ));
        } catch (Exception e) {
            log.error("❌ [InvoiceController] Lỗi khi lấy danh sách invoice lines cho userId: {}, unitId: {}", user.getId(), unitId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách hóa đơn: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/unit/{unitId}")
    public ResponseEntity<?> getInvoiceLinesByUnitId(
            @PathVariable String unitId,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
            ));
        }
        String userUnitId = user.getUnitId();
        if (userUnitId == null || !userUnitId.equals(unitId)) {
            log.warn("⚠️ [InvoiceController] User {} không có quyền xem invoices của unitId: {}", user.getId(), unitId);
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Bạn không có quyền xem hóa đơn của căn hộ này"
            ));
        }

        try {
            log.info("📋 [InvoiceController] Lấy danh sách invoice lines cho unitId: {}, userId: {}", unitId, user.getId());
            
            List<InvoiceLineResponseDto> invoiceLines = invoiceService.getInvoiceLinesForFlutter(unitId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Lấy danh sách hóa đơn thành công",
                    "data", invoiceLines
            ));
        } catch (Exception e) {
            log.error("❌ [InvoiceController] Lỗi khi lấy danh sách invoice lines cho unitId: {}", unitId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách hóa đơn: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{invoiceId}/vnpay-url")
    public ResponseEntity<?> createVnpayUrl(
            @PathVariable String invoiceId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Bạn chưa đăng nhập"
            ));
        }

        try {
            log.info("💳 [InvoiceController] Tạo VNPAY URL cho invoice: {}, userId: {}", invoiceId, userId);
            
            String paymentUrl = invoiceService.createVnpayPaymentUrl(invoiceId, request);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tạo URL thanh toán thành công",
                    "paymentUrl", paymentUrl
            ));
        } catch (RuntimeException ex) {
            log.error("❌ [InvoiceController] Lỗi tạo VNPAY URL cho invoice: {}", invoiceId, ex);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("❌ [InvoiceController] Lỗi hệ thống khi tạo VNPAY URL: {}", ex);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Lỗi hệ thống"
            ));
        }
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<?> handleVnpayReturn(HttpServletRequest request) {
        Map<String, String> params = vnpayService.getVnpayParams(request);
        log.info("[VNPAY RETURN] ✅ Callback nhận được cho invoice: {}", params);

        try {
            boolean valid = vnpayService.validateReturn(new HashMap<>(params));
            log.info("[VNPAY RETURN] ✅ Chữ ký hợp lệ: {}", valid);

            String txnRef = params.get("vnp_TxnRef");
            if (txnRef == null || !txnRef.contains("_")) {
                log.warn("[VNPAY RETURN] ❌ Thiếu hoặc sai định dạng vnp_TxnRef: {}", txnRef);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Thiếu hoặc sai định dạng mã giao dịch (vnp_TxnRef)"
                ));
            }
            String invoiceId = invoiceService.getInvoiceIdFromTxnRef(txnRef);
            log.info("[VNPAY RETURN] 🔍 Invoice ID trích xuất được: {}", invoiceId);

            String responseCode = params.get("vnp_ResponseCode");
            String transactionStatus = params.get("vnp_TransactionStatus");
            log.info("[VNPAY RETURN] ↩️ ResponseCode={}, TransactionStatus={}", responseCode, transactionStatus);

            if (valid && "00".equals(responseCode) && "00".equals(transactionStatus)) {
                String userEmail = null;
                try {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
                        userEmail = userDetails.getUsername(); // Email là username
                    }
                } catch (Exception e) {
                    log.warn("[VNPAY RETURN] Không thể lấy user email từ authentication: {}", e.getMessage());
                }
                
                invoiceService.handleVnpayCallback(invoiceId, params, userEmail);
                log.info("[VNPAY RETURN] ✅ Invoice {} đã được cập nhật sang PAID", invoiceId);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Thanh toán thành công!",
                        "invoiceId", invoiceId
                ));
            } else {
                log.warn("[VNPAY RETURN] ❌ Thanh toán thất bại - ResponseCode={}, Valid={}", responseCode, valid);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Thanh toán thất bại hoặc chữ ký không hợp lệ",
                        "invoiceId", invoiceId,
                        "responseCode", responseCode,
                        "valid", valid
                ));
            }

        } catch (Exception ex) {
            log.error("[VNPAY RETURN ERROR]", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Lỗi hệ thống khi xử lý kết quả thanh toán"
            ));
        }
    }

    @GetMapping("/vnpay/redirect")
    public ResponseEntity<?> redirectAfterPayment(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> params = vnpayService.getVnpayParams(request);
        log.info("[VNPAY REDIRECT] 🔁 Người dùng được redirect về với params: {}", params);

        ResponseEntity<?> result = handleVnpayReturn(request);

        String txnRef = params.getOrDefault("vnp_TxnRef", "");
        String invoiceId = "";
        try {
            if (txnRef.contains("_")) {
                invoiceId = invoiceService.getInvoiceIdFromTxnRef(txnRef);
            }
        } catch (Exception e) {
            log.warn("[VNPAY REDIRECT] Không thể lấy invoiceId: {}", e.getMessage());
        }

        String responseCode = params.get("vnp_ResponseCode");
        String redirectUrl = "qhomeapp://vnpay-result?invoiceId=" + invoiceId + "&responseCode=" + responseCode;
        log.info("[VNPAY REDIRECT] 🔁 Điều hướng người dùng về app URL: {}", redirectUrl);

        response.sendRedirect(redirectUrl);
        return result;
    }

    @PutMapping("/{invoiceId}/pay")
    public ResponseEntity<?> payInvoice(
            @PathVariable String invoiceId,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
            ));
        }

        try {
            log.info("💳 [InvoiceController] Thanh toán invoice (deprecated): {}, userId: {}", invoiceId, userId);
            
            invoiceService.payInvoice(invoiceId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Thanh toán hóa đơn thành công"
            ));
        } catch (Exception e) {
            log.error("❌ [InvoiceController] Lỗi khi thanh toán invoice: {}", invoiceId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Lỗi khi thanh toán hóa đơn: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/paid/all")
    public ResponseEntity<?> getAllPaidInvoices(Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
            ));
        }

        try {
            log.info("📋 [InvoiceController] Lấy tất cả hóa đơn đã thanh toán cho userId: {}", userId);
            
            List<UnifiedPaidInvoiceDto> paidInvoices = invoiceService.getAllPaidInvoices(userId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Lấy danh sách hóa đơn đã thanh toán thành công",
                    "data", paidInvoices
            ));
        } catch (Exception e) {
            log.error("❌ [InvoiceController] Lỗi khi lấy danh sách hóa đơn đã thanh toán cho userId: {}", userId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách hóa đơn: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/electricity/monthly")
    public ResponseEntity<?> getElectricityMonthlyData(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
            ));
        }

        String unitId = user.getUnitId();
        log.info("📊 [InvoiceController] Lấy dữ liệu tiền điện theo tháng cho userId: {}, unitId: {}", user.getId(), unitId);
        
        if (unitId == null || unitId.isBlank()) {
            log.warn("⚠️ [InvoiceController] User {} không có unitId", user.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Bạn chưa được gán vào căn hộ nào",
                    "data", List.of()
            ));
        }

        try {
            List<ElectricityMonthlyDto> monthlyData = invoiceService.getElectricityMonthlyData(unitId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Lấy dữ liệu tiền điện thành công",
                    "data", monthlyData
            ));
        } catch (Exception e) {
            log.error("❌ [InvoiceController] Lỗi khi lấy dữ liệu tiền điện cho userId: {}, unitId: {}", user.getId(), unitId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy dữ liệu tiền điện: " + e.getMessage()
            ));
        }
    }
}

