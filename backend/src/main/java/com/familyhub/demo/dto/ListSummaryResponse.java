package com.familyhub.demo.dto;

import com.familyhub.demo.model.ListKind;

import java.util.UUID;

public record ListSummaryResponse(
        UUID id,
        String name,
        ListKind kind,
        int totalItems,
        int completedItems
) {
}
