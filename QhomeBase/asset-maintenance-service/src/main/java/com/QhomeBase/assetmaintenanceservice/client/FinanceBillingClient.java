package com.QhomeBase.assetmaintenanceservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class FinanceBillingClient {
    
    private final WebClient financeWebClient;

    public FinanceBillingClient(WebClient financeWebClient) {
        this.financeWebClient = financeWebClient;
    }

    public Mono<Map<String, Object>> createInvoice(Map<String, Object> request) {
        log.debug("Calling finance service to create invoice for service booking");
        return financeWebClient
                .post()
                .uri("/api/invoices")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof WebClientRequestException)
                        .doBeforeRetry(retrySignal -> 
                            log.warn("Retrying finance service invoice creation (attempt {}/3): {}", 
                                retrySignal.totalRetries() + 1, retrySignal.failure().getMessage())))
                .doOnSuccess(result -> log.debug("Finance returned invoice {}", result != null ? result.get("id") : "null"))
                .doOnError(error -> log.error("Finance invoice creation failed after retries", error));
    }

    public Map<String, Object> createInvoiceSync(Map<String, Object> request) {
        try {
            Map<String, Object> invoice = createInvoice(request).block();
            log.info("Created invoice {} in finance-billing for service booking", 
                    invoice != null ? invoice.get("id") : "null");
            return invoice;
        } catch (Exception e) {
            log.error("❌ FAILED to create invoice in finance-billing", e);
            throw new RuntimeException("Failed to create invoice in finance-billing: " + e.getMessage(), e);
        }
    }
}
