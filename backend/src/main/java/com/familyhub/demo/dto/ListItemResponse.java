package com.familyhub.demo.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ListItemResponse(
        UUID id,
        String text,
        boolean completed,
        LocalDateTime completedAt,
        UUID categoryId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
