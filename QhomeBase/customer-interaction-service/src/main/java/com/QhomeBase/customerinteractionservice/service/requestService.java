package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.dto.RequestDTO;
import com.QhomeBase.customerinteractionservice.dto.StatusCountDTO;
import com.QhomeBase.customerinteractionservice.model.Request;
import com.QhomeBase.customerinteractionservice.repository.requestRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class requestService {
    private final requestRepository requestRepository;
    private static final int PAGE_SIZE = 5;

    public requestService(requestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public Request createRequest(Request newRequest) {
        return requestRepository.save(newRequest);
    }

    public RequestDTO mapToDto(Request entity) {
        return new RequestDTO(
            entity.getId(),
            entity.getRequestCode(),
            entity.getTenantId(),
            entity.getResidentId(),
            entity.getResidentName(),
            entity.getImagePath(),
            entity.getTitle(),
            entity.getContent(),
            entity.getStatus(),
            entity.getPriority(),
            entity.getCreatedAt().toString().replace("T", " "),
            entity.getUpdatedAt().toString().replace("T", " ")
        );
    }

    public RequestDTO getRequestById(UUID id) {
        return requestRepository.findById(id).map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + id));
    }

    // Method to get filtered requests with pagination
    public Page<RequestDTO> getFilteredRequests(
            String projectCode,
            String title,
            String residentName,
            UUID tenantId,
            String status,
            String priority,
            int pageNo,
            String dateFrom,
            String dateTo) {

        Specification<Request> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (projectCode != null && !projectCode.isEmpty()) {
                predicates.add(cb.like(root.get("requestCode"), "%" + projectCode + "%"));
            }
            if (title != null && !title.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
            }
            if (residentName != null && !residentName.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("residentName")), "%" + residentName.toLowerCase() + "%"));
            }
            if (tenantId != null) {
                predicates.add(cb.equal(root.get("tenantId"), tenantId));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null && !priority.isEmpty()) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            if (dateFrom != null && !dateFrom.isEmpty()) {
                LocalDate fromDate = LocalDate.parse(dateFrom);
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }

            if (dateTo != null && !dateTo.isEmpty()) {
                LocalDate toDate = LocalDate.parse(dateTo);
                predicates.add(cb.lessThan(root.get("createdAt"), toDate.plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(pageNo, PAGE_SIZE); 

        return requestRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    public Map<String, Long> getRequestCounts(String projectCode, String title, String residentName, UUID tenantId, String status, String priority, String dateFrom, String dateTo) {

        List<StatusCountDTO> countsByStatus = requestRepository.countRequestsByStatus(
                projectCode, title, residentName, tenantId, status, priority, dateFrom, dateTo
        );

        Map<String, Long> result = countsByStatus.stream()
                .collect(Collectors.toMap(StatusCountDTO::getStatus, StatusCountDTO::getCount));

        long total = result.values().stream().mapToLong(Long::longValue).sum();
        result.put("total", total);

        return result;
    }


    // Create a new request
    public void createNewRequest(RequestDTO dto) {
        Request entity = new Request();
        entity.setId(dto.getId());
        entity.setRequestCode(dto.getRequestCode());
        entity.setTenantId(dto.getTenantId());
        entity.setResidentId(dto.getResidentId());
        entity.setResidentName(dto.getResidentName());
        entity.setImagePath(dto.getImagePath());
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setStatus(dto.getStatus());
        entity.setPriority(dto.getPriority());
        entity.setCreatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        requestRepository.save(entity);
    }

    // Update an existing request
    public void updateRequest(RequestDTO dto) {
        Request entity = requestRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + dto.getId()));
        entity.setTenantId(dto.getTenantId());
        entity.setResidentId(dto.getResidentId());
        entity.setResidentName(dto.getResidentName());
        entity.setImagePath(dto.getImagePath());
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setStatus(dto.getStatus());
        entity.setPriority(dto.getPriority());
        entity.setUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        requestRepository.save(entity);
    }

    // Update status of a request
    public void updateStatus(UUID requestId, String newStatus) {
        Request entity = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));
        entity.setStatus(newStatus);
        requestRepository.save(entity);
    }
}
