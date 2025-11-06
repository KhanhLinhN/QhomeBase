package com.QhomeBase.customerinteractionservice.controller;

import com.QhomeBase.customerinteractionservice.dto.notification.*;
import com.QhomeBase.customerinteractionservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("@authz.canManageNotifications()")
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request) {
        
        NotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.canManageNotifications()")
    public ResponseEntity<NotificationResponse> updateNotification(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateNotificationRequest request) {
        
        NotificationResponse response = notificationService.updateNotification(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.canManageNotifications()")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable("id") UUID id,
            org.springframework.security.core.Authentication authentication) {
        
        com.QhomeBase.customerinteractionservice.security.UserPrincipal principal = 
            (com.QhomeBase.customerinteractionservice.security.UserPrincipal) authentication.getPrincipal();
        
        notificationService.deleteNotification(id, principal.uid());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("@authz.canViewNotifications()")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        List<NotificationResponse> notifications = notificationService.getAllNotifications();
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationDetailResponse> getNotificationById(@PathVariable("id") UUID id) {
        NotificationDetailResponse response = notificationService.getNotificationDetailById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/resident")
    public ResponseEntity<List<NotificationResponse>> getNotificationsForResident(
            @RequestParam UUID residentId,
            @RequestParam UUID buildingId) {
        
        List<NotificationResponse> notifications = notificationService.getNotificationsForResident(residentId, buildingId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/role")
    @PreAuthorize("@authz.canViewNotifications()")
    public ResponseEntity<List<NotificationResponse>> getNotificationsForRole(
            @RequestParam String role,
            @RequestParam UUID userId) {
        
        List<NotificationResponse> notifications = notificationService.getNotificationsForRole(role, userId);
        return ResponseEntity.ok(notifications);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Access denied"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error: " + ex.getMessage()));
    }

    record ErrorResponse(int status, String message) {}
}















