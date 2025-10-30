package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.MeterReadingSessionCreateReq;
import com.QhomeBase.baseservice.dto.MeterReadingSessionDto;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.MeterReadingSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/meter-reading-sessions")
@RequiredArgsConstructor
public class MeterReadingSessionController {

    private final MeterReadingSessionService sessionService;

    @PostMapping
    public ResponseEntity<MeterReadingSessionDto> startSession(
            @Valid @RequestBody MeterReadingSessionCreateReq request,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        MeterReadingSessionDto session = sessionService.startSession(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @PatchMapping("/{sessionId}/complete")
    public ResponseEntity<MeterReadingSessionDto> completeSession(
            @PathVariable UUID sessionId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        MeterReadingSessionDto session = sessionService.completeSession(sessionId, principal);
        return ResponseEntity.ok(session);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<MeterReadingSessionDto> getSessionById(@PathVariable UUID sessionId) {
        MeterReadingSessionDto session = sessionService.getById(sessionId);
        return ResponseEntity.ok(session);
    }

    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<List<MeterReadingSessionDto>> getSessionsByAssignment(
            @PathVariable UUID assignmentId) {
        List<MeterReadingSessionDto> sessions = sessionService.getByAssignmentId(assignmentId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<MeterReadingSessionDto>> getSessionsByStaff(@PathVariable UUID staffId) {
        List<MeterReadingSessionDto> sessions = sessionService.getByReaderId(staffId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/my-sessions")
    public ResponseEntity<List<MeterReadingSessionDto>> getMySessions(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        List<MeterReadingSessionDto> sessions = sessionService.getByReaderId(principal.uid());
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/my-active-session")
    public ResponseEntity<MeterReadingSessionDto> getMyActiveSession(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        MeterReadingSessionDto session = sessionService.getActiveSessionByReader(principal.uid());
        if (session == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(session);
    }

    @GetMapping("/staff/{staffId}/completed")
    public ResponseEntity<List<MeterReadingSessionDto>> getCompletedSessionsByStaff(
            @PathVariable UUID staffId) {
        List<MeterReadingSessionDto> sessions = sessionService.getCompletedSessionsByReader(staffId);
        return ResponseEntity.ok(sessions);
    }
}

