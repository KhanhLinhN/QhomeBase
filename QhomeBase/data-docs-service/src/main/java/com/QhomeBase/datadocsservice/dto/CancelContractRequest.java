package com.QhomeBase.datadocsservice.dto;

import java.time.LocalDate;

public record CancelContractRequest(
        LocalDate scheduledDate // Ngày đã hẹn để nhân viên tới kiểm tra (nếu null thì dùng cuối tháng của tháng hủy hợp đồng)
) {
}
