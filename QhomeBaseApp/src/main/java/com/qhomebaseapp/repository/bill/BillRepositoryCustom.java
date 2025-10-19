package com.qhomebaseapp.repository.bill;

import com.qhomebaseapp.model.Bill;
import java.util.List;

public interface BillRepositoryCustom {
    List<Bill> searchBills(Long userId, String month, String billType, String status);
}
