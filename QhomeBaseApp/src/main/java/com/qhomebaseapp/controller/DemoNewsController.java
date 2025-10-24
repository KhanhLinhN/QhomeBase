package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.news.NewsAttachmentDto;
import com.qhomebaseapp.dto.news.NewsNotificationDto;
import com.qhomebaseapp.model.News;
import com.qhomebaseapp.model.NewsAttachment;
import com.qhomebaseapp.repository.news.NewsAttachmentRepository;
import com.qhomebaseapp.repository.news.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<NewsNotificationDto> sendNewsNotification(
            @RequestParam String title,
            @RequestParam String summary,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(required = false) Boolean visibleToAll,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Long categoryId,
            @RequestPart(required = false) List<MultipartFile> files
    ) throws IOException {

        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        News news = News.builder()
                .title(title)
                .summary(summary)
                .content(content != null ? content : summary)
                .author(author)
                .source(source)
                .pinned(pinned != null ? pinned : false)
                .visibleToAll(visibleToAll != null ? visibleToAll : true)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .publishedAt(LocalDateTime.now())
                .build();

        newsRepository.save(news);

        List<NewsAttachmentDto> attachments = new ArrayList<>();

        if (files != null && !files.isEmpty()) {
            String projectDir = new File("").getAbsolutePath();
            String folderPath = projectDir + File.separator + "uploads" + File.separator + "newsAttachments";
            File folder = new File(folderPath);
            if (!folder.exists()) folder.mkdirs();

            for (MultipartFile file : files) {
                File destFile = new File(folder, file.getOriginalFilename());
                file.transferTo(destFile);

                NewsAttachment attachment = NewsAttachment.builder()
                        .news(news)
                        .filename(file.getOriginalFilename())
                        .url("/uploads/newsAttachments/" + file.getOriginalFilename())
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
        NewsNotificationDto dto = NewsNotificationDto.builder()
                .newsId(news.getId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .publishedAt(news.getPublishedAt())
                .attachments(attachments)
                .build();
        newsWebSocketController.sendNotification(dto);

        return ResponseEntity.ok(dto);
    }
}
