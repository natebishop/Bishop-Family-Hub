package com.familyhub.demo.service;

import com.familyhub.demo.dto.BulkCreateListItemsRequest;
import com.familyhub.demo.dto.ClearCompletedResponse;
import com.familyhub.demo.dto.CreateListItemRequest;
import com.familyhub.demo.dto.CreateListRequest;
import com.familyhub.demo.dto.ListDetailResponse;
import com.familyhub.demo.dto.ListItemResponse;
import com.familyhub.demo.dto.UpdateListItemRequest;
import com.familyhub.demo.dto.UpdateListRequest;
import com.familyhub.demo.exception.BadRequestException;
import com.familyhub.demo.exception.ConflictException;
import com.familyhub.demo.exception.ResourceNotFoundException;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.ListCategory;
import com.familyhub.demo.model.ListCategoryCatalogScope;
import com.familyhub.demo.model.ListCategoryDisplayMode;
import com.familyhub.demo.model.ListKind;
import com.familyhub.demo.model.SharedList;
import com.familyhub.demo.model.SharedListItem;
import com.familyhub.demo.repository.ListCategoryCatalogScopeRepository;
import com.familyhub.demo.repository.ListCategoryRepository;
import com.familyhub.demo.repository.ListPreferencesRepository;
import com.familyhub.demo.repository.SharedListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.familyhub.demo.TestDataFactory.LIST_ID;
import static com.familyhub.demo.TestDataFactory.LIST_ITEM_ID;
import static com.familyhub.demo.TestDataFactory.createFamily;
import static com.familyhub.demo.TestDataFactory.createGeneralList;
import static com.familyhub.demo.TestDataFactory.createGroceryList;
import static com.familyhub.demo.TestDataFactory.createListCategory;
import static com.familyhub.demo.TestDataFactory.createListItem;
import static com.familyhub.demo.TestDataFactory.createListWithCompletedItems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListServiceTest {

    @Mock
    private SharedListRepository sharedListRepository;

    @Mock
    private ListCategoryRepository listCategoryRepository;

    @Mock
    private ListPreferencesRepository listPreferencesRepository;

    @Mock
    private ListCategoryCatalogScopeRepository scopeRepository;

    @InjectMocks
    private ListService listService;

    private Family family;
    private SharedList groceryList;
    private SharedList generalList;
    private ListCategory produceCategory;
    private ListCategoryCatalogScope groceryScope;
    private ListCategoryCatalogScope generalScope;

    @BeforeEach
    void setUp() {
        family = createFamily();
        groceryList = createGroceryList(family);
        generalList = createGeneralList(family);
        produceCategory = createListCategory(family, ListKind.GROCERY, "Produce", 0);
        groceryList.setItems(new ArrayList<>(List.of(
                createListItem(groceryList, LIST_ITEM_ID, "Bananas", produceCategory, false, null)
        )));

        groceryScope = new ListCategoryCatalogScope();
        groceryScope.setId(UUID.randomUUID());
        groceryScope.setFamily(family);
        groceryScope.setKind(ListKind.GROCERY);

        generalScope = new ListCategoryCatalogScope();
        generalScope.setId(UUID.randomUUID());
        generalScope.setFamily(family);
        generalScope.setKind(ListKind.GENERAL);
    }

    // -------------------------------------------------------------------------
    // getLists
    // -------------------------------------------------------------------------

    @Test
    void getLists_returnsFamilyScopedSummaries() {
        when(sharedListRepository.findByFamilyWithItems(family)).thenReturn(List.of(createListWithCompletedItems(family)));

        var result = listService.getLists(family);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().totalItems()).isEqualTo(3);
        assertThat(result.getFirst().completedItems()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // createList — display mode selection
    // -------------------------------------------------------------------------

    @Test
    void createList_generalKind_alwaysFlat() {
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GENERAL)).thenReturn(Optional.of(generalScope));
        SharedList saved = createGeneralList(family);
        saved.setCategoryDisplayMode(ListCategoryDisplayMode.FLAT);
        when(sharedListRepository.saveAndFlush(any(SharedList.class))).thenReturn(saved);
        when(listCategoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GENERAL))
                .thenReturn(List.of());

        ListDetailResponse result = listService.createList(new CreateListRequest("Movies", ListKind.GENERAL), family);

        assertThat(result.categoryDisplayMode()).isEqualTo(ListCategoryDisplayMode.FLAT);
    }

    @Test
    void createList_groceryWithCategories_createsGrouped() {
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(groceryScope));
        when(listCategoryRepository.countByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(3L);
        SharedList saved = createGroceryList(family);
        saved.setCategoryDisplayMode(ListCategoryDisplayMode.GROUPED);
        when(sharedListRepository.saveAndFlush(any(SharedList.class))).thenReturn(saved);
        when(listCategoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of(produceCategory));

        ListDetailResponse result = listService.createList(new CreateListRequest("Grocery Run", ListKind.GROCERY), family);

        assertThat(result.categoryDisplayMode()).isEqualTo(ListCategoryDisplayMode.GROUPED);
    }

    @Test
    void createList_groceryWithEmptyCatalog_createsFlat() {
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(groceryScope));
        when(listCategoryRepository.countByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(0L);
        SharedList saved = createGroceryList(family);
        saved.setCategoryDisplayMode(ListCategoryDisplayMode.FLAT);
        when(sharedListRepository.saveAndFlush(any(SharedList.class))).thenReturn(saved);
        when(listCategoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of());

        ListDetailResponse result = listService.createList(new CreateListRequest("Grocery Run", ListKind.GROCERY), family);

        assertThat(result.categoryDisplayMode()).isEqualTo(ListCategoryDisplayMode.FLAT);
    }

    @Test
    void createList_acquiresScopeLockBeforePersisting() {
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(groceryScope));
        when(listCategoryRepository.countByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(1L);
        SharedList saved = createGroceryList(family);
        when(sharedListRepository.saveAndFlush(any(SharedList.class))).thenReturn(saved);
        when(listCategoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of());

        listService.createList(new CreateListRequest("Grocery Run", ListKind.GROCERY), family);

        // Scope lock BEFORE save
        InOrder order = inOrder(scopeRepository, sharedListRepository);
        order.verify(scopeRepository).lockByFamilyAndKind(family, ListKind.GROCERY);
        order.verify(sharedListRepository).saveAndFlush(any(SharedList.class));
    }

    // -------------------------------------------------------------------------
    // updateList — GROUPED path enforces scope lock and non-empty catalog
    // -------------------------------------------------------------------------

    @Test
    void updateList_groupedPath_emptyCatalog_throwsConflict() {
        when(sharedListRepository.findKindByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(ListKind.GROCERY));
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(groceryScope));
        when(listCategoryRepository.countByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(0L);

        assertThatThrownBy(() -> listService.updateList(
                LIST_ID,
                new UpdateListRequest(ListCategoryDisplayMode.GROUPED, null),
                family
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("category");
    }

    @Test
    void updateList_groupedPath_generalKind_emptyCatalog_throwsConflict() {
        when(sharedListRepository.findKindByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(ListKind.GENERAL));
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GENERAL)).thenReturn(Optional.of(generalScope));
        when(listCategoryRepository.countByFamilyAndKind(family, ListKind.GENERAL)).thenReturn(0L);

        assertThatThrownBy(() -> listService.updateList(
                LIST_ID,
                new UpdateListRequest(ListCategoryDisplayMode.GROUPED, null),
                family
        )).isInstanceOf(ConflictException.class);
    }

    @Test
    void updateList_groupedPath_generalKind_withCategories_succeeds() {
        when(sharedListRepository.findKindByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(ListKind.GENERAL));
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GENERAL)).thenReturn(Optional.of(generalScope));
        when(listCategoryRepository.countByFamilyAndKind(family, ListKind.GENERAL)).thenReturn(2L);
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(generalList));
        when(sharedListRepository.saveAndFlush(any(SharedList.class))).thenReturn(generalList);
        when(listCategoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GENERAL))
                .thenReturn(List.of());

        ListDetailResponse result = listService.updateList(
                LIST_ID,
                new UpdateListRequest(ListCategoryDisplayMode.GROUPED, null),
                family
        );

        assertThat(result).isNotNull();
    }

    @Test
    void updateList_groupedPath_lockOrderingBeforeAggregateLoad() {
        when(sharedListRepository.findKindByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(ListKind.GROCERY));
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(groceryScope));
        when(listCategoryRepository.countByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(2L);
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(groceryList));
        when(sharedListRepository.saveAndFlush(any(SharedList.class))).thenReturn(groceryList);
        when(listCategoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of());

        listService.updateList(LIST_ID, new UpdateListRequest(ListCategoryDisplayMode.GROUPED, null), family);

        // Ordering: read kind → lock scope → (count check) → load aggregate
        InOrder order = inOrder(sharedListRepository, scopeRepository, listCategoryRepository);
        order.verify(sharedListRepository).findKindByFamilyAndId(family, LIST_ID);
        order.verify(scopeRepository).lockByFamilyAndKind(family, ListKind.GROCERY);
        order.verify(listCategoryRepository).countByFamilyAndKind(family, ListKind.GROCERY);
        order.verify(sharedListRepository).findDetailByFamilyAndId(family, LIST_ID);
    }

    @Test
    void updateList_listNotFound_throws404() {
        when(sharedListRepository.findKindByFamilyAndId(family, LIST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listService.updateList(
                LIST_ID,
                new UpdateListRequest(ListCategoryDisplayMode.GROUPED, null),
                family
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateList_flatPath_noScopeLock() {
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(groceryList));
        when(sharedListRepository.saveAndFlush(any(SharedList.class))).thenReturn(groceryList);
        when(listCategoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of());

        listService.updateList(LIST_ID, new UpdateListRequest(ListCategoryDisplayMode.FLAT, null), family);

        // No scope lock should have been acquired on the flat path
        verify(scopeRepository, org.mockito.Mockito.never()).lockByFamilyAndKind(any(), any());
    }

    // -------------------------------------------------------------------------
    // createItem — category assignment with scope lock
    // -------------------------------------------------------------------------

    @Test
    void createItem_withCategory_acquiresScopeLockBeforeLoadingAggregate() {
        when(sharedListRepository.findKindByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(ListKind.GROCERY));
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(groceryScope));
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(groceryList));
        when(listCategoryRepository.findByFamilyAndId(family, produceCategory.getId()))
                .thenReturn(Optional.of(produceCategory));
        when(sharedListRepository.saveAndFlush(any(SharedList.class))).thenReturn(groceryList);

        listService.createItem(LIST_ID, new CreateListItemRequest("Spinach", produceCategory.getId()), family);

        InOrder order = inOrder(sharedListRepository, scopeRepository);
        order.verify(sharedListRepository).findKindByFamilyAndId(family, LIST_ID);
        order.verify(scopeRepository).lockByFamilyAndKind(family, ListKind.GROCERY);
        order.verify(sharedListRepository).findDetailByFamilyAndId(family, LIST_ID);
    }

    @Test
    void createItem_withCategory_wrongKind_throwsBadRequest() {
        ListCategory todoCategory = createListCategory(family, ListKind.TODO, "Urgent", 0);
        when(sharedListRepository.findKindByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(ListKind.GROCERY));
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(groceryScope));
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(groceryList));
        when(listCategoryRepository.findByFamilyAndId(family, todoCategory.getId()))
                .thenReturn(Optional.of(todoCategory));

        assertThatThrownBy(() -> listService.createItem(
                LIST_ID,
                new CreateListItemRequest("Task", todoCategory.getId()),
                family
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    void createItem_generalListWithCategory_succeeds() {
        ListCategory generalCategory = createListCategory(family, ListKind.GENERAL, "Documents", 0);
        SharedList gList = createGeneralList(family);
        gList.setItems(new ArrayList<>());
        when(sharedListRepository.findKindByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(ListKind.GENERAL));
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GENERAL)).thenReturn(Optional.of(generalScope));
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(gList));
        when(listCategoryRepository.findByFamilyAndId(family, generalCategory.getId()))
                .thenReturn(Optional.of(generalCategory));
        when(sharedListRepository.saveAndFlush(any(SharedList.class))).thenReturn(gList);

        // General lists now support categories — should NOT throw
        var result = listService.createItem(
                LIST_ID,
                new CreateListItemRequest("Report", generalCategory.getId()),
                family
        );
        assertThat(result).isNotNull();
    }

    @Test
    void createItem_noCategory_noScopeLock() {
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(groceryList));
        when(sharedListRepository.saveAndFlush(any(SharedList.class))).thenReturn(groceryList);

        listService.createItem(LIST_ID, new CreateListItemRequest("Milk", null), family);

        verify(scopeRepository, org.mockito.Mockito.never()).lockByFamilyAndKind(any(), any());
    }

    @Test
    void createItem_listNotFound_throws404() {
        UUID catId = UUID.randomUUID();
        when(sharedListRepository.findKindByFamilyAndId(family, LIST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listService.createItem(
                LIST_ID,
                new CreateListItemRequest("Milk", catId),
                family
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // createItemsBulk — transactional bulk append reusing the createItem path
    // -------------------------------------------------------------------------

    @Test
    void createItemsBulk_appendsAllItemsInRequestOrder() {
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(groceryList));
        when(sharedListRepository.saveAndFlush(any(SharedList.class))).thenReturn(groceryList);

        List<ListItemResponse> created = listService.createItemsBulk(
                LIST_ID,
                new BulkCreateListItemsRequest(List.of(
                        new CreateListItemRequest("2 chicken breasts", null),
                        new CreateListItemRequest("1 tbsp olive oil", null),
                        new CreateListItemRequest("2 cups broccoli", null)
                )),
                family
        );

        assertThat(created).extracting(ListItemResponse::text)
                .containsExactly("2 chicken breasts", "1 tbsp olive oil", "2 cups broccoli");
    }

    @Test
    void createItemsBulk_acceptsMaxItems() {
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(groceryList));
        when(sharedListRepository.saveAndFlush(any(SharedList.class))).thenReturn(groceryList);
        List<CreateListItemRequest> items = IntStream.range(0, 100)
                .mapToObj(i -> new CreateListItemRequest("item " + i, null))
                .toList();

        List<ListItemResponse> created = listService.createItemsBulk(
                LIST_ID, new BulkCreateListItemsRequest(items), family);

        assertThat(created).hasSize(100);
        assertThat(created.get(0).text()).isEqualTo("item 0");
        assertThat(created.get(99).text()).isEqualTo("item 99");
    }

    // Defense-in-depth guard: the service rejects an over-max batch before touching the repository,
    // even though the DTO's @Size normally catches this at the request boundary.
    @Test
    void createItemsBulk_overMaxItems_throwsBadRequestBeforeRepositoryAccess() {
        List<CreateListItemRequest> items = IntStream.range(0, BulkCreateListItemsRequest.MAX_BULK_ITEMS + 1)
                .mapToObj(i -> new CreateListItemRequest("item " + i, null))
                .toList();

        assertThatThrownBy(() -> listService.createItemsBulk(
                LIST_ID, new BulkCreateListItemsRequest(items), family))
                .isInstanceOf(BadRequestException.class);

        verify(sharedListRepository, org.mockito.Mockito.never())
                .findDetailByFamilyAndId(any(Family.class), any(UUID.class));
    }

    @Test
    void createItemsBulk_appendsAfterExistingItems_returnsOnlyNewItems() {
        // groceryList already has one item ("Bananas"), so existingCount is non-zero.
        int before = groceryList.getItems().size();
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(groceryList));
        when(sharedListRepository.saveAndFlush(any(SharedList.class))).thenReturn(groceryList);

        List<ListItemResponse> created = listService.createItemsBulk(
                LIST_ID,
                new BulkCreateListItemsRequest(List.of(
                        new CreateListItemRequest("2 chicken breasts", null),
                        new CreateListItemRequest("1 tbsp olive oil", null)
                )),
                family
        );

        // Response holds ONLY the two new items in request order (not the pre-existing "Bananas").
        assertThat(created).extracting(ListItemResponse::text)
                .containsExactly("2 chicken breasts", "1 tbsp olive oil");
        // The aggregate now holds the pre-existing item plus the two appended ones, in insertion order.
        assertThat(groceryList.getItems()).hasSize(before + 2);
        assertThat(groceryList.getItems())
                .extracting(SharedListItem::getText)
                .containsExactly("Bananas", "2 chicken breasts", "1 tbsp olive oil");
    }

    @Test
    void createItemsBulk_listNotFound_throws404() {
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listService.createItemsBulk(
                LIST_ID,
                new BulkCreateListItemsRequest(List.of(new CreateListItemRequest("milk", null))),
                family
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createItemsBulk_assignsValidSameKindCategory() {
        when(sharedListRepository.findKindByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(ListKind.GROCERY));
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(groceryScope));
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(groceryList));
        when(listCategoryRepository.findByFamilyAndId(family, produceCategory.getId()))
                .thenReturn(Optional.of(produceCategory));
        when(sharedListRepository.saveAndFlush(any(SharedList.class))).thenReturn(groceryList);

        List<ListItemResponse> created = listService.createItemsBulk(
                LIST_ID,
                new BulkCreateListItemsRequest(List.of(
                        new CreateListItemRequest("2 chicken breasts", produceCategory.getId())
                )),
                family
        );

        assertThat(created).hasSize(1);
        assertThat(created.get(0).categoryId()).isEqualTo(produceCategory.getId());
    }

    @Test
    void createItemsBulk_missingCategory_throws404AndAppendsNothing() {
        UUID missingCategoryId = UUID.randomUUID();
        int before = groceryList.getItems().size();
        when(sharedListRepository.findKindByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(ListKind.GROCERY));
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(groceryScope));
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(groceryList));
        when(listCategoryRepository.findByFamilyAndId(family, missingCategoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listService.createItemsBulk(
                LIST_ID,
                new BulkCreateListItemsRequest(List.of(new CreateListItemRequest("mystery", missingCategoryId))),
                family
        )).isInstanceOf(ResourceNotFoundException.class);

        assertThat(groceryList.getItems()).hasSize(before);
    }

    // Proves prevalidation: the wrong-kind second item is rejected DURING category
    // resolution, before any row is appended and before saveAndFlush is called.
    @Test
    void createItemsBulk_wrongKindCategory_throwsAndAppendsNothing() {
        // createListCategory hardcodes LIST_CATEGORY_ID, so give the TODO category a distinct id
        // to avoid colliding with produceCategory's id on the two findByFamilyAndId stubs.
        ListCategory todoCategory = createListCategory(family, ListKind.TODO, "Urgent", 0);
        todoCategory.setId(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));
        int before = groceryList.getItems().size();
        when(sharedListRepository.findKindByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(ListKind.GROCERY));
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(groceryScope));
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(groceryList));
        when(listCategoryRepository.findByFamilyAndId(family, produceCategory.getId()))
                .thenReturn(Optional.of(produceCategory));
        when(listCategoryRepository.findByFamilyAndId(family, todoCategory.getId()))
                .thenReturn(Optional.of(todoCategory));

        assertThatThrownBy(() -> listService.createItemsBulk(
                LIST_ID,
                new BulkCreateListItemsRequest(List.of(
                        new CreateListItemRequest("ok row", produceCategory.getId()),
                        new CreateListItemRequest("bad row", todoCategory.getId()) // wrong-kind category
                )),
                family
        )).isInstanceOf(BadRequestException.class);

        assertThat(groceryList.getItems()).hasSize(before);
        verify(sharedListRepository, org.mockito.Mockito.never()).saveAndFlush(any(SharedList.class));
    }

    // -------------------------------------------------------------------------
    // updateItem — category assignment with scope lock
    // -------------------------------------------------------------------------

    @Test
    void updateItem_withCategory_acquiresScopeLockBeforeLoadingAggregate() {
        when(sharedListRepository.findKindByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(ListKind.GROCERY));
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(groceryScope));
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(groceryList));
        when(listCategoryRepository.findByFamilyAndId(family, produceCategory.getId()))
                .thenReturn(Optional.of(produceCategory));
        when(sharedListRepository.saveAndFlush(any(SharedList.class))).thenReturn(groceryList);

        listService.updateItem(LIST_ID, LIST_ITEM_ID,
                new UpdateListItemRequest("Bananas", false, produceCategory.getId()), family);

        InOrder order = inOrder(sharedListRepository, scopeRepository);
        order.verify(sharedListRepository).findKindByFamilyAndId(family, LIST_ID);
        order.verify(scopeRepository).lockByFamilyAndKind(family, ListKind.GROCERY);
        order.verify(sharedListRepository).findDetailByFamilyAndId(family, LIST_ID);
    }

    @Test
    void updateItem_categoryFromWrongKind_throwsBadRequest() {
        ListCategory todoCategory = createListCategory(family, ListKind.TODO, "Urgent", 0);
        when(sharedListRepository.findKindByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(ListKind.GROCERY));
        when(scopeRepository.lockByFamilyAndKind(family, ListKind.GROCERY)).thenReturn(Optional.of(groceryScope));
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(groceryList));
        when(listCategoryRepository.findByFamilyAndId(family, todoCategory.getId())).thenReturn(Optional.of(todoCategory));

        assertThatThrownBy(() -> listService.updateItem(
                LIST_ID,
                LIST_ITEM_ID,
                new UpdateListItemRequest("Bananas", false, todoCategory.getId()),
                family
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateItem_missingItem_throwsResourceNotFound() {
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(groceryList));

        assertThatThrownBy(() -> listService.updateItem(
                LIST_ID,
                UUID.fromString("00000000-0000-0000-0000-000000000099"),
                new UpdateListItemRequest("Bananas", false, null),
                family
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateItem_noCategory_noScopeLock() {
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(groceryList));
        when(sharedListRepository.saveAndFlush(any(SharedList.class))).thenReturn(groceryList);

        listService.updateItem(LIST_ID, LIST_ITEM_ID,
                new UpdateListItemRequest("Bananas", false, null), family);

        verify(scopeRepository, org.mockito.Mockito.never()).lockByFamilyAndKind(any(), any());
    }

    // -------------------------------------------------------------------------
    // mapDetail — includes categories for ALL kinds including GENERAL
    // -------------------------------------------------------------------------

    @Test
    void getList_generalKind_includesCategories() {
        ListCategory generalCategory = createListCategory(family, ListKind.GENERAL, "Documents", 0);
        SharedList gList = createGeneralList(family);
        gList.setItems(new ArrayList<>());
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(gList));
        when(listCategoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GENERAL))
                .thenReturn(List.of(generalCategory));

        ListDetailResponse result = listService.getList(LIST_ID, family);

        assertThat(result.categories()).hasSize(1);
        assertThat(result.categories().getFirst().name()).isEqualTo("Documents");
    }

    @Test
    void getList_generalKind_emptyCategories_returnsEmptyList() {
        SharedList gList = createGeneralList(family);
        gList.setItems(new ArrayList<>());
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(gList));
        when(listCategoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GENERAL))
                .thenReturn(List.of());

        ListDetailResponse result = listService.getList(LIST_ID, family);

        assertThat(result.categories()).isEmpty();
    }

    @Test
    void getList_groceryKind_includesOrderedCategories() {
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID)).thenReturn(Optional.of(groceryList));
        when(listCategoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY))
                .thenReturn(List.of(produceCategory));

        ListDetailResponse result = listService.getList(LIST_ID, family);

        assertThat(result.categories()).hasSize(1);
        assertThat(result.categories().getFirst().name()).isEqualTo("Produce");
    }

    // -------------------------------------------------------------------------
    // clearCompleted
    // -------------------------------------------------------------------------

    @Test
    void clearCompleted_removesCompletedItemsAndReturnsCount() {
        when(sharedListRepository.findDetailByFamilyAndId(family, LIST_ID))
                .thenReturn(Optional.of(createListWithCompletedItems(family)));
        when(sharedListRepository.saveAndFlush(any(SharedList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClearCompletedResponse response = listService.clearCompleted(LIST_ID, family);

        assertThat(response.removedCount()).isEqualTo(2);
        verify(sharedListRepository).saveAndFlush(any(SharedList.class));
    }
}
