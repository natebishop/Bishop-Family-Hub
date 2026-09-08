package com.familyhub.demo.service;

import com.familyhub.demo.dto.*;
import com.familyhub.demo.exception.BadRequestException;
import com.familyhub.demo.exception.ConflictException;
import com.familyhub.demo.exception.ResourceNotFoundException;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.ListCategory;
import com.familyhub.demo.model.ListCategoryCatalogScope;
import com.familyhub.demo.model.ListKind;
import com.familyhub.demo.repository.ListCategoryCatalogScopeRepository;
import com.familyhub.demo.repository.ListCategoryRepository;
import com.familyhub.demo.repository.SharedListItemRepository;
import com.familyhub.demo.repository.SharedListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListCategoryService {

    private final ListCategoryCatalogScopeRepository scopeRepository;
    private final ListCategoryRepository categoryRepository;
    private final SharedListItemRepository itemRepository;
    private final SharedListRepository listRepository;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public ListCategoryCatalogResponse getCatalog(ListKind kind, Family family) {
        List<ListCategory> categories =
                categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, kind);
        return buildCatalogResponse(family, kind, categories);
    }

    @Transactional
    public ListCategoryManagementEntry create(CreateListCategoryRequest request, Family family) {
        lock(family, request.kind());

        String name = request.name().trim();
        if (categoryRepository.existsByNormalizedName(family, request.kind(), name, null)) {
            throw new ConflictException("A category with this name already exists.");
        }

        int sortOrder = (int) categoryRepository.countByFamilyAndKind(family, request.kind());

        ListCategory category = new ListCategory();
        category.setFamily(family);
        category.setKind(request.kind());
        category.setName(name);
        category.setSortOrder(sortOrder);

        ListCategory saved = categoryRepository.saveAndFlush(category);
        return new ListCategoryManagementEntry(saved.getId(), request.kind(), name, sortOrder, 0L);
    }

    @Transactional
    public ListCategoryManagementEntry rename(UUID id, RenameListCategoryRequest request, Family family) {
        ListCategory category = lockAndRefetch(family, id);

        String name = request.name().trim();
        if (categoryRepository.existsByNormalizedName(family, category.getKind(), name, id)) {
            throw new ConflictException("A category with this name already exists.");
        }

        category.setName(name);
        categoryRepository.saveAndFlush(category);

        long itemCount = itemRepository.countByCategory(family.getId(), id);
        return new ListCategoryManagementEntry(
                id, category.getKind(), name, category.getSortOrder(), itemCount);
    }

    @Transactional
    public CategoryDeleteResult delete(UUID id, Family family) {
        ListCategory category = lockAndRefetch(family, id);
        ListKind kind = category.getKind();

        // 1. Clear item assignments BEFORE deleting the category (FK constraint)
        long uncategorizedItemCount = itemRepository.clearCategory(family.getId(), kind, id);

        // 2. Delete and flush (clearCategory used clearAutomatically=true so category is detached;
        //    delete() will merge+remove or we can use deleteById — both are fine)
        categoryRepository.delete(category);
        categoryRepository.flush();

        // 3. Compact remaining sort orders densely (0, 1, 2, …)
        List<ListCategory> remaining =
                categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, kind);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setSortOrder(i);
        }
        categoryRepository.saveAll(remaining);
        categoryRepository.flush();

        // 4. Flatten grouped lists only when no categories remain
        long flattenedListCount = remaining.isEmpty()
                ? listRepository.flattenGroupedLists(family, kind)
                : 0L;

        return new CategoryDeleteResult(uncategorizedItemCount, flattenedListCount);
    }

    @Transactional
    public ListCategoryCatalogResponse reorder(ReorderListCategoriesRequest request, Family family) {
        lock(family, request.kind());

        // Load current ordered categories
        List<ListCategory> currentCategories =
                categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, request.kind());
        List<UUID> currentIds = currentCategories.stream()
                .map(ListCategory::getId)
                .toList();

        // 1. Stale baseline check (409) — MUST happen before membership validation (400)
        if (!currentIds.equals(request.expectedCategoryIds())) {
            throw new ConflictException("Categories changed since you last loaded them.");
        }

        // 2. Membership validation (400): same size, no duplicates, exact same set
        List<UUID> targetIds = request.categoryIds();
        if (targetIds.size() != currentIds.size()
                || new HashSet<>(targetIds).size() != targetIds.size()
                || !new HashSet<>(targetIds).equals(new HashSet<>(currentIds))) {
            throw new BadRequestException("Invalid category order.");
        }

        // 3. Rewrite sort orders
        Map<UUID, ListCategory> categoryById = currentCategories.stream()
                .collect(Collectors.toMap(ListCategory::getId, Function.identity()));
        for (int i = 0; i < targetIds.size(); i++) {
            categoryById.get(targetIds.get(i)).setSortOrder(i);
        }
        categoryRepository.saveAll(currentCategories);
        categoryRepository.flush();

        // 4. Return fresh catalog in the new order
        List<ListCategory> reordered =
                categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, request.kind());
        return buildCatalogResponse(family, request.kind(), reordered);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private ListCategoryCatalogScope lock(Family family, ListKind kind) {
        return scopeRepository.lockByFamilyAndKind(family, kind)
                .orElseThrow(() -> new IllegalStateException("List category catalog scope is missing"));
    }

    private ListCategory lockAndRefetch(Family family, UUID categoryId) {
        scopeRepository.lockByFamilyAndCategoryId(family, categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("List Category", categoryId));
        return categoryRepository.findByFamilyAndId(family, categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("List Category", categoryId));
    }

    private ListCategoryCatalogResponse buildCatalogResponse(
            Family family, ListKind kind, List<ListCategory> categories) {

        Map<UUID, Long> usageByCategory = itemRepository.countUsage(family.getId(), kind)
                .stream()
                .collect(Collectors.toMap(
                        SharedListItemRepository.CategoryUsageCount::getCategoryId,
                        SharedListItemRepository.CategoryUsageCount::getItemCount));

        long groupedListCount = listRepository.countGroupedLists(family, kind);

        List<ListCategoryManagementEntry> entries = categories.stream()
                .map(cat -> new ListCategoryManagementEntry(
                        cat.getId(),
                        cat.getKind(),
                        cat.getName(),
                        cat.getSortOrder(),
                        usageByCategory.getOrDefault(cat.getId(), 0L)))
                .toList();

        return new ListCategoryCatalogResponse(kind, groupedListCount, entries);
    }
}
