package com.familyhub.demo.dto;

import com.familyhub.demo.model.ListKind;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReorderListCategoriesRequest(
        @NotNull ListKind kind,
        @NotNull List<@NotNull UUID> expectedCategoryIds,
        @NotNull List<@NotNull UUID> categoryIds
) {}
