package com.familyhub.demo.mapper;

import com.familyhub.demo.dto.MealSlotEntryResponse;
import com.familyhub.demo.dto.MealSlotResponse;
import com.familyhub.demo.model.MealSlot;
import com.familyhub.demo.model.MealSlotEntry;
import com.familyhub.demo.model.MealSlotRole;
import com.familyhub.demo.model.MealType;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class MealMapper {
    private MealMapper() {
    }

    public static MealSlotResponse toSlotDto(MealSlot slot) {
        return new MealSlotResponse(
                slot.getId(),
                slot.getWeekStartDate(),
                slot.getDayIndex(),
                slot.getMealType(),
                slot.getEntries().stream()
                        .filter(entry -> entry.getRole() == MealSlotRole.PRIMARY)
                        .findFirst()
                        .map(MealMapper::toEntryDto)
                        .orElse(null),
                slot.getEntries().stream()
                        .filter(entry -> entry.getRole() == MealSlotRole.EXTRA)
                        .sorted(Comparator.comparingInt(MealSlotEntry::getSortOrder))
                        .map(MealMapper::toEntryDto)
                        .toList(),
                slot.getNote()
        );
    }

    public static MealSlotResponse emptySlot(LocalDate weekStartDate, int dayIndex, MealType mealType) {
        return new MealSlotResponse(
                null,
                weekStartDate,
                dayIndex,
                mealType,
                null,
                List.of(),
                null
        );
    }

    public static MealSlotEntryResponse toEntryDto(MealSlotEntry entry) {
        return new MealSlotEntryResponse(
                entry.getId(),
                entry.getRole(),
                entry.getSourceType(),
                entry.getRecipe() == null ? null : entry.getRecipe().getId(),
                entry.getTitleSnapshot(),
                entry.getImageUrlSnapshot(),
                entry.getNoteSnapshot()
        );
    }
}
