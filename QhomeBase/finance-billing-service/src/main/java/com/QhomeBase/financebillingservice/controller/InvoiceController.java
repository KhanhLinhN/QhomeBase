package com.QhomeBase.financebillingservice.controller;

import com.QhomeBase.financebillingservice.dto.*;
import com.QhomeBase.financebillingservice.model.InvoiceStatus;
import com.QhomeBase.financebillingservice.service.InvoiceService;
import com.QhomeBase.financebillingservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.QhomeBase.financebillingservice.service.vnpay.VnpayService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {
    
    private final InvoiceService invoiceService;
    private final JwtUtil jwtUtil;
    private final VnpayService vnpayService;
    
    @GetMapping("/resident/{residentId}")
    public ResponseEntity<List<InvoiceDto>> getInvoicesByResident(@PathVariable UUID residentId) {
        List<InvoiceDto> invoices = invoiceService.getInvoicesByResident(residentId);
        return ResponseEntity.ok(invoices);
    }
    
    @GetMapping("/resident/{residentId}/unpaid")
    public ResponseEntity<List<InvoiceDto>> getUnpaidInvoicesByResident(@PathVariable UUID residentId) {
        List<InvoiceDto> invoices = invoiceService.getInvoicesByResidentAndStatus(residentId, InvoiceStatus.PUBLISHED);
        return ResponseEntity.ok(invoices);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDto> getInvoiceById(@PathVariable UUID id) {
        InvoiceDto invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(invoice);
    }
    
    @GetMapping("/unit/{unitId}")
    public ResponseEntity<List<InvoiceDto>> getInvoicesByUnit(@PathVariable UUID unitId) {
        List<InvoiceDto> invoices = invoiceService.getInvoicesByUnit(unitId);
        return ResponseEntity.ok(invoices);
    }
    
    @GetMapping("/service/{serviceCode}")
    public ResponseEntity<List<InvoiceDto>> getInvoicesByServiceCode(@PathVariable String serviceCode) {
        List<InvoiceDto> invoices = invoiceService.getInvoicesByServiceCode(serviceCode);
        return ResponseEntity.ok(invoices);
    }
    
    @GetMapping("/resident/{residentId}/service/{serviceCode}")
    public ResponseEntity<List<InvoiceDto>> getInvoicesByResidentAndServiceCode(
            @PathVariable UUID residentId,
            @PathVariable String serviceCode) {
        List<InvoiceDto> invoices = invoiceService.getInvoicesByResidentAndServiceCode(residentId, serviceCode);
        return ResponseEntity.ok(invoices);
    }
    
    @PostMapping
    public ResponseEntity<InvoiceDto> createInvoice(@RequestBody CreateInvoiceRequest request) {
        InvoiceDto invoice = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }

    @PostMapping("/vehicle-registration-payment")
    public ResponseEntity<?> recordVehicleRegistrationPayment(@RequestBody VehicleRegistrationPaymentRequest request) {
        try {
            InvoiceDto invoice = invoiceService.recordVehicleRegistrationPayment(request);
            return ResponseEntity.ok(invoice);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/elevator-card-payment")
    public ResponseEntity<?> recordElevatorCardPayment(@RequestBody ElevatorCardPaymentRequest request) {
        try {
            InvoiceDto invoice = invoiceService.recordElevatorCardPayment(request);
            return ResponseEntity.ok(invoice);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<InvoiceDto> updateInvoiceStatus(
            @PathVariable UUID id,
            @RequestBody UpdateInvoiceStatusRequest request) {
        InvoiceDto invoice = invoiceService.updateInvoiceStatus(id, request);
        return ResponseEntity.ok(invoice);
    }
    
    @DeleteMapping("/{id}/void")
    public ResponseEntity<Void> voidInvoice(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        invoiceService.voidInvoice(id, reason);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getMyInvoices(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "unitId") UUID unitId,
            @RequestParam(value = "cycleId", required = false) UUID cycleId) {
        try {
            UUID userId = jwtUtil.getUserIdFromHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or missing authentication token"));
            }
            
            List<InvoiceLineResponseDto> invoices = invoiceService.getMyInvoices(userId, unitId, cycleId);
            Map<String, Object> response = new HashMap<>();
            response.put("data", invoices);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Log and return empty list to avoid breaking mobile flow while backend data is prepared
            return ResponseEntity.ok(Map.of("data", List.of()));
        }
    }
    
    @GetMapping("/me/unpaid-by-category")
    public ResponseEntity<?> getMyUnpaidInvoicesByCategory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "unitId") UUID unitId,
            @RequestParam(value = "cycleId", required = false) UUID cycleId) {
        try {
            UUID userId = jwtUtil.getUserIdFromHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or missing authentication token"));
            }

            List<InvoiceCategoryResponseDto> categories = invoiceService.getUnpaidInvoicesByCategory(userId, unitId, cycleId);
            Map<String, Object> response = new HashMap<>();
            response.put("data", categories);
            response.put("allPaid", categories.isEmpty());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Log và trả về kết quả rỗng để tránh gây lỗi cho ứng dụng di động
            return ResponseEntity.ok(Map.of(
                    "data", List.of(),
                    "allPaid", true
            ));
        }
    }

    @GetMapping("/me/paid-by-category")
    public ResponseEntity<?> getMyPaidInvoicesByCategory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "unitId") UUID unitId,
            @RequestParam(value = "cycleId", required = false) UUID cycleId) {
        try {
            UUID userId = jwtUtil.getUserIdFromHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or missing authentication token"));
            }

            List<InvoiceCategoryResponseDto> categories = invoiceService.getPaidInvoicesByCategory(userId, unitId, cycleId);
            Map<String, Object> response = new HashMap<>();
            response.put("data", categories);
            response.put("hasPaid", !categories.isEmpty());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error retrieving paid invoices for user {}", authHeader, e);
            return ResponseEntity.ok(Map.of(
                    "data", List.of(),
                    "hasPaid", false
            ));
        }
    }

    @GetMapping("/electricity/monthly")
    public ResponseEntity<?> getElectricityMonthlyData(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "unitId", required = false) UUID unitId) {
        try {
            UUID userId = jwtUtil.getUserIdFromHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or missing authentication token"));
            }
            
            List<ElectricityMonthlyDto> monthlyData = invoiceService.getElectricityMonthlyData(userId, unitId);
            Map<String, Object> response = new HashMap<>();
            response.put("data", monthlyData);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Log and return empty list to avoid breaking mobile flow while backend data is prepared
            return ResponseEntity.ok(Map.of("data", List.of()));
        }
    }

    @PostMapping("/{invoiceId}/vnpay-url")
    public ResponseEntity<?> createVnpayUrl(
            @PathVariable UUID invoiceId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "unitId", required = false) UUID unitId,
            HttpServletRequest request) {
        UUID userId = jwtUtil.getUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or missing authentication token"));
        }

        try {
            String paymentUrl = invoiceService.createVnpayPaymentUrl(invoiceId, userId, request, unitId);
            return ResponseEntity.ok(Map.of(
                    "paymentUrl", paymentUrl,
                    "invoiceId", invoiceId.toString()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<?> handleVnpayReturn(HttpServletRequest request) {
        Map<String, String> params = vnpayService.extractParams(request);
        try {
            InvoiceService.VnpayCallbackResult result = invoiceService.handleVnpayCallback(params);
            Map<String, Object> body = buildVnpayResponse(result, params);
            HttpStatus status = result.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    @GetMapping("/vnpay/redirect")
    public ResponseEntity<?> redirectAfterPayment(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> params = vnpayService.extractParams(request);
        InvoiceService.VnpayCallbackResult result;
        try {
            result = invoiceService.handleVnpayCallback(params);
        } catch (Exception e) {
            String fallbackUrl = "qhomeapp://vnpay-result?success=false&message=" + e.getMessage();
            response.sendRedirect(fallbackUrl);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }

        Map<String, Object> body = buildVnpayResponse(result, params);
        String invoiceId = result.invoiceId() != null ? result.invoiceId().toString() : "";
        String redirectUrl = new StringBuilder("qhomeapp://vnpay-result")
                .append("?invoiceId=").append(invoiceId)
                .append("&responseCode=").append(result.responseCode() != null ? result.responseCode() : "")
                .append("&success=").append(result.success())
                .toString();

        response.sendRedirect(redirectUrl);
        HttpStatus status = result.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> buildVnpayResponse(InvoiceService.VnpayCallbackResult result, Map<String, String> params) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", result.success());
        body.put("invoiceId", result.invoiceId() != null ? result.invoiceId().toString() : null);
        body.put("responseCode", result.responseCode());
        body.put("signatureValid", result.signatureValid());
        body.put("params", params);
        return body;
    }
}

