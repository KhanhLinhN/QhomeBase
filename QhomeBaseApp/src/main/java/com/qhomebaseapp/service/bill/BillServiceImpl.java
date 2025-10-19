//package com.qhomebaseapp.service.bill;
//
//import com.qhomebaseapp.dto.bill.BillDto;
//import com.qhomebaseapp.model.Bill;
//import com.qhomebaseapp.model.User;
//import com.qhomebaseapp.repository.bill.BillRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class BillServiceImpl implements BillService {
//
//    private final BillRepository billRepository;
//
//    private BillDto toDto(Bill bill) {
//        return BillDto.builder()
//                .id(bill.getId())
//                .billType(bill.getBillType())
//                .amount(bill.getAmount())
//                .billingMonth(bill.getBillingMonth())
//                .status(bill.getStatus())
//                .description(bill.getDescription())
//                .paymentDate(bill.getPaymentDate())
//                .build();
//    }
//
//    @Override
//    public List<BillDto> getUnpaidBills(User user) {
//        return billRepository.findByUserAndStatus(user, "UNPAID")
//                .stream()
//                .map(this::toDto)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<BillDto> getPaidBills(User user) {
//        return billRepository.findByUserAndStatus(user, "PAID")
//                .stream()
//                .map(this::toDto)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public BillDto getBillDetail(Long billId, User user) {
//        Bill bill = billRepository.findById(billId)
//                .filter(b -> b.getUser().getId().equals(user.getId()))
//                .orElseThrow(() -> new RuntimeException("Bill not found"));
//        return toDto(bill);
//    }
//
//    @Override
//    public BillDto payBill(Long billId, User user) {
//        Bill bill = billRepository.findById(billId)
//                .filter(b -> b.getUser().getId().equals(user.getId()))
//                .orElseThrow(() -> new RuntimeException("Bill not found"));
//
//        bill.setStatus("PAID");
//        bill.setPaymentDate(LocalDateTime.now());
//        Bill updated = billRepository.save(bill);
//
//        return toDto(updated);
//    }
//}
