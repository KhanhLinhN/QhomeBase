package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.bill.BillStatisticsDto;
import com.qhomebaseapp.model.Bill;
import com.qhomebaseapp.service.bill.BillService;
import com.qhomebaseapp.security.CustomUserDetails;
import com.qhomebaseapp.service.vnpay.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;
    private final VnpayService vnpayService;

    private Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return null;
    }

    @GetMapping("/unpaid")
    public ResponseEntity<?> getUnpaidBills(Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        List<Bill> bills = billService.getUnpaidBillsByUserId(userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Danh sách hóa đơn chưa thanh toán",
                "data", bills
        ));
    }

    @GetMapping("/paid")
    public ResponseEntity<?> getPaidBills(Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        List<Bill> bills = billService.getPaidBillsByUserId(userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Danh sách hóa đơn đã thanh toán",
                "data", bills
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBillDetail(@PathVariable Long id, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        try {
            Bill bill = billService.getBillDetail(id, userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Chi tiết hóa đơn",
                    "data", bill
            ));
        } catch (RuntimeException ex) {
            log.error("Bill {} not found for user {}", id, userId, ex);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<?> createVnpayPayment(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null)
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        try {
            String paymentUrl = billService.createVnpayPaymentUrl(id, userId, request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "paymentUrl", paymentUrl
            ));
        } catch (RuntimeException ex) {
            log.error("Create VNPAY payment for bill {} failed for user {}", id, userId, ex);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
            ));
        }
    }


    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(
            @RequestParam(value = "billType", required = false, defaultValue = "ALL") String billType,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        List<BillStatisticsDto> stats = billService.getStatisticsByUserId(userId, billType);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Thống kê hóa đơn theo tháng",
                "data", stats
        ));
    }

    @GetMapping("/by-month")
    public ResponseEntity<?> getBillsByMonth(
            @RequestParam("month") String month,
            @RequestParam(value = "billType", required = false) String billType,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null)
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        log.info("🔎 [getBillsByMonth] month={}, billType={}", month, billType);

        try {
            List<Bill> bills = billService.getBillsByMonth(userId, month, billType);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Danh sách hóa đơn theo tháng",
                    "data", bills
            ));
        } catch (Exception e) {
            log.error("❌ [getBillsByMonth] Lỗi xử lý dữ liệu: {}", e.getMessage(), e);
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "message", "Dữ liệu không hợp lệ hoặc lỗi xử lý: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<?> handleVnpayReturn(@RequestParam Map<String, String> params) {
        log.info("[VNPAY RETURN] Callback params: {}", params);

        try {
            boolean valid = vnpayService.validateReturn(params);

            String txnRef = params.get("vnp_TxnRef");
            String responseCode = params.get("vnp_ResponseCode");
            String amount = params.get("vnp_Amount");

            if (txnRef == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "Thiếu mã giao dịch"
                ));
            }

            Long billId = Long.valueOf(txnRef);

            if (valid && "00".equals(responseCode)) {
                billService.markAsPaid(billId, params);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Thanh toán thành công!",
                        "billId", billId
                ));
            } else {
                log.warn("[VNPAY FAILED] Bill {}, responseCode={}, valid={}", billId, responseCode, valid);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Thanh toán thất bại hoặc chữ ký không hợp lệ",
                        "billId", billId,
                        "responseCode", responseCode
                ));
            }
        } catch (Exception ex) {
            log.error("[VNPAY RETURN ERROR]", ex);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false, "message", "Lỗi hệ thống"
            ));
        }
    }
    @PostMapping("/{id}/vnpay-url")
    public ResponseEntity<?> createVnpayUrl(
            @PathVariable Long id,
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
            String paymentUrl = billService.createVnpayPaymentUrl(id, userId, request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tạo URL thanh toán thành công",
                    "paymentUrl", paymentUrl
            ));
        } catch (RuntimeException ex) {
            log.error("❌ [VNPAY URL ERROR] billId={}, userId={}, err={}", id, userId, ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("🔥 [VNPAY URL EXCEPTION]", ex);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Lỗi hệ thống khi tạo URL thanh toán"
            ));
        }
    }
}
