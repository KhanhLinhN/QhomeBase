package com.QhomeBase.chatservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateMessageRequest {
    
    @Size(max = 5000, message = "Message content must not exceed 5000 characters")
    private String content;
    
    private String messageType; // TEXT, IMAGE, FILE
    
    private String imageUrl;
    
    private String fileUrl;
    
    private String fileName;
    
    private Long fileSize;
    
    private UUID replyToMessageId; // For replying to a message
}

