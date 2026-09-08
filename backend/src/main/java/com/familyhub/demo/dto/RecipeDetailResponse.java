package com.familyhub.demo.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RecipeDetailResponse(
        UUID id,
        String title,
        String imageUrl,
        List<String> ingredients,
        List<String> instructions,
        String note,
        String sourceUrl,
        List<String> tags,
        boolean favorite,
        LocalDateTime updatedAt
) {
}
