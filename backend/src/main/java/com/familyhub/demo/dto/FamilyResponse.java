package com.familyhub.demo.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FamilyResponse(
        UUID id,
        String name,
        String timezone,
        List<FamilyMemberResponse> members,
        LocalDateTime createdAt
) {
}
