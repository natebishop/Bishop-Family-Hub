package com.familyhub.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateListItemRequest(
        @NotBlank
        @Size(max = 100, message = "Item text must be 100 characters or less")
        String text,

        @NotNull
        Boolean completed,

        UUID categoryId
) {
}
