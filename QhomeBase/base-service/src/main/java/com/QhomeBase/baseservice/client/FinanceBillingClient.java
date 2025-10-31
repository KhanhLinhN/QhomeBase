package com.QhomeBase.baseservice.client;

import com.QhomeBase.baseservice.dto.BillingImportedReadingDto;
import com.QhomeBase.baseservice.dto.VehicleActivatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

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
        }
    }

    public Mono<Void> importMeterReadings(List<BillingImportedReadingDto> readings) {
        return financeWebClient
                .post()
                .uri("/api/meter-readings/import")
                .bodyValue(readings)
                .retrieve()
                .toBodilessEntity()
                .then();
    }

    public void importMeterReadingsSync(List<BillingImportedReadingDto> readings) {
        try {
            importMeterReadings(readings).block();
            log.info("✅ Imported {} readings to finance-billing", readings != null ? readings.size() : 0);
        } catch (Exception e) {
            log.error("❌ FAILED to import meter readings to finance-billing", e);
        }
    }
}