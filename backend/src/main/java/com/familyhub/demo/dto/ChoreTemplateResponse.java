package com.familyhub.demo.dto;

import com.familyhub.demo.model.ChoreCadence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ChoreTemplateResponse(
        UUID id,
        String title,
        UUID assignedToMemberId,
        ChoreCadence cadence,
        LocalDate activeFrom,
        boolean archived,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
