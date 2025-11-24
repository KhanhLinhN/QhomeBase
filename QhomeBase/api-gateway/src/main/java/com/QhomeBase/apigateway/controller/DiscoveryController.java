package com.QhomeBase.apigateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Discovery Controller - Exposes backend information for automatic discovery
 * This allows Flutter app to automatically discover ngrok URL without manual input
 */
@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    @Value("${vnpay.base-url:}")
    private String vnpayBaseUrl;

    @Value("${server.port:8989}")
    private int serverPort;

    private static final String NGROK_API_URL = "http://localhost:4040/api/tunnels";

    /**
     * Get backend discovery information
     * Returns ngrok URL if available, or local network info
     * Flutter app can call this endpoint to automatically discover backend URL
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getDiscoveryInfo() {
        Map<String, Object> info = new HashMap<>();
        
        String publicUrl = null;
        
        // Priority 1: Use VNPAY_BASE_URL if set (from environment variable)
        // But only if it's NOT localhost (localhost doesn't work for mobile devices)
        if (vnpayBaseUrl != null && !vnpayBaseUrl.isEmpty() && !vnpayBaseUrl.contains("your-ngrok-url")) {
            // Validate URL format before using it
            if (isValidUrl(vnpayBaseUrl) && !isLocalhost(vnpayBaseUrl)) {
                publicUrl = vnpayBaseUrl.trim();
            }
            // If it's localhost or invalid, publicUrl remains null - Flutter will use discovered IP
        }
        
        // Priority 2: Try to auto-detect from ngrok API if not set
        if (publicUrl == null || publicUrl.isEmpty()) {
            publicUrl = tryGetNgrokUrlFromApi();
        }
        
        // Final validation - only return valid URLs
        if (publicUrl != null && (!isValidUrl(publicUrl) || publicUrl.length() < 10)) {
            publicUrl = null; // Invalid URL, don't return it
        }
        
        info.put("publicUrl", publicUrl); // ngrok URL if available, null if localhost or invalid
        info.put("localPort", serverPort);
        info.put("apiBasePath", "/api");
        info.put("discoveryMethod", publicUrl != null ? "ngrok" : "local");
        
        // Build full API URL
        String apiUrl = publicUrl != null && !publicUrl.isEmpty()
            ? publicUrl + "/api" 
            : "http://localhost:" + serverPort + "/api";
        info.put("apiUrl", apiUrl);
        
        return ResponseEntity.ok(info);
    }
    
    /**
     * Validate if URL is in correct format
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.isEmpty() || url.trim().isEmpty()) {
            return false;
        }
        try {
            // Basic validation: must start with http:// or https://
            String trimmed = url.trim();
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                return false;
            }
            // Must have at least scheme://host format
            if (trimmed.length() < 10) { // Minimum: "http://a.b"
                return false;
            }
            // Try to parse as URI to validate format
            URI uri = URI.create(trimmed);
            return uri.getHost() != null && !uri.getHost().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if URL is localhost (doesn't work for mobile devices)
     */
    private boolean isLocalhost(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("localhost") || 
               lowerUrl.contains("127.0.0.1") ||
               lowerUrl.startsWith("http://localhost") ||
               lowerUrl.startsWith("https://localhost");
    }

    /**
     * Try to get ngrok URL from ngrok API
     * This works if ngrok is running on the same machine
     */
    private String tryGetNgrokUrlFromApi() {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NGROK_API_URL))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 && response.body() != null) {
                // Simple JSON parsing - look for "public_url" with "https://"
                String body = response.body();
                int httpsIndex = body.indexOf("\"public_url\":\"https://");
                if (httpsIndex != -1) {
                    int start = httpsIndex + "\"public_url\":\"".length();
                    int end = body.indexOf("\"", start);
                    if (end != -1) {
                        String ngrokUrl = body.substring(start, end);
                        // Remove trailing slash if present
                        if (ngrokUrl.endsWith("/")) {
                            ngrokUrl = ngrokUrl.substring(0, ngrokUrl.length() - 1);
                        }
                        return ngrokUrl;
                    }
                }
            }
        } catch (Exception e) {
            // Ngrok API not available - this is expected if ngrok is not running
            // or not on the same machine
        }
        return null;
    }

    /**
     * Health check endpoint for discovery
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "api-gateway");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}
