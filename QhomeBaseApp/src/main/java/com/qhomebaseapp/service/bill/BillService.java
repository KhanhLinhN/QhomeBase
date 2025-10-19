package com.qhomebaseapp.service.bill;

import com.qhomebaseapp.dto.bill.BillStatisticsDto;
import com.qhomebaseapp.model.Bill;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.repository.bill.BillRepository;
import com.qhomebaseapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final UserRepository userRepository;

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

    public Bill payBill(Long billId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Bill bill = billRepository.findById(billId)
                .filter(b -> b.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        bill.setStatus("PAID");
        bill.setPaymentDate(LocalDateTime.now());
        return billRepository.save(bill);
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


}
