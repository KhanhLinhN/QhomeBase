package com.QhomeBase.assetmaintenanceservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
public class VnpayProperties {

    private String tmnCode;
    private String hashSecret;
    private String vnpUrl;
    private String baseUrl; // Base URL for building return URLs (e.g., https://xxx.ngrok.io)
    private String returnUrl; // Full return URL or will be built from baseUrl
    private String serviceBookingReturnUrl; // Full return URL or will be built from baseUrl
    private String version;
    private String command;

    /**
     * Get return URL for asset maintenance VNPay callback
     * If returnUrl is set, use it; otherwise build from baseUrl
     */
    public String getReturnUrl() {
        if (StringUtils.hasText(returnUrl)) {
            return returnUrl;
        }
        if (StringUtils.hasText(baseUrl)) {
            String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return base + "/api/asset-maintenance/bookings/vnpay/redirect";
        }
        return returnUrl;
    }

    /**
     * Get return URL for service booking VNPay callback
     */
    public String getServiceBookingReturnUrl() {
        if (StringUtils.hasText(serviceBookingReturnUrl)) {
            return serviceBookingReturnUrl;
        }
        return getReturnUrl(); // Fallback to default return URL
    }
}

