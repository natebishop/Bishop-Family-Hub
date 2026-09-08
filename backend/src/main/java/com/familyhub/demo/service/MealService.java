package com.familyhub.demo.service;

import com.familyhub.demo.dto.MealBoardResponse;
import com.familyhub.demo.dto.MealDayResponse;
import com.familyhub.demo.dto.DuplicateMealSlotRequest;
import com.familyhub.demo.dto.MealEntryRequest;
import com.familyhub.demo.dto.MealSlotResponse;
import com.familyhub.demo.dto.MoveMealSlotRequest;
import com.familyhub.demo.dto.RemoveMealSlotRequest;
import com.familyhub.demo.dto.SaveMealPlanRequest;
import com.familyhub.demo.dto.SaveMealPlanSlotRequest;
import com.familyhub.demo.dto.UpsertMealSlotRequest;
import com.familyhub.demo.exception.BadRequestException;
import com.familyhub.demo.exception.ConflictException;
import com.familyhub.demo.exception.ResourceNotFoundException;
import com.familyhub.demo.mapper.MealMapper;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.MealCollisionMode;
import com.familyhub.demo.model.MealEntrySourceType;
import com.familyhub.demo.model.MealSlot;
import com.familyhub.demo.model.MealSlotEntry;
import com.familyhub.demo.model.MealSlotRole;
import com.familyhub.demo.model.MealType;
import com.familyhub.demo.model.Recipe;
import com.familyhub.demo.model.RecipeConstraints;
import com.familyhub.demo.repository.MealSlotRepository;
import com.familyhub.demo.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MealService {
    private final MealSlotRepository mealSlotRepository;
    private final RecipeRepository recipeRepository;

    public MealBoardResponse getBoard(LocalDate weekStartDate, Family family) {
        validateWeekStartDate(weekStartDate);
        List<MealSlot> slots = mealSlotRepository.findByFamilyAndWeekStartDateOrderByDayIndexAscMealTypeAsc(
                family,
                weekStartDate
        );
        Map<SlotKey, MealSlot> slotsByKey = slots.stream()
                .collect(Collectors.toMap(
                        slot -> new SlotKey(slot.getDayIndex(), slot.getMealType()),
                        Function.identity(),
                        (existing, duplicate) -> existing));

        List<MealDayResponse> days = new ArrayList<>();
        for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
            int currentDayIndex = dayIndex;
            List<MealSlotResponse> slotResponses = List.of(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER)
                    .stream()
                    .map(mealType -> {
                        MealSlot slot = slotsByKey.get(new SlotKey(currentDayIndex, mealType));
                        return slot == null
                                ? MealMapper.emptySlot(weekStartDate, currentDayIndex, mealType)
                                : MealMapper.toSlotDto(slot);
                    })
                    .toList();
            days.add(new MealDayResponse(weekStartDate.plusDays(dayIndex), dayIndex, slotResponses));
        }

        return new MealBoardResponse(weekStartDate, days);
    }

    @Transactional
    public MealSlotResponse upsertSlot(UpsertMealSlotRequest request, Family family) {
        validateWeekStartDate(request.weekStartDate());
        MealSlot slot = mealSlotRepository.findByFamilyAndWeekStartDateAndDayIndexAndMealType(
                family,
                request.weekStartDate(),
                request.dayIndex(),
                request.mealType()
        ).orElseGet(() -> newSlot(family, request.weekStartDate(), request.dayIndex(), request.mealType()));

        List<MealSlotEntry> requestedEntries = snapshotEntries(slot, request.primary(), normalizedExtras(request.extras()));
        applyRequestedBlock(slot, requestedEntries, RecipeFieldValidator.optionalText(request.note()), request.collisionMode());

        return MealMapper.toSlotDto(mealSlotRepository.saveAndFlush(slot));
    }

    @Transactional
    public MealBoardResponse savePlan(SaveMealPlanRequest request, Family family) {
        validateWeekStartDate(request.weekStartDate());
        validateUniqueTargets(request.slots());

        List<MealSlot> existingSlots = mealSlotRepository.findByFamilyAndWeekStartDateOrderByDayIndexAscMealTypeAsc(
                family,
                request.weekStartDate()
        );
        Map<SlotKey, MealSlot> existingSlotsByKey = existingSlots.stream()
                .collect(Collectors.toMap(
                        slot -> new SlotKey(slot.getDayIndex(), slot.getMealType()),
                        Function.identity(),
                        (existing, duplicate) -> existing));

        for (SaveMealPlanSlotRequest requestedSlot : request.slots()) {
            MealSlot existingSlot = existingSlotsByKey.get(new SlotKey(requestedSlot.dayIndex(), requestedSlot.mealType()));
            if (existingSlot != null && !existingSlot.getEntries().isEmpty()) {
                throw new ConflictException("Some meal slots are no longer empty.");
            }
        }

        List<MealSlot> targetSlots = new ArrayList<>();
        for (SaveMealPlanSlotRequest requestedSlot : request.slots()) {
            SlotKey key = new SlotKey(requestedSlot.dayIndex(), requestedSlot.mealType());
            MealSlot slot = existingSlotsByKey.get(key);
            if (slot == null) {
                slot = newSlot(family, request.weekStartDate(), requestedSlot.dayIndex(), requestedSlot.mealType());
            }
            List<MealSlotEntry> requestedEntries = snapshotEntries(
                    slot,
                    requestedSlot.primary(),
                    normalizedExtras(requestedSlot.extras())
            );
            replaceEntries(slot, requestedEntries);
            slot.setNote(RecipeFieldValidator.optionalText(requestedSlot.note()));
            targetSlots.add(slot);
        }

        mealSlotRepository.saveAll(targetSlots);
        mealSlotRepository.flush();
        return getBoard(request.weekStartDate(), family);
    }

    @Transactional
    public MealBoardResponse moveSlot(MoveMealSlotRequest request, Family family) {
        validateWeekStartDate(request.sourceWeekStartDate());
        validateWeekStartDate(request.destinationWeekStartDate());
        MealSlot source = getSourceSlot(
                family,
                request.sourceWeekStartDate(),
                request.sourceDayIndex(),
                request.sourceMealType()
        );
        if (sameSlot(
                request.sourceWeekStartDate(),
                request.sourceDayIndex(),
                request.sourceMealType(),
                request.destinationWeekStartDate(),
                request.destinationDayIndex(),
                request.destinationMealType()
        )) {
            return getBoard(request.destinationWeekStartDate(), family);
        }

        MealSlot destination = getOrCreateSlot(
                family,
                request.destinationWeekStartDate(),
                request.destinationDayIndex(),
                request.destinationMealType()
        );
        List<MealSlotEntry> movingEntries = copyEntries(source, destination);

        applyCopiedBlock(destination, movingEntries, source.getNote(), request.collisionMode());

        mealSlotRepository.saveAndFlush(destination);
        mealSlotRepository.delete(source);
        return getBoard(request.destinationWeekStartDate(), family);
    }

    @Transactional
    public MealBoardResponse duplicateSlot(DuplicateMealSlotRequest request, Family family) {
        validateWeekStartDate(request.sourceWeekStartDate());
        validateWeekStartDate(request.destinationWeekStartDate());
        MealSlot source = getSourceSlot(
                family,
                request.sourceWeekStartDate(),
                request.sourceDayIndex(),
                request.sourceMealType()
        );
        if (sameSlot(
                request.sourceWeekStartDate(),
                request.sourceDayIndex(),
                request.sourceMealType(),
                request.destinationWeekStartDate(),
                request.destinationDayIndex(),
                request.destinationMealType()
        )) {
            return getBoard(request.destinationWeekStartDate(), family);
        }
        MealSlot destination = getOrCreateSlot(
                family,
                request.destinationWeekStartDate(),
                request.destinationDayIndex(),
                request.destinationMealType()
        );

        applyCopiedBlock(destination, copyEntries(source, destination), source.getNote(), request.collisionMode());

        mealSlotRepository.saveAndFlush(destination);
        return getBoard(request.destinationWeekStartDate(), family);
    }

    @Transactional
    public MealBoardResponse removeSlot(RemoveMealSlotRequest request, Family family) {
        validateWeekStartDate(request.weekStartDate());
        MealSlot slot = getSourceSlot(
                family,
                request.weekStartDate(),
                request.dayIndex(),
                request.mealType()
        );
        mealSlotRepository.delete(slot);
        return getBoard(request.weekStartDate(), family);
    }

    private MealSlot getSourceSlot(Family family, LocalDate weekStartDate, int dayIndex, MealType mealType) {
        MealSlot slot = mealSlotRepository.findByFamilyAndWeekStartDateAndDayIndexAndMealType(
                family,
                weekStartDate,
                dayIndex,
                mealType
        ).orElseThrow(() -> new ResourceNotFoundException("Meal slot not found."));
        if (!hasPrimary(slot)) {
            throw new ResourceNotFoundException("Meal slot not found.");
        }
        return slot;
    }

    private MealSlot getOrCreateSlot(Family family, LocalDate weekStartDate, int dayIndex, MealType mealType) {
        return mealSlotRepository.findByFamilyAndWeekStartDateAndDayIndexAndMealType(
                family,
                weekStartDate,
                dayIndex,
                mealType
        ).orElseGet(() -> newSlot(family, weekStartDate, dayIndex, mealType));
    }

    private MealSlot newSlot(Family family, LocalDate weekStartDate, int dayIndex, MealType mealType) {
        MealSlot slot = new MealSlot();
        slot.setFamily(family);
        slot.setWeekStartDate(weekStartDate);
        slot.setDayIndex(dayIndex);
        slot.setMealType(mealType);
        return slot;
    }

    private void applyRequestedBlock(
            MealSlot slot,
            List<MealSlotEntry> requestedEntries,
            String requestedNote,
            MealCollisionMode collisionMode
    ) {
        applyBlock(slot, requestedEntries, requestedNote, collisionMode, true);
    }

    private void applyCopiedBlock(
            MealSlot slot,
            List<MealSlotEntry> copiedEntries,
            String copiedNote,
            MealCollisionMode collisionMode
    ) {
        applyBlock(slot, copiedEntries, copiedNote, collisionMode, false);
    }

    private void applyBlock(
            MealSlot slot,
            List<MealSlotEntry> incomingEntries,
            String incomingNote,
            MealCollisionMode collisionMode,
            boolean collisionModeMayBeAbsent
    ) {
        if (hasPrimary(slot)) {
            if (collisionMode == null) {
                throw new BadRequestException("Collision mode is required when target slot already has a primary meal.");
            }
            if (collisionMode == MealCollisionMode.ADD_AS_EXTRA) {
                appendAsExtras(slot, incomingEntries);
                return;
            }
        } else if (!collisionModeMayBeAbsent && collisionMode == null) {
            throw new BadRequestException("Collision mode is required.");
        }

        replaceEntries(slot, incomingEntries);
        slot.setNote(incomingNote);
    }

    private void replaceEntries(MealSlot slot, List<MealSlotEntry> entries) {
        boolean needsOrphanFlush = slot.getId() != null && !slot.getEntries().isEmpty();
        slot.getEntries().clear();
        if (needsOrphanFlush) {
            mealSlotRepository.saveAndFlush(slot);
        }
        for (int i = 0; i < entries.size(); i++) {
            MealSlotEntry entry = entries.get(i);
            entry.setSlot(slot);
            entry.setRole(i == 0 ? MealSlotRole.PRIMARY : MealSlotRole.EXTRA);
            entry.setSortOrder(i);
            slot.getEntries().add(entry);
        }
    }

    private void appendAsExtras(MealSlot slot, List<MealSlotEntry> entries) {
        int nextSortOrder = slot.getEntries().stream()
                .mapToInt(MealSlotEntry::getSortOrder)
                .max()
                .orElse(0) + 1;
        for (MealSlotEntry entry : entries) {
            entry.setSlot(slot);
            entry.setRole(MealSlotRole.EXTRA);
            entry.setSortOrder(nextSortOrder++);
            slot.getEntries().add(entry);
        }
    }

    private List<MealSlotEntry> snapshotEntries(MealSlot slot, MealEntryRequest primary, List<MealEntryRequest> extras) {
        List<MealSlotEntry> entries = new ArrayList<>();
        entries.add(snapshotEntry(primary, slot));
        for (MealEntryRequest extra : extras) {
            entries.add(snapshotEntry(extra, slot));
        }
        return entries;
    }

    private List<MealEntryRequest> normalizedExtras(List<MealEntryRequest> extras) {
        if (extras == null) {
            return List.of();
        }
        return extras.stream().filter(Objects::nonNull).toList();
    }

    private void validateUniqueTargets(List<SaveMealPlanSlotRequest> slots) {
        if (slots == null || slots.isEmpty()) {
            throw new BadRequestException("At least one meal plan slot is required.");
        }

        Set<SlotKey> targets = new HashSet<>();
        for (SaveMealPlanSlotRequest slot : slots) {
            if (slot == null) {
                throw new BadRequestException("Meal plan slot is required.");
            }
            if (!targets.add(new SlotKey(slot.dayIndex(), slot.mealType()))) {
                throw new BadRequestException("Meal plan contains duplicate target slots.");
            }
        }
    }

    private List<MealSlotEntry> copyEntries(MealSlot source, MealSlot destination) {
        return source.getEntries().stream()
                .sorted((left, right) -> Integer.compare(left.getSortOrder(), right.getSortOrder()))
                .map(entry -> copyEntry(entry, destination))
                .toList();
    }

    private MealSlotEntry copyEntry(MealSlotEntry source, MealSlot destination) {
        MealSlotEntry copy = new MealSlotEntry();
        copy.setSlot(destination);
        copy.setRole(source.getRole());
        copy.setSortOrder(source.getSortOrder());
        copy.setSourceType(source.getSourceType());
        copy.setRecipe(source.getRecipe());
        copy.setTitleSnapshot(source.getTitleSnapshot());
        copy.setImageUrlSnapshot(source.getImageUrlSnapshot());
        copy.setNoteSnapshot(source.getNoteSnapshot());
        return copy;
    }

    private MealSlotEntry snapshotEntry(MealEntryRequest request, MealSlot slot) {
        MealSlotEntry entry = new MealSlotEntry();
        entry.setSlot(slot);
        entry.setSourceType(request.sourceType());

        if (request.sourceType() == MealEntrySourceType.RECIPE) {
            if (request.recipeId() == null) {
                throw new BadRequestException("Recipe id is required for recipe-backed meals.");
            }
            Recipe recipe = recipeRepository.findByIdAndFamily(request.recipeId(), slot.getFamily())
                    .orElseThrow(() -> new ResourceNotFoundException("Recipe", request.recipeId()));
            entry.setRecipe(recipe);
            entry.setTitleSnapshot(recipe.getTitle());
            entry.setImageUrlSnapshot(recipe.getImageUrl());
            entry.setNoteSnapshot(recipeNoteSnapshot(request, recipe));
        } else {
            entry.setTitleSnapshot(quickMealTitle(request.title()));
            entry.setImageUrlSnapshot(RecipeFieldValidator.optionalHttpUrl(request.imageUrl(), "Meal image URL"));
            entry.setNoteSnapshot(RecipeFieldValidator.optionalText(request.note()));
        }

        return entry;
    }

    private String recipeNoteSnapshot(MealEntryRequest request, Recipe recipe) {
        String mealSpecificNote = RecipeFieldValidator.optionalText(request.note());
        return mealSpecificNote == null ? recipe.getNote() : mealSpecificNote;
    }

    private String quickMealTitle(String value) {
        String title = RecipeFieldValidator.optionalText(value);
        if (title == null) {
            throw new BadRequestException("Meal title is required.");
        }
        if (title.length() > RecipeConstraints.TITLE_MAX_LENGTH) {
            throw new BadRequestException("Meal title must be 160 characters or less.");
        }
        return title;
    }

    private void validateWeekStartDate(LocalDate weekStartDate) {
        if (weekStartDate == null) {
            throw new BadRequestException("Week start date is required.");
        }
        if (weekStartDate.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new BadRequestException("Week start date must be a Sunday.");
        }
    }

    private boolean hasPrimary(MealSlot slot) {
        return slot.getEntries().stream().anyMatch(entry -> entry.getRole() == MealSlotRole.PRIMARY);
    }

    private boolean sameSlot(
            LocalDate sourceWeekStartDate,
            int sourceDayIndex,
            MealType sourceMealType,
            LocalDate destinationWeekStartDate,
            int destinationDayIndex,
            MealType destinationMealType
    ) {
        return sourceWeekStartDate.equals(destinationWeekStartDate)
                && sourceDayIndex == destinationDayIndex
                && sourceMealType == destinationMealType;
    }

    private record SlotKey(int dayIndex, MealType mealType) {
    }
}
