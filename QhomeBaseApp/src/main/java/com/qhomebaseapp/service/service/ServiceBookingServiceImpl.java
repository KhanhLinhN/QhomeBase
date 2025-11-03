package com.qhomebaseapp.service.service;

import com.qhomebaseapp.dto.service.AvailableServiceDto;
import com.qhomebaseapp.dto.service.ServiceBookingRequestDto;
import com.qhomebaseapp.dto.service.ServiceBookingResponseDto;
import com.qhomebaseapp.dto.service.ServiceDto;
import com.qhomebaseapp.dto.service.ServiceTypeDto;
import com.qhomebaseapp.model.Service;
import com.qhomebaseapp.model.ServiceAvailability;
import com.qhomebaseapp.model.ServiceBooking;
import com.qhomebaseapp.model.ServiceBookingSlot;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.repository.service.*;
import com.qhomebaseapp.repository.UserRepository;
import com.qhomebaseapp.service.user.EmailService;
import com.qhomebaseapp.service.vnpay.VnpayService;
import com.qhomebaseapp.config.VnpayProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceBookingServiceImpl implements ServiceBookingService {

    private final ServiceRepository serviceRepository;
    private final ServiceBookingRepository bookingRepository;
    private final ServiceAvailabilityRepository availabilityRepository;
    private final ServiceBookingSlotRepository slotRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;

    @Override
    public List<ServiceDto> getServicesByCategory(Long categoryId) {
        List<Service> services = serviceRepository.findByCategory_IdAndIsActiveTrue(categoryId);
        return services.stream().map(this::toServiceDto).collect(Collectors.toList());
    }

    @Override
    public List<ServiceDto> getServicesByCategoryCode(String categoryCode) {
        List<Service> services = serviceRepository.findByCategory_CodeAndIsActiveTrue(categoryCode);
        return services.stream().map(this::toServiceDto).collect(Collectors.toList());
    }

    @Override
    public ServiceDto getServiceById(Long serviceId) {
        return serviceRepository.findById(serviceId)
                .map(this::toServiceDto)
                .orElseThrow(() -> new RuntimeException("Service not found"));
    }

    @Override
    public List<ServiceTypeDto> getServiceTypesByCategoryCode(String categoryCode) {
        List<Service> services = serviceRepository.findByCategory_CodeAndIsActiveTrue(categoryCode);
        
        // Group services by type (extract from code: e.g., "BBQ_ZONE_A" -> "BBQ")
        return services.stream()
                .collect(Collectors.groupingBy(service -> {
                    String code = service.getCode();
                    // Extract type from code (part before first underscore or whole code if no underscore)
                    int underscoreIndex = code.indexOf('_');
                    if (underscoreIndex > 0) {
                        return code.substring(0, underscoreIndex);
                    }
                    // Fallback: extract from name (e.g., "Khu nướng BBQ - Tòa A" -> "BBQ")
                    String name = service.getName();
                    if (name.contains("BBQ") || name.contains("bbq") || name.contains("Bbq")) {
                        return "BBQ";
                    }
                    if (name.contains("Tennis") || name.contains("tennis")) {
                        return "TENNIS";
                    }
                    if (name.contains("Pool") || name.contains("pool") || name.contains("Bơi") || name.contains("bơi")) {
                        return "POOL";
                    }
                    // Default: use code
                    return code;
                }))
                .entrySet().stream()
                .map(entry -> {
                    String typeCode = entry.getKey();
                    List<Service> typeServices = entry.getValue();
                    
                    // Get type name from first service's name or code
                    String typeName = typeServices.get(0).getName();
                    // Extract type name (e.g., "Khu nướng BBQ - Tòa A" -> "Nướng BBQ" or keep first part)
                    if (typeName.contains("BBQ") || typeName.contains("bbq")) {
                        typeName = "Nướng BBQ";
                    } else if (typeName.contains("Tennis") || typeName.contains("tennis")) {
                        typeName = "Sân Tennis";
                    } else if (typeName.contains("Pool") || typeName.contains("pool") || typeName.contains("Bơi")) {
                        typeName = "Hồ Bơi";
                    } else {
                        // Extract meaningful part from name (before first "-" or "Tòa")
                        int dashIndex = typeName.indexOf(" - ");
                        if (dashIndex > 0) {
                            typeName = typeName.substring(0, dashIndex).trim();
                        } else {
                            int toaIndex = typeName.indexOf("Tòa");
                            if (toaIndex > 0) {
                                typeName = typeName.substring(0, toaIndex).trim();
                            }
                        }
                    }
                    
                    // Get description from first service
                    String description = typeServices.get(0).getDescription();
                    if (description != null && description.length() > 100) {
                        description = description.substring(0, 100) + "...";
                    }
                    
                    return ServiceTypeDto.builder()
                            .typeCode(typeCode)
                            .typeName(typeName)
                            .description(description)
                            .serviceCount(typeServices.size())
                            .build();
                })
                .sorted((a, b) -> a.getTypeName().compareTo(b.getTypeName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceDto> getServicesByCategoryCodeAndType(String categoryCode, String serviceType) {
        List<Service> services = serviceRepository.findByCategory_CodeAndIsActiveTrue(categoryCode);
        
        // Filter services by type
        return services.stream()
                .filter(service -> {
                    String code = service.getCode();
                    // Check if service code starts with type
                    if (code.startsWith(serviceType + "_") || code.equals(serviceType)) {
                        return true;
                    }
                    // Check in name as fallback
                    String name = service.getName();
                    String typeUpper = serviceType.toUpperCase();
                    return (name.contains("BBQ") || name.contains("bbq")) && typeUpper.contains("BBQ")
                            || (name.contains("Tennis") || name.contains("tennis")) && typeUpper.contains("TENNIS")
                            || (name.contains("Pool") || name.contains("pool") || name.contains("Bơi")) && typeUpper.contains("POOL");
                })
                .map(this::toServiceDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AvailableServiceDto> getAvailableServices(
            Long categoryId, 
            LocalDate date, 
            LocalTime startTime, 
            LocalTime endTime) {
        
        List<Service> services = serviceRepository.findByCategory_IdAndIsActiveTrue(categoryId);
        
        return buildAvailableServiceDtoList(services, date, startTime, endTime);
    }

    @Override
    public List<AvailableServiceDto> getAvailableServicesByCategoryCode(
            String categoryCode,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime) {
        
        List<Service> services = serviceRepository.findByCategory_CodeAndIsActiveTrue(categoryCode);
        
        return buildAvailableServiceDtoList(services, date, startTime, endTime);
    }

    @Override
    public List<AvailableServiceDto> getAvailableServicesByCategoryCodeAndType(
            String categoryCode,
            String serviceType,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime) {
        
        List<Service> services = serviceRepository.findByCategory_CodeAndIsActiveTrue(categoryCode);
        
        // Filter services by type
        List<Service> filteredServices = services.stream()
                .filter(service -> {
                    String code = service.getCode();
                    // Check if service code starts with type
                    if (code.startsWith(serviceType + "_") || code.equals(serviceType)) {
                        return true;
                    }
                    // Check in name as fallback
                    String name = service.getName();
                    String typeUpper = serviceType.toUpperCase();
                    return (name.contains("BBQ") || name.contains("bbq")) && typeUpper.contains("BBQ")
                            || (name.contains("Tennis") || name.contains("tennis")) && typeUpper.contains("TENNIS")
                            || (name.contains("Pool") || name.contains("pool") || name.contains("Bơi")) && typeUpper.contains("POOL");
                })
                .collect(Collectors.toList());
        
        return buildAvailableServiceDtoList(filteredServices, date, startTime, endTime);
    }

    private List<AvailableServiceDto> buildAvailableServiceDtoList(
            List<Service> services,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime) {
        
        return services.stream()
                .map(service -> {
                    boolean isAvailable = checkServiceAvailability(service.getId(), date, startTime, endTime);
                    List<ServiceBooking> existingBookings = bookingRepository.findOverlappingBookings(
                            service.getId(), date, startTime, endTime);
                    
                    // Calculate total booked people in the time range
                    Integer bookedPeople = bookingRepository.sumBookedPeople(
                            service.getId(), date, startTime, endTime);
                    if (bookedPeople == null) bookedPeople = 0;
                    
                    int maxCapacity = service.getMaxCapacity() != null ? service.getMaxCapacity() : 0;
                    int availableCapacity = Math.max(0, maxCapacity - bookedPeople);
                    
                    int currentBookings = existingBookings.size();
                    String availabilityStatus;
                    if (!isAvailable || bookedPeople >= maxCapacity) {
                        availabilityStatus = "FULL";
                    } else if (bookedPeople == 0) {
                        availabilityStatus = "AVAILABLE";
                    } else {
                        availabilityStatus = "PARTIAL";
                    }
                    
                    // Calculate estimated amount
                    long hours = ChronoUnit.HOURS.between(startTime, endTime);
                    if (hours <= 0) hours = 1;
                    BigDecimal estimatedAmount = service.getPricePerHour() != null ? 
                            service.getPricePerHour().multiply(BigDecimal.valueOf(hours)) : BigDecimal.ZERO;
                    
                    return AvailableServiceDto.builder()
                            .id(service.getId())
                            .code(service.getCode())
                            .name(service.getName())
                            .description(service.getDescription())
                            .location(service.getLocation())
                            .mapUrl(service.getMapUrl())
                            .pricePerHour(service.getPricePerHour())
                            .estimatedTotalAmount(estimatedAmount)
                            .maxCapacity(maxCapacity)
                            .minDurationHours(service.getMinDurationHours())
                            .maxDurationHours(service.getMaxDurationHours())
                            .rules(service.getRules())
                            .isAvailable(isAvailable && availableCapacity > 0)
                            .availabilityStatus(availabilityStatus)
                            .currentBookings(currentBookings)
                            .bookedPeople(bookedPeople)
                            .availableCapacity(availableCapacity)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private boolean checkServiceAvailability(Long serviceId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int dayOfWeekValue = dayOfWeek.getValue() % 7; 
        
        List<ServiceAvailability> availabilities = availabilityRepository
                .findByService_IdAndIsAvailableTrue(serviceId);
        
        boolean hasSchedule = availabilities.stream()
                .anyMatch(avail -> avail.getDayOfWeek().equals(dayOfWeekValue) &&
                        !startTime.isBefore(avail.getStartTime()) &&
                        !endTime.isAfter(avail.getEndTime()));
        
        if (!hasSchedule) {
            return false;
        }
        
        List<ServiceBooking> overlapping = bookingRepository.findOverlappingBookings(
                serviceId, date, startTime, endTime);
        
        return overlapping.isEmpty();
    }

    @Override
    @Transactional
    public ServiceBookingResponseDto createBooking(ServiceBookingRequestDto request, Long userId) {
        try {
            log.info("Creating booking for user {}, service {}, date {}, time {} - {}", 
                    userId, request.getServiceId(), request.getBookingDate(), 
                    request.getStartTime(), request.getEndTime());
            
            Service service = serviceRepository.findById(request.getServiceId())
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            
            log.info("Service found: {}", service.getId());
            
            // Validate booking time must be in the future (at least 1 hour from now)
            ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
            ZonedDateTime nowVietnam = ZonedDateTime.now(vietnamZone);
            LocalDateTime bookingDateTime = LocalDateTime.of(request.getBookingDate(), request.getStartTime());
            ZonedDateTime bookingDateTimeVietnam = bookingDateTime.atZone(vietnamZone);
            
            // Check if booking time is at least 1 hour from now
            long hoursUntilBooking = ChronoUnit.HOURS.between(nowVietnam, bookingDateTimeVietnam);
            if (hoursUntilBooking < 1) {
                log.warn("Booking time {} is too soon. Current time: {}", bookingDateTimeVietnam, nowVietnam);
                throw new RuntimeException("Thời gian đặt chỗ phải ít nhất 1 giờ sau thời điểm hiện tại. " +
                        "Thời gian hiện tại: " + nowVietnam.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ". " +
                        "Vui lòng chọn thời gian hợp lệ.");
            }
            
            // Check if booking date is in the past
            if (request.getBookingDate().isBefore(nowVietnam.toLocalDate())) {
                log.warn("Booking date {} is in the past. Current date: {}", request.getBookingDate(), nowVietnam.toLocalDate());
                throw new RuntimeException("Không thể đặt chỗ cho ngày trong quá khứ. Vui lòng chọn ngày hợp lệ.");
            }
            
            // Check if booking time is today but in the past
            if (request.getBookingDate().equals(nowVietnam.toLocalDate()) && 
                request.getStartTime().isBefore(nowVietnam.toLocalTime())) {
                log.warn("Booking time {} is in the past today. Current time: {}", 
                        request.getStartTime(), nowVietnam.toLocalTime());
                throw new RuntimeException("Không thể đặt chỗ cho thời gian trong quá khứ. Vui lòng chọn thời gian hợp lệ.");
            }
            
            // Check if user has any unpaid bookings
            List<ServiceBooking> unpaidBookings = bookingRepository.findByUser_IdAndPaymentStatus(
                    userId, "UNPAID");
            if (!unpaidBookings.isEmpty()) {
                log.warn("User {} has {} unpaid bookings. Cannot create new booking until payment is completed.", 
                        userId, unpaidBookings.size());
                throw new RuntimeException("Bạn có dịch vụ chưa thanh toán. Vui lòng thanh toán trước khi đặt dịch vụ mới.");
            }
            
            if (!checkServiceAvailability(service.getId(), request.getBookingDate(), 
                    request.getStartTime(), request.getEndTime())) {
                log.warn("Service {} is not available for date {} and time {} - {}", 
                        service.getId(), request.getBookingDate(), 
                        request.getStartTime(), request.getEndTime());
                throw new RuntimeException("Service is not available for the selected time slot");
            }
            
            // Validate number of people based on capacity
            int requestedPeople = request.getNumberOfPeople() != null ? request.getNumberOfPeople() : 1;
            int maxCapacity = service.getMaxCapacity() != null ? service.getMaxCapacity() : 0;
            
            if (maxCapacity > 0 && requestedPeople > maxCapacity) {
                log.warn("Requested {} people exceeds max capacity {} for service {}", 
                        requestedPeople, maxCapacity, service.getId());
                throw new RuntimeException("Số người tham gia (" + requestedPeople + 
                        ") vượt quá sức chứa tối đa của khu vực (" + maxCapacity + " người)");
            }
            
            // Check available capacity in the time range
            Integer bookedPeople = bookingRepository.sumBookedPeople(
                    service.getId(), request.getBookingDate(), 
                    request.getStartTime(), request.getEndTime());
            if (bookedPeople == null) bookedPeople = 0;
            
            int availableCapacity = maxCapacity - bookedPeople;
            if (requestedPeople > availableCapacity) {
                log.warn("Requested {} people but only {} available ({} booked out of {} max)", 
                        requestedPeople, availableCapacity, bookedPeople, maxCapacity);
                throw new RuntimeException("Khu vực này chỉ còn " + availableCapacity + 
                        " chỗ trống (đã có " + bookedPeople + "/" + maxCapacity + " người đặt). " +
                        "Vui lòng giảm số người tham gia hoặc chọn thời gian khác.");
            }
            
            List<ServiceBooking> overlapping = bookingRepository.findOverlappingBookings(
                    service.getId(), request.getBookingDate(), 
                    request.getStartTime(), request.getEndTime());
            if (!overlapping.isEmpty()) {
                log.warn("Time slot is already booked. Overlapping bookings: {}", overlapping.size());
                throw new RuntimeException("Time slot is already booked");
            }
            
            long hours = ChronoUnit.HOURS.between(request.getStartTime(), request.getEndTime());
            if (hours <= 0) hours = 1;
            BigDecimal durationHours = BigDecimal.valueOf(hours);
            BigDecimal totalAmount = service.getPricePerHour() != null ?
                    service.getPricePerHour().multiply(durationHours) : BigDecimal.ZERO;
            
            log.info("Calculated duration: {} hours, total amount: {}", durationHours, totalAmount);
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            log.info("User found: {}", user.getId());
            
            ServiceBooking booking = ServiceBooking.builder()
                    .service(service)
                    .user(user)
                    .bookingDate(request.getBookingDate())
                    .startTime(request.getStartTime())
                    .endTime(request.getEndTime())
                    .durationHours(durationHours)
                    .numberOfPeople(request.getNumberOfPeople() != null ? request.getNumberOfPeople() : 1)
                    .purpose(request.getPurpose())
                    .totalAmount(totalAmount)
                    .paymentStatus("UNPAID")
                    .status("PENDING")
                    .termsAccepted(request.getTermsAccepted() != null && request.getTermsAccepted())
                    .build();
            
            log.info("Saving booking...");
            booking = bookingRepository.save(booking);
            log.info("Booking saved with ID: {}", booking.getId());
            
            ServiceBookingSlot slot = ServiceBookingSlot.builder()
                    .booking(booking)
                    .service(service)
                    .slotDate(request.getBookingDate())
                    .startTime(request.getStartTime())
                    .endTime(request.getEndTime())
                    .build();
            
            log.info("Saving booking slot...");
            slotRepository.save(slot);
            log.info("Booking slot saved");
            
            return toBookingResponseDto(booking);
        } catch (Exception e) {
            log.error("Error creating booking: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<ServiceBookingResponseDto> getUserBookings(Long userId) {
        List<ServiceBooking> bookings = bookingRepository.findByUser_Id(userId);
        return bookings.stream()
                .map(this::toBookingResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public ServiceBookingResponseDto getBookingById(Long bookingId, Long userId) {
        ServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (!booking.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to booking");
        }
        
        return toBookingResponseDto(booking);
    }

    private ServiceDto toServiceDto(Service service) {
        return ServiceDto.builder()
                .id(service.getId())
                .categoryId(service.getCategory().getId())
                .categoryName(service.getCategory().getName())
                .code(service.getCode())
                .name(service.getName())
                .description(service.getDescription())
                .location(service.getLocation())
                .mapUrl(service.getMapUrl())
                .pricePerHour(service.getPricePerHour())
                .pricePerSession(service.getPricePerSession())
                .pricingType(service.getPricingType())
                .maxCapacity(service.getMaxCapacity())
                .minDurationHours(service.getMinDurationHours())
                .maxDurationHours(service.getMaxDurationHours())
                .advanceBookingDays(service.getAdvanceBookingDays())
                .rules(service.getRules())
                .isActive(service.getIsActive())
                .build();
    }

    private ServiceBookingResponseDto toBookingResponseDto(ServiceBooking booking) {
        return ServiceBookingResponseDto.builder()
                .id(booking.getId())
                .serviceId(booking.getService().getId())
                .serviceName(booking.getService().getName())
                .serviceLocation(booking.getService().getLocation())
                .userId(booking.getUser().getId())
                .userName(booking.getUser().getFullName())
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
                .termsAccepted(booking.getTermsAccepted())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    @Override
    public String createVnpayPaymentUrl(Long bookingId, Long userId, HttpServletRequest request) {
        ServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (!booking.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to booking");
        }
        
        if (!"UNPAID".equals(booking.getPaymentStatus())) {
            throw new RuntimeException("Booking is already paid");
        }
        
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        
        String orderInfo = "Thanh toán đặt chỗ " + booking.getService().getName() + " #" + bookingId;
        String baseUrl = vnpayProperties.getReturnUrl().replace("/api/invoices/vnpay/redirect", "");
        String returnUrl = baseUrl + "/api/service-booking/vnpay/redirect";
        
        return vnpayService.createPaymentUrl(
                bookingId, 
                orderInfo, 
                booking.getTotalAmount(), 
                clientIp, 
                returnUrl
        );
    }

    @Override
    @Transactional
    public void handleVnpayCallback(Long bookingId, String transactionRef, OffsetDateTime paymentDate, String userEmail) {
        ServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        booking.setPaymentStatus("PAID");
        booking.setPaymentDate(paymentDate);
        booking.setPaymentGateway("VNPAY");
        booking.setVnpayTransactionRef(transactionRef);
        booking.setStatus("APPROVED"); 
        booking.setUpdatedAt(OffsetDateTime.now());
        
        bookingRepository.save(booking);
        
        try {
            // Use provided email or get from booking user
            String emailToSend = userEmail;
            if (emailToSend == null || emailToSend.isEmpty()) {
                User user = booking.getUser();
                if (user != null && user.getEmail() != null) {
                    emailToSend = user.getEmail();
                }
            }
            
            if (emailToSend != null && !emailToSend.isEmpty()) {
                User user = booking.getUser();
                String emailSubject = "Thanh toán thành công - Đặt chỗ dịch vụ";
                String paymentDateStr = paymentDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                java.text.NumberFormat currencyFormat = java.text.NumberFormat.getNumberInstance(new java.util.Locale("vi", "VN"));
                String amountStr = currencyFormat.format(booking.getTotalAmount()) + " VNĐ";
                
                String emailBody = String.format(
                    "Xin chào %s,\n\n" +
                    "Thanh toán đặt chỗ dịch vụ của bạn đã được xử lý thành công!\n\n" +
                    "Thông tin đặt chỗ:\n" +
                    "- Dịch vụ: %s\n" +
                    "- Vị trí: %s\n" +
                    "- Ngày đặt: %s\n" +
                    "- Thời gian: %s - %s\n" +
                    "- Số người: %d\n" +
                    "- Tổng số tiền: %s\n" +
                    "- Ngày giờ thanh toán: %s\n" +
                    "- Phương thức thanh toán: VNPAY\n" +
                    "- Mã giao dịch: %s\n\n" +
                    "Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!\n\n" +
                    "Trân trọng,\n" +
                    "Hệ thống QHomeBase",
                    user.getFullName() != null ? user.getFullName() : user.getEmail().split("@")[0],
                    booking.getService().getName(),
                    booking.getService().getLocation(),
                    booking.getBookingDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    booking.getStartTime().toString(),
                    booking.getEndTime().toString(),
                    booking.getNumberOfPeople(),
                    amountStr,
                    paymentDateStr,
                    transactionRef != null ? transactionRef : "N/A"
                );
                
                emailService.sendEmail(emailToSend, emailSubject, emailBody);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<ServiceBookingResponseDto> getUnpaidBookings(Long userId) {
        List<ServiceBooking> unpaidBookings = bookingRepository.findByUser_IdAndPaymentStatus(userId, "UNPAID");
        return unpaidBookings.stream()
                .map(this::toBookingResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelExpiredUnpaidBookings() {
        OffsetDateTime cutoffTime = OffsetDateTime.now().minusMinutes(10);
        List<ServiceBooking> expiredBookings = bookingRepository.findUnpaidBookingsOlderThan(cutoffTime);
        
        log.info("Found {} expired unpaid bookings to cancel", expiredBookings.size());
        
        for (ServiceBooking booking : expiredBookings) {
            booking.setStatus("CANCELLED");
            booking.setUpdatedAt(OffsetDateTime.now());
            bookingRepository.save(booking);
            log.info("Cancelled expired booking ID: {}", booking.getId());
        }
    }
}

