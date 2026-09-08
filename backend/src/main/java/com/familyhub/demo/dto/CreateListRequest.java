package com.familyhub.demo.dto;

import com.familyhub.demo.model.ListKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateListRequest(
        @NotBlank
        @Size(max = 100, message = "List name must be 100 characters or less")
        String name,

        @NotNull
        ListKind kind
) {
}
