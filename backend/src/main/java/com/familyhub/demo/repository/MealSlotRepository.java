package com.familyhub.demo.repository;

import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.MealSlot;
import com.familyhub.demo.model.MealType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealSlotRepository extends JpaRepository<MealSlot, UUID> {

    @EntityGraph(attributePaths = "entries")
    List<MealSlot> findByFamilyAndWeekStartDateOrderByDayIndexAscMealTypeAsc(Family family, LocalDate weekStartDate);

    @EntityGraph(attributePaths = "entries")
    Optional<MealSlot> findByFamilyAndWeekStartDateAndDayIndexAndMealType(
            Family family,
            LocalDate weekStartDate,
            int dayIndex,
            MealType mealType
    );
}
