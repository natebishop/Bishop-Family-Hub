package com.familyhub.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record SaveMealPlanRequest(
        @NotNull LocalDate weekStartDate,
        @Valid @NotEmpty @Size(max = 21) List<@NotNull SaveMealPlanSlotRequest> slots
) {
}
