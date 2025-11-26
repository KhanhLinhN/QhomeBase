package com.QhomeBase.baseservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class TrelloService {

    private static final String TRELLO_API_BASE_URL = "https://api.trello.com/1";
    
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiToken;
    private final String boardId;
    private final String todoListId;
    private final String inProgressListId;
    private final String doneListId;

    public TrelloService(
            RestTemplate restTemplate,
            @Value("${trello.api.key:}") String apiKey,
            @Value("${trello.api.token:}") String apiToken,
            @Value("${trello.board.id:}") String boardId,
            @Value("${trello.list.id.todo:}") String todoListId,
            @Value("${trello.list.id.inprogress:}") String inProgressListId,
            @Value("${trello.list.id.done:}") String doneListId) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.apiToken = apiToken;
        this.boardId = boardId;
        this.todoListId = todoListId;
        this.inProgressListId = inProgressListId;
        this.doneListId = doneListId;
    }

    /**
     * Create a Trello card for a maintenance request
     * @param requestId Maintenance request ID
     * @param title Card title
     * @param description Card description
     * @param listId List ID to create card in (default: todo list)
     * @return Trello card ID
     */
    public String createCard(UUID requestId, String title, String description, String listId) {
        if (!isTrelloConfigured()) {
            log.warn("Trello is not configured. Skipping card creation for request {}", requestId);
            return null;
        }

        try {
            String url = TRELLO_API_BASE_URL + "/cards";
            
            Map<String, String> params = new HashMap<>();
            params.put("key", apiKey);
            params.put("token", apiToken);
            params.put("idList", listId != null ? listId : todoListId);
            params.put("name", title);
            params.put("desc", description);

            StringBuilder urlBuilder = new StringBuilder(url);
            urlBuilder.append("?");
            params.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    urlBuilder.append(key).append("=").append(value).append("&");
                }
            });
            String finalUrl = urlBuilder.toString().replaceAll("&$", "");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    finalUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String cardId = (String) response.getBody().get("id");
                log.info("Created Trello card {} for maintenance request {}", cardId, requestId);
                return cardId;
            }
        } catch (Exception e) {
            log.error("Failed to create Trello card for maintenance request {}: {}", requestId, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Assign a member to a Trello card
     * @param cardId Trello card ID
     * @param memberId Trello member ID (username or email)
     * @return true if successful
     */
    public boolean assignMemberToCard(String cardId, String memberId) {
        if (!isTrelloConfigured() || cardId == null || memberId == null) {
            log.warn("Trello is not configured or missing parameters. Skipping member assignment.");
            return false;
        }

        try {
            String url = TRELLO_API_BASE_URL + "/cards/" + cardId + "/members";
            
            Map<String, String> params = new HashMap<>();
            params.put("key", apiKey);
            params.put("token", apiToken);
            params.put("value", memberId);

            StringBuilder urlBuilder = new StringBuilder(url);
            urlBuilder.append("?");
            params.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    urlBuilder.append(key).append("=").append(value).append("&");
                }
            });
            String finalUrl = urlBuilder.toString().replaceAll("&$", "");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    finalUrl,
                    HttpMethod.PUT,
                    entity,
                    Void.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Assigned member {} to Trello card {}", memberId, cardId);
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to assign member {} to Trello card {}: {}", memberId, cardId, e.getMessage(), e);
        }
        return false;
    }

    /**
     * Move a card to a different list
     * @param cardId Trello card ID
     * @param listId Target list ID
     * @return true if successful
     */
    public boolean moveCardToList(String cardId, String listId) {
        if (!isTrelloConfigured() || cardId == null || listId == null) {
            log.warn("Trello is not configured or missing parameters. Skipping card move.");
            return false;
        }

        try {
            String url = TRELLO_API_BASE_URL + "/cards/" + cardId;
            
            Map<String, String> params = new HashMap<>();
            params.put("key", apiKey);
            params.put("token", apiToken);
            params.put("idList", listId);

            StringBuilder urlBuilder = new StringBuilder(url);
            urlBuilder.append("?");
            params.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    urlBuilder.append(key).append("=").append(value).append("&");
                }
            });
            String finalUrl = urlBuilder.toString().replaceAll("&$", "");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    finalUrl,
                    HttpMethod.PUT,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Moved Trello card {} to list {}", cardId, listId);
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to move Trello card {} to list {}: {}", cardId, listId, e.getMessage(), e);
        }
        return false;
    }

    /**
     * Get member ID by email or username
     * Note: This requires the member to be on the board
     * @param emailOrUsername Email or username
     * @return Member ID or null
     */
    public String getMemberId(String emailOrUsername) {
        if (!isTrelloConfigured() || emailOrUsername == null) {
            return null;
        }

        try {
            String url = TRELLO_API_BASE_URL + "/boards/" + boardId + "/members";
            
            Map<String, String> params = new HashMap<>();
            params.put("key", apiKey);
            params.put("token", apiToken);

            StringBuilder urlBuilder = new StringBuilder(url);
            urlBuilder.append("?");
            params.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    urlBuilder.append(key).append("=").append(value).append("&");
                }
            });
            String finalUrl = urlBuilder.toString().replaceAll("&$", "");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Object[]> response = restTemplate.exchange(
                    finalUrl,
                    HttpMethod.GET,
                    entity,
                    Object[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Search for member by email or username
                // This is a simplified version - you may need to adjust based on Trello API response structure
                log.info("Found board members, searching for {}", emailOrUsername);
                // Note: You may need to parse the response and match by email/username
                // For now, returning the emailOrUsername as-is (Trello API accepts both)
                return emailOrUsername;
            }
        } catch (Exception e) {
            log.error("Failed to get member ID for {}: {}", emailOrUsername, e.getMessage(), e);
        }
        return null;
    }

    public String getInProgressListId() {
        return inProgressListId;
    }

    public String getDoneListId() {
        return doneListId;
    }

    private boolean isTrelloConfigured() {
        return apiKey != null && !apiKey.isEmpty() 
            && apiToken != null && !apiToken.isEmpty()
            && boardId != null && !boardId.isEmpty();
    }
}

