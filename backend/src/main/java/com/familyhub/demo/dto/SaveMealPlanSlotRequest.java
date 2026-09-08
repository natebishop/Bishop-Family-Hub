package com.familyhub.demo.dto;

import com.familyhub.demo.model.MealType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SaveMealPlanSlotRequest(
        @NotNull @Min(0) @Max(6) Integer dayIndex,
        @NotNull MealType mealType,
        @Valid @NotNull MealEntryRequest primary,
        List<@Valid MealEntryRequest> extras,
        String note
) {
}
