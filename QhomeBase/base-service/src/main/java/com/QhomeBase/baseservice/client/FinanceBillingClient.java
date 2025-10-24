package com.QhomeBase.baseservice.client;

import com.QhomeBase.baseservice.dto.VehicleActivatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class FinanceBillingClient {
    
    @Qualifier("financeWebClient")
    private final WebClient financeWebClient;

    public Mono<Void> notifyVehicleActivated(VehicleActivatedEvent event) {
        return financeWebClient
                .post()
                .uri("/api/parking/invoices/generate-prorata")
                .bodyValue(event)
                .retrieve()
                .toBodilessEntity()
                .then();
    }

    public void notifyVehicleActivatedSync(VehicleActivatedEvent event) {
        try {
            notifyVehicleActivated(event).block();
        } catch (Exception e) {
            // Silently ignore errors
        }
    }
}