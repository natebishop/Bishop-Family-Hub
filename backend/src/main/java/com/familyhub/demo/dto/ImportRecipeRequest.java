package com.familyhub.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record ImportRecipeRequest(
        @NotBlank String url
) {
}
