package com.familyhub.demo.dto;

import com.familyhub.demo.model.MealCollisionMode;
import com.familyhub.demo.model.MealType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DuplicateMealSlotRequest(
        @NotNull LocalDate sourceWeekStartDate,
        @NotNull @Min(0) @Max(6) Integer sourceDayIndex,
        @NotNull MealType sourceMealType,
        @NotNull LocalDate destinationWeekStartDate,
        @NotNull @Min(0) @Max(6) Integer destinationDayIndex,
        @NotNull MealType destinationMealType,
        @NotNull MealCollisionMode collisionMode
) {
}
