package com.familyhub.demo.repository;

import com.familyhub.demo.config.TestcontainersConfig;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.FamilyColor;
import com.familyhub.demo.model.FamilyMember;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class FamilyMemberRepositoryTest {

    @Autowired
    FamilyMemberRepository familyMemberRepository;

    @Autowired
    TestEntityManager em;

    private Family persistFamily(String username) {
        Family family = new Family();
        family.setName("Family " + username);
        family.setUsername(username);
        family.setPasswordHash("$2a$10$dummyhashfortesting");
        return em.persistFlushFind(family);
    }

    private FamilyMember persistMember(Family family, String name) {
        FamilyMember member = new FamilyMember();
        member.setFamily(family);
        member.setName(name);
        member.setColor(FamilyColor.CORAL);
        return em.persistFlushFind(member);
    }

    @Test
    void findByFamily_returnsMembersForFamily() {
        Family family = persistFamily("member_family_1");
        persistMember(family, "Alice");
        persistMember(family, "Bob");

        List<FamilyMember> members = familyMemberRepository.findByFamily(family);

        assertThat(members).hasSize(2);
        assertThat(members).extracting(FamilyMember::getName)
                .containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    void findByFamily_doesNotReturnOtherFamilyMembers() {
        Family family1 = persistFamily("member_family_2");
        Family family2 = persistFamily("member_family_3");
        persistMember(family1, "Alice");
        persistMember(family2, "Bob");

        List<FamilyMember> members = familyMemberRepository.findByFamily(family1);

        assertThat(members).hasSize(1);
        assertThat(members.getFirst().getName()).isEqualTo("Alice");
    }
}
