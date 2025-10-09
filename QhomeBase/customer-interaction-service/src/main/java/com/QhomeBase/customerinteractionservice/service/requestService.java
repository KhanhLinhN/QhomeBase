package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.dto.RequestDTO;
import com.QhomeBase.customerinteractionservice.model.Request;
import com.QhomeBase.customerinteractionservice.repository.requestRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
            entity.getTenantId(),
            entity.getResidentId(),
            entity.getResident_name(),
            entity.getImage_path(),
            entity.getTitle(),
            entity.getContent(),
            entity.getStatus(),
            entity.getPriority(),
            entity.getCreated_at(),
            entity.getUpdated_at()
        );          
    }

    public RequestDTO getRequestById(UUID id) {
        return requestRepository.findById(id).map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + id));
    }

    // Method to get filtered requests with pagination
    public Page<RequestDTO> getFilteredRequests(
            UUID requestId,
            String title,
            String residentName,
            UUID tenantId,
            String status,
            String priority,
            int pageNo) {

        Specification<Request> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (requestId != null) {
                predicates.add(cb.like(root.get("id"), "%" + requestId.toString() + "%"));
            }
            if (title != null && !title.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
            }
            if (residentName != null && !residentName.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("resident_name")), "%" + residentName.toLowerCase() + "%"));
            }
            if (tenantId != null) {
                predicates.add(cb.equal(root.get("tenant_id"), tenantId));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null && !priority.isEmpty()) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(pageNo, PAGE_SIZE); 

        return requestRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    // Create a new request
    public void createNewRequest(RequestDTO dto) {
        Request entity = new Request();
        entity.setId(dto.getId());
        entity.setTenantId(dto.getTenantId());
        entity.setResidentId(dto.getResidentId());
        entity.setResident_name(dto.getResident_name());
        entity.setImage_path(dto.getImage_path());
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setStatus(dto.getStatus());
        entity.setPriority(dto.getPriority());
        entity.setCreated_at(dto.getCreated_at());
        entity.setUpdated_at(dto.getUpdated_at());
        requestRepository.save(entity);
    }

    // Update an existing request
    public void updateRequest(RequestDTO dto) {
        Request entity = requestRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + dto.getId()));
        entity.setTenantId(dto.getTenantId());
        entity.setResidentId(dto.getResidentId());
        entity.setResident_name(dto.getResident_name());
        entity.setImage_path(dto.getImage_path());
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setStatus(dto.getStatus());
        entity.setPriority(dto.getPriority());
        entity.setCreated_at(dto.getCreated_at());
        entity.setUpdated_at(dto.getUpdated_at());
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
