package com.familyhub.demo.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CalendarSelectionRequest(
        @NotNull List<String> calendarIds
) {}
