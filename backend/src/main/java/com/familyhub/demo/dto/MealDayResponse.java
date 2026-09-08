package com.familyhub.demo.dto;

import java.time.LocalDate;
import java.util.List;

public record MealDayResponse(
        LocalDate date,
        int dayIndex,
        List<MealSlotResponse> slots
) {
}
