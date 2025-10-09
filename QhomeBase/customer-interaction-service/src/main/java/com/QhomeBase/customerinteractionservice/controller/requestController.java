package com.QhomeBase.customerinteractionservice.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.QhomeBase.customerinteractionservice.dto.RequestDTO;
import com.QhomeBase.customerinteractionservice.dto.RequestMapper;
import com.QhomeBase.customerinteractionservice.service.requestService;

@RestController
@RequestMapping("/api/requests")
public class requestController {

    private final requestService requestService;

    public requestController(requestService requestService, RequestMapper requestMapper) {
        this.requestService = requestService;
    }

   @GetMapping()
   public Page<RequestDTO> getRequests(
           @RequestParam(required = false) UUID requestId,
           @RequestParam(required = false) String title,
           @RequestParam(required = false) String residentName,
           @RequestParam(required = false) UUID tenantId,
           @RequestParam(required = false) String status,
           @RequestParam(required = false) String priority,
           @RequestParam(defaultValue = "0") int pageNo)
   {

       Page<RequestDTO> requestPage = requestService.getFilteredRequests(
               requestId, title, residentName, tenantId, status, priority, pageNo
       );

       return requestPage;
   }

}
