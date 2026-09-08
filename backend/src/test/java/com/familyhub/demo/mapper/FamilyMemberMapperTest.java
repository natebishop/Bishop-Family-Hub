package com.familyhub.demo.mapper;

import com.familyhub.demo.dto.FamilyMemberRequest;
import com.familyhub.demo.dto.FamilyMemberResponse;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.FamilyColor;
import com.familyhub.demo.model.FamilyMember;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.familyhub.demo.TestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;

class FamilyMemberMapperTest {

    @Test
    void toEntity_mapsAllFields() {
        Family family = createFamily();
        family.setFamilyMembers(List.of());
        FamilyMemberRequest request = createFamilyMemberRequest();

        FamilyMember entity = FamilyMemberMapper.toEntity(request, family);

        assertThat(entity.getName()).isEqualTo("Test Member");
        assertThat(entity.getColor()).isEqualTo(FamilyColor.CORAL);
        assertThat(entity.getEmail()).isEqualTo("member@test.com");
        assertThat(entity.getFamily()).isEqualTo(family);
    }

    @Test
    void toDto_mapsAllFields() {
        Family family = createFamily();
        family.setFamilyMembers(List.of());
        FamilyMember member = createFamilyMember(family);

        FamilyMemberResponse response = FamilyMemberMapper.toDto(member);

        assertThat(response.id()).isEqualTo(MEMBER_ID);
        assertThat(response.name()).isEqualTo("Test Member");
        assertThat(response.color()).isEqualTo(FamilyColor.CORAL);
        assertThat(response.email()).isEqualTo("member@test.com");
    }
}
