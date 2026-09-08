package com.familyhub.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameListCategoryRequest(
        @NotBlank @Size(max = 100) String name
) {
    // Canonicalize to the trimmed name before validation so @Size(max=100) and @NotBlank
    // judge the stored value, not surrounding whitespace.
    public RenameListCategoryRequest {
        name = name == null ? null : name.trim();
    }
}
