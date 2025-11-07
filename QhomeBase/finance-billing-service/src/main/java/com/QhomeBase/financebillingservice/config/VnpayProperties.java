package com.QhomeBase.financebillingservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "vnpay")
public class VnpayProperties {
    private String tmnCode;
    private String hashSecret;
    private String vnpUrl;
    private String returnUrl;
    private String version;
    private String command;
}


