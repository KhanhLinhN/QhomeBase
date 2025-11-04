package com.qhomebaseapp.service.service;

import com.qhomebaseapp.dto.service.AvailableServiceDto;
import com.qhomebaseapp.dto.service.BarSlotDto;
import com.qhomebaseapp.dto.service.BookingItemDto;
import com.qhomebaseapp.dto.service.ServiceBookingRequestDto;
import com.qhomebaseapp.dto.service.ServiceBookingResponseDto;
import com.qhomebaseapp.dto.service.ServiceComboDto;
import com.qhomebaseapp.dto.service.ServiceDto;
import com.qhomebaseapp.dto.service.ServiceOptionDto;
import com.qhomebaseapp.dto.service.ServiceTicketDto;
import com.qhomebaseapp.dto.service.ServiceTypeDto;
import com.qhomebaseapp.dto.service.TimeSlotDto;
import com.qhomebaseapp.model.BarSlot;
import com.qhomebaseapp.model.ServiceBookingItem;
import com.qhomebaseapp.model.ServiceCombo;
import com.qhomebaseapp.model.ServiceOption;
import com.qhomebaseapp.model.ServiceTicket;
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
import com.qhomebaseapp.exception.UnpaidBookingException;
import com.qhomebaseapp.exception.ServiceNotAvailableException;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceBookingServiceImpl implements ServiceBookingService {

    private final ServiceRepository serviceRepository;
    private final ServiceBookingRepository bookingRepository;
    private final ServiceAvailabilityRepository availabilityRepository;
    private final ServiceBookingSlotRepository slotRepository;
    private final ServiceOptionRepository optionRepository;
    private final ServiceComboRepository comboRepository;
    private final ServiceTicketRepository ticketRepository;
    private final BarSlotRepository barSlotRepository;
    private final ServiceBookingItemRepository bookingItemRepository;
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
                    
                    // Get type name - chỉ hiển thị tên ngắn gọn (trừ BBQ)
                    String typeName;
                    
                    // Map theo code để lấy tên ngắn gọn
                    if (typeCode.equalsIgnoreCase("BBQ")) {
                        // BBQ: Giữ tên chi tiết
                        typeName = "Nướng BBQ";
                    } else if (typeCode.equalsIgnoreCase("SPA")) {
                        typeName = "Spa";
                    } else if (typeCode.equalsIgnoreCase("POOL")) {
                        typeName = "Hồ bơi"; // Chỉ hiển thị "Hồ bơi", không phải "Hồ bơi A"
                    } else if (typeCode.equalsIgnoreCase("PLAYGROUND")) {
                        typeName = "Khu vui chơi điện tử";
                    } else if (typeCode.equalsIgnoreCase("BAR")) {
                        typeName = "Bar"; // Chỉ hiển thị "Bar", không phải "Bar A"
                    } else {
                        // Fallback: Lấy từ code
                        typeName = typeCode;
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
    
    /**
     * Kiểm tra service availability và trả về thông tin chi tiết nếu không available
     * @return null nếu available, hoặc message lý do nếu không available
     */
    private String checkServiceAvailabilityWithReason(Long serviceId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int dayOfWeekValue = dayOfWeek.getValue() % 7; 
        
        List<ServiceAvailability> availabilities = availabilityRepository
                .findByService_IdAndIsAvailableTrue(serviceId);
        
        // Kiểm tra schedule
        boolean hasSchedule = availabilities.stream()
                .anyMatch(avail -> avail.getDayOfWeek().equals(dayOfWeekValue) &&
                        !startTime.isBefore(avail.getStartTime()) &&
                        !endTime.isAfter(avail.getEndTime()));
        
        if (!hasSchedule) {
            // Tìm schedule gần nhất để hiển thị thông tin
            String dayName = dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("vi"));
            Optional<ServiceAvailability> closestSchedule = availabilities.stream()
                    .filter(avail -> avail.getDayOfWeek().equals(dayOfWeekValue))
                    .findFirst();
            
            if (closestSchedule.isPresent()) {
                ServiceAvailability schedule = closestSchedule.get();
                return String.format("Dịch vụ không hoạt động vào %s trong khung giờ bạn đã chọn (%s - %s). " +
                        "Giờ hoạt động: %s - %s", 
                        dayName, startTime, endTime, schedule.getStartTime(), schedule.getEndTime());
            } else {
                return String.format("Dịch vụ không hoạt động vào %s. Vui lòng chọn ngày khác.", dayName);
            }
        }
        
        // Kiểm tra overlapping bookings
        List<ServiceBooking> overlapping = bookingRepository.findOverlappingBookings(
                serviceId, date, startTime, endTime);
        
        if (!overlapping.isEmpty()) {
            return String.format("Khung giờ %s - %s ngày %s đã được đặt trước. Vui lòng chọn thời gian khác.",
                    startTime, endTime, date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        
        return null; // Available
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
                throw new UnpaidBookingException("Bạn có dịch vụ chưa thanh toán. Vui lòng thanh toán trước khi đặt dịch vụ mới.");
            }
            
            // Validate theo booking_type (lấy từ database, không hardcode)
            String bookingType = service.getBookingType() != null ? service.getBookingType() : "STANDARD";
            BigDecimal durationHours = BigDecimal.ONE; // Default 1 giờ
            
            // COMBO_BASED (SPA, Bar, KFC, etc.): Không cần check availability, không cần slot
            // TICKET_BASED (Pool, Playground): Chỉ cần check capacity, không cần time slot cụ thể
            // OPTION_BASED (BBQ): Cần check availability và time slot
            // STANDARD: Cần check availability và time slot
            if (!"COMBO_BASED".equals(bookingType)) {
                
                // Với TICKET_BASED: Có thể không cần time slot, chỉ cần ngày
                if ("TICKET_BASED".equals(bookingType)) {
                    // Pool/Playground: Chỉ validate capacity nếu có
                    int requestedPeople = request.getNumberOfPeople() != null ? request.getNumberOfPeople() : 1;
                    int maxCapacity = service.getMaxCapacity() != null ? service.getMaxCapacity() : 0;
                    
                    if (maxCapacity > 0 && requestedPeople > maxCapacity) {
                        throw new RuntimeException("Số người tham gia (" + requestedPeople + 
                                ") vượt quá sức chứa tối đa (" + maxCapacity + " người)");
                    }
                    
                    // Set default time nếu không có
                    LocalTime defaultStartTime = request.getStartTime() != null ? 
                            request.getStartTime() : LocalTime.of(6, 0);
                    LocalTime defaultEndTime = request.getEndTime() != null ? 
                            request.getEndTime() : LocalTime.of(22, 0);
                    
                    // Update request với default times nếu null
                    if (request.getStartTime() == null) {
                        request.setStartTime(defaultStartTime);
                    }
                    if (request.getEndTime() == null) {
                        request.setEndTime(defaultEndTime);
                    }
                    
                    long hours = ChronoUnit.HOURS.between(defaultStartTime, defaultEndTime);
                    if (hours <= 0) hours = 8; // Default 8 giờ cho vé trọn ngày
                    durationHours = BigDecimal.valueOf(hours);
                } else {
                    // OPTION_BASED và STANDARD: Check availability và time slot
                    if (request.getStartTime() == null || request.getEndTime() == null) {
                        throw new RuntimeException("Vui lòng chọn thời gian cho dịch vụ này.");
                    }
                    
                    // Kiểm tra availability với thông tin chi tiết
                    String availabilityReason = checkServiceAvailabilityWithReason(
                            service.getId(), request.getBookingDate(), 
                            request.getStartTime(), request.getEndTime());
                    
                    if (availabilityReason != null) {
                        log.warn("Service {} is not available for date {} and time {} - {}. Reason: {}", 
                                service.getId(), request.getBookingDate(), 
                                request.getStartTime(), request.getEndTime(), availabilityReason);
                        throw new ServiceNotAvailableException(availabilityReason);
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
                    
                    // Chỉ check overlapping bookings nếu service không có giới hạn capacity
                    if (maxCapacity == 0) {
                        List<ServiceBooking> overlapping = bookingRepository.findOverlappingBookings(
                                service.getId(), request.getBookingDate(), 
                                request.getStartTime(), request.getEndTime());
                        if (!overlapping.isEmpty()) {
                            log.warn("Time slot is already booked. Overlapping bookings: {}", overlapping.size());
                            String reason = checkServiceAvailabilityWithReason(
                                    service.getId(), request.getBookingDate(), 
                                    request.getStartTime(), request.getEndTime());
                            throw new ServiceNotAvailableException(
                                    reason != null ? reason : "Khung giờ đã được đặt trước. Vui lòng chọn thời gian khác.",
                                    "TIME_SLOT_BOOKED");
                        }
                    }
                    
                    long hours = ChronoUnit.HOURS.between(request.getStartTime(), request.getEndTime());
                    if (hours <= 0) hours = 1;
                    durationHours = BigDecimal.valueOf(hours);
                }
            } else {
                // COMBO_BASED: Không cần time slot, set default
                if (request.getStartTime() == null) {
                    request.setStartTime(LocalTime.of(9, 0)); // Default 9:00
                }
                if (request.getEndTime() == null) {
                    // Lấy duration từ combo nếu có
                    if (request.getSelectedComboId() != null) {
                        ServiceCombo combo = comboRepository.findById(request.getSelectedComboId()).orElse(null);
                        if (combo != null && combo.getDurationMinutes() != null) {
                            request.setEndTime(request.getStartTime().plusMinutes(combo.getDurationMinutes()));
                        } else {
                            request.setEndTime(request.getStartTime().plusHours(1)); // Default 1 giờ
                        }
                    } else {
                        request.setEndTime(request.getStartTime().plusHours(1));
                    }
                }
                long hours = ChronoUnit.HOURS.between(request.getStartTime(), request.getEndTime());
                if (hours <= 0) hours = 1;
                durationHours = BigDecimal.valueOf(hours);
            }
            
            // Calculate total amount based on booking_type (from database, not hardcoded)
            BigDecimal totalAmount = BigDecimal.ZERO;
            java.util.List<ServiceBookingItem> bookingItems = new java.util.ArrayList<>();
            
            // Xác định loại booking và tính toán giá dựa trên booking_type
            if ("OPTION_BASED".equals(bookingType)) {
                // Option-based: Slot base + options + extra hours (e.g., BBQ)
                totalAmount = calculateOptionBasedBookingAmount(service, request, bookingItems, durationHours);
            } else if ("COMBO_BASED".equals(bookingType)) {
                // Combo-based: Chọn combo (e.g., SPA, Bar, KFC)
                totalAmount = calculateComboBasedBookingAmount(service, request, bookingItems);
            } else if ("TICKET_BASED".equals(bookingType)) {
                // Ticket-based: Chọn ticket (e.g., Pool, Playground)
                totalAmount = calculateTicketBasedBookingAmount(service, request, bookingItems);
            } else {
                // STANDARD or NULL: Tính theo pricing type (HOURLY/PER_SESSION)
                String pricingType = service.getPricingType() != null ? service.getPricingType() : "HOURLY";
                
                if ("PER_SESSION".equalsIgnoreCase(pricingType) || "SESSION".equalsIgnoreCase(pricingType)) {
                    totalAmount = service.getPricePerSession() != null ? 
                            service.getPricePerSession() : BigDecimal.ZERO;
                    log.info("Using PER_SESSION pricing: pricePerSession={}, totalAmount={}", 
                            service.getPricePerSession(), totalAmount);
                } else {
                    totalAmount = service.getPricePerHour() != null ?
                            service.getPricePerHour().multiply(durationHours) : BigDecimal.ZERO;
                    log.info("Using HOURLY pricing: pricePerHour={}, hours={}, totalAmount={}", 
                            service.getPricePerHour(), durationHours, totalAmount);
                }
            }
            
            if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Total amount is zero or negative for service {} with booking_type {}", 
                        service.getId(), bookingType);
                throw new RuntimeException("Không thể tính toán giá dịch vụ. Vui lòng liên hệ quản trị viên.");
            }
            
            log.info("Calculated total amount: {} for service {} (booking_type: {})", 
                    totalAmount, service.getId(), bookingType);
            
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
            
            // Save booking items (options, combos, tickets)
            if (!bookingItems.isEmpty()) {
                log.info("Saving {} booking items...", bookingItems.size());
                for (ServiceBookingItem item : bookingItems) {
                    item.setBooking(booking);
                    bookingItemRepository.save(item);
                }
                log.info("Booking items saved");
            }
            
            return toBookingResponseDto(booking);
        } catch (UnpaidBookingException | ServiceNotAvailableException e) {
            // Don't log as error - these are expected business logic
            // Just rethrow to let GlobalExceptionHandler handle it
            throw e;
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
                .bookingType(service.getBookingType())
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
                .bookingItems(loadBookingItems(booking.getId()))
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
    
    private List<BookingItemDto> loadBookingItems(Long bookingId) {
        List<ServiceBookingItem> items = bookingItemRepository.findByBooking_Id(bookingId);
        return items.stream()
                .map(this::toBookingItemDto)
                .collect(Collectors.toList());
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
                
                // Load booking items để hiển thị trong email
                List<ServiceBookingItem> bookingItems = bookingItemRepository.findByBooking_Id(booking.getId());
                
                // Build chi tiết items với mã
                StringBuilder itemsDetail = new StringBuilder();
                if (!bookingItems.isEmpty()) {
                    itemsDetail.append("\nChi tiết đặt chỗ:\n");
                    for (ServiceBookingItem item : bookingItems) {
                        itemsDetail.append(String.format("- %s (Mã: %s): %s x %d = %s VNĐ\n",
                                item.getItemName(),
                                item.getItemCode(),
                                item.getItemType(),
                                item.getQuantity(),
                                currencyFormat.format(item.getTotalPrice())));
                        
                        // Nếu là combo, thêm thông tin dịch vụ bao gồm
                        if ("COMBO".equals(item.getItemType()) && item.getMetadata() != null) {
                            itemsDetail.append(String.format("  + Dịch vụ bao gồm: %s\n", item.getMetadata()));
                        }
                    }
                }
                
                String emailBody = String.format(
                    "Xin chào %s,\n\n" +
                    "Thanh toán đặt chỗ dịch vụ của bạn đã được xử lý thành công!\n\n" +
                    "Thông tin đặt chỗ:\n" +
                    "- Dịch vụ: %s\n" +
                    "- Vị trí: %s\n" +
                    "- Ngày đặt: %s\n" +
                    "- Thời gian: %s - %s\n" +
                    "- Số người: %d\n" +
                    "%s" +
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
                    itemsDetail.toString(),
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
    
    @Override
    public List<TimeSlotDto> getTimeSlotsForService(Long serviceId, LocalDate date) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int dayOfWeekValue = dayOfWeek.getValue() % 7;
        
        // Lấy schedule cho ngày này
        List<ServiceAvailability> availabilities = availabilityRepository
                .findByService_IdAndIsAvailableTrue(serviceId);
        
        Optional<ServiceAvailability> scheduleForDay = availabilities.stream()
                .filter(avail -> avail.getDayOfWeek().equals(dayOfWeekValue))
                .findFirst();
        
        if (scheduleForDay.isEmpty()) {
            // Không có schedule cho ngày này
            return List.of();
        }
        
        ServiceAvailability schedule = scheduleForDay.get();
        LocalTime scheduleStart = schedule.getStartTime();
        LocalTime scheduleEnd = schedule.getEndTime();
        
        // Lấy minDurationHours từ service (mặc định 1 giờ)
        int slotDurationHours = service.getMinDurationHours() != null ? service.getMinDurationHours() : 1;
        
        // Tạo các time slots với khoảng cách 1 giờ
        List<TimeSlotDto> timeSlots = new java.util.ArrayList<>();
        LocalTime currentStart = scheduleStart;
        
        // Validate: Ngày không được trong quá khứ và phải ít nhất 1 giờ từ bây giờ
        ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime nowVietnam = ZonedDateTime.now(vietnamZone);
        LocalDateTime nowLocal = nowVietnam.toLocalDateTime();
        boolean isToday = date.equals(nowLocal.toLocalDate());
        
        while (currentStart.isBefore(scheduleEnd)) {
            LocalTime currentEnd = currentStart.plusHours(slotDurationHours);
            
            // Không vượt quá schedule end time
            if (currentEnd.isAfter(scheduleEnd)) {
                break;
            }
            
            // Kiểm tra nếu slot này đã quá khứ (cho ngày hôm nay)
            boolean isPastSlot = false;
            if (isToday) {
                LocalDateTime slotDateTime = LocalDateTime.of(date, currentStart);
                long hoursUntilSlot = ChronoUnit.HOURS.between(nowLocal, slotDateTime);
                if (hoursUntilSlot < 1) {
                    isPastSlot = true;
                }
            } else if (date.isBefore(nowLocal.toLocalDate())) {
                isPastSlot = true;
            }
            
            // Kiểm tra availability
            boolean isAvailable = !isPastSlot;
            String reason = null;
            Integer bookedPeople = null;
            Integer availableCapacity = null;
            
            if (!isPastSlot) {
                // Kiểm tra capacity nếu service có maxCapacity
                int maxCapacity = service.getMaxCapacity() != null ? service.getMaxCapacity() : 0;
                
                if (maxCapacity > 0) {
                    // Service có giới hạn capacity - check tổng số người đã book
                    bookedPeople = bookingRepository.sumBookedPeople(
                            serviceId, date, currentStart, currentEnd);
                    if (bookedPeople == null) bookedPeople = 0;
                    availableCapacity = maxCapacity - bookedPeople;
                    
                    // Chỉ unavailable khi đã hết chỗ (availableCapacity <= 0)
                    if (availableCapacity <= 0) {
                        isAvailable = false;
                        reason = "Đã hết chỗ";
                    }
                } else {
                    // Service không có giới hạn capacity - check overlapping bookings
                    // (một booking sẽ chiếm toàn bộ slot)
                    List<ServiceBooking> overlapping = bookingRepository.findOverlappingBookings(
                            serviceId, date, currentStart, currentEnd);
                    
                    if (!overlapping.isEmpty()) {
                        isAvailable = false;
                        reason = "Đã được đặt trước";
                    }
                }
            } else {
                reason = "Thời gian đã qua";
            }
            
            TimeSlotDto slot = TimeSlotDto.builder()
                    .startTime(currentStart)
                    .endTime(currentEnd)
                    .available(isAvailable)
                    .reason(reason)
                    .bookedPeople(bookedPeople)
                    .availableCapacity(availableCapacity)
                    .build();
            
            timeSlots.add(slot);
            
            // Di chuyển sang slot tiếp theo (tăng 1 giờ)
            currentStart = currentStart.plusHours(1);
        }
        
        return timeSlots;
    }
    
    @Override
    public List<ServiceOptionDto> getServiceOptions(Long serviceId) {
        List<ServiceOption> options = optionRepository.findByService_IdAndIsActiveTrue(serviceId);
        return options.stream()
                .sorted((a, b) -> Integer.compare(
                        a.getSortOrder() != null ? a.getSortOrder() : 0,
                        b.getSortOrder() != null ? b.getSortOrder() : 0))
                .map(this::toServiceOptionDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ServiceComboDto> getServiceCombos(Long serviceId) {
        List<ServiceCombo> combos = comboRepository.findByService_IdAndIsActiveTrue(serviceId);
        return combos.stream()
                .sorted((a, b) -> Integer.compare(
                        a.getSortOrder() != null ? a.getSortOrder() : 0,
                        b.getSortOrder() != null ? b.getSortOrder() : 0))
                .map(this::toServiceComboDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ServiceTicketDto> getServiceTickets(Long serviceId) {
        List<ServiceTicket> tickets = ticketRepository.findByService_IdAndIsActiveTrue(serviceId);
        return tickets.stream()
                .sorted((a, b) -> Integer.compare(
                        a.getSortOrder() != null ? a.getSortOrder() : 0,
                        b.getSortOrder() != null ? b.getSortOrder() : 0))
                .map(this::toServiceTicketDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<BarSlotDto> getBarSlots(Long serviceId) {
        List<BarSlot> slots = barSlotRepository.findByService_IdAndIsActiveTrueOrderBySortOrderAsc(serviceId);
        return slots.stream()
                .map(this::toBarSlotDto)
                .collect(Collectors.toList());
    }
    
    // Mapper methods
    private ServiceOptionDto toServiceOptionDto(ServiceOption option) {
        return ServiceOptionDto.builder()
                .id(option.getId())
                .code(option.getCode())
                .name(option.getName())
                .description(option.getDescription())
                .price(option.getPrice())
                .unit(option.getUnit())
                .isRequired(option.getIsRequired())
                .isActive(option.getIsActive())
                .sortOrder(option.getSortOrder())
                .build();
    }
    
    private ServiceComboDto toServiceComboDto(ServiceCombo combo) {
        return ServiceComboDto.builder()
                .id(combo.getId())
                .code(combo.getCode())
                .name(combo.getName())
                .description(combo.getDescription())
                .servicesIncluded(combo.getServicesIncluded())
                .durationMinutes(combo.getDurationMinutes())
                .price(combo.getPrice())
                .isActive(combo.getIsActive())
                .sortOrder(combo.getSortOrder())
                .build();
    }
    
    private ServiceTicketDto toServiceTicketDto(ServiceTicket ticket) {
        return ServiceTicketDto.builder()
                .id(ticket.getId())
                .code(ticket.getCode())
                .name(ticket.getName())
                .ticketType(ticket.getTicketType())
                .durationHours(ticket.getDurationHours())
                .price(ticket.getPrice())
                .maxPeople(ticket.getMaxPeople())
                .description(ticket.getDescription())
                .isActive(ticket.getIsActive())
                .sortOrder(ticket.getSortOrder())
                .build();
    }
    
    private BarSlotDto toBarSlotDto(BarSlot slot) {
        return BarSlotDto.builder()
                .id(slot.getId())
                .code(slot.getCode())
                .name(slot.getName())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .note(slot.getNote())
                .isActive(slot.getIsActive())
                .sortOrder(slot.getSortOrder())
                .build();
    }
    
    private BookingItemDto toBookingItemDto(ServiceBookingItem item) {
        return BookingItemDto.builder()
                .id(item.getId())
                .itemType(item.getItemType())
                .itemId(item.getItemId())
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .metadata(item.getMetadata())
                .build();
    }
    
    // ============================================
    // Helper methods để tính toán giá - Generic cho tất cả services cùng booking_type
    // ============================================
    
    /**
     * Tính giá cho OPTION_BASED services: Slot base + options + extra hours
     * Generic method - works for BBQ and any other option-based services
     */
    private BigDecimal calculateOptionBasedBookingAmount(Service service, ServiceBookingRequestDto request, 
                                                         java.util.List<ServiceBookingItem> bookingItems,
                                                         BigDecimal durationHours) {
        BigDecimal total = BigDecimal.ZERO;
        
        // Tính base price cho slot (dùng durationHours đã tính)
        BigDecimal basePrice = service.getPricePerHour() != null ? 
                service.getPricePerHour().multiply(durationHours) : BigDecimal.ZERO;
        total = total.add(basePrice);
        
        log.info("Option-based service base price: {} ({} hours)", basePrice, durationHours);
        
        // Thêm options nếu có
        if (request.getSelectedOptions() != null && !request.getSelectedOptions().isEmpty()) {
            for (ServiceBookingRequestDto.BookingItemRequestDto optionRequest : request.getSelectedOptions()) {
                ServiceOption option = optionRepository.findById(optionRequest.getItemId())
                        .orElseThrow(() -> new RuntimeException("Option not found: " + optionRequest.getItemId()));
                
                int quantity = optionRequest.getQuantity() != null ? optionRequest.getQuantity() : 1;
                BigDecimal itemTotal = option.getPrice().multiply(BigDecimal.valueOf(quantity));
                total = total.add(itemTotal);
                
                // Tạo booking item
                ServiceBookingItem item = ServiceBookingItem.builder()
                        .itemType("OPTION")
                        .itemId(option.getId())
                        .itemCode(option.getCode())
                        .itemName(option.getName())
                        .quantity(quantity)
                        .unitPrice(option.getPrice())
                        .totalPrice(itemTotal)
                        .metadata(null)
                        .build();
                bookingItems.add(item);
                
                log.info("Option: {} x {} = {}", option.getName(), quantity, itemTotal);
            }
        }
        
        // Thêm extra hours nếu có (tìm option có code chứa "EXTRA_HOUR" hoặc tương tự)
        if (request.getExtraHours() != null && request.getExtraHours() > 0) {
            // Thêm extra hours: 100.000 VNĐ/giờ (ví dụ: 2 giờ = 200.000 VNĐ)
            BigDecimal pricePerExtraHour = BigDecimal.valueOf(100000);
            BigDecimal extraHoursPrice = pricePerExtraHour.multiply(BigDecimal.valueOf(request.getExtraHours()));
            total = total.add(extraHoursPrice);
            
            // Tìm option extra hour để lưu vào booking items
            ServiceOption extraHourOption = optionRepository.findByService_IdAndCodeContaining(
                    service.getId(), "EXTRA_HOUR").stream().findFirst().orElse(null);
            
            // Tạo booking item cho extra hours
            String extraHourName = extraHourOption != null ? extraHourOption.getName() : "Thuê thêm giờ";
            String extraHourCode = extraHourOption != null ? extraHourOption.getCode() : "BBQ_EXTRA_HOUR";
            
            ServiceBookingItem item = ServiceBookingItem.builder()
                    .itemType("OPTION")
                    .itemId(extraHourOption != null ? extraHourOption.getId() : null)
                    .itemCode(extraHourCode)
                    .itemName(extraHourName)
                    .quantity(request.getExtraHours())
                    .unitPrice(pricePerExtraHour)
                    .totalPrice(extraHoursPrice)
                    .metadata(null)
                    .build();
            bookingItems.add(item);
            
            log.info("Extra hours: {} giờ x {} VNĐ/giờ = {} VNĐ", 
                    request.getExtraHours(), pricePerExtraHour, extraHoursPrice);
        }
        
        return total;
    }
    
    /**
     * Tính giá cho COMBO_BASED services: Chọn combo
     * Generic method - works for SPA, Bar, KFC, and any other combo-based services
     * Giá = combo price * số người
     */
    private BigDecimal calculateComboBasedBookingAmount(Service service, ServiceBookingRequestDto request,
                                                         java.util.List<ServiceBookingItem> bookingItems) {
        if (request.getSelectedComboId() == null) {
            throw new RuntimeException("Vui lòng chọn gói combo cho dịch vụ này.");
        }
        
        ServiceCombo combo = comboRepository.findById(request.getSelectedComboId())
                .orElseThrow(() -> new RuntimeException("Combo not found: " + request.getSelectedComboId()));
        
        if (!combo.getService().getId().equals(service.getId())) {
            throw new RuntimeException("Combo không thuộc dịch vụ này.");
        }
        
        // Tính số người
        int numberOfPeople = request.getNumberOfPeople() != null ? request.getNumberOfPeople() : 1;
        
        // Giá = combo price * số người
        BigDecimal comboPrice = combo.getPrice();
        BigDecimal totalPrice = comboPrice.multiply(BigDecimal.valueOf(numberOfPeople));
        
        // Tạo booking item với quantity = số người
        ServiceBookingItem item = ServiceBookingItem.builder()
                .itemType("COMBO")
                .itemId(combo.getId())
                .itemCode(combo.getCode())
                .itemName(combo.getName())
                .quantity(numberOfPeople) // ⭐ Số người
                .unitPrice(comboPrice)
                .totalPrice(totalPrice)
                .metadata(combo.getServicesIncluded()) // Lưu dịch vụ bao gồm
                .build();
        bookingItems.add(item);
        
        log.info("Combo-based service combo: {} x {} người = {}", combo.getName(), numberOfPeople, totalPrice);
        
        // Nếu service có bar slots, cần validate slot
        if (request.getSelectedBarSlotId() != null) {
            BarSlot slot = barSlotRepository.findById(request.getSelectedBarSlotId())
                    .orElseThrow(() -> new RuntimeException("Bar slot not found: " + request.getSelectedBarSlotId()));
            
            if (!slot.getService().getId().equals(service.getId())) {
                throw new RuntimeException("Slot không thuộc dịch vụ này.");
            }
            
            // Set startTime và endTime từ slot
            request.setStartTime(slot.getStartTime());
            request.setEndTime(slot.getEndTime());
            
            log.info("Combo-based service with slot: {} - {}", slot.getStartTime(), slot.getEndTime());
        }
        
        return totalPrice;
    }
    
    /**
     * Tính giá cho TICKET_BASED services: Chọn ticket
     * Generic method - works for Pool, Playground, and any other ticket-based services
     * For Playground: price = ticket price * number of people
     */
    private BigDecimal calculateTicketBasedBookingAmount(Service service, ServiceBookingRequestDto request,
                                                         java.util.List<ServiceBookingItem> bookingItems) {
        if (request.getSelectedTicketId() == null) {
            throw new RuntimeException("Vui lòng chọn vé cho dịch vụ này.");
        }
        
        ServiceTicket ticket = ticketRepository.findById(request.getSelectedTicketId())
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + request.getSelectedTicketId()));
        
        if (!ticket.getService().getId().equals(service.getId())) {
            throw new RuntimeException("Vé không thuộc dịch vụ này.");
        }
        
        int numberOfPeople = request.getNumberOfPeople() != null ? request.getNumberOfPeople() : 1;
        
        // Validate số người cho vé gia đình
        if ("FAMILY".equalsIgnoreCase(ticket.getTicketType())) {
            if (ticket.getMaxPeople() != null && numberOfPeople > ticket.getMaxPeople()) {
                throw new RuntimeException("Vé gia đình chỉ dành cho tối đa " + ticket.getMaxPeople() + " người.");
            }
        }
        
        // Tính giá: Tất cả ticket-based services = vé * số người
        BigDecimal ticketPrice = ticket.getPrice();
        BigDecimal totalPrice = ticketPrice.multiply(BigDecimal.valueOf(numberOfPeople));
        int quantity = numberOfPeople;
        
        log.info("Ticket-based service pricing: ticket {} x {} people = {}", ticketPrice, numberOfPeople, totalPrice);
        
        // Tạo booking item
        ServiceBookingItem item = ServiceBookingItem.builder()
                .itemType("TICKET")
                .itemId(ticket.getId())
                .itemCode(ticket.getCode())
                .itemName(ticket.getName())
                .quantity(quantity)
                .unitPrice(ticketPrice)
                .totalPrice(totalPrice)
                .metadata(ticket.getDescription())
                .build();
        bookingItems.add(item);
        
        return totalPrice;
    }
}

