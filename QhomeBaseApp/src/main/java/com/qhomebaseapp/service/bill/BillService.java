package com.qhomebaseapp.service.bill;

import com.qhomebaseapp.dto.bill.BillStatisticsDto;
import com.qhomebaseapp.model.Bill;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.repository.bill.BillRepository;
import com.qhomebaseapp.repository.UserRepository;
import com.qhomebaseapp.service.vnpay.VnpayService;
import com.qhomebaseapp.service.user.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.DatatypeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final VnpayService vnpayService;
    private final EmailService emailService;

    public List<Bill> getUnpaidBillsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return billRepository.findByUserAndStatus(user, "UNPAID");
    }

    public List<Bill> getPaidBillsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return billRepository.findByUserAndStatus(user, "PAID");
    }


    public Bill getBillDetail(Long billId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return billRepository.findById(billId)
                .filter(b -> b.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Bill not found"));
    }

    public List<BillStatisticsDto> getStatisticsByUserId(Long userId, String billTypeFilter) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Bill> bills = billRepository.findByUserOrderByBillingMonthDesc(user);

        Map<String, String> billTypeMap = Map.of(
                "ELECTRICITY", "Điện",
                "WATER", "Nước",
                "INTERNET", "Internet"
        );

        if (billTypeFilter != null && !billTypeFilter.equalsIgnoreCase("ALL")) {
            bills = bills.stream()
                    .filter(b -> billTypeMap.getOrDefault(b.getBillType(), b.getBillType())
                            .equalsIgnoreCase(billTypeFilter))
                    .toList();
        }
        Map<AbstractMap.SimpleEntry<String, String>, BigDecimal> grouped = bills.stream()
                .collect(Collectors.groupingBy(
                        b -> new AbstractMap.SimpleEntry<>(
                                b.getBillingMonth().toString().substring(0, 7),
                                billTypeMap.getOrDefault(b.getBillType(), b.getBillType())),
                        Collectors.reducing(BigDecimal.ZERO, Bill::getAmount, BigDecimal::add)
                ));

        return grouped.entrySet().stream()
                .map(e -> BillStatisticsDto.builder()
                        .month(e.getKey().getKey())
                        .billType(e.getKey().getValue())
                        .totalAmount(e.getValue())
                        .build())
                .sorted(Comparator.comparing(BillStatisticsDto::getMonth))
                .toList();
    }

    public List<Bill> getBillsByMonth(Long userId, String month, String billTypeFilter) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Bill> bills = billRepository.findByUserOrderByBillingMonthDesc(user);

        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("Sai định dạng tháng (yyyy-MM)");
        }

        Map<String, String> billTypeMap = Map.of(
                "ELECTRICITY", "Điện",
                "WATER", "Nước",
                "INTERNET", "Internet"
        );
        bills = bills.stream()
                .filter(b -> b.getBillingMonth() != null &&
                        b.getBillingMonth().toString().startsWith(month))
                .collect(Collectors.toList());
        if (billTypeFilter != null && !billTypeFilter.isEmpty() && !billTypeFilter.equalsIgnoreCase("ALL")) {
            bills = bills.stream()
                    .filter(b -> billTypeMap.getOrDefault(b.getBillType(), b.getBillType())
                            .equalsIgnoreCase(billTypeFilter))
                    .collect(Collectors.toList());
        }

        bills.sort(Comparator.comparing(Bill::getBillingMonth).reversed());
        return bills;
    }

    public Bill payBill(Long billId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Bill bill = billRepository.findById(billId)
                .filter(b -> b.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        bill.setStatus("PAID");
        bill.setPaymentDate(LocalDateTime.now());
        Bill savedBill = billRepository.save(bill);

        messagingTemplate.convertAndSend(
                "/topic/bills/" + userId,
                Map.of(
                        "billId", savedBill.getId(),
                        "status", savedBill.getStatus()
                )
        );

        return savedBill;
    }

    public Bill getBill(Long billId) {
        return billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
    }

    public String createVnpayPaymentUrl(Long billId, Long userId, HttpServletRequest request) {
        Bill bill = getBill(billId);

        if (!bill.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thanh toán hóa đơn này");
        }

        if ("PAID".equalsIgnoreCase(bill.getStatus())) {
            throw new RuntimeException("Hóa đơn đã được thanh toán trước đó");
        }

        if (!"PENDING".equalsIgnoreCase(bill.getVnpayStatus())) {
            bill.setVnpayStatus("PENDING");
            billRepository.save(bill);
        }

        BigDecimal amount = bill.getAmount();
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) clientIp = request.getRemoteAddr();

        String orderInfo = "Thanh toán hóa đơn #" + bill.getId();

        return vnpayService.createPaymentUrl(bill.getId(), orderInfo, amount, clientIp);
    }

    public void markAsPaid(Long billId, Map<String, String> vnpParams) {
        Bill bill = getBill(billId);

        if (!"PAID".equalsIgnoreCase(bill.getStatus())) {
            bill.setStatus("PAID");
            bill.setVnpayStatus("SUCCESS");
            bill.setPaymentGateway("VNPAY");
            bill.setPaymentDate(LocalDateTime.now());

            if (vnpParams != null) {
                bill.setVnpTransactionNo(vnpParams.get("vnp_TransactionNo"));
                bill.setVnpBankCode(vnpParams.get("vnp_BankCode"));
                bill.setVnpCardType(vnpParams.get("vnp_CardType"));

                String payDate = vnpParams.get("vnp_PayDate");
                if (payDate != null && payDate.length() == 14) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                        bill.setVnpPayDate(LocalDateTime.parse(payDate, formatter));
                    } catch (Exception e) {
                        bill.setVnpPayDate(LocalDateTime.now());
                    }
                }
            }
            billRepository.save(bill);
            
            // Gửi email thông báo thanh toán thành công
            try {
                User user = bill.getUser();
                if (user != null && user.getEmail() != null) {
                    String emailSubject = "Thanh toán thành công - Hóa đơn #" + bill.getId();
                    LocalDateTime paymentDateTime = bill.getPaymentDate() != null ? bill.getPaymentDate() : LocalDateTime.now();
                    String paymentDateStr = paymentDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                    NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
                    String amountStr = currencyFormat.format(bill.getAmount()) + " VNĐ";
                    String paymentMethod = bill.getPaymentGateway() != null ? bill.getPaymentGateway() : "VNPAY";
                    
                    String emailBody = String.format(
                        "Xin chào %s,\n\n" +
                        "Thanh toán hóa đơn của bạn đã được xử lý thành công!\n\n" +
                        "Thông tin thanh toán:\n" +
                        "- Mã hóa đơn: #%s\n" +
                        "- Loại hóa đơn: %s\n" +
                        "- Tổng số tiền: %s\n" +
                        "- Ngày giờ thanh toán: %s\n" +
                        "- Phương thức thanh toán: %s\n" +
                        "%s\n\n" +
                        "Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!\n\n" +
                        "Trân trọng,\n" +
                        "Hệ thống QHomeBase",
                        user.getEmail().split("@")[0], // Tên user từ email
                        bill.getId(),
                        bill.getBillType() != null ? bill.getBillType() : "N/A",
                        amountStr,
                        paymentDateStr,
                        paymentMethod,
                        bill.getVnpTransactionNo() != null ? "- Mã giao dịch: " + bill.getVnpTransactionNo() : ""
                    );
                    
                    emailService.sendEmail(user.getEmail(), emailSubject, emailBody);
                    log.info("✅ [BillService] Đã gửi email thông báo thanh toán thành công cho user: {}", user.getEmail());
                }
            } catch (Exception e) {
                log.error("❌ [BillService] Lỗi khi gửi email thông báo thanh toán: {}", e.getMessage(), e);
                // Không throw exception để không ảnh hưởng đến flow thanh toán
            }
        }
    }
}
