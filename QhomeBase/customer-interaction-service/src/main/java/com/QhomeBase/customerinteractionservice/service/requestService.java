package com.QhomeBase.customerinteractionservice.service;

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

    public Page<Request> getFilteredRequests(
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
                predicates.add(cb.equal(root.get("id"), requestId));
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

        return requestRepository.findAll(spec, pageable);
    }
}
