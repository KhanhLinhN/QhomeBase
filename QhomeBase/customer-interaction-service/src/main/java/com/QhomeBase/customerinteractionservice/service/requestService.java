package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.client.BaseServiceClient;
import com.QhomeBase.customerinteractionservice.client.dto.ResidentResponse;
import com.QhomeBase.customerinteractionservice.dto.RequestDTO;
import com.QhomeBase.customerinteractionservice.dto.StatusCountDTO;
import com.QhomeBase.customerinteractionservice.model.ProcessingLog;
import com.QhomeBase.customerinteractionservice.model.Request;
import com.QhomeBase.customerinteractionservice.repository.processingLogRepository;
import com.QhomeBase.customerinteractionservice.repository.requestRepository;
import lombok.extern.slf4j.Slf4j;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class requestService {
    private final requestRepository requestRepository;
    private final processingLogRepository processingLogRepository;
    private final BaseServiceClient baseServiceClient;
    private static final int PAGE_SIZE = 5;

    public requestService(requestRepository requestRepository,
                          processingLogRepository processingLogRepository,
                          BaseServiceClient baseServiceClient) {
        this.requestRepository = requestRepository;
        this.processingLogRepository = processingLogRepository;
        this.baseServiceClient = baseServiceClient;
    }

    public Request createRequest(Request newRequest) {
        return requestRepository.save(newRequest);
    }

    public RequestDTO mapToDto(Request entity) {
        return new RequestDTO(
            entity.getId(),
            entity.getRequestCode(),
            entity.getResidentId(),
            entity.getResidentName(),
            entity.getImagePath(),
            entity.getTitle(),
            entity.getContent(),
            entity.getStatus(),
            entity.getCreatedAt().toString().replace("T", " "),
            entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString().replace("T", " ") : null
        );
    }

    public RequestDTO getRequestById(UUID id) {
        return requestRepository.findById(id).map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + id));
    }

    // Method to get filtered requests with pagination
    public Page<RequestDTO> getFilteredRequests(
            String status,
            int pageNo,
            String dateFrom,
            String dateTo) {

        Specification<Request> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (StringUtils.hasText(dateFrom)) {
                LocalDate fromDate = LocalDate.parse(dateFrom);
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }

            if (StringUtils.hasText(dateTo)) {
                LocalDate toDate = LocalDate.parse(dateTo);
                predicates.add(cb.lessThan(root.get("createdAt"), toDate.plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(pageNo, PAGE_SIZE); 

        return requestRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    public Map<String, Long> getRequestCounts(String dateFrom, String dateTo) {

        List<StatusCountDTO> countsByStatus = requestRepository.countRequestsByStatus(
                dateFrom, dateTo
        );

        Map<String, Long> result = countsByStatus.stream()
                .collect(Collectors.toMap(StatusCountDTO::getStatus, StatusCountDTO::getCount));

        long total = result.values().stream().mapToLong(Long::longValue).sum();
        result.put("total", total);

        return result;
    }


    private String generateRequestCode() {
        String prefix = "REQ";
        String year = String.valueOf(LocalDateTime.now().getYear());
        long count = requestRepository.count() + 1;
        return String.format("%s-%s-%05d", prefix, year, count);
    }


    public RequestDTO createNewRequest(RequestDTO dto, Authentication authentication) {
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        UUID userId = extractUserId(auth);
        if (userId == null) {
            throw new IllegalArgumentException("User information is required to create a request");
        }

        ResidentResponse resident = baseServiceClient.getResidentByUserId(userId);
        if (resident == null) {
            throw new IllegalArgumentException("Resident information could not be resolved for user: " + userId);
        }

        Request entity = new Request();
        entity.setId(dto.getId());
        entity.setRequestCode(dto.getRequestCode() != null ? dto.getRequestCode() : generateRequestCode());
        entity.setResidentId(resident.id());
        entity.setResidentName(resident.fullName());
        entity.setImagePath(dto.getImagePath());
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "Pending");
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        Request savedRequest = requestRepository.save(entity);

        ProcessingLog newLog = new ProcessingLog();
        newLog.setRecordId(savedRequest.getId());
        newLog.setStaffInCharge(null);
        newLog.setStaffInChargeName(null);
        newLog.setContent("Request created by: " + savedRequest.getResidentName());
        newLog.setRequestStatus(savedRequest.getStatus());
        newLog.setCreatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        processingLogRepository.save(newLog);
        return this.mapToDto(savedRequest);
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.QhomeBase.customerinteractionservice.security.UserPrincipal userPrincipal) {
            return userPrincipal.uid();
        }
        return null;
    }
}
