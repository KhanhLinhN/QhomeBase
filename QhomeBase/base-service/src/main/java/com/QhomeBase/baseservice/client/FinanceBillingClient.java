package com.QhomeBase.baseservice.client;

import com.QhomeBase.baseservice.dto.VehicleActivatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
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
        log.info("🚗 Notifying finance service: Vehicle activated - vehicleId={}, plateNo={}, residentId={}", 
                event.getVehicleId(), event.getPlateNo(), event.getResidentId());
        
        try {
            notifyVehicleActivated(event).block();
            log.info("✅ Finance service notified successfully - Invoice should be created for vehicle: {}", 
                    event.getPlateNo());
        } catch (Exception e) {
            log.error("❌ FAILED to notify finance service for vehicle: {} ({}). Invoice NOT created!", 
                    event.getPlateNo(), event.getVehicleId(), e);
            log.error("   Error details: {}", e.getMessage());
            log.error("   Finance service may be down. Please create invoice manually or retry.");
            // Don't throw - allow approval to succeed even if invoice creation fails
            // Invoice can be created manually later
        }
    }
}