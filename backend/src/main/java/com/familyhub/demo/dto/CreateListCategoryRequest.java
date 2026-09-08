package com.familyhub.demo.dto;

import com.familyhub.demo.model.ListKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateListCategoryRequest(
        @NotNull ListKind kind,
        @NotBlank @Size(max = 100) String name
) {
    // Canonicalize to the trimmed name before validation so @Size(max=100) and @NotBlank
    // judge the stored value, not surrounding whitespace.
    public CreateListCategoryRequest {
        name = name == null ? null : name.trim();
    }
}
