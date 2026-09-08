package com.familyhub.demo.mapper;

import com.familyhub.demo.dto.FamilyResponse;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.service.FamilyTimezoneResolver;

public class FamilyMapper {
    private FamilyMapper() {}

    public static FamilyResponse toDto(Family family) {
        return new FamilyResponse(
                family.getId(),
                family.getName(),
                FamilyTimezoneResolver.resolveStoredTimezoneOrDefault(family.getTimezone()),
                family.getFamilyMembers().stream()
                        .map(FamilyMemberMapper::toDto)
                        .toList(),
                family.getCreatedAt()
        );
    }
}
