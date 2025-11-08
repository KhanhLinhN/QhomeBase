package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.dto.service.AcceptServiceBookingTermsRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.AdminApproveServiceBookingRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.AdminCompleteServiceBookingRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.AdminRejectServiceBookingRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.AdminUpdateServiceBookingPaymentRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.CancelServiceBookingRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.CreateServiceBookingItemRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.CreateServiceBookingRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceBookingCatalogDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceBookingDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceBookingItemDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceBookingSlotDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceBookingSlotRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceComboDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceOptionDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceOptionGroupDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceTicketDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.UpdateServiceBookingItemRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.UpdateServiceBookingSlotsRequest;
import com.QhomeBase.assetmaintenanceservice.model.service.*;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceBookingStatus;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServicePaymentStatus;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceBookingItemType;
import com.QhomeBase.assetmaintenanceservice.repository.*;
import com.QhomeBase.assetmaintenanceservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class ServiceBookingService {

    private final ServiceRepository serviceRepository;
    private final ServiceBookingRepository serviceBookingRepository;
    private final ServiceBookingItemRepository serviceBookingItemRepository;
    private final ServiceBookingSlotRepository serviceBookingSlotRepository;
    private final ServiceAvailabilityRepository serviceAvailabilityRepository;
    private final ServiceComboRepository serviceComboRepository;
    private final ServiceOptionRepository serviceOptionRepository;
    private final ServiceTicketRepository serviceTicketRepository;
    private final ServiceOptionGroupRepository serviceOptionGroupRepository;
    private final ServiceConfigService serviceConfigService;
    private UserPrincipal principal(Object authenticationPrincipal) {
        if (authenticationPrincipal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }
        throw new IllegalStateException("Unsupported authentication principal");
    }

    @Transactional
    public ServiceBookingDto createBooking(CreateServiceBookingRequest request, Object authenticationPrincipal) {
        UserPrincipal principal = principal(authenticationPrincipal);

        var service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + request.getServiceId()));

        ServiceBooking booking = new ServiceBooking();
        booking.setService(service);
        booking.setUserId(principal.uid());
        booking.setBookingDate(request.getBookingDate());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setDurationHours(resolveDurationHours(request.getStartTime(), request.getEndTime(), request.getDurationHours()));
        booking.setNumberOfPeople(request.getNumberOfPeople());
        booking.setPurpose(trimToNull(request.getPurpose()));
        booking.setTermsAccepted(Boolean.TRUE.equals(request.getTermsAccepted()));
        booking.setPaymentStatus(ServicePaymentStatus.UNPAID);
        booking.setStatus(ServiceBookingStatus.PENDING);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            booking.getBookingItems().addAll(buildItems(booking, request.getItems()));
        }

        booking.getBookingSlots().addAll(buildSlots(booking, request.getSlots(),
                request.getBookingDate(), request.getStartTime(), request.getEndTime()));

        booking.setTotalAmount(calculateTotalAmount(booking));

        ServiceBooking saved = serviceBookingRepository.save(booking);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ServiceBookingDto> getMyBookings(Object authenticationPrincipal,
                                                ServiceBookingStatus status,
                                                LocalDate fromDate,
                                                LocalDate toDate) {
        UserPrincipal principal = principal(authenticationPrincipal);
        List<ServiceBooking> bookings = serviceBookingRepository.findAllByUserIdOrderByCreatedAtDesc(principal.uid());
        return bookings.stream()
                .filter(booking -> matchesStatus(booking, status))
                .filter(booking -> matchesDateRange(booking, fromDate, toDate))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceBookingDto getMyBooking(UUID bookingId, Object authenticationPrincipal) {
        UserPrincipal principal = principal(authenticationPrincipal);
        ServiceBooking booking = serviceBookingRepository.findByIdAndUserId(bookingId, principal.uid())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        return toDto(booking);
    }

    private BigDecimal resolveDurationHours(LocalTime start, LocalTime end, BigDecimal requestedDuration) {
        if (start != null && end != null) {
            Duration duration = Duration.between(start, end);
            if (!duration.isNegative() && !duration.isZero()) {
                BigDecimal minutes = BigDecimal.valueOf(duration.toMinutes());
                return minutes.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            }
        }
        return requestedDuration;
    }

    @Transactional
    public ServiceBookingDto cancelMyBooking(UUID bookingId,
                                             CancelServiceBookingRequest request,
                                             Object authenticationPrincipal) {
        UserPrincipal principal = principal(authenticationPrincipal);
        ServiceBooking booking = serviceBookingRepository.findByIdAndUserId(bookingId, principal.uid())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (!canCancel(booking.getStatus())) {
            throw new IllegalStateException("Booking cannot be cancelled in current status: " + booking.getStatus());
        }

        booking.setStatus(ServiceBookingStatus.CANCELLED);
        booking.setRejectionReason(trimToNull(request.getReason()));
        if (booking.getPaymentStatus() == ServicePaymentStatus.UNPAID) {
            booking.setPaymentStatus(ServicePaymentStatus.CANCELLED);
        }

        return toDto(booking);
    }

    @Transactional
    public ServiceBookingDto acceptTerms(UUID bookingId,
                                         AcceptServiceBookingTermsRequest request,
                                         Object authenticationPrincipal) {
        UserPrincipal principal = principal(authenticationPrincipal);
        ServiceBooking booking = serviceBookingRepository.findByIdAndUserId(bookingId, principal.uid())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        booking.setTermsAccepted(Boolean.TRUE.equals(request.getAccepted()));
        return toDto(booking);
    }

    @Transactional
    public ServiceBookingDto addBookingItem(UUID bookingId,
                                            CreateServiceBookingItemRequest request,
                                            Object authenticationPrincipal,
                                            boolean allowManageAny) {
        ServiceBooking booking = loadBookingForMutation(bookingId, authenticationPrincipal, allowManageAny);
        ensurePending(booking);
        ServiceBookingItem item = buildItem(booking, request);
        booking.getBookingItems().add(item);
        booking.setTotalAmount(calculateTotalAmount(booking));

        return toDto(booking);
    }

    @Transactional
    public ServiceBookingDto updateBookingItem(UUID bookingId,
                                               UUID itemId,
                                               UpdateServiceBookingItemRequest request,
                                               Object authenticationPrincipal,
                                               boolean allowManageAny) {
        ServiceBooking booking = loadBookingForMutation(bookingId, authenticationPrincipal, allowManageAny);
        ensurePending(booking);

        ServiceBookingItem item = serviceBookingItemRepository.findByIdAndBookingId(itemId, booking.getId())
                .orElseThrow(() -> new IllegalArgumentException("Booking item not found: " + itemId));

        if (StringUtils.hasText(request.getItemName())) {
            item.setItemName(request.getItemName().trim());
        }
        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }
        if (request.getUnitPrice() != null) {
            item.setUnitPrice(request.getUnitPrice());
        }
        if (request.getTotalPrice() != null) {
            item.setTotalPrice(request.getTotalPrice());
        } else {
            item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        if (request.getMetadata() != null) {
            item.setMetadata(request.getMetadata());
        }

        booking.setTotalAmount(calculateTotalAmount(booking));
        return toDto(booking);
    }

    @Transactional
    public ServiceBookingDto deleteBookingItem(UUID bookingId,
                                               UUID itemId,
                                               Object authenticationPrincipal,
                                               boolean allowManageAny) {
        ServiceBooking booking = loadBookingForMutation(bookingId, authenticationPrincipal, allowManageAny);
        ensurePending(booking);

        ServiceBookingItem item = serviceBookingItemRepository.findByIdAndBookingId(itemId, booking.getId())
                .orElseThrow(() -> new IllegalArgumentException("Booking item not found: " + itemId));

        booking.getBookingItems().remove(item);
        booking.setTotalAmount(calculateTotalAmount(booking));

        return toDto(booking);
    }

    @Transactional
    public ServiceBookingDto updateBookingSlots(UUID bookingId,
                                                UpdateServiceBookingSlotsRequest request,
                                                Object authenticationPrincipal,
                                                boolean allowManageAny) {
        ServiceBooking booking = loadBookingForMutation(bookingId, authenticationPrincipal, allowManageAny);
        ensurePending(booking);

        booking.getBookingSlots().clear();
        booking.getBookingSlots().addAll(buildSlots(booking, request.getSlots(),
                booking.getBookingDate(), booking.getStartTime(), booking.getEndTime()));

        return toDto(booking);
    }

    @Transactional(readOnly = true)
    public List<ServiceBookingDto> searchBookings(ServiceBookingStatus status,
                                                  UUID serviceId,
                                                  UUID userId,
                                                  LocalDate fromDate,
                                                  LocalDate toDate) {
        List<ServiceBooking> bookings = serviceBookingRepository.findAll();
        bookings.sort(Comparator.comparing(ServiceBooking::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return bookings.stream()
                .filter(booking -> status == null || booking.getStatus() == status)
                .filter(booking -> serviceId == null || booking.getService().getId().equals(serviceId))
                .filter(booking -> userId == null || booking.getUserId().equals(userId))
                .filter(booking -> matchesDateRange(booking, fromDate, toDate))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceBookingDto getBooking(UUID bookingId) {
        ServiceBooking booking = serviceBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        return toDto(booking);
    }

    @Transactional(readOnly = true)
    public ServiceBookingCatalogDto getBookingCatalog(UUID serviceId) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        List<ServiceComboDto> combos = service.getCombos().stream()
                .filter(Objects::nonNull)
                .filter(combo -> Boolean.TRUE.equals(combo.getIsActive()))
                .map(serviceConfigService::toComboDto)
                .collect(Collectors.toList());

        List<ServiceOptionDto> options = service.getOptions().stream()
                .filter(Objects::nonNull)
                .filter(option -> Boolean.TRUE.equals(option.getIsActive()))
                .map(serviceConfigService::toOptionDto)
                .collect(Collectors.toList());

        List<ServiceOptionGroupDto> optionGroups = service.getOptionGroups().stream()
                .filter(Objects::nonNull)
                .map(serviceConfigService::toOptionGroupDto)
                .collect(Collectors.toList());

        List<ServiceTicketDto> tickets = service.getTickets().stream()
                .filter(Objects::nonNull)
                .filter(ticket -> Boolean.TRUE.equals(ticket.getIsActive()))
                .map(serviceConfigService::toTicketDto)
                .collect(Collectors.toList());

        return ServiceBookingCatalogDto.builder()
                .serviceId(service.getId())
                .serviceCode(service.getCode())
                .serviceName(service.getName())
                .pricingType(service.getPricingType())
                .combos(combos)
                .options(options)
                .optionGroups(optionGroups)
                .tickets(tickets)
                .build();
    }

    @Transactional
    public ServiceBookingDto approveBooking(UUID bookingId,
                                            AdminApproveServiceBookingRequest request,
                                            UUID approverId) {
        ServiceBooking booking = serviceBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != ServiceBookingStatus.PENDING) {
            throw new IllegalStateException("Only pending bookings can be approved");
        }
        booking.setStatus(ServiceBookingStatus.APPROVED);
        booking.setApprovedBy(approverId);
        booking.setApprovedAt(OffsetDateTime.now());
        booking.setRejectionReason(null);
        return toDto(booking);
    }

    @Transactional
    public ServiceBookingDto rejectBooking(UUID bookingId,
                                           AdminRejectServiceBookingRequest request,
                                           UUID approverId) {
        ServiceBooking booking = serviceBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() == ServiceBookingStatus.REJECTED || booking.getStatus() == ServiceBookingStatus.CANCELLED) {
            return toDto(booking);
        }
        booking.setStatus(ServiceBookingStatus.REJECTED);
        booking.setApprovedBy(approverId);
        booking.setApprovedAt(OffsetDateTime.now());
        booking.setRejectionReason(trimToNull(request.getRejectionReason()));
        if (booking.getPaymentStatus() == ServicePaymentStatus.UNPAID) {
            booking.setPaymentStatus(ServicePaymentStatus.CANCELLED);
        }
        return toDto(booking);
    }

    @Transactional
    public ServiceBookingDto completeBooking(UUID bookingId,
                                             AdminCompleteServiceBookingRequest request,
                                             UUID operatorId) {
        ServiceBooking booking = serviceBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != ServiceBookingStatus.APPROVED) {
            throw new IllegalStateException("Only approved bookings can be completed");
        }
        booking.setStatus(ServiceBookingStatus.COMPLETED);
        booking.setApprovedBy(operatorId);
        booking.setApprovedAt(OffsetDateTime.now());
        return toDto(booking);
    }

    @Transactional
    public ServiceBookingDto updatePayment(UUID bookingId,
                                           AdminUpdateServiceBookingPaymentRequest request) {
        ServiceBooking booking = serviceBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        booking.setPaymentStatus(request.getPaymentStatus());
        booking.setPaymentDate(request.getPaymentDate());
        booking.setPaymentGateway(trimToNull(request.getPaymentGateway()));
        booking.setVnpayTransactionRef(trimToNull(request.getTransactionReference()));

        return toDto(booking);
    }

    private ServiceBooking loadBookingForMutation(UUID bookingId,
                                                  Object authenticationPrincipal,
                                                  boolean allowManageAny) {
        if (allowManageAny) {
            return serviceBookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        }
        UserPrincipal principal = principal(authenticationPrincipal);
        return serviceBookingRepository.findByIdAndUserId(bookingId, principal.uid())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
    }

    private boolean matchesStatus(ServiceBooking booking, ServiceBookingStatus status) {
        return status == null || booking.getStatus() == status;
    }

    private boolean matchesDateRange(ServiceBooking booking, LocalDate from, LocalDate to) {
        LocalDate bookingDate = booking.getBookingDate();
        if (from != null && bookingDate.isBefore(from)) {
            return false;
        }
        if (to != null && bookingDate.isAfter(to)) {
            return false;
        }
        return true;
    }

    private boolean canCancel(ServiceBookingStatus status) {
        return status == ServiceBookingStatus.PENDING || status == ServiceBookingStatus.APPROVED;
    }

    private void ensurePending(ServiceBooking booking) {
        if (booking.getStatus() != ServiceBookingStatus.PENDING) {
            throw new IllegalStateException("Only pending bookings can be modified");
        }
    }

    private List<ServiceBookingItem> buildItems(ServiceBooking booking,
                                                List<CreateServiceBookingItemRequest> requests) {
        ServiceItemCatalog catalog = loadItemCatalog(booking);
        return requests.stream()
                .map(request -> buildItem(booking, request, catalog))
                .collect(Collectors.toList());
    }

    private ServiceBookingItem buildItem(ServiceBooking booking,
                                         CreateServiceBookingItemRequest request) {
        return buildItem(booking, request, null);
    }

    private ServiceBookingItem buildItem(ServiceBooking booking,
                                         CreateServiceBookingItemRequest request,
                                         ServiceItemCatalog catalog) {
        ServiceBookingItem item = new ServiceBookingItem();
        item.setBooking(booking);
        item.setItemType(request.getItemType());
        item.setItemId(request.getItemId());
        ItemSource source = resolveItemSource(booking.getService().getId(), request, catalog);
        String code = StringUtils.hasText(request.getItemCode()) ? request.getItemCode().trim() : source.code();
        String name = StringUtils.hasText(request.getItemName()) ? request.getItemName().trim() : source.name();

        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Booking item code is required");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Booking item name is required");
        }
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        if (quantity <= 0) {
            throw new IllegalArgumentException("Booking item quantity must be positive");
        }
        BigDecimal unitPrice = source.unitPrice;
        if (unitPrice == null) {
            throw new IllegalArgumentException("Booking item unit price is required");
        }
        BigDecimal totalPrice = request.getTotalPrice() != null
                ? request.getTotalPrice()
                : unitPrice.multiply(BigDecimal.valueOf(quantity));
        item.setItemCode(code);
        item.setItemName(name);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setTotalPrice(totalPrice);
        item.setMetadata(request.getMetadata());
        return item;
    }
    private List<ServiceBookingSlot> buildSlots(ServiceBooking booking,
                                                List<ServiceBookingSlotRequest> requests,
                                                LocalDate fallbackDate,
                                                LocalTime fallbackStart,
                                                LocalTime fallbackEnd) {
        Map<Integer, List<ServiceAvailability>> availabilityCache = new HashMap<>();
        List<ServiceBookingSlot> slots = new ArrayList<>();
        if (requests == null || requests.isEmpty()) {
            slots.add(buildSlot(booking, fallbackDate, fallbackStart, fallbackEnd, availabilityCache, slots));
            return slots;
        }
        for (ServiceBookingSlotRequest request : requests) {
            slots.add(buildSlot(booking, request.getSlotDate(), request.getStartTime(), request.getEndTime(), availabilityCache, slots));
        }
        return slots;
    }

    private ServiceBookingSlot buildSlot(ServiceBooking booking,
                                         LocalDate date,
                                         LocalTime start,
                                         LocalTime end,
                                         Map<Integer, List<ServiceAvailability>> availabilityCache,
                                         List<ServiceBookingSlot> existingSlots) {
        validateSlot(booking, date, start, end, availabilityCache, existingSlots);
        ServiceBookingSlot slot = new ServiceBookingSlot();
        slot.setBooking(booking);
        slot.setService(booking.getService());
        slot.setSlotDate(date);
        slot.setStartTime(start);
        slot.setEndTime(end);
        return slot;
    }

    private BigDecimal calculateTotalAmount(ServiceBooking booking) {
        BigDecimal baseCharge = calculateBaseCharge(booking);
        BigDecimal itemsTotal = booking.getBookingItems().stream()
                .map(item -> {
                    if (item.getTotalPrice() != null) {
                        return item.getTotalPrice();
                    }
                    BigDecimal unitPrice = item.getUnitPrice();
                    Integer quantity = item.getQuantity();
                    if (unitPrice == null || quantity == null) {
                        return BigDecimal.ZERO;
                    }
                    return unitPrice.multiply(BigDecimal.valueOf(quantity));
                })
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return baseCharge.add(itemsTotal);
    }

    private BigDecimal calculateBaseCharge(ServiceBooking booking) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = booking.getService();
        if (service == null || service.getPricingType() == null) {
            return BigDecimal.ZERO;
        }
        return switch (service.getPricingType()) {
            case HOURLY -> calculateHourlyCharge(service.getPricePerHour(), booking);
            case SESSION -> defaultCharge(service.getPricePerSession());
            case FREE -> BigDecimal.ZERO;
        };
    }

    private BigDecimal calculateHourlyCharge(BigDecimal pricePerHour, ServiceBooking booking) {
        if (pricePerHour == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal durationHours = booking.getDurationHours();
        if (durationHours == null) {
            if (booking.getStartTime() != null && booking.getEndTime() != null && booking.getEndTime().isAfter(booking.getStartTime())) {
                long minutes = Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
                if (minutes > 0) {
                    durationHours = BigDecimal.valueOf(minutes)
                            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
                }
            }
        }
        if (durationHours == null || durationHours.compareTo(BigDecimal.ZERO) <= 0) {
            durationHours = BigDecimal.ONE;
        }
        return pricePerHour.multiply(durationHours);
    }

    private BigDecimal defaultCharge(BigDecimal price) {
        return price != null ? price : BigDecimal.ZERO;
    }


    private void validateSlot(ServiceBooking booking,
                              LocalDate date,
                              LocalTime start,
                              LocalTime end,
                              Map<Integer, List<ServiceAvailability>> availabilityCache,
                              List<ServiceBookingSlot> existingSlots) {
        if (date == null) {
            throw new IllegalArgumentException("Slot date is required");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Slot start time and end time are required");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Slot end time must be after start time");
        }

        UUID serviceId = booking.getService().getId();
        ensureSlotWithinAvailability(serviceId, date, start, end, availabilityCache);
        ensureSlotNotOverlapping(serviceId, date, start, end, booking.getId());
        ensureNoInternalOverlap(date, start, end, existingSlots);
    }

    private void ensureSlotWithinAvailability(UUID serviceId,
                                              LocalDate date,
                                              LocalTime start,
                                              LocalTime end,
                                              Map<Integer, List<ServiceAvailability>> availabilityCache) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        List<ServiceAvailability> availabilities = availabilityCache.computeIfAbsent(dayOfWeek,
                key -> serviceAvailabilityRepository.findByServiceIdAndDayOfWeekAndIsAvailableTrueOrderByStartTimeAsc(serviceId, key));

        if (availabilities.isEmpty()) {
            throw new IllegalArgumentException("Service has no available slots configured for " + date.getDayOfWeek());
        }

        boolean matches = availabilities.stream()
                .anyMatch(availability ->
                        !start.isBefore(availability.getStartTime()) && !end.isAfter(availability.getEndTime()));

        if (!matches) {
            throw new IllegalArgumentException(String.format(
                    "Requested slot %s %s-%s is outside service availability", date, start, end));
        }
    }

    private void ensureSlotNotOverlapping(UUID serviceId,
                                          LocalDate date,
                                          LocalTime start,
                                          LocalTime end,
                                          UUID bookingId) {
        boolean overlap;
        if (bookingId == null) {
            overlap = serviceBookingSlotRepository
                    .existsByServiceIdAndSlotDateAndStartTimeLessThanAndEndTimeGreaterThan(serviceId, date, end, start);
        } else {
            overlap = serviceBookingSlotRepository
                    .existsByServiceIdAndSlotDateAndStartTimeLessThanAndEndTimeGreaterThanAndBooking_IdNot(
                            serviceId, date, end, start, bookingId);
        }
        if (overlap) {
            throw new IllegalArgumentException(String.format(
                    "Requested slot %s %s-%s is already booked for this service", date, start, end));
        }
    }

    private void ensureNoInternalOverlap(LocalDate date,
                                         LocalTime start,
                                         LocalTime end,
                                         List<ServiceBookingSlot> existingSlots) {
        for (ServiceBookingSlot existing : existingSlots) {
            if (existing.getSlotDate() != null && existing.getSlotDate().equals(date)) {
                if (start.isBefore(existing.getEndTime()) && end.isAfter(existing.getStartTime())) {
                    throw new IllegalArgumentException(String.format(
                            "Requested slot %s %s-%s overlaps with another slot in this booking request (%s-%s)",
                            date, start, end, existing.getStartTime(), existing.getEndTime()));
                }
            }
        }
    }

    private ServiceBookingDto toDto(ServiceBooking booking) {
        if (booking == null) {
            return null;
        }
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = booking.getService();
        return ServiceBookingDto.builder()
                .id(booking.getId())
                .serviceId(service != null ? service.getId() : null)
                .serviceCode(service != null ? service.getCode() : null)
                .serviceName(service != null ? service.getName() : null)
                .servicePricingType(service != null ? service.getPricingType() : null)
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .durationHours(booking.getDurationHours())
                .numberOfPeople(booking.getNumberOfPeople())
                .purpose(booking.getPurpose())
                .totalAmount(booking.getTotalAmount())
                .paymentStatus(booking.getPaymentStatus())
                .paymentDate(booking.getPaymentDate())
                .paymentGateway(booking.getPaymentGateway())
                .vnpayTransactionRef(booking.getVnpayTransactionRef())
                .status(booking.getStatus())
                .userId(booking.getUserId())
                .approvedBy(booking.getApprovedBy())
                .approvedAt(booking.getApprovedAt())
                .rejectionReason(booking.getRejectionReason())
                .termsAccepted(booking.getTermsAccepted())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .bookingItems(toItemDtos(booking.getBookingItems()))
                .bookingSlots(toSlotDtos(booking.getBookingSlots()))
                .build();
    }

    private List<ServiceBookingItemDto> toItemDtos(List<ServiceBookingItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .map(this::toItemDto)
                .collect(Collectors.toList());
    }

    private ServiceBookingItemDto toItemDto(ServiceBookingItem item) {
        return ServiceBookingItemDto.builder()
                .id(item.getId())
                .bookingId(item.getBooking() != null ? item.getBooking().getId() : null)
                .itemType(item.getItemType())
                .itemId(item.getItemId())
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .metadata(normalizeMetadata(item.getMetadata()))
                .createdAt(item.getCreatedAt())
                .build();
    }

    private List<ServiceBookingSlotDto> toSlotDtos(List<ServiceBookingSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return List.of();
        }
        return slots.stream()
                .filter(Objects::nonNull)
                .map(this::toSlotDto)
                .collect(Collectors.toList());
    }

    private ServiceBookingSlotDto toSlotDto(ServiceBookingSlot slot) {
        return ServiceBookingSlotDto.builder()
                .id(slot.getId())
                .bookingId(slot.getBooking() != null ? slot.getBooking().getId() : null)
                .serviceId(slot.getService() != null ? slot.getService().getId() : null)
                .slotDate(slot.getSlotDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .createdAt(slot.getCreatedAt())
                .build();
    }

    private java.util.Map<String, Object> normalizeMetadata(Object metadata) {
        if (metadata == null) {
            return null;
        }
        if (metadata instanceof java.util.Map<?, ?> map) {
            return map.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            entry -> String.valueOf(entry.getKey()),
                            java.util.Map.Entry::getValue
                    ));
        }
        return Collections.singletonMap("value", metadata);
    }

    private ServiceItemCatalog loadItemCatalog(ServiceBooking booking) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = booking.getService();
        if (service == null || service.getId() == null) {
            throw new IllegalStateException("Booking must reference a persisted service");
        }
        UUID serviceId = service.getId();

        Map<UUID, ServiceCombo> combos = serviceComboRepository.findAllByServiceId(serviceId).stream()
                .collect(Collectors.toMap(ServiceCombo::getId, combo -> combo));
        Map<UUID, ServiceOption> options = serviceOptionRepository.findAllByServiceId(serviceId).stream()
                .collect(Collectors.toMap(ServiceOption::getId, option -> option));
        Map<UUID, ServiceTicket> tickets = serviceTicketRepository.findAllByServiceId(serviceId).stream()
                .collect(Collectors.toMap(ServiceTicket::getId, ticket -> ticket));
        Map<UUID, ServiceOptionGroup> optionGroups = serviceOptionGroupRepository.findAllByServiceId(serviceId).stream()
                .collect(Collectors.toMap(ServiceOptionGroup::getId, group -> group));

        return new ServiceItemCatalog(combos, options, tickets, optionGroups);
    }

    private ItemSource resolveItemSource(UUID serviceId,
                                         CreateServiceBookingItemRequest request,
                                         ServiceItemCatalog catalog) {
        ServiceBookingItemType itemType = request.getItemType();
        UUID itemId = request.getItemId();
        if (itemType == null || itemId == null) {
            throw new IllegalArgumentException("Booking item type and item ID are required");
        }

        switch (itemType) {
            case COMBO -> {
                ServiceCombo combo = catalog != null
                        ? catalog.combos().get(itemId)
                        : serviceComboRepository.findById(itemId).orElse(null);
                if (combo == null || combo.getService() == null || !combo.getService().getId().equals(serviceId)) {
                    throw new IllegalArgumentException("Combo does not belong to the service: " + itemId);
                }
                BigDecimal priceOfItem =  combo.getPrice();
                return new ItemSource(combo.getCode(), combo.getName(), priceOfItem);
            }
            case OPTION -> {
                ServiceOption option = catalog != null
                        ? catalog.options().get(itemId)
                        : serviceOptionRepository.findById(itemId).orElse(null);
                if (option == null || option.getService() == null || !option.getService().getId().equals(serviceId)) {
                    throw new IllegalArgumentException("Option does not belong to the service: " + itemId);
                }
                BigDecimal priceOfItem =  option.getPrice();
                return new ItemSource(option.getCode(), option.getName(), priceOfItem);
            }
            case TICKET -> {
                ServiceTicket ticket = catalog != null
                        ? catalog.tickets().get(itemId)
                        : serviceTicketRepository.findById(itemId).orElse(null);
                if (ticket == null || ticket.getService() == null || !ticket.getService().getId().equals(serviceId)) {
                    throw new IllegalArgumentException("Ticket does not belong to the service: " + itemId);
                }
                BigDecimal priceOfItem =  ticket.getPrice();
                return new ItemSource(ticket.getCode(),ticket.getName(), priceOfItem);
            }

            default -> throw new IllegalArgumentException("Unsupported booking item type: " + itemType);
        }
    }

    private record ServiceItemCatalog(
            Map<UUID, ServiceCombo> combos,
            Map<UUID, ServiceOption> options,
            Map<UUID, ServiceTicket> tickets,
            Map<UUID, ServiceOptionGroup> optionGroups
    ) {
    }

    private record ItemSource(String code, String name, BigDecimal unitPrice) {
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}


 