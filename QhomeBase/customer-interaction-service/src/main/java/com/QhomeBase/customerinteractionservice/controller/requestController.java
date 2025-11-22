package com.QhomeBase.customerinteractionservice.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.QhomeBase.customerinteractionservice.dto.RequestApproveRequest;
import com.QhomeBase.customerinteractionservice.dto.RequestDTO;
import com.QhomeBase.customerinteractionservice.service.processingLogService;
import com.QhomeBase.customerinteractionservice.service.requestService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/customer-interaction/requests")
public class requestController {

    private final requestService requestService;
    private final processingLogService processingLogService;

    public requestController(requestService requestService, processingLogService processingLogService) {
        this.requestService = requestService;
        this.processingLogService = processingLogService;
    }

   @GetMapping()
   public Page<RequestDTO> getRequestsList(
           @RequestParam(required = false) String status,
           @RequestParam(defaultValue = "0") int pageNo,
           @RequestParam(required = false) String dateFrom,
           @RequestParam(required = false) String dateTo)
   {

       Page<RequestDTO> requestPage = requestService.getFilteredRequests(
               status, pageNo, dateFrom, dateTo
       );

       return requestPage;
   }

    @GetMapping("/counts")
    public Map<String, Long> getRequestCounts(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo)
    {
        return requestService.getRequestCounts(
                dateFrom, dateTo
        );
    }

    @GetMapping("/{id}")
    public RequestDTO getRequest(@PathVariable UUID id)
    {
        return requestService.getRequestById(id);
    }

    @PostMapping("/createRequest")
    public ResponseEntity<?> addNewRequest(@RequestBody RequestDTO requestDTO, Authentication auth)
    {
        try {
            RequestDTO savedRequest = requestService.createNewRequest(requestDTO, auth);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedRequest);
        } catch (IllegalStateException e) {
            // Rate limit exception - sẽ được GlobalExceptionHandler xử lý
            throw e;
        } catch (IllegalArgumentException e) {
            // Validation exception - sẽ được GlobalExceptionHandler xử lý
            throw e;
        }
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<RequestDTO> approveRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody RequestApproveRequest approveRequest,
            Authentication authentication) {
        try {
            RequestDTO approvedRequest = processingLogService.approveRequest(requestId, approveRequest, authentication);
            return ResponseEntity.ok(approvedRequest);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(null);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

}
