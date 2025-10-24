package com.qhomebaseapp.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreatePostRequest {
    @NotBlank(message = "Nội dung bài viết không được để trống")
    @Size(max = 5000, message = "Nội dung tối đa 5000 ký tự")
    private String content;
    private String topic;
    private List<String> imageUrls;
}