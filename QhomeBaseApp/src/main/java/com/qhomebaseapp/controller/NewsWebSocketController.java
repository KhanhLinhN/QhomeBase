package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.news.NewsNotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class NewsWebSocketController {

    private final SimpMessagingTemplate template;

    public void sendNotification(NewsNotificationDto dto) {
        template.convertAndSend("/topic/news", dto);
    }
}
