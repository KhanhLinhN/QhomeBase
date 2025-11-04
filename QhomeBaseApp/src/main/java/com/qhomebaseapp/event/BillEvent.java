package com.qhomebaseapp.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BillEvent {
    private Long userId;
    private Long billId;
    private String status;
}
