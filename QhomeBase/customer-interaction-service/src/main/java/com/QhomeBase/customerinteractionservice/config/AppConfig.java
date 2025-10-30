package com.QhomeBase.customerinteractionservice.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class AppConfig {
    @Value("${app.file-base-url:${data-docs-service.url:http://localhost:8082}}")
    private String fileBaseUrl;
}
