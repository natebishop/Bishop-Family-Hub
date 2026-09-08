package com.familyhub.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.familyhub.demo.model.ChoreScope;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateCurrentPeriodCompletionRequest(
        @NotNull
        ChoreScope scope,

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate periodStartDate
) {
}
