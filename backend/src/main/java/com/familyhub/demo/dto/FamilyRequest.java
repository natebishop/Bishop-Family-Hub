package com.familyhub.demo.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FamilyRequest(
        @Size(min = 1, max = 50, message = "Name must be between 1 and 50 characters")
        String name,

        @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
        @Pattern(regexp = "^[a-z0-9_]+$", message = "Username can only contain lowercase letters, numbers, and underscores")
        String username,

        String timezone
) {}
