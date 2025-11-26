package com.QhomeBase.baseservice.config;

import com.QhomeBase.baseservice.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public WebClient iamWebClient(@Value("${iam.service.url:http://localhost:8088}") String iamServiceUrl) {
        return WebClient.builder()
                .baseUrl(iamServiceUrl)
                .filter(addJwtTokenFilter())
                .build();
    }
    @Bean
    public WebClient financeWebClient(@Value("${finance.billing.service.url:http://localhost:8085}") String financeServiceUrl) {
        return WebClient.builder()
                .baseUrl(financeServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .filter(addJwtTokenFilter())
                .build();
    }

    @Bean
    public WebClient contractWebClient(@Value("${contract.service.url:http://localhost:8082}") String contractServiceUrl) {
        return WebClient.builder()
                .baseUrl(contractServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .filter(addJwtTokenFilter())
                .build();
    }


    private ExchangeFilterFunction addJwtTokenFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            try {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                    String token = principal.token();
                    if (token != null && !token.isEmpty()) {
                        ClientRequest newRequest = ClientRequest.from(clientRequest)
                                .header("Authorization", "Bearer " + token)
                                .build();
                        return Mono.just(newRequest);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to add JWT token to request: " + e.getMessage());
            }
            return Mono.just(clientRequest);
        });
    }
}
