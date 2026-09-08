package com.familyhub.demo.repository;

import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.ListPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ListPreferencesRepository extends JpaRepository<ListPreferences, UUID> {

    Optional<ListPreferences> findByFamily(Family family);
}
