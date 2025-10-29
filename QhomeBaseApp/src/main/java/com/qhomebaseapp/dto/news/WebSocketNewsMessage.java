package com.qhomebaseapp.dto.news;


import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketNewsMessage {
    private String type;
    private UUID newsId;
    private String title;
    private String summary;
    private String coverImageUrl;
    private Instant timestamp;
    private String status;
    private String deepLink;
}
