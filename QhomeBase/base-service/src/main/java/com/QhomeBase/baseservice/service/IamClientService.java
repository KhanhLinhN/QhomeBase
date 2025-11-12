package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.ResidentAccountDto;
import com.QhomeBase.baseservice.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class IamClientService {
    
    private final WebClient webClient;

    public IamClientService(@Qualifier("iamWebClient") WebClient webClient) {
        this.webClient = webClient;
    }
    
    public ResidentAccountDto createUserForResident(
            String username, 
            String email, 
            String password, 
            boolean autoGenerate,
            UUID residentId) {
        return createUserForResident(username, email, password, autoGenerate, residentId, null);
    }
    
    public ResidentAccountDto createUserForResident(
            String username, 
            String email, 
            String password, 
            boolean autoGenerate,
            UUID residentId,
            String token) {
        
        CreateUserRequest request = new CreateUserRequest(
                username,
                email,
                password,
                autoGenerate,
                residentId
        );
        
        try {
            String authToken = token != null ? token : getCurrentToken();
            log.info("Calling IAM service to create user for resident {}: username={}, email={}, autoGenerate={}, tokenPresent={}, tokenSource={}", 
                    residentId, username, email, autoGenerate, authToken != null && !authToken.isEmpty(), 
                    token != null ? "parameter" : "SecurityContext");
            
            UserAccountResponse response;
            if (authToken != null && !authToken.isEmpty()) {
                log.info("Forwarding token to IAM service. Token length: {}", authToken.length());
                response = webClient
                        .post()
                        .uri("/api/users/create-for-resident")
                        .header("Authorization", "Bearer " + authToken)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(UserAccountResponse.class)
                        .block();
            } else {
                log.error("No JWT token available when calling IAM service - this will cause 403 Forbidden");
                response = webClient
                        .post()
                        .uri("/api/users/create-for-resident")
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(UserAccountResponse.class)
                        .block();
            }
            
            if (response == null) {
                throw new RuntimeException("Failed to create user account: null response");
            }
            
            return new ResidentAccountDto(
                    response.userId(),
                    response.username(),
                    response.email(),
                    response.roles(),
                    response.active()
            );
        } catch (WebClientResponseException e) {
            log.error("Error calling IAM service to create user: {}", e.getMessage());
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new IllegalArgumentException("Failed to create user account: " + e.getMessage());
            }
            throw new RuntimeException("Failed to create user account: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error calling IAM service", e);
            throw new RuntimeException("Failed to create user account: " + e.getMessage(), e);
        }
    }
    
    public ResidentAccountDto getUserAccountInfo(UUID userId) {
        try {
            String token = getCurrentToken();
            log.debug("Getting user account info for userId: {}, tokenPresent: {}", userId, token != null && !token.isEmpty());
            
            var webClientBuilder = webClient
                    .get()
                    .uri("/api/users/{userId}/account-info", userId);
            
            if (token != null && !token.isEmpty()) {
                webClientBuilder = webClientBuilder.header("Authorization", "Bearer " + token);
                log.debug("Added Authorization header to IAM service request");
            } else {
                log.warn("No token available when calling IAM service for userId: {}", userId);
            }
            
            log.debug("Calling IAM service: GET /api/users/{}/account-info", userId);
            UserAccountResponse response = webClientBuilder
                    .retrieve()
                    .bodyToMono(UserAccountResponse.class)
                    .block();
            
            if (response == null) {
                log.warn("IAM service returned null response for userId: {}", userId);
                return null;
            }
            
            log.debug("Successfully retrieved user account info: username={}, email={}, roles={}", 
                    response.username(), response.email(), response.roles());
            
            return new ResidentAccountDto(
                    response.userId(),
                    response.username(),
                    response.email(),
                    response.roles(),
                    response.active()
            );
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.debug("User account not found in IAM service for userId: {}", userId);
                return null;
            }
            log.error("Error calling IAM service to get user account for userId {}: status={}, message={}, responseBody={}", 
                    userId, e.getStatusCode(), e.getMessage(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to get user account: " + e.getMessage() + " (status: " + e.getStatusCode() + ")", e);
        } catch (Exception e) {
            log.error("Unexpected error calling IAM service for userId {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get user account: " + e.getMessage(), e);
        }
    }
    
    public boolean usernameExists(String username) {
        return usernameExists(username, null);
    }
    
    public boolean usernameExists(String username, String token) {
        try {
            String authToken = token != null ? token : getCurrentToken();
            var webClientBuilder = webClient.get()
                    .uri("/api/users/by-username/{username}", username);
            
            if (authToken != null && !authToken.isEmpty()) {
                webClientBuilder = webClientBuilder.header("Authorization", "Bearer " + authToken);
            }
            
            webClientBuilder
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return true;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            log.error("Error checking username existence: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error checking username existence", e);
            return false;
        }
    }
    
    public boolean emailExists(String email) {
        return emailExists(email, null);
    }
    
    public boolean emailExists(String email, String token) {
        try {
            String authToken = token != null ? token : getCurrentToken();
            var webClientBuilder = webClient.get()
                    .uri("/api/users/by-email/{email}", email);
            
            if (authToken != null && !authToken.isEmpty()) {
                webClientBuilder = webClientBuilder.header("Authorization", "Bearer " + authToken);
            }
            
            webClientBuilder
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return true;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            log.error("Error checking email existence: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error checking email existence", e);
            return false;
        }
    }
    
    private record CreateUserRequest(
            String username,
            String email,
            String password,
            boolean autoGenerate,
            UUID residentId
    ) {}
    
    private record UserAccountResponse(
            UUID userId,
            String username,
            String email,
            List<String> roles,
            boolean active
    ) {}
    
    private String getCurrentToken() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                String token = principal.token();
                log.debug("Retrieved token from SecurityContext: tokenPresent={}, username={}, roles={}", 
                        token != null && !token.isEmpty(), principal.username(), principal.roles());
                return token;
            } else {
                log.warn("No UserPrincipal found in SecurityContext. Authentication: {}", auth);
            }
        } catch (Exception e) {
            log.error("Failed to get current token: {}", e.getMessage(), e);
        }
        return null;
    }
}

