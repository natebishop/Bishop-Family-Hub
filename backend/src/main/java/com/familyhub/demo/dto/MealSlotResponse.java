package com.familyhub.demo.dto;

import com.familyhub.demo.model.MealType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MealSlotResponse(
        UUID id,
        LocalDate weekStartDate,
        int dayIndex,
        MealType mealType,
        MealSlotEntryResponse primary,
        List<MealSlotEntryResponse> extras,
        String note
) {
}
