package com.qhomebaseapp.service.bill;

import com.qhomebaseapp.dto.bill.BillStatisticsDto;
import com.qhomebaseapp.model.Bill;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.repository.bill.BillRepository;
import com.qhomebaseapp.repository.UserRepository;
import com.qhomebaseapp.service.vnpay.VnpayService;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final VnpayService vnpayService;

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

    public String createVnpayPaymentUrl(Long billId, Long userId, HttpServletRequest request) {

        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        if (!bill.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thanh toán hóa đơn này");
        }

        if (!"UNPAID".equalsIgnoreCase(bill.getStatus())) {
            throw new RuntimeException("Hóa đơn đã được thanh toán hoặc không hợp lệ");
        }

        BigDecimal amount = bill.getAmount();
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }

        String orderInfo = "Thanh toán hóa đơn #" + bill.getId();

        String paymentUrl = vnpayService.createPaymentUrl(
                bill.getId(),
                orderInfo,
                amount,
                clientIp
        );

        log.info("✅ [BILL PAYMENT] Tạo URL VNPAY cho billId={}, userId={}, url={}", billId, userId, paymentUrl);
        return paymentUrl;
    }

    public void markAsPaid(Long billId, Map<String, String> vnpParams) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        if (!"PAID".equalsIgnoreCase(bill.getStatus())) {
            bill.setStatus("PAID");
            bill.setPaymentDate(LocalDateTime.now());
            bill.setPaymentGateway("VNPAY");

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
                        log.warn("⚠️ [markAsPaid] Không parse được vnp_PayDate: {}", payDate);
                        bill.setVnpPayDate(LocalDateTime.now());
                    }
                }

            }
            billRepository.save(bill);
            messagingTemplate.convertAndSend(
                    "/topic/bills/" + bill.getUser().getId(),
                    Map.of(
                            "billId", bill.getId(),
                            "status", bill.getStatus(),
                            "paymentGateway", bill.getPaymentGateway()
                    )
            );
            log.info("✅ [markAsPaid] Bill {} đã cập nhật trạng thái PAID qua VNPAY", billId);
        } else {
            log.info("ℹ️ [markAsPaid] Bill {} đã thanh toán trước đó, bỏ qua", billId);
        }
    }

}
