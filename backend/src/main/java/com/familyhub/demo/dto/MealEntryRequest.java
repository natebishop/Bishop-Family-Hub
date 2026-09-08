package com.familyhub.demo.dto;

import com.familyhub.demo.model.MealEntrySourceType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MealEntryRequest(
        @NotNull MealEntrySourceType sourceType,
        UUID recipeId,
        String title,
        String imageUrl,
        String note
) {
}
