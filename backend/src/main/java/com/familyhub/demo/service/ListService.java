package com.familyhub.demo.service;

import com.familyhub.demo.dto.*;
import com.familyhub.demo.exception.BadRequestException;
import com.familyhub.demo.exception.ConflictException;
import com.familyhub.demo.exception.ResourceNotFoundException;
import com.familyhub.demo.mapper.ListMapper;
import com.familyhub.demo.model.*;
import com.familyhub.demo.repository.ListCategoryCatalogScopeRepository;
import com.familyhub.demo.repository.ListCategoryRepository;
import com.familyhub.demo.repository.ListPreferencesRepository;
import com.familyhub.demo.repository.SharedListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListService {
    private final SharedListRepository sharedListRepository;
    private final ListCategoryRepository listCategoryRepository;
    private final ListPreferencesRepository listPreferencesRepository;
    private final ListCategoryCatalogScopeRepository scopeRepository;

    public List<ListSummaryResponse> getLists(Family family) {
        return sharedListRepository.findByFamilyWithItems(family)
                .stream()
                .map(ListMapper::toSummaryDto)
                .toList();
    }

    @Transactional
    public ListDetailResponse createList(CreateListRequest request, Family family) {
        ListKind kind = request.kind();

        // Acquire scope lock BEFORE constructing/persisting the aggregate
        lockScope(family, kind);

        // Choose display mode: GENERAL → FLAT; GROCERY/TODO → GROUPED iff catalog non-empty, else FLAT
        ListCategoryDisplayMode displayMode;
        if (kind == ListKind.GENERAL) {
            displayMode = ListCategoryDisplayMode.FLAT;
        } else {
            displayMode = listCategoryRepository.countByFamilyAndKind(family, kind) > 0
                    ? ListCategoryDisplayMode.GROUPED
                    : ListCategoryDisplayMode.FLAT;
        }

        SharedList list = new SharedList();
        list.setFamily(family);
        list.setName(request.name().trim());
        list.setKind(kind);
        list.setCategoryDisplayMode(displayMode);
        list.setShowCompletedOverride(null);

        return mapDetail(sharedListRepository.saveAndFlush(list));
    }

    public ListDetailResponse getList(UUID id, Family family) {
        return mapDetail(getListOrThrow(id, family));
    }

    @Transactional
    public ListDetailResponse updateList(UUID id, UpdateListRequest request, Family family) {
        if (request.categoryDisplayMode() == ListCategoryDisplayMode.GROUPED) {
            // Read immutable kind BEFORE acquiring any entity lock
            ListKind kind = sharedListRepository.findKindByFamilyAndId(family, id)
                    .orElseThrow(() -> new ResourceNotFoundException("List", id));

            // Acquire scope lock
            lockScope(family, kind);

            // Verify catalog is non-empty (post-lock authoritative check)
            if (listCategoryRepository.countByFamilyAndKind(family, kind) == 0) {
                throw new ConflictException("Create a category first.");
            }

            // Refetch aggregate AFTER lock (authoritative)
            SharedList list = getListOrThrow(id, family);
            list.setCategoryDisplayMode(request.categoryDisplayMode());
            list.setShowCompletedOverride(request.showCompletedOverride());
            return mapDetail(sharedListRepository.saveAndFlush(list));
        }

        // FLAT path: no scope lock needed
        SharedList list = getListOrThrow(id, family);
        list.setCategoryDisplayMode(request.categoryDisplayMode());
        list.setShowCompletedOverride(request.showCompletedOverride());
        return mapDetail(sharedListRepository.saveAndFlush(list));
    }

    @Transactional
    public ListItemResponse createItem(UUID listId, CreateListItemRequest request, Family family) {
        // When a category is requested, route-and-lock the scope BEFORE loading the aggregate:
        // read the immutable kind projection first, then lock, then load. This keeps Hibernate
        // autoflush from grabbing item/list locks ahead of the scope lock.
        if (request.categoryId() != null) {
            ListKind kind = sharedListRepository.findKindByFamilyAndId(family, listId)
                    .orElseThrow(() -> new ResourceNotFoundException("List", listId));
            lockScope(family, kind);
        }

        SharedList list = getListOrThrow(listId, family);

        SharedListItem item = new SharedListItem();
        item.setList(list);
        item.setText(request.text().trim());
        item.setCompleted(false);
        item.setCompletedAt(null);
        item.setCategory(resolveCategory(list, request.categoryId()));
        list.getItems().add(item);

        SharedList saved = sharedListRepository.saveAndFlush(list);
        return ListMapper.toItemDto(saved.getItems().getLast());
    }

    @Transactional
    public List<ListItemResponse> createItemsBulk(UUID listId, BulkCreateListItemsRequest request, Family family) {
        // Defense-in-depth: the DTO's @Size already bounds this at the request boundary, but guard the
        // service too so any non-validated caller (e.g. another service) cannot exceed the batch cap.
        if (request.items().size() > BulkCreateListItemsRequest.MAX_BULK_ITEMS) {
            throw new BadRequestException(
                    "A bulk append may contain at most " + BulkCreateListItemsRequest.MAX_BULK_ITEMS + " items");
        }

        // Same scope-lock ordering as createItem: if any item assigns a category, read the immutable
        // kind projection first, then lock the (family, kind) scope, then load the aggregate. This keeps
        // Hibernate autoflush from grabbing item/list locks ahead of the scope lock.
        boolean anyCategory = request.items().stream().anyMatch(item -> item.categoryId() != null);
        if (anyCategory) {
            ListKind kind = sharedListRepository.findKindByFamilyAndId(family, listId)
                    .orElseThrow(() -> new ResourceNotFoundException("List", listId));
            lockScope(family, kind);
        }

        SharedList list = getListOrThrow(listId, family);

        // Prevalidation contract: resolve and validate EVERY category BEFORE mutating the aggregate,
        // so a single bad item never appends a partial row. resolveCategory runs queries, so appending
        // items first could let Hibernate autoflush persist a partial batch before a later item throws.
        // `.toList()` forces resolution eagerly in request order; the first invalid item throws here,
        // before any item is added to the collection and before saveAndFlush runs.
        List<ListCategory> resolvedCategories = request.items().stream()
                .map(item -> resolveCategory(list, item.categoryId()))
                .toList();

        int existingCount = list.getItems().size();
        for (int i = 0; i < request.items().size(); i++) {
            SharedListItem item = new SharedListItem();
            item.setList(list);
            item.setText(request.items().get(i).text().trim());
            item.setCompleted(false);
            item.setCompletedAt(null);
            item.setCategory(resolvedCategories.get(i));
            list.getItems().add(item);
        }

        // One transaction; rolls back entirely on any failure. Read the created rows back off the
        // saved aggregate (like createItem) so generated ids/timestamps are populated, preserving
        // request order by taking the freshly appended tail.
        SharedList saved = sharedListRepository.saveAndFlush(list);
        return saved.getItems().subList(existingCount, saved.getItems().size()).stream()
                .map(ListMapper::toItemDto)
                .toList();
    }

    @Transactional
    public ListItemResponse updateItem(UUID listId, UUID itemId, UpdateListItemRequest request, Family family) {
        // When a category is requested, route-and-lock the scope BEFORE loading the aggregate
        // (immutable-kind projection first, then lock, then load), matching createItem's ordering.
        if (request.categoryId() != null) {
            ListKind kind = sharedListRepository.findKindByFamilyAndId(family, listId)
                    .orElseThrow(() -> new ResourceNotFoundException("List", listId));
            lockScope(family, kind);
        }

        SharedList list = getListOrThrow(listId, family);
        SharedListItem item = list.getItems().stream()
                .filter(candidate -> candidate.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("List Item", itemId));

        // Resolve and validate the requested category BEFORE mutating the managed item, so a stale,
        // missing, foreign, or wrong-kind category is rejected before any field is changed.
        // PATCH-replace semantics: a null categoryId clears the assignment (selects "Uncategorized").
        ListCategory category = resolveCategory(list, request.categoryId());
        applyItemMutation(item, request);
        item.setCategory(category);

        sharedListRepository.saveAndFlush(list);
        return ListMapper.toItemDto(item);
    }

    @Transactional
    public void deleteItem(UUID listId, UUID itemId, Family family) {
        SharedList list = getListOrThrow(listId, family);
        boolean removed = list.getItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new ResourceNotFoundException("List Item", itemId);
        }

        sharedListRepository.saveAndFlush(list);
    }

    @Transactional
    public ClearCompletedResponse clearCompleted(UUID listId, Family family) {
        SharedList list = getListOrThrow(listId, family);
        int before = list.getItems().size();
        list.getItems().removeIf(SharedListItem::isCompleted);
        sharedListRepository.saveAndFlush(list);
        return new ClearCompletedResponse(before - list.getItems().size());
    }

    public ListPreferencesResponse getPreferences(Family family) {
        ListPreferences preferences = listPreferencesRepository.findByFamily(family)
                .orElseThrow(() -> new ResourceNotFoundException("List Preferences", family.getId()));
        return new ListPreferencesResponse(preferences.isShowCompletedByDefault());
    }

    @Transactional
    public ListPreferencesResponse updatePreferences(UpdateListPreferencesRequest request, Family family) {
        ListPreferences preferences = listPreferencesRepository.findByFamily(family)
                .orElseThrow(() -> new ResourceNotFoundException("List Preferences", family.getId()));
        preferences.setShowCompletedByDefault(request.showCompletedByDefault());
        return new ListPreferencesResponse(listPreferencesRepository.saveAndFlush(preferences).isShowCompletedByDefault());
    }

    private ListCategoryCatalogScope lockScope(Family family, ListKind kind) {
        return scopeRepository.lockByFamilyAndKind(family, kind)
                .orElseThrow(() -> new IllegalStateException("List category catalog scope is missing"));
    }

    private void applyItemMutation(SharedListItem item, UpdateListItemRequest request) {
        item.setText(request.text().trim());
        boolean completed = Boolean.TRUE.equals(request.completed());
        item.setCompleted(completed);
        if (completed) {
            if (item.getCompletedAt() == null) {
                item.setCompletedAt(LocalDateTime.now());
            }
        } else {
            item.setCompletedAt(null);
        }
    }

    private ListCategory resolveCategory(SharedList list, UUID categoryId) {
        if (categoryId == null) {
            return null;
        }

        ListCategory category = listCategoryRepository.findByFamilyAndId(list.getFamily(), categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("List Category", categoryId));
        if (category.getKind() != list.getKind()) {
            throw new BadRequestException("Category kind does not match list kind.");
        }
        return category;
    }

    private SharedList getListOrThrow(UUID id, Family family) {
        return sharedListRepository.findDetailByFamilyAndId(family, id)
                .orElseThrow(() -> new ResourceNotFoundException("List", id));
    }

    private ListDetailResponse mapDetail(SharedList list) {
        List<ListCategoryOption> categories =
                listCategoryRepository.findByFamilyAndKindOrderBySortOrderAsc(list.getFamily(), list.getKind())
                        .stream()
                        .map(ListMapper::toCategoryDto)
                        .toList();

        return ListMapper.toDetailDto(list, categories);
    }
}
