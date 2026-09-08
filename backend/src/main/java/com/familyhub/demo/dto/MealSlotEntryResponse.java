package com.familyhub.demo.dto;

import com.familyhub.demo.model.MealEntrySourceType;
import com.familyhub.demo.model.MealSlotRole;

import java.util.UUID;

public record MealSlotEntryResponse(
        UUID id,
        MealSlotRole role,
        MealEntrySourceType sourceType,
        UUID recipeId,
        String title,
        String imageUrl,
        String note
) {
}
