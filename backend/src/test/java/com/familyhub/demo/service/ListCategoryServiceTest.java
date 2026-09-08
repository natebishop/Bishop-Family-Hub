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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.familyhub.demo.TestDataFactory.FAMILY_ID;
import static com.familyhub.demo.TestDataFactory.createFamily;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCategoryServiceTest {

    @Mock
    private ListCategoryCatalogScopeRepository scopeRepository;

    @Mock
    private ListCategoryRepository categoryRepository;

    @Mock
    private SharedListItemRepository itemRepository;

    @Mock
    private SharedListRepository listRepository;

    @InjectMocks
    private ListCategoryService listCategoryService;

    private Family family;
    private ListCategoryCatalogScope scope;
    private ListCategory produce;
    private ListCategory dairy;

    // Stable UUIDs for two-category tests
    private static final UUID CAT_A_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID CAT_B_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    @BeforeEach
    void setUp() {
        family = createFamily();

        scope = new ListCategoryCatalogScope();
        scope.setId(UUID.randomUUID());
        scope.setFamily(family);
        scope.setKind(ListKind.GROCERY);

        produce = new ListCategory();
        produce.setId(CAT_A_ID);
        produce.setFamily(family);
        produce.setKind(ListKind.GROCERY);
        produce.setName("Produce");
        produce.setSortOrder(0);

        dairy = new ListCategory();
        dairy.setId(CAT_B_ID);
        dairy.setFamily(family);
        dairy.setKind(ListKind.GROCERY);
        dairy.setName("Dairy");
        dairy.setSortOrder(1);
    }

    // -------------------------------------------------------------------------
    // getCatalog
    // -------------------------------------------------------------------------

    @Test
    void getCatalog_returnsOrderedEntriesWithPerCategoryItemCountsAndGroupedListCount() {
        // Arrange
        when(categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of(produce, dairy));
        when(listRepository.countGroupedLists(family, ListKind.GROCERY)).thenReturn(2L);

        // Usage: produce has 3 items, dairy has 0 (not in result set)
        SharedListItemRepository.CategoryUsageCount usage = mockUsage(CAT_A_ID, 3L);
        when(itemRepository.countUsage(FAMILY_ID, ListKind.GROCERY)).thenReturn(List.of(usage));

        // Act
        ListCategoryCatalogResponse response = listCategoryService.getCatalog(ListKind.GROCERY, family);

        // Assert
        assertThat(response.kind()).isEqualTo(ListKind.GROCERY);
        assertThat(response.groupedListCount()).isEqualTo(2L);
        assertThat(response.categories()).hasSize(2);

        ListCategoryManagementEntry first = response.categories().get(0);
        assertThat(first.id()).isEqualTo(CAT_A_ID);
        assertThat(first.name()).isEqualTo("Produce");
        assertThat(first.sortOrder()).isEqualTo(0);
        assertThat(first.itemCount()).isEqualTo(3L);

        ListCategoryManagementEntry second = response.categories().get(1);
        assertThat(second.id()).isEqualTo(CAT_B_ID);
        assertThat(second.name()).isEqualTo("Dairy");
        assertThat(second.sortOrder()).isEqualTo(1);
        assertThat(second.itemCount()).isEqualTo(0L); // default for no usage
    }

    @Test
    void getCatalog_withNoCategories_returnsEmptyListAndGroupedCount() {
        when(categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of());
        when(listRepository.countGroupedLists(family, ListKind.GROCERY)).thenReturn(0L);
        when(itemRepository.countUsage(FAMILY_ID, ListKind.GROCERY)).thenReturn(List.of());

        ListCategoryCatalogResponse response = listCategoryService.getCatalog(ListKind.GROCERY, family);

        assertThat(response.categories()).isEmpty();
        assertThat(response.groupedListCount()).isEqualTo(0L);
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void create_locksScopeFirst_thenSaves() {
        // Arrange
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(scope));
        when(categoryRepository.existsByNormalizedName(family, ListKind.GROCERY, "Produce", null)).thenReturn(false);
        when(categoryRepository.countByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(2L);
        ListCategory saved = savedCategory(UUID.randomUUID(), "Produce", ListKind.GROCERY, 2);
        when(categoryRepository.saveAndFlush(any(ListCategory.class))).thenReturn(saved);

        CreateListCategoryRequest request = new CreateListCategoryRequest(ListKind.GROCERY, "Produce");

        // Act
        ListCategoryManagementEntry entry = listCategoryService.create(request, family);

        // Assert ordering: scope lock before save
        InOrder order = inOrder(scopeRepository, categoryRepository);
        order.verify(scopeRepository).lockByFamilyAndKind(family, ListKind.GROCERY);
        order.verify(categoryRepository).saveAndFlush(any(ListCategory.class));

        assertThat(entry.name()).isEqualTo("Produce");
        assertThat(entry.sortOrder()).isEqualTo(2);
        assertThat(entry.itemCount()).isEqualTo(0L);
    }

    @Test
    void create_trimsName() {
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(scope));
        when(categoryRepository.existsByNormalizedName(family, ListKind.GROCERY, "Produce", null)).thenReturn(false);
        when(categoryRepository.countByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(0L);
        ListCategory saved = savedCategory(UUID.randomUUID(), "Produce", ListKind.GROCERY, 0);
        when(categoryRepository.saveAndFlush(any(ListCategory.class))).thenReturn(saved);

        listCategoryService.create(new CreateListCategoryRequest(ListKind.GROCERY, "  Produce  "), family);

        // Verify the name passed for dup-check was trimmed
        verify(categoryRepository).existsByNormalizedName(family, ListKind.GROCERY, "Produce", null);
    }

    @Test
    void create_duplicateName_throwsConflictException() {
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(scope));
        when(categoryRepository.existsByNormalizedName(family, ListKind.GROCERY, "Produce", null)).thenReturn(true);

        assertThatThrownBy(() ->
                listCategoryService.create(new CreateListCategoryRequest(ListKind.GROCERY, "Produce"), family))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(categoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_appendsAtCurrentCount_asSortOrder() {
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(scope));
        when(categoryRepository.existsByNormalizedName(family, ListKind.GROCERY, "Meat", null)).thenReturn(false);
        when(categoryRepository.countByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(5L);
        ListCategory saved = savedCategory(UUID.randomUUID(), "Meat", ListKind.GROCERY, 5);
        when(categoryRepository.saveAndFlush(any(ListCategory.class))).thenReturn(saved);

        ListCategoryManagementEntry entry =
                listCategoryService.create(new CreateListCategoryRequest(ListKind.GROCERY, "Meat"), family);

        assertThat(entry.sortOrder()).isEqualTo(5);
    }

    @Test
    void create_missingScopeRow_throwsIllegalState() {
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                listCategoryService.create(new CreateListCategoryRequest(ListKind.GROCERY, "Produce"), family))
                .isInstanceOf(IllegalStateException.class);
    }

    // -------------------------------------------------------------------------
    // rename
    // -------------------------------------------------------------------------

    @Test
    void rename_locksAndRefetches_trimsName_preservesSortOrderAndKind() {
        when(scopeRepository.lockByFamilyAndCategoryId(family, CAT_A_ID)).thenReturn(Optional.of(scope));
        when(categoryRepository.findByFamilyAndId(family, CAT_A_ID)).thenReturn(Optional.of(produce));
        when(categoryRepository.existsByNormalizedName(family, ListKind.GROCERY, "Organic Produce", CAT_A_ID))
                .thenReturn(false);
        when(categoryRepository.saveAndFlush(produce)).thenReturn(produce);
        when(itemRepository.countByCategory(FAMILY_ID, CAT_A_ID)).thenReturn(7L);

        ListCategoryManagementEntry entry =
                listCategoryService.rename(CAT_A_ID, new RenameListCategoryRequest("  Organic Produce  "), family);

        assertThat(entry.id()).isEqualTo(CAT_A_ID);
        assertThat(entry.name()).isEqualTo("Organic Produce");
        assertThat(entry.sortOrder()).isEqualTo(0);
        assertThat(entry.kind()).isEqualTo(ListKind.GROCERY);
        assertThat(entry.itemCount()).isEqualTo(7L);
    }

    @Test
    void rename_duplicateName_throwsConflictException() {
        when(scopeRepository.lockByFamilyAndCategoryId(family, CAT_A_ID)).thenReturn(Optional.of(scope));
        when(categoryRepository.findByFamilyAndId(family, CAT_A_ID)).thenReturn(Optional.of(produce));
        when(categoryRepository.existsByNormalizedName(family, ListKind.GROCERY, "Dairy", CAT_A_ID))
                .thenReturn(true);

        assertThatThrownBy(() ->
                listCategoryService.rename(CAT_A_ID, new RenameListCategoryRequest("Dairy"), family))
                .isInstanceOf(ConflictException.class);

        verify(categoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void rename_categoryNotFound_viaLock_throwsResourceNotFoundException() {
        when(scopeRepository.lockByFamilyAndCategoryId(family, CAT_A_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                listCategoryService.rename(CAT_A_ID, new RenameListCategoryRequest("New Name"), family))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rename_categoryNotFound_viaRefetch_throwsResourceNotFoundException() {
        when(scopeRepository.lockByFamilyAndCategoryId(family, CAT_A_ID)).thenReturn(Optional.of(scope));
        when(categoryRepository.findByFamilyAndId(family, CAT_A_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                listCategoryService.rename(CAT_A_ID, new RenameListCategoryRequest("New Name"), family))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void delete_correctOrder_clearItems_deleteCategory_compactOrder_flattenIfEmpty() {
        when(scopeRepository.lockByFamilyAndCategoryId(family, CAT_A_ID)).thenReturn(Optional.of(scope));
        when(categoryRepository.findByFamilyAndId(family, CAT_A_ID)).thenReturn(Optional.of(produce));
        when(itemRepository.clearCategory(FAMILY_ID, ListKind.GROCERY, CAT_A_ID)).thenReturn(3);
        // After deletion, no categories remain
        when(categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of());
        when(listRepository.flattenGroupedLists(family, ListKind.GROCERY)).thenReturn(2);

        CategoryDeleteResult result = listCategoryService.delete(CAT_A_ID, family);

        // Verify ordering: scope lock → refetch → clearCategory → delete → load remaining → flatten
        InOrder order = inOrder(scopeRepository, itemRepository, categoryRepository, listRepository);
        order.verify(scopeRepository).lockByFamilyAndCategoryId(family, CAT_A_ID);
        order.verify(categoryRepository).findByFamilyAndId(family, CAT_A_ID);
        order.verify(itemRepository).clearCategory(FAMILY_ID, ListKind.GROCERY, CAT_A_ID);
        order.verify(categoryRepository).delete(produce);
        order.verify(categoryRepository).flush();
        order.verify(categoryRepository).findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY);
        order.verify(listRepository).flattenGroupedLists(family, ListKind.GROCERY);

        assertThat(result.uncategorizedItemCount()).isEqualTo(3L);
        assertThat(result.flattenedListCount()).isEqualTo(2L);
    }

    @Test
    void delete_withRemainingCategories_compactsDenseOrder_doesNotFlatten() {
        when(scopeRepository.lockByFamilyAndCategoryId(family, CAT_A_ID)).thenReturn(Optional.of(scope));
        when(categoryRepository.findByFamilyAndId(family, CAT_A_ID)).thenReturn(Optional.of(produce));
        when(itemRepository.clearCategory(FAMILY_ID, ListKind.GROCERY, CAT_A_ID)).thenReturn(0);

        // dairy remains after produce is deleted (sortOrder=1, compacted to 0)
        when(categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of(dairy));
        when(categoryRepository.saveAll(anyList())).thenReturn(List.of(dairy));

        CategoryDeleteResult result = listCategoryService.delete(CAT_A_ID, family);

        // flatten must NOT be called when categories remain
        verify(listRepository, never()).flattenGroupedLists(any(), any());

        // saveAll should have been called to rewrite dense order
        verify(categoryRepository).saveAll(anyList());

        assertThat(result.uncategorizedItemCount()).isEqualTo(0L);
        assertThat(result.flattenedListCount()).isEqualTo(0L);
    }

    @Test
    void delete_compactsSortOrderDensely() {
        // Produce (0) is deleted; dairy (1) should be rewritten to 0
        when(scopeRepository.lockByFamilyAndCategoryId(family, CAT_A_ID)).thenReturn(Optional.of(scope));
        when(categoryRepository.findByFamilyAndId(family, CAT_A_ID)).thenReturn(Optional.of(produce));
        when(itemRepository.clearCategory(FAMILY_ID, ListKind.GROCERY, CAT_A_ID)).thenReturn(0);
        when(categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of(dairy));
        when(categoryRepository.saveAll(anyList())).thenReturn(List.of(dairy));

        listCategoryService.delete(CAT_A_ID, family);

        // dairy's sortOrder should have been updated to 0
        assertThat(dairy.getSortOrder()).isEqualTo(0);
    }

    @Test
    void delete_categoryNotFound_viaLock_throwsResourceNotFoundException() {
        when(scopeRepository.lockByFamilyAndCategoryId(family, CAT_A_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listCategoryService.delete(CAT_A_ID, family))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_categoryNotFound_viaRefetch_throwsResourceNotFoundException() {
        when(scopeRepository.lockByFamilyAndCategoryId(family, CAT_A_ID)).thenReturn(Optional.of(scope));
        when(categoryRepository.findByFamilyAndId(family, CAT_A_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listCategoryService.delete(CAT_A_ID, family))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // reorder
    // -------------------------------------------------------------------------

    @Test
    void reorder_staleBaseline_throwsConflictException_beforeMembershipCheck() {
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(scope));
        when(categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of(produce, dairy));

        // expected says [B, A] but current is [A, B] — stale baseline
        ReorderListCategoriesRequest request = new ReorderListCategoriesRequest(
                ListKind.GROCERY,
                List.of(CAT_B_ID, CAT_A_ID),   // wrong expected → stale
                List.of(CAT_B_ID, CAT_A_ID)
        );

        assertThatThrownBy(() -> listCategoryService.reorder(request, family))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("changed");
    }

    @Test
    void reorder_staleBaselineAndInvalidMembership_throwsConflictNotBadRequest() {
        // Both the baseline is stale AND the membership is invalid (duplicate).
        // Proves the 409 stale-baseline check runs BEFORE the 400 membership check:
        // if membership ran first, the duplicate [A, A] would throw BadRequestException.
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(scope));
        when(categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of(produce, dairy));

        ReorderListCategoriesRequest request = new ReorderListCategoriesRequest(
                ListKind.GROCERY,
                List.of(CAT_B_ID, CAT_A_ID),   // stale: does not match current [A, B]
                List.of(CAT_A_ID, CAT_A_ID)    // invalid membership: duplicate
        );

        assertThatThrownBy(() -> listCategoryService.reorder(request, family))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void reorder_duplicateCategoryIds_throwsBadRequestException() {
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(scope));
        when(categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of(produce, dairy));

        // Correct expected baseline, but duplicate in target
        ReorderListCategoriesRequest request = new ReorderListCategoriesRequest(
                ListKind.GROCERY,
                List.of(CAT_A_ID, CAT_B_ID),
                List.of(CAT_A_ID, CAT_A_ID)  // duplicate
        );

        assertThatThrownBy(() -> listCategoryService.reorder(request, family))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reorder_missingCategoryId_throwsBadRequestException() {
        UUID unknownId = UUID.randomUUID();
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(scope));
        when(categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of(produce, dairy));

        // Correct expected, but target has wrong/unknown ID
        ReorderListCategoriesRequest request = new ReorderListCategoriesRequest(
                ListKind.GROCERY,
                List.of(CAT_A_ID, CAT_B_ID),
                List.of(CAT_A_ID, unknownId)   // unknown → not in current set
        );

        assertThatThrownBy(() -> listCategoryService.reorder(request, family))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reorder_wrongSize_throwsBadRequestException() {
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(scope));
        when(categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of(produce, dairy));

        // Correct expected, but target has only one ID
        ReorderListCategoriesRequest request = new ReorderListCategoriesRequest(
                ListKind.GROCERY,
                List.of(CAT_A_ID, CAT_B_ID),
                List.of(CAT_A_ID)   // missing B
        );

        assertThatThrownBy(() -> listCategoryService.reorder(request, family))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reorder_unchangedOrder_succeeds() {
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(scope));
        when(categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of(produce, dairy));
        when(categoryRepository.saveAll(anyList())).thenReturn(List.of(produce, dairy));
        when(listRepository.countGroupedLists(family, ListKind.GROCERY)).thenReturn(1L);
        when(itemRepository.countUsage(FAMILY_ID, ListKind.GROCERY)).thenReturn(List.of());

        ReorderListCategoriesRequest request = new ReorderListCategoriesRequest(
                ListKind.GROCERY,
                List.of(CAT_A_ID, CAT_B_ID),
                List.of(CAT_A_ID, CAT_B_ID)
        );

        ListCategoryCatalogResponse response = listCategoryService.reorder(request, family);

        assertThat(response.kind()).isEqualTo(ListKind.GROCERY);
    }

    @Test
    void reorder_changedOrder_rewritesDensely_returnsUpdatedCatalog() {
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(scope));
        when(categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of(produce, dairy));
        when(categoryRepository.saveAll(anyList())).thenReturn(List.of(dairy, produce));
        when(listRepository.countGroupedLists(family, ListKind.GROCERY)).thenReturn(0L);
        when(itemRepository.countUsage(FAMILY_ID, ListKind.GROCERY)).thenReturn(List.of());

        // Swap: B first, A second
        ReorderListCategoriesRequest request = new ReorderListCategoriesRequest(
                ListKind.GROCERY,
                List.of(CAT_A_ID, CAT_B_ID),
                List.of(CAT_B_ID, CAT_A_ID)
        );

        listCategoryService.reorder(request, family);

        // dairy (B) should now be at index 0, produce (A) at index 1
        assertThat(dairy.getSortOrder()).isEqualTo(0);
        assertThat(produce.getSortOrder()).isEqualTo(1);
    }

    @Test
    void reorder_singleCategory_succeeds() {
        ListCategory only = produce;  // only one category
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(scope));
        when(categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of(only));
        when(categoryRepository.saveAll(anyList())).thenReturn(List.of(only));
        when(listRepository.countGroupedLists(family, ListKind.GROCERY)).thenReturn(0L);
        when(itemRepository.countUsage(FAMILY_ID, ListKind.GROCERY)).thenReturn(List.of());

        ReorderListCategoriesRequest request = new ReorderListCategoriesRequest(
                ListKind.GROCERY,
                List.of(CAT_A_ID),
                List.of(CAT_A_ID)
        );

        ListCategoryCatalogResponse response = listCategoryService.reorder(request, family);

        assertThat(response.categories()).hasSize(1);
    }

    @Test
    void reorder_emptyCategories_succeeds() {
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(scope));
        when(categoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of());
        when(categoryRepository.saveAll(anyList())).thenReturn(List.of());
        when(listRepository.countGroupedLists(family, ListKind.GROCERY)).thenReturn(0L);
        when(itemRepository.countUsage(FAMILY_ID, ListKind.GROCERY)).thenReturn(List.of());

        ReorderListCategoriesRequest request = new ReorderListCategoriesRequest(
                ListKind.GROCERY,
                List.of(),
                List.of()
        );

        ListCategoryCatalogResponse response = listCategoryService.reorder(request, family);

        assertThat(response.categories()).isEmpty();
    }

    @Test
    void reorder_missingScopeRow_throwsIllegalState() {
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.empty());

        ReorderListCategoriesRequest request = new ReorderListCategoriesRequest(
                ListKind.GROCERY, List.of(), List.of()
        );

        assertThatThrownBy(() -> listCategoryService.reorder(request, family))
                .isInstanceOf(IllegalStateException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ListCategory savedCategory(UUID id, String name, ListKind kind, int sortOrder) {
        ListCategory cat = new ListCategory();
        cat.setId(id);
        cat.setFamily(family);
        cat.setKind(kind);
        cat.setName(name);
        cat.setSortOrder(sortOrder);
        return cat;
    }

    private SharedListItemRepository.CategoryUsageCount mockUsage(UUID categoryId, long count) {
        return new SharedListItemRepository.CategoryUsageCount() {
            @Override
            public UUID getCategoryId() { return categoryId; }
            @Override
            public long getItemCount() { return count; }
        };
    }
}
