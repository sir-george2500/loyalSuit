package com.loyalsuit.modules.hrm.application.dto;

import com.loyalsuit.modules.hrm.domain.Award;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AwardResponse(
        UUID id,
        UUID employeeId,
        String title,
        String category,
        LocalDate awardedOn,
        String description,
        String awardedBy,
        Instant createdAt) {

    public static AwardResponse from(Award a) {
        return new AwardResponse(a.getId(), a.getEmployeeId(), a.getTitle(), a.getCategory().name(),
                a.getAwardedOn(), a.getDescription(), a.getAwardedBy(), a.getCreatedAt());
    }
}
