package com.familyhub.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegisterRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-z0-9_]+$", message = "Username can only contain lowercase letters, numbers, and underscores")
    String username,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 - 100 characters")
    String password,

    @NotBlank(message = "Family name is required")
    @Size(max = 50, message = "Family name must be 50 characters or less")
    String familyName,

    @NotEmpty(message = "A family must have at least one member")
    List<@Valid FamilyMemberRequest> members,

    @Size(max = 100, message = "Timezone must be 100 characters or less")
    String timezone
){}
