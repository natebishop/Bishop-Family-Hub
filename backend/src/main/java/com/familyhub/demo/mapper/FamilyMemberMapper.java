package com.familyhub.demo.mapper;

import com.familyhub.demo.dto.FamilyMemberRequest;
import com.familyhub.demo.dto.FamilyMemberResponse;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.FamilyMember;

public class FamilyMemberMapper {
    private FamilyMemberMapper() {}

    public static FamilyMember toEntity(FamilyMemberRequest request, Family owner) {
        FamilyMember familyMember = new FamilyMember();

        familyMember.setName(request.name());
        familyMember.setEmail(request.email());
        familyMember.setColor(request.color());
        familyMember.setAvatarUrl(request.avatarUrl());
        familyMember.setFamily(owner);

        return familyMember;
    }

    public static FamilyMemberResponse toDto(FamilyMember familyMember) {
        FamilyMemberResponse response = new FamilyMemberResponse(
                familyMember.getId(),
                familyMember.getName(),
                familyMember.getColor(),
                familyMember.getEmail(),
                familyMember.getAvatarUrl()
        );

        return response;
    }
}
