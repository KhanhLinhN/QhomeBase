package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.MeterReadingSessionCreateReq;
import com.QhomeBase.baseservice.dto.MeterReadingSessionDto;
import com.QhomeBase.baseservice.model.MeterReadingAssignment;
import com.QhomeBase.baseservice.model.MeterReadingSession;
import com.QhomeBase.baseservice.repository.MeterReadingAssignmentRepository;
import com.QhomeBase.baseservice.repository.MeterReadingSessionRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeterReadingSessionService {

    private final MeterReadingSessionRepository sessionRepository;
    private final MeterReadingAssignmentRepository assignmentRepository;

    @Transactional
    public MeterReadingSessionDto startSession(MeterReadingSessionCreateReq req, UserPrincipal principal) {
        MeterReadingAssignment assignment = assignmentRepository.findById(req.assignmentId())
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        if (assignment.getCompletedAt() != null) {
            throw new IllegalStateException("Cannot start session for completed assignment");
        }

        sessionRepository.findActiveSessionByReader(principal.uid())
                .ifPresent(activeSession -> {
                    throw new IllegalStateException(
                            "Staff already has an active session. Please complete it before starting a new one.");
                });

        MeterReadingSession session = MeterReadingSession.builder()
                .assignment(assignment)
                .cycle(assignment.getCycle())
                .building(assignment.getBuilding())
                .service(assignment.getService())
                .readerId(principal.uid())
                .deviceInfo(req.deviceInfo())
                .build();

        session = sessionRepository.save(session);
        return toDto(session);
    }

    @Transactional
    public MeterReadingSessionDto completeSession(UUID sessionId, UserPrincipal principal) {
        MeterReadingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.getReaderId().equals(principal.uid())) {
            throw new IllegalStateException("Only the session owner can complete it");
        }

        if (session.isCompleted()) {
            throw new IllegalStateException("Session is already completed");
        }

        session.setCompletedAt(OffsetDateTime.now());
        session = sessionRepository.save(session);

        return toDto(session);
    }

    @Transactional(readOnly = true)
    public MeterReadingSessionDto getById(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
    }

    @Transactional(readOnly = true)
    public List<MeterReadingSessionDto> getByAssignmentId(UUID assignmentId) {
        return sessionRepository.findByAssignmentId(assignmentId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeterReadingSessionDto> getByReaderId(UUID readerId) {
        return sessionRepository.findByReaderId(readerId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MeterReadingSessionDto getActiveSessionByReader(UUID readerId) {
        return sessionRepository.findActiveSessionByReader(readerId)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<MeterReadingSessionDto> getCompletedSessionsByReader(UUID readerId) {
        return sessionRepository.findCompletedSessionsByReader(readerId).stream()
                .map(this::toDto)
                .toList();
    }

    private MeterReadingSessionDto toDto(MeterReadingSession session) {
        return new MeterReadingSessionDto(
                session.getId(),
                session.getAssignment() != null ? session.getAssignment().getId() : null,
                session.getCycle() != null ? session.getCycle().getId() : null,
                session.getBuilding() != null ? session.getBuilding().getId() : null,
                session.getService() != null ? session.getService().getId() : null,
                session.getReaderId(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getUnitsRead(),
                session.getDeviceInfo(),
                session.isCompleted(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}

