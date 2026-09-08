package com.familyhub.demo.repository;

import com.familyhub.demo.config.TestcontainersConfig;
import com.familyhub.demo.model.Family;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(TestcontainersConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class FamilyRepositoryTest {

    @Autowired
    FamilyRepository familyRepository;

    @Autowired
    TestEntityManager em;

    private Family persistFamily(String username) {
        Family family = new Family();
        family.setName("Family " + username);
        family.setUsername(username);
        family.setPasswordHash("$2a$10$dummyhashfortesting");
        return em.persistFlushFind(family);
    }

    @Test
    void existsByUsername_exists_returnsTrue() {
        persistFamily("exists_test");

        assertThat(familyRepository.existsByUsername("exists_test")).isTrue();
    }

    @Test
    void existsByUsername_notExists_returnsFalse() {
        assertThat(familyRepository.existsByUsername("no_such_user")).isFalse();
    }

    @Test
    void findByUsername_exists_returnsFamily() {
        Family saved = persistFamily("findme");

        Optional<Family> found = familyRepository.findByUsername("findme");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getName()).isEqualTo("Family findme");
    }

    @Test
    void findByUsername_notExists_returnsEmpty() {
        assertThat(familyRepository.findByUsername("ghost")).isEmpty();
    }

    @Test
    void uniqueUsernameConstraint_throwsDataIntegrityViolation() {
        persistFamily("duplicate");

        Family duplicate = new Family();
        duplicate.setName("Another");
        duplicate.setUsername("duplicate");
        duplicate.setPasswordHash("$2a$10$dummyhashfortesting");

        assertThatThrownBy(() -> {
            familyRepository.saveAndFlush(duplicate);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
