package com.qhomebaseapp.repository.bill;

import com.qhomebaseapp.model.Bill;
import com.qhomebaseapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByUserAndStatus(User user, String status);
    List<Bill> findByUserOrderByBillingMonthDesc(User user);
}
