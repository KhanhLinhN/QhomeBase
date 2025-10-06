package com.qhomebaseapp.dto.news;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsAttachmentDto {
    private Long id;
    private String filename;
    private String url;
}
