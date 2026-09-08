package com.familyhub.demo.dto;

import com.familyhub.demo.model.FamilyColor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record FamilyMemberRequest(
    @NotEmpty
    @Size(max = 30, message = "Name must be 30 characters or less")
    String name,

    @NotNull
    FamilyColor color,

    @Email
    @Size(max = 254, message = "Email is too long")
    String email,

    @Size(max = 254, message = "Avatar url is too long")
    String avatarUrl
) {
}
