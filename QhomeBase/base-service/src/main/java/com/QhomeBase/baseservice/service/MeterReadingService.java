package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.MeterReadingCreateReq;
import com.QhomeBase.baseservice.dto.MeterReadingDto;
import com.QhomeBase.baseservice.model.Meter;
import com.QhomeBase.baseservice.model.MeterReading;
import com.QhomeBase.baseservice.model.MeterReadingAssignment;
import com.QhomeBase.baseservice.model.MeterReadingSession;
import com.QhomeBase.baseservice.repository.MeterReadingAssignmentRepository;
import com.QhomeBase.baseservice.repository.MeterReadingRepository;
import com.QhomeBase.baseservice.repository.MeterReadingSessionRepository;
import com.QhomeBase.baseservice.repository.MeterRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class MeterReadingService {
    private final MeterReadingRepository readingRepo;
    private final MeterRepository meterRepo;
    private final MeterReadingAssignmentRepository assignmentRepo;
    private final MeterReadingSessionRepository sessionRepo;

    public MeterReadingDto create(MeterReadingCreateReq meterReadingCreateReq, Authentication auth){
        var p = (UserPrincipal) auth.getPrincipal();
        UUID readerId = p.uid();
        MeterReadingSession meterReadingSession = sessionRepo.findById(meterReadingCreateReq.sessionId()).orElse(null);
        MeterReadingAssignment meterReadingAssignment = assignmentRepo.findById(meterReadingSession.getAssignment().getId()).orElse(null);
        if (meterReadingAssignment == null){
            throw new IllegalArgumentException("Assignment not found");
        }
        Meter meter = meterRepo.findById(meterReadingCreateReq.meterId())
                .orElseThrow(() -> new IllegalArgumentException("Meter not found"));
        validateMeterInScope(meterReadingAssignment, meter);
        BigDecimal previousIndex = getPreviousIndex(meter.getId());
        MeterReading meterReading = MeterReading.builder()
                .meter(meter)
                .assignment(meterReadingAssignment)
                .session(meterReadingSession)
                .readingDate(meterReadingCreateReq.readingDate())
                .prevIndex(previousIndex)
                .currIndex(meterReadingCreateReq.currIndex())
                .note(meterReadingCreateReq.note())
                .readerId(readerId)
                .photoFileId(meterReadingCreateReq.photoFileId())
                .build();
        meterRepo.save(meter);
        return toDto(meterReading);

    }
    public BigDecimal getPreviousIndex (UUID meterId){
        List<MeterReading> meterReadingList = readingRepo.findByMeterId(meterId);
        MeterReading latest = meterReadingList.stream()
                .filter(r -> r != null && r.getReadingDate() != null)
                .sorted(
                        Comparator.comparing(MeterReading::getReadingDate).reversed()
                                .thenComparing(MeterReading::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .findFirst()
                .orElse(null);
        return latest != null ? latest.getCurrIndex() : null;
    }
    public void validateMeterInScope(MeterReadingAssignment a, Meter m) {
        UUID assBuilding = a.getBuilding().getId();
        UUID mBuilding = m.getUnit().getBuilding().getId();
        if ( assBuilding != mBuilding) {
            throw new IllegalArgumentException("Not same building");
        }
        if (m.getUnit().getFloor() <= a.getFloorFrom() || m.getUnit().getFloor() >= a.getFloorTo()) {
            throw new IllegalArgumentException("Not same floor");
        }
    }

    public  MeterReadingDto toDto(MeterReading r) {
        BigDecimal prev   = r.getPrevIndex();
        BigDecimal curr   = r.getCurrIndex();
        BigDecimal usage  = null;
        if (curr != null && prev != null) {
            usage = curr.subtract(prev);
            if (usage.signum() < 0) {
                throw new IllegalArgumentException("Current index must be >= previous index");
            }
        }
        return new MeterReadingDto(
                r.getId(),
                r.getAssignment() != null ? r.getAssignment().getId() : null,
                r.getSession() != null ? r.getSession().getId() : null,
                r.getMeter().getId(),
                r.getMeter().getMeterCode(),
                r.getMeter().getUnit() != null ? r.getMeter().getUnit().getId() : null,
                r.getMeter().getUnit() != null ? r.getMeter().getUnit().getCode() : null,
                r.getMeter().getUnit() != null ? r.getMeter().getUnit().getFloor() : null,
                r.getPrevIndex(),
                r.getCurrIndex(),
                usage,
                r.getReadingDate(),
                r.getNote(),
                r.getReaderId(),
                r.getPhotoFileId(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
