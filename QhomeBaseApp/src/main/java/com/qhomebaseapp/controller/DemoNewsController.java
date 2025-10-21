package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.news.NewsAttachmentDto;
import com.qhomebaseapp.dto.news.NewsNotificationDto;
import com.qhomebaseapp.model.News;
import com.qhomebaseapp.model.NewsAttachment;
import com.qhomebaseapp.repository.news.NewsAttachmentRepository;
import com.qhomebaseapp.repository.news.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoNewsController {

    private final NewsWebSocketController newsWebSocketController;
    private final NewsRepository newsRepository;
    private final NewsAttachmentRepository newsAttachmentRepository;

    @PostMapping("/send-news-notification")
    public String sendNewsNotification(
            @RequestParam String title,
            @RequestParam String summary,
            @RequestParam(required = false) Long categoryId,
            @RequestPart(required = false) List<MultipartFile> files
    ) throws IOException {

        // 1️⃣ Tạo News
        News news = News.builder()
                .title(title)
                .summary(summary)
                .content(summary)
                .publishedAt(LocalDateTime.now())
                .visibleToAll(true)
                .pinned(false)
                .build();
        newsRepository.save(news);

        // 2️⃣ Xử lý files nếu có
        List<NewsAttachmentDto> attachments = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            // Thư mục uploads/newsAttachments
            String projectDir = new File("").getAbsolutePath();
            String folderPath = projectDir + File.separator + "uploads" + File.separator + "newsAttachments";
            File folder = new File(folderPath);
            if (!folder.exists()) folder.mkdirs();

            for (MultipartFile file : files) {
                File destFile = new File(folder, file.getOriginalFilename());
                file.transferTo(destFile);

                // Lưu attachment vào DB
                NewsAttachment attachment = NewsAttachment.builder()
                        .news(news)
                        .filename(file.getOriginalFilename())
                        .url("/uploads/newsAttachments/" + file.getOriginalFilename()) // URL public
                        .build();
                newsAttachmentRepository.save(attachment);

                attachments.add(
                        NewsAttachmentDto.builder()
                                .id(attachment.getId())
                                .filename(attachment.getFilename())
                                .url(attachment.getUrl())
                                .build()
                );
            }
        }

        // 3️⃣ Tạo DTO và gửi qua websocket
        NewsNotificationDto dto = NewsNotificationDto.builder()
                .newsId(news.getId())
                .title(title)
                .summary(summary)
                .publishedAt(news.getPublishedAt())
                .attachments(attachments)
                .build();

        newsWebSocketController.sendNotification(dto);

        return "News created with ID=" + news.getId();
    }
}
