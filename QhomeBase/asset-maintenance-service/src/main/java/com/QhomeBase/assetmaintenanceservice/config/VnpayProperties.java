package com.QhomeBase.assetmaintenanceservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
public class VnpayProperties {

    private String tmnCode;
    private String hashSecret;
    private String vnpUrl;
    private String returnUrl;
    private String serviceBookingReturnUrl;
    private String version;
    private String command;
}

