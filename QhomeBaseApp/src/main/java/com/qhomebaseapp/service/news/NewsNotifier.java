package com.qhomebaseapp.service.news;

import com.qhomebaseapp.model.News;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewsNotifier {

    private final SimpMessagingTemplate template;

    public void notifyNewNews(News news) {
        template.convertAndSend("/topic/news", news);
    }
}
