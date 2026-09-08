package com.familyhub.demo.dto;

import java.time.LocalDate;
import java.util.List;

public record MealBoardResponse(
        LocalDate weekStartDate,
        List<MealDayResponse> days
) {
}
