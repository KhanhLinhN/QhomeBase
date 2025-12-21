package com.QhomeBase.assetmaintenanceservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinanceBillingClient {

    private final WebClient financeWebClient;

    /**
     * Create invoice synchronously via finance-billing-service
     * @param invoiceRequest Map containing invoice data (will be converted to CreateInvoiceRequest)
     * @return Map containing the created invoice response
     */
    public Map<String, Object> createInvoiceSync(Map<String, Object> invoiceRequest) {
        try {
            log.debug("Creating invoice via finance-billing-service: {}", invoiceRequest);
            
            Map<String, Object> response = financeWebClient
                    .post()
                    .uri("/api/invoices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(invoiceRequest)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException
                                    && ((WebClientResponseException) throwable).getStatusCode().is5xxServerError()))
                    .block();
            
            log.info("✅ Invoice created successfully via finance-billing-service");
            return response;
        } catch (WebClientResponseException e) {
            log.error("❌ Failed to create invoice via finance-billing-service: Status={}, Body={}", 
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to create invoice: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Unexpected error creating invoice via finance-billing-service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create invoice: " + e.getMessage(), e);
        }
    }
}
