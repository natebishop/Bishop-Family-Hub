package com.familyhub.demo.dto;

import com.familyhub.demo.model.ChoreScope;

import java.time.LocalDate;

public record ChoreCurrentPeriodStateResponse(
        ChoreScope scope,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        ChoreBoardItemResponse item
) {
}
