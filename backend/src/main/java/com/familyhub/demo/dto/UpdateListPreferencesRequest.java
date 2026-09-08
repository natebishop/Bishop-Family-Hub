package com.familyhub.demo.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateListPreferencesRequest(
        @NotNull
        Boolean showCompletedByDefault
) {
}
