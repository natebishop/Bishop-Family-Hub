package com.familyhub.demo.dto;

import com.familyhub.demo.model.ListCategoryDisplayMode;
import jakarta.validation.constraints.NotNull;

// PATCH replaces both fields on every request. showCompletedOverride is tri-state:
// null = use family default, true/false = per-list override.
public record UpdateListRequest(
        @NotNull
        ListCategoryDisplayMode categoryDisplayMode,

        Boolean showCompletedOverride
) {
}
