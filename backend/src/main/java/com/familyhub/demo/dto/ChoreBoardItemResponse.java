package com.familyhub.demo.dto;

import com.familyhub.demo.model.ChoreCadence;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChoreBoardItemResponse(
        UUID templateId,
        String title,
        ChoreCadence cadence,
        UUID assignedToMemberId,
        boolean completed,
        LocalDateTime completedAt
) {
}
