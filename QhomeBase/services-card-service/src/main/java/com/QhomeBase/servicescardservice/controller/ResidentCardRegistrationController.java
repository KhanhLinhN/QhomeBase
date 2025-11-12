package com.QhomeBase.servicescardservice.controller;

import com.QhomeBase.servicescardservice.dto.CardRegistrationAdminDecisionRequest;
import com.QhomeBase.servicescardservice.dto.ResidentCardRegistrationCreateDto;
import com.QhomeBase.servicescardservice.dto.ResidentCardRegistrationDto;
import com.QhomeBase.servicescardservice.service.ResidentCardRegistrationService;
import com.QhomeBase.servicescardservice.service.ResidentCardRegistrationService.ResidentCardPaymentResponse;
import com.QhomeBase.servicescardservice.service.ResidentCardRegistrationService.ResidentCardPaymentResult;
import com.QhomeBase.servicescardservice.service.vnpay.VnpayService;
import com.QhomeBase.servicescardservice.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/resident-card")
@RequiredArgsConstructor
@Slf4j
public class ResidentCardRegistrationController {

    private final ResidentCardRegistrationService registrationService;
    private final JwtUtil jwtUtil;
    private final VnpayService vnpayService;

    @GetMapping("/admin/registrations")
    public ResponseEntity<?> getRegistrationsForAdmin(@RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String paymentStatus,
                                                      @RequestHeader HttpHeaders headers) {
        UUID adminId = jwtUtil.getUserIdFromHeaders(headers);
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            return ResponseEntity.ok(
                    registrationService.getRegistrationsForAdmin(
                            status != null && !status.isBlank() ? status.trim() : null,
                            paymentStatus != null && !paymentStatus.isBlank() ? paymentStatus.trim() : null
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Lỗi tải danh sách đăng ký", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể lấy danh sách đăng ký"));
        }
    }

    @GetMapping("/admin/registrations/{registrationId}")
    public ResponseEntity<?> getRegistrationForAdmin(@PathVariable String registrationId,
                                                     @RequestHeader HttpHeaders headers) {
        UUID adminId = jwtUtil.getUserIdFromHeaders(headers);
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            UUID regUuid = UUID.fromString(registrationId);
            ResidentCardRegistrationDto dto = registrationService.getRegistrationForAdmin(regUuid);
            return ResponseEntity.ok(toResponse(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/admin/registrations/{registrationId}/decision")
    public ResponseEntity<?> processAdminDecision(@PathVariable String registrationId,
                                                  @Valid @RequestBody CardRegistrationAdminDecisionRequest request,
                                                  @RequestHeader HttpHeaders headers) {
        UUID adminId = jwtUtil.getUserIdFromHeaders(headers);
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            UUID regUuid = UUID.fromString(registrationId);
            ResidentCardRegistrationDto dto = registrationService.processAdminDecision(adminId, regUuid, request);
            return ResponseEntity.ok(toResponse(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/vnpay-url")
    public ResponseEntity<?> createRegistrationAndPay(@RequestBody ResidentCardRegistrationCreateDto dto,
                                                      @RequestHeader HttpHeaders headers,
                                                      HttpServletRequest request) {
        UUID userId = jwtUtil.getUserIdFromHeaders(headers);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            ResidentCardPaymentResponse response = registrationService.createAndInitiatePayment(userId, dto, request);
            Map<String, Object> body = new HashMap<>();
            body.put("registrationId", response.registrationId() != null ? response.registrationId().toString() : null);
            body.put("paymentUrl", response.paymentUrl());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Lỗi tạo đăng ký", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể khởi tạo đăng ký thẻ cư dân"));
        }
    }

    @GetMapping("/{registrationId}")
    public ResponseEntity<?> getRegistration(@PathVariable String registrationId,
                                             @RequestHeader HttpHeaders headers) {
        UUID userId = jwtUtil.getUserIdFromHeaders(headers);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            UUID registrationUuid = UUID.fromString(registrationId);
            ResidentCardRegistrationDto dto = registrationService.getRegistration(userId, registrationUuid);
            return ResponseEntity.ok(toResponse(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{registrationId}/cancel")
    public ResponseEntity<?> cancelRegistration(@PathVariable String registrationId,
                                                @RequestHeader HttpHeaders headers) {
        UUID userId = jwtUtil.getUserIdFromHeaders(headers);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }
        try {
            UUID registrationUuid = UUID.fromString(registrationId);
            registrationService.cancelRegistration(userId, registrationUuid);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<?> handleVnpayReturn(HttpServletRequest request) {
        Map<String, String> params = vnpayService.extractParams(request);
        try {
            ResidentCardPaymentResult result = registrationService.handleVnpayCallback(params);
            Map<String, Object> body = buildVnpayResponse(result, params);
            HttpStatus status = result.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(body);
        } catch (Exception e) {
            log.error("❌ [ResidentCard] Lỗi xử lý callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/vnpay/redirect")
    public ResponseEntity<?> redirectAfterPayment(HttpServletRequest request,
                                                  HttpServletResponse response) throws IOException {
        Map<String, String> params = vnpayService.extractParams(request);
        ResidentCardPaymentResult result;
        try {
            result = registrationService.handleVnpayCallback(params);
        } catch (Exception e) {
            String fallback = "qhomeapp://vnpay-resident-card-result?success=false&message=" + e.getMessage();
            response.sendRedirect(fallback);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }

        Map<String, Object> body = buildVnpayResponse(result, params);
        String registrationId = result.registrationId() != null ? result.registrationId().toString() : "";
        String redirectUrl = new StringBuilder("qhomeapp://vnpay-resident-card-result")
                .append("?registrationId=").append(registrationId)
                .append("&responseCode=").append(result.responseCode() != null ? result.responseCode() : "")
                .append("&success=").append(result.success())
                .toString();
        response.sendRedirect(redirectUrl);
        HttpStatus status = result.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> toResponse(ResidentCardRegistrationDto dto) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", dto.id() != null ? dto.id().toString() : null);
        body.put("userId", dto.userId() != null ? dto.userId().toString() : null);
        body.put("unitId", dto.unitId() != null ? dto.unitId().toString() : null);
        body.put("requestType", dto.requestType());
        body.put("residentId", dto.residentId() != null ? dto.residentId().toString() : null);
        body.put("fullName", dto.fullName());
        body.put("apartmentNumber", dto.apartmentNumber());
        body.put("buildingName", dto.buildingName());
        body.put("citizenId", dto.citizenId());
        body.put("phoneNumber", dto.phoneNumber());
        body.put("note", dto.note());
        body.put("status", dto.status());
        body.put("paymentStatus", dto.paymentStatus());
        body.put("paymentAmount", dto.paymentAmount());
        body.put("paymentDate", dto.paymentDate());
        body.put("paymentGateway", dto.paymentGateway());
        body.put("vnpayTransactionRef", dto.vnpayTransactionRef());
        body.put("adminNote", dto.adminNote());
        body.put("approvedBy", dto.approvedBy() != null ? dto.approvedBy().toString() : null);
        body.put("approvedAt", dto.approvedAt());
        body.put("rejectionReason", dto.rejectionReason());
        body.put("createdAt", dto.createdAt());
        body.put("updatedAt", dto.updatedAt());
        return body;
    }

    private Map<String, Object> buildVnpayResponse(ResidentCardPaymentResult result, Map<String, String> params) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", result.success());
        body.put("registrationId", result.registrationId() != null ? result.registrationId().toString() : null);
        body.put("responseCode", result.responseCode());
        body.put("signatureValid", result.signatureValid());
        body.put("params", params);
        return body;
    }
}


