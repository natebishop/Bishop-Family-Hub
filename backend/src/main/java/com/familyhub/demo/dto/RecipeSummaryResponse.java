package com.familyhub.demo.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RecipeSummaryResponse(
        UUID id,
        String title,
        String imageUrl,
        boolean favorite,
        List<String> tags,
        LocalDateTime updatedAt
) {
}
