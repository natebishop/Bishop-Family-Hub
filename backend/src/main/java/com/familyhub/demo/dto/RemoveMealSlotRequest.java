package com.familyhub.demo.dto;

import com.familyhub.demo.model.MealType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RemoveMealSlotRequest(
        @NotNull LocalDate weekStartDate,
        @NotNull @Min(0) @Max(6) Integer dayIndex,
        @NotNull MealType mealType
) {
}
