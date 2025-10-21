package com.qhomebaseapp.service.news;

import com.qhomebaseapp.dto.news.NewsNotificationDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // gửi tới userId cụ thể
    public void sendNotification(Long userId, NewsNotificationDto dto) {
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, dto);
    }

    // gửi tới tất cả user
    public void broadcastNotification(NewsNotificationDto dto) {
        messagingTemplate.convertAndSend("/topic/notifications/all", dto);
    }
}
