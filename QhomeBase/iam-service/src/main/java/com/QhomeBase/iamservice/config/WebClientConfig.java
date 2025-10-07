package com.QhomeBase.iamservice.config;

import com.QhomeBase.iamservice.security.JwtIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final WebClient webClientConfig;
    private JwtIssuer jwtIssuer;
    @Bean
    public WebClient baseClient(@Value("${base_url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ")
                .build();
    }

}
