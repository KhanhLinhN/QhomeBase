package com.QhomeBase.customerinteractionservice.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import com.QhomeBase.customerinteractionservice.dto.RequestDTO;
import com.QhomeBase.customerinteractionservice.service.requestService;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/customer-interaction/requests")
public class requestController {

    private final requestService requestService;

    public requestController(requestService requestService) {
        this.requestService = requestService;
    }

   @GetMapping()
   public Page<RequestDTO> getRequestsList(
           @RequestParam(required = false) String projectCode,
           @RequestParam(required = false) String title,
           @RequestParam(required = false) String residentName,
           @RequestParam(required = false) UUID tenantId,
           @RequestParam(required = false) String status,
           @RequestParam(required = false) String priority,
           @RequestParam(defaultValue = "0") int pageNo,
           @RequestParam(required = false) String dateFrom,
           @RequestParam(required = false) String dateTo)
   {

       Page<RequestDTO> requestPage = requestService.getFilteredRequests(
               projectCode, title, residentName, tenantId, status, priority, pageNo, dateFrom, dateTo
       );

       return requestPage;
   }

    @GetMapping("/counts")
    public Map<String, Long> getRequestCounts(
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String residentName,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo)
    {
        return requestService.getRequestCounts(
                projectCode, title, residentName, tenantId, priority, dateFrom, dateTo
        );
    }

    @GetMapping("/{id}")
    public RequestDTO getRequest(@PathVariable UUID id)
    {
        return requestService.getRequestById(id);
    }

    @PostMapping("/createRequest")
    public ResponseEntity<RequestDTO> addNewRequest(@RequestBody RequestDTO requestDTO)
    {
        RequestDTO createdRequest = requestService.addRequest(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRequest);
    }

}
