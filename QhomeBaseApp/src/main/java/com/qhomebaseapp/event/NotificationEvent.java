package com.qhomebaseapp.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationEvent {
    private Long userId;
    private Long newsId;
    private String title;
    private String summary;
    private boolean isRead;
}
