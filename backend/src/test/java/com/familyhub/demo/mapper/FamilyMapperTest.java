package com.familyhub.demo.mapper;

import com.familyhub.demo.dto.FamilyResponse;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.FamilyMember;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.familyhub.demo.TestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;

class FamilyMapperTest {

    @Test
    void toDto_mapsAllFieldsAndNestedMembers() {
        Family family = createFamily();
        FamilyMember member = createFamilyMember(family);
        family.setFamilyMembers(List.of(member));

        FamilyResponse response = FamilyMapper.toDto(family);

        assertThat(response.id()).isEqualTo(FAMILY_ID);
        assertThat(response.name()).isEqualTo("Test Family");
        assertThat(response.createdAt()).isEqualTo(family.getCreatedAt());
        assertThat(response.members()).hasSize(1);
        assertThat(response.members().getFirst().name()).isEqualTo("Test Member");
    }

    @Test
    void toDto_includesStoredTimezone() {
        Family family = createFamily();
        family.setFamilyMembers(List.of());
        family.setTimezone("Asia/Tokyo");

        FamilyResponse response = FamilyMapper.toDto(family);

        assertThat(response.timezone()).isEqualTo("Asia/Tokyo");
    }

    @Test
    void toDto_invalidStoredTimezone_resolvesToDefault() {
        Family family = createFamily();
        family.setFamilyMembers(List.of());
        family.setTimezone("Not/AZone");

        FamilyResponse response = FamilyMapper.toDto(family);

        assertThat(response.timezone()).isEqualTo("America/Los_Angeles");
    }
}
