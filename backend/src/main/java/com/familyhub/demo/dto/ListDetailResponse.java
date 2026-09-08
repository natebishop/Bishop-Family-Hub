package com.familyhub.demo.dto;

import com.familyhub.demo.model.ListCategoryDisplayMode;
import com.familyhub.demo.model.ListKind;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ListDetailResponse(
        UUID id,
        String name,
        ListKind kind,
        ListCategoryDisplayMode categoryDisplayMode,
        Boolean showCompletedOverride,
        List<ListCategoryOption> categories,
        List<ListItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
