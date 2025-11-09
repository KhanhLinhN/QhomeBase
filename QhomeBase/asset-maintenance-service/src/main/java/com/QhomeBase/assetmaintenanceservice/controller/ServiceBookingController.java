package com.QhomeBase.assetmaintenanceservice.controller;

import com.QhomeBase.assetmaintenanceservice.dto.service.*;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceBookingStatus;
import com.QhomeBase.assetmaintenanceservice.security.UserPrincipal;
import com.QhomeBase.assetmaintenanceservice.service.ServiceBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/asset-maintenance")
@RequiredArgsConstructor
public class ServiceBookingController {

    private final ServiceBookingService bookingService;



    @GetMapping("/services/{serviceId}/booking/catalog")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingCatalogDto> getBookingCatalog(@PathVariable UUID serviceId) {
        return ResponseEntity.ok(bookingService.getBookingCatalog(serviceId));
    }

    @PostMapping("/bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingDto> createBooking(@Valid @RequestBody CreateServiceBookingRequest request,
                                                           Authentication authentication) {
        ServiceBookingDto created = bookingService.createBooking(request, authentication.getPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServiceBookingDto>> getMyBookings(
            @RequestParam(required = false) ServiceBookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication
    ) {
        List<ServiceBookingDto> bookings = bookingService.getMyBookings(authentication.getPrincipal(), status, fromDate, toDate);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/bookings/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingDto> getMyBooking(@PathVariable UUID bookingId,
                                                          Authentication authentication) {
        return ResponseEntity.ok(bookingService.getMyBooking(bookingId, authentication.getPrincipal()));
    }

    @PatchMapping("/bookings/{bookingId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingDto> cancelMyBooking(@PathVariable UUID bookingId,
                                                             @Valid @RequestBody(required = false) CancelServiceBookingRequest request,
                                                             Authentication authentication) {
        CancelServiceBookingRequest effectiveRequest = request != null ? request : new CancelServiceBookingRequest(null);
        return ResponseEntity.ok(bookingService.cancelMyBooking(bookingId, effectiveRequest, authentication.getPrincipal()));
    }

    @PatchMapping("/bookings/{bookingId}/accept-terms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingDto> acceptTerms(@PathVariable UUID bookingId,
                                                         @Valid @RequestBody AcceptServiceBookingTermsRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(bookingService.acceptTerms(bookingId, request, authentication.getPrincipal()));
    }

    @PostMapping("/bookings/{bookingId}/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingDto> addBookingItem(@PathVariable UUID bookingId,
                                                            @Valid @RequestBody CreateServiceBookingItemRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(bookingService.addBookingItem(bookingId, request, authentication.getPrincipal(), false));
    }

    @PutMapping("/bookings/{bookingId}/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingDto> updateBookingItem(@PathVariable UUID bookingId,
                                                               @PathVariable UUID itemId,
                                                               @Valid @RequestBody UpdateServiceBookingItemRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(bookingService.updateBookingItem(bookingId, itemId, request, authentication.getPrincipal(), false));
    }

    @DeleteMapping("/bookings/{bookingId}/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingDto> deleteBookingItem(@PathVariable UUID bookingId,
                                                               @PathVariable UUID itemId,
                                                               Authentication authentication) {
        return ResponseEntity.ok(bookingService.deleteBookingItem(bookingId, itemId, authentication.getPrincipal(), false));
    }

    @PutMapping("/bookings/{bookingId}/slots")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceBookingDto> updateBookingSlots(@PathVariable UUID bookingId,
                                                                @Valid @RequestBody UpdateServiceBookingSlotsRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(bookingService.updateBookingSlots(bookingId, request, authentication.getPrincipal(), false));
    }

    /* Administrative endpoints */

    @GetMapping("/admin/bookings")
    @PreAuthorize("@authz.canViewServiceBooking()")
    public ResponseEntity<List<ServiceBookingDto>> searchBookings(
            @RequestParam(required = false) ServiceBookingStatus status,
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        List<ServiceBookingDto> bookings = bookingService.searchBookings(status, serviceId, userId, fromDate, toDate);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/admin/bookings/{bookingId}")
    @PreAuthorize("@authz.canViewServiceBooking()")
    public ResponseEntity<ServiceBookingDto> getBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.getBooking(bookingId));
    }

    @PatchMapping("/admin/bookings/{bookingId}/approve")
    @PreAuthorize("@authz.canManageServiceBooking()")
    public ResponseEntity<ServiceBookingDto> approveBooking(@PathVariable UUID bookingId,
                                                            @Valid @RequestBody(required = false) AdminApproveServiceBookingRequest request,
                                                            Authentication authentication) {
        AdminApproveServiceBookingRequest effectiveRequest = request != null ? request : new AdminApproveServiceBookingRequest(null);
        return ResponseEntity.ok(bookingService.approveBooking(bookingId, effectiveRequest, resolveUserId(authentication)));
    }

    @PatchMapping("/admin/bookings/{bookingId}/reject")
    @PreAuthorize("@authz.canManageServiceBooking()")
    public ResponseEntity<ServiceBookingDto> rejectBooking(@PathVariable UUID bookingId,
                                                           @Valid @RequestBody AdminRejectServiceBookingRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(bookingService.rejectBooking(bookingId, request, resolveUserId(authentication)));
    }

    @PatchMapping("/admin/bookings/{bookingId}/complete")
    @PreAuthorize("@authz.canManageServiceBooking()")
    public ResponseEntity<ServiceBookingDto> completeBooking(@PathVariable UUID bookingId,
                                                             @Valid @RequestBody(required = false) AdminCompleteServiceBookingRequest request,
                                                             Authentication authentication) {
        AdminCompleteServiceBookingRequest effectiveRequest = request != null ? request : new AdminCompleteServiceBookingRequest(null);
        return ResponseEntity.ok(bookingService.completeBooking(bookingId, effectiveRequest, resolveUserId(authentication)));
    }

    @PatchMapping("/admin/bookings/{bookingId}/payment")
    @PreAuthorize("@authz.canManageServiceBooking()")
    public ResponseEntity<ServiceBookingDto> updatePayment(@PathVariable UUID bookingId,
                                                           @Valid @RequestBody AdminUpdateServiceBookingPaymentRequest request) {
        return ResponseEntity.ok(bookingService.updatePayment(bookingId, request));
    }

    @PutMapping("/admin/bookings/{bookingId}/slots")
    @PreAuthorize("@authz.canManageServiceBooking()")
    public ResponseEntity<ServiceBookingDto> adminUpdateSlots(@PathVariable UUID bookingId,
                                                              @Valid @RequestBody UpdateServiceBookingSlotsRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(bookingService.updateBookingSlots(bookingId, request, authentication.getPrincipal(), true));
    }

    @PostMapping("/admin/bookings/{bookingId}/items")
    @PreAuthorize("@authz.canManageServiceBooking()")
    public ResponseEntity<ServiceBookingDto> adminAddItem(@PathVariable UUID bookingId,
                                                          @Valid @RequestBody CreateServiceBookingItemRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(bookingService.addBookingItem(bookingId, request, authentication.getPrincipal(), true));
    }

    @PutMapping("/admin/bookings/{bookingId}/items/{itemId}")
    @PreAuthorize("@authz.canManageServiceBooking()")
    public ResponseEntity<ServiceBookingDto> adminUpdateItem(@PathVariable UUID bookingId,
                                                             @PathVariable UUID itemId,
                                                             @Valid @RequestBody UpdateServiceBookingItemRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(bookingService.updateBookingItem(bookingId, itemId, request, authentication.getPrincipal(), true));
    }

    @DeleteMapping("/admin/bookings/{bookingId}/items/{itemId}")
    @PreAuthorize("@authz.canManageServiceBooking()")
    public ResponseEntity<ServiceBookingDto> adminDeleteItem(@PathVariable UUID bookingId,
                                                             @PathVariable UUID itemId,
                                                             Authentication authentication) {
        return ResponseEntity.ok(bookingService.deleteBookingItem(bookingId, itemId, authentication.getPrincipal(), true));
    }

    private UUID resolveUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.uid();
        }
        throw new IllegalStateException("Unsupported authentication principal");
    }
}


