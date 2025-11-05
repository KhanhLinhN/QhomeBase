package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.ResidentAccountDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
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
        
        CreateUserRequest request = new CreateUserRequest(
                username,
                email,
                password,
                autoGenerate,
                residentId
        );
        
        try {
            UserAccountResponse response = webClient
                    .post()
                    .uri("/api/users/create-for-resident")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(UserAccountResponse.class)
                    .block();
            
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
            UserAccountResponse response = webClient
                    .get()
                    .uri("/api/users/{userId}/account-info", userId)
                    .retrieve()
                    .bodyToMono(UserAccountResponse.class)
                    .block();
            
            if (response == null) {
                return null;
            }
            
            return new ResidentAccountDto(
                    response.userId(),
                    response.username(),
                    response.email(),
                    response.roles(),
                    response.active()
            );
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            log.error("Error calling IAM service to get user account: {}", e.getMessage());
            throw new RuntimeException("Failed to get user account: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error calling IAM service", e);
            throw new RuntimeException("Failed to get user account: " + e.getMessage(), e);
        }
    }
    
    public boolean usernameExists(String username) {
        try {
            webClient
                    .get()
                    .uri("/api/users/by-username/{username}", username)
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
        try {
            webClient
                    .get()
                    .uri("/api/users/by-email/{email}", email)
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
}

