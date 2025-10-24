package com.QhomeBase.customerinteractionservice.dto.news;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkAsReadResponse {

    private Boolean success;
    private String message;
    private Instant readAt;

    public static MarkAsReadResponse success(Instant readAt) {
        return MarkAsReadResponse.builder()
                .success(true)
                .message("Marked as read")
                .readAt(readAt)
                .build();
    }

    public static MarkAsReadResponse alreadyRead(Instant readAt) {
        return MarkAsReadResponse.builder()
                .success(true)
                .message("Already marked as read")
                .readAt(readAt)
                .build();
    }
}

