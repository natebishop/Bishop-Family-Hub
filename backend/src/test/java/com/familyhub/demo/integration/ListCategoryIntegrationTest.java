package com.familyhub.demo.integration;

import com.familyhub.demo.config.TestcontainersConfig;
import com.familyhub.demo.dto.CreateListCategoryRequest;
import com.familyhub.demo.dto.RenameListCategoryRequest;
import com.familyhub.demo.dto.ReorderListCategoriesRequest;
import com.familyhub.demo.exception.ConflictException;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.ListKind;
import com.familyhub.demo.repository.FamilyRepository;
import com.familyhub.demo.repository.ListCategoryRepository;
import com.familyhub.demo.repository.SharedListRepository;
import com.familyhub.demo.service.ListCategoryService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class ListCategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private ListCategoryRepository listCategoryRepository;

    @Autowired
    private SharedListRepository sharedListRepository;

    @Autowired
    private ListCategoryService listCategoryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager txManager;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String uniqueUsername() {
        return "cat" + System.nanoTime();
    }

    private String registerAndGetToken(String username) throws Exception {
        String authBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "TestPassword123!",
                                  "familyName": "Category Family",
                                  "members": [
                                    { "name": "Alice", "color": "coral" }
                                  ]
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(authBody, "$.data.token");
    }

    private String createList(String token, String name, String kind) throws Exception {
        String body = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "kind": "%s"}
                                """.formatted(name, kind)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.id");
    }

    // -------------------------------------------------------------------------
    // 1. Family + kind isolation
    // -------------------------------------------------------------------------

    @Test
    void familyIsolation_crossFamilyCategoryIdReturns404() throws Exception {
        String userA = uniqueUsername();
        String userB = uniqueUsername();
        String tokenA = registerAndGetToken(userA);
        String tokenB = registerAndGetToken(userB);

        Family familyA = familyRepository.findByUsername(userA).orElseThrow();
        Family familyB = familyRepository.findByUsername(userB).orElseThrow();

        // A creates a General category
        var catA = listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Private"), familyA);
        UUID catAId = catA.id();

        // B tries to rename A's category → must 404 (not reveal existence)
        mockMvc.perform(patch("/api/lists/categories/{id}", catAId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Hacked"}
                                """))
                .andExpect(status().isNotFound());

        // B tries to delete A's category → must 404
        mockMvc.perform(delete("/api/lists/categories/{id}", catAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // A's category still exists unchanged
        assertThat(listCategoryRepository.findByFamilyAndId(familyA, catAId)).isPresent();
    }

    @Test
    void kindIsolation_groceryCategoryNotVisibleInGeneralCatalog() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        // Family already has 5 Grocery categories from seeding
        // Fetch general catalog — should have 0 categories
        mockMvc.perform(get("/api/lists/categories").param("kind", "general")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories").isEmpty());

        // Fetch grocery catalog — should have seeded categories
        mockMvc.perform(get("/api/lists/categories").param("kind", "grocery")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories.length()").value(5));
    }

    @Test
    void kindIsolation_reorderWithForeignKindIds_returns400() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        // Get grocery categories (5 seeded) and to-do categories (3 seeded)
        String catalogBody = mockMvc.perform(get("/api/lists/categories").param("kind", "grocery")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> groceryIds = JsonPath.read(catalogBody, "$.data.categories[*].id");

        String todoBody = mockMvc.perform(get("/api/lists/categories").param("kind", "to-do")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> todoIds = JsonPath.read(todoBody, "$.data.categories[*].id");

        // VALID baseline: expectedCategoryIds == the real to-do catalog, so the stale-baseline (409)
        // check passes. Then membership validation runs and rejects the foreign (grocery) target ids:
        // grocery has 5 entries, to-do has 3, so the size differs → BadRequest("Invalid category order") → 400.
        // No foreign existence is revealed — the rejection is purely a membership mismatch.
        mockMvc.perform(put("/api/lists/categories/order")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind": "to-do",
                                  "expectedCategoryIds": %s,
                                  "categoryIds": %s
                                }
                                """.formatted(toJsonArray(todoIds), toJsonArray(groceryIds))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reorder_validBaseline_foreignFamilyIdInTarget_returns400_withoutRevealingExistence() throws Exception {
        String userA = uniqueUsername();
        String userB = uniqueUsername();
        String tokenA = registerAndGetToken(userA);
        String tokenB = registerAndGetToken(userB);
        Family familyA = familyRepository.findByUsername(userA).orElseThrow();
        Family familyB = familyRepository.findByUsername(userB).orElseThrow();

        // B has exactly ONE General category — a non-empty catalog so the baseline can match.
        var catB = listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Mine"), familyB);

        // A has a General category B must not be able to reference.
        var catA = listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Secret"), familyA);

        // VALID baseline for B: expectedCategoryIds == B's real catalog ([catB]). The stale-baseline (409)
        // check therefore PASSES. The target is the SAME SIZE (1 element) but swaps in A's foreign id:
        // membership validation finds the set {catA} != current set {catB} → BadRequest → 400.
        // The 400 is identical whether catA exists or not, so foreign existence/ownership is never revealed.
        mockMvc.perform(put("/api/lists/categories/order")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind": "general",
                                  "expectedCategoryIds": ["%s"],
                                  "categoryIds": ["%s"]
                                }
                                """.formatted(catB.id(), catA.id())))
                .andExpect(status().isBadRequest());

        // B's own category is untouched (still sortOrder 0, still present).
        var bCats = listCategoryRepository.findByFamilyAndKindOrderBySortOrderAsc(familyB, ListKind.GENERAL);
        assertThat(bCats).hasSize(1);
        assertThat(bCats.get(0).getId()).isEqualTo(catB.id());
        assertThat(bCats.get(0).getSortOrder()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // 2. itemCount spans lists without join multiplication
    // -------------------------------------------------------------------------

    @Test
    void itemCount_acrossTwoListsSameCatalog_noJoinMultiplication() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        // Get the first grocery category (Produce at sortOrder 0)
        String catBody = mockMvc.perform(get("/api/lists/categories").param("kind", "grocery")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String produceCatId = JsonPath.read(catBody, "$.data.categories[0].id");

        // Create 2 grocery lists
        String list1Id = createList(token, "List One", "grocery");
        String list2Id = createList(token, "List Two", "grocery");

        // Add 2 items to list1 with Produce, 1 item to list2 with Produce
        addItem(token, list1Id, "Bananas", produceCatId);
        addItem(token, list1Id, "Apples", produceCatId);
        addItem(token, list2Id, "Oranges", produceCatId);

        // Fetch catalog and verify itemCount = 3 (not 6 due to join multiplication)
        String catalogBody = mockMvc.perform(get("/api/lists/categories").param("kind", "grocery")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Find the Produce category in the catalog and check its itemCount
        List<String> ids = JsonPath.read(catalogBody, "$.data.categories[*].id");
        List<Integer> counts = JsonPath.read(catalogBody, "$.data.categories[*].itemCount");
        int produceIndex = ids.indexOf(produceCatId);
        assertThat(counts.get(produceIndex)).isEqualTo(3);
    }

    private void addItem(String token, String listId, String text, String categoryId) throws Exception {
        mockMvc.perform(post("/api/lists/{id}/items", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "%s", "categoryId": "%s"}
                                """.formatted(text, categoryId)))
                .andExpect(status().isCreated());
    }

    // -------------------------------------------------------------------------
    // 3. Delete semantics: uncategorized count, compaction, flatten
    // -------------------------------------------------------------------------

    @Test
    void delete_movesItemsToNull_andCompactsSortOrders_andFlattensOnFinal() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        // Create a General list
        String listId = createList(token, "Notes", "general");

        // Create 2 categories
        var cat1 = listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Work"), family);
        var cat2 = listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Personal"), family);
        String cat1Id = cat1.id().toString();
        String cat2Id = cat2.id().toString();

        // Switch list to grouped
        mockMvc.perform(patch("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryDisplayMode": "grouped"}
                                """))
                .andExpect(status().isOk());

        // Add 2 items to cat1, 1 item to cat2
        String item1Id = addAndGetItemId(token, listId, "Meeting notes", cat1Id);
        String item2Id = addAndGetItemId(token, listId, "Report", cat1Id);
        String item3Id = addAndGetItemId(token, listId, "Exercise", cat2Id);

        // Delete cat1 — should move 2 items to null, cat2 remains
        String deleteBody = mockMvc.perform(delete("/api/lists/categories/{id}", cat1.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uncategorizedItemCount").value(2))
                .andExpect(jsonPath("$.data.flattenedListCount").value(0))  // cat2 still exists
                .andReturn().getResponse().getContentAsString();

        // cat2 should now have sortOrder 0 (compacted)
        String catBody = mockMvc.perform(get("/api/lists/categories").param("kind", "general")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Integer> sortOrders = JsonPath.read(catBody, "$.data.categories[*].sortOrder");
        assertThat(sortOrders).containsExactly(0);

        // Delete cat2 — FINAL delete: should flatten the grouped list
        mockMvc.perform(delete("/api/lists/categories/{id}", cat2.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uncategorizedItemCount").value(1))
                .andExpect(jsonPath("$.data.flattenedListCount").value(1));

        // List is now flat
        mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryDisplayMode").value("flat"));
    }

    private String addAndGetItemId(String token, String listId, String text, String categoryId) throws Exception {
        String body = mockMvc.perform(post("/api/lists/{id}/items", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "%s", "categoryId": "%s"}
                                """.formatted(text, categoryId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.id");
    }

    // -------------------------------------------------------------------------
    // 4. Delete rollback (atomicity via trigger)
    // -------------------------------------------------------------------------

    @Test
    void delete_rollback_onTriggerFailure_leavesEverythingUnchanged() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        // Create a General category
        var cat = listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Rollback Target"), family);
        UUID catId = cat.id();

        // Create a list and add an item to the category
        String listId = createList(token, "Notes", "general");
        addItem(token, listId, "Note 1", catId.toString());

        // Switch list to grouped mode
        listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Another"), family);
        mockMvc.perform(patch("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryDisplayMode": "grouped"}
                                """))
                .andExpect(status().isOk());

        // Capture the EXACT catalog ordering before the failed delete. There are two GENERAL
        // categories: "Rollback Target" (sortOrder 0, the delete target) and "Another" (sortOrder 1).
        // A delete that partially applied before rolling back would have compacted "Another" from 1→0,
        // so re-asserting these exact ids+sortOrders proves the ORDER rolled back, not just row existence.
        String beforeBody = mockMvc.perform(get("/api/lists/categories").param("kind", "general")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> idsBefore = JsonPath.read(beforeBody, "$.data.categories[*].id");
        List<Integer> sortOrdersBefore = JsonPath.read(beforeBody, "$.data.categories[*].sortOrder");
        assertThat(sortOrdersBefore).containsExactly(0, 1);

        // Install a trigger that prevents DELETE on list_category
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION trg_block_category_delete()
                RETURNS TRIGGER LANGUAGE plpgsql AS $$
                BEGIN
                  RAISE EXCEPTION 'Intentional rollback trigger for test';
                END;
                $$;
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER block_category_delete
                BEFORE DELETE ON list_category
                FOR EACH ROW EXECUTE FUNCTION trg_block_category_delete();
                """);

        try {
            // Attempt to delete through the API — should fail with 5xx
            mockMvc.perform(delete("/api/lists/categories/{id}", catId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().is5xxServerError());

            // Verify full rollback: category row still exists
            assertThat(listCategoryRepository.findByFamilyAndId(family, catId)).isPresent();

            // Item still has the category assignment
            mockMvc.perform(get("/api/lists/{id}", listId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items[0].categoryId").value(catId.toString()));

            // List still in GROUPED mode (not flattened by a rolled-back delete)
            mockMvc.perform(get("/api/lists/{id}", listId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.categoryDisplayMode").value("grouped"));

            // sortOrder fully intact: the EXACT same ids in the EXACT same positions with the EXACT
            // same sortOrder values as before the failed delete. "Rollback Target" is still at 0 and
            // "Another" is still at 1 — proving order rolled back, not merely that the row survived.
            String afterBody = mockMvc.perform(get("/api/lists/categories").param("kind", "general")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<String> idsAfter = JsonPath.read(afterBody, "$.data.categories[*].id");
            List<Integer> sortOrdersAfter = JsonPath.read(afterBody, "$.data.categories[*].sortOrder");
            assertThat(idsAfter).isEqualTo(idsBefore);
            assertThat(sortOrdersAfter).isEqualTo(sortOrdersBefore);
            assertThat(sortOrdersAfter).containsExactly(0, 1);
            // The delete target specifically is still present at its original sortOrder 0.
            assertThat(idsAfter.get(0)).isEqualTo(catId.toString());

        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS block_category_delete ON list_category");
            jdbcTemplate.execute("DROP FUNCTION IF EXISTS trg_block_category_delete");
        }
    }

    // -------------------------------------------------------------------------
    // 5. Duplicate-name race
    // -------------------------------------------------------------------------

    @Test
    void duplicateNameRace_exactlyOneSucceeds_zeroUnexpectedErrors() throws Exception {
        String username = uniqueUsername();
        registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger unexpectedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Two threads try to create "Travel" and "travel" (case-insensitive collision)
        String[] names = {"Travel", "travel"};
        for (String name : names) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, name), family);
                    successCount.incrementAndGet();
                } catch (ConflictException | DataIntegrityViolationException ex) {
                    conflictCount.incrementAndGet();
                } catch (Exception ex) {
                    unexpectedCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(unexpectedCount.get()).isEqualTo(0);
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        // Exactly one row exists in DB
        long count = listCategoryRepository.countByFamilyAndKind(family, ListKind.GENERAL);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void duplicateNameRace_throughHttpApi_oneCreatedOneConflict_noServerError() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger created201 = new AtomicInteger(0);
        AtomicInteger conflict409 = new AtomicInteger(0);
        AtomicInteger serverError5xx = new AtomicInteger(0);
        AtomicInteger otherStatus = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Two concurrent POSTs that collide case-insensitively must resolve at the HTTP boundary as
        // exactly one 201 and one 409 — never a 500 from the unique-index backstop escaping the advice.
        String[] names = {"Travel", "travel"};
        for (String name : names) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    int status = mockMvc.perform(post("/api/lists/categories")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"kind\":\"general\",\"name\":\"" + name + "\"}"))
                            .andReturn().getResponse().getStatus();
                    if (status == 201) created201.incrementAndGet();
                    else if (status == 409) conflict409.incrementAndGet();
                    else if (status >= 500) serverError5xx.incrementAndGet();
                    else otherStatus.incrementAndGet();
                } catch (Exception ex) {
                    otherStatus.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(serverError5xx.get()).as("no concurrent POST may escape as 5xx").isEqualTo(0);
        assertThat(otherStatus.get()).isEqualTo(0);
        assertThat(created201.get()).isEqualTo(1);
        assertThat(conflict409.get()).isEqualTo(1);

        // Exactly one row persisted
        assertThat(listCategoryRepository.countByFamilyAndKind(family, ListKind.GENERAL)).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // 6. Concurrent empty-catalog appends produce dense distinct sortOrders
    // -------------------------------------------------------------------------

    @Test
    void concurrentCreates_distinctNames_produceDenseDistinctSortOrders() throws Exception {
        String username = uniqueUsername();
        registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger unexpectedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final String name = "Category" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, name), family);
                    successCount.incrementAndGet();
                } catch (Exception ex) {
                    unexpectedCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(unexpectedCount.get()).isEqualTo(0);
        assertThat(successCount.get()).isEqualTo(threadCount);

        // All sort orders must be dense and distinct: 0..n-1
        var categories = listCategoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GENERAL);
        assertThat(categories).hasSize(threadCount);
        Set<Integer> sortOrders = categories.stream()
                .map(c -> c.getSortOrder())
                .collect(Collectors.toSet());
        assertThat(sortOrders).containsExactlyInAnyOrder(0, 1, 2, 3, 4);
    }

    // -------------------------------------------------------------------------
    // 7. Reorder race: stale expectedCategoryIds → 409, no lost/duplicated order
    // -------------------------------------------------------------------------

    @Test
    void reorderRace_staleBaseline_returns409_orderUnchanged() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        // Create 2 General categories
        var cat1 = listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Alpha"), family);
        var cat2 = listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Beta"), family);

        List<UUID> originalOrder = List.of(cat1.id(), cat2.id());

        // Thread 1 captures baseline [cat1, cat2]; target [cat2, cat1]
        // Meanwhile, a new category is added (making the baseline stale)
        var cat3 = listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Gamma"), family);

        // Now try to reorder with stale expectedCategoryIds → 409
        mockMvc.perform(put("/api/lists/categories/order")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind": "general",
                                  "expectedCategoryIds": ["%s", "%s"],
                                  "categoryIds": ["%s", "%s"]
                                }
                                """.formatted(cat1.id(), cat2.id(), cat2.id(), cat1.id())))
                .andExpect(status().isConflict());

        // Sort orders must be unchanged (0, 1, 2) — no corruption
        var cats = listCategoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GENERAL);
        List<UUID> actualOrder = cats.stream().map(c -> c.getId()).toList();
        List<Integer> actualSortOrders = cats.stream().map(c -> c.getSortOrder()).toList();
        assertThat(actualSortOrders).containsExactly(0, 1, 2);
        assertThat(actualOrder).containsExactly(cat1.id(), cat2.id(), cat3.id());
    }

    @Test
    void reorderContention_lockSerializedAgainstConcurrentCreate_returns409_orderIntact() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        var cat1 = listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Alpha"), family);
        var cat2 = listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Beta"), family);

        TransactionTemplate txTemplate = new TransactionTemplate(txManager);
        CountDownLatch aHoldsLock = new CountDownLatch(1);
        CountDownLatch aMayCommit = new CountDownLatch(1);
        CountDownLatch bDone = new CountDownLatch(1);
        AtomicReference<UUID> cat3Id = new AtomicReference<>();
        AtomicInteger bStatus = new AtomicInteger(-1);

        // Thread A opens a transaction, creates a 3rd category (acquiring + holding the (family, GENERAL)
        // scope lock), and parks with the transaction open so the lock stays held.
        Thread a = new Thread(() -> txTemplate.executeWithoutResult(status -> {
            var created = listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Gamma"), family);
            cat3Id.set(created.id());
            aHoldsLock.countDown();
            awaitLatch(aMayCommit);
        }));

        // Thread B submits a reorder whose baseline [cat1, cat2] is about to go stale. It must block on the
        // scope lock until A commits, then observe [cat1, cat2, cat3] and return 409 — never overwriting order.
        Thread b = new Thread(() -> {
            try {
                bStatus.set(mockMvc.perform(put("/api/lists/categories/order")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "kind": "general",
                                          "expectedCategoryIds": ["%s", "%s"],
                                          "categoryIds": ["%s", "%s"]
                                        }
                                        """.formatted(cat1.id(), cat2.id(), cat2.id(), cat1.id())))
                        .andReturn().getResponse().getStatus());
            } catch (Exception ex) {
                bStatus.set(-2);
            } finally {
                bDone.countDown();
            }
        });

        a.start();
        awaitLatch(aHoldsLock);   // A holds the scope lock with its create still uncommitted
        b.start();                // B contends for the same scope lock and blocks
        try {
            awaitScopeLockWaiter();
            assertThat(bDone.await(250, TimeUnit.MILLISECONDS))
                    .as("reorder request must remain blocked while the create transaction holds the scope lock")
                    .isFalse();
        } finally {
            aMayCommit.countDown();   // let A commit and release the lock
        }
        boolean bFinished = bDone.await(20, TimeUnit.SECONDS);
        a.join(5000);
        b.join(5000);

        assertThat(bFinished).isTrue();
        assertThat(bStatus.get()).as("reorder serialized after the create must see a stale baseline").isEqualTo(409);

        // The catalog is intact and dense: [cat1, cat2, cat3] at 0,1,2 — B wrote nothing.
        var cats = listCategoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GENERAL);
        assertThat(cats.stream().map(c -> c.getId()).toList())
                .containsExactly(cat1.id(), cat2.id(), cat3Id.get());
        assertThat(cats.stream().map(c -> c.getSortOrder()).toList()).containsExactly(0, 1, 2);
    }

    // -------------------------------------------------------------------------
    // 8. Reorder membership validation
    // -------------------------------------------------------------------------

    @Test
    void reorder_emptyCatalog_succeeds() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        // GENERAL catalog is empty — reorder with empty lists should succeed
        mockMvc.perform(put("/api/lists/categories/order")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind": "general",
                                  "expectedCategoryIds": [],
                                  "categoryIds": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories").isEmpty());
    }

    @Test
    void reorder_singleCategory_samePlacement_succeeds() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        var cat = listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Solo"), family);

        mockMvc.perform(put("/api/lists/categories/order")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind": "general",
                                  "expectedCategoryIds": ["%s"],
                                  "categoryIds": ["%s"]
                                }
                                """.formatted(cat.id(), cat.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0].id").value(cat.id().toString()));
    }

    @Test
    void reorder_malformedRequest_missingKind_returns400() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        UUID id = UUID.randomUUID();
        mockMvc.perform(put("/api/lists/categories/order")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedCategoryIds": ["%s"],
                                  "categoryIds": ["%s"]
                                }
                                """.formatted(id, id)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reorder_foreignFamilyBaseline_returns409_withoutRevealingExistence() throws Exception {
        String userA = uniqueUsername();
        String userB = uniqueUsername();
        String tokenA = registerAndGetToken(userA);
        String tokenB = registerAndGetToken(userB);
        Family familyA = familyRepository.findByUsername(userA).orElseThrow();

        // A creates a category that B must never be able to address.
        var catA = listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Secret"), familyA);

        // B's GENERAL catalog is EMPTY, so its current ids = []. B sends expectedCategoryIds=[catA.id()].
        // The stale-baseline check runs FIRST and unconditionally: currentIds([]) != expected([catA.id()])
        // → ConflictException → 409, every time (deterministic; membership validation is never reached).
        // The 409 is identical whether catA exists or not, so foreign existence/ownership is never revealed.
        mockMvc.perform(put("/api/lists/categories/order")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind": "general",
                                  "expectedCategoryIds": ["%s"],
                                  "categoryIds": ["%s"]
                                }
                                """.formatted(catA.id(), catA.id())))
                .andExpect(status().isConflict());
    }

    // -------------------------------------------------------------------------
    // 9. Static route precedence
    // -------------------------------------------------------------------------

    @Test
    void staticRoute_getCatalog_notCapturedByListIdEndpoint() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        // GET /api/lists/categories?kind=general must be handled by ListCategoryController
        // not confused with GET /api/lists/{id} where id=categories
        mockMvc.perform(get("/api/lists/categories")
                        .param("kind", "general")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kind").value("general"))
                .andExpect(jsonPath("$.data.categories").isArray());
    }

    @Test
    void staticRoute_putOrder_notCapturedByListIdEndpoint() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        // PUT /api/lists/categories/order must be handled by ListCategoryController
        // not confused with PUT /api/lists/{id} where id=categories/order
        mockMvc.perform(put("/api/lists/categories/order")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind": "general",
                                  "expectedCategoryIds": [],
                                  "categoryIds": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kind").value("general"));
    }

    @Test
    void getCatalog_fullRoundTrip_createRenameDelete() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        // Create a General category through the API
        String createBody = mockMvc.perform(post("/api/lists/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kind": "general", "name": "Travel Plans"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Travel Plans"))
                .andExpect(jsonPath("$.data.sortOrder").value(0))
                .andReturn().getResponse().getContentAsString();

        String catId = JsonPath.read(createBody, "$.data.id");

        // Catalog should reflect the new category
        mockMvc.perform(get("/api/lists/categories").param("kind", "general")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0].id").value(catId))
                .andExpect(jsonPath("$.data.categories[0].name").value("Travel Plans"));

        // Rename it
        mockMvc.perform(patch("/api/lists/categories/{id}", catId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Summer Travel"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Summer Travel"));

        // Delete it
        mockMvc.perform(delete("/api/lists/categories/{id}", catId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uncategorizedItemCount").value(0))
                .andExpect(jsonPath("$.data.flattenedListCount").value(0));

        // Catalog is empty again
        mockMvc.perform(get("/api/lists/categories").param("kind", "general")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories").isEmpty());
    }

    @Test
    void createCategory_duplicateName_returns409() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        // Create
        mockMvc.perform(post("/api/lists/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kind": "general", "name": "Hobbies"}
                                """))
                .andExpect(status().isCreated());

        // Duplicate (same name, case-insensitive collision)
        mockMvc.perform(post("/api/lists/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kind": "general", "name": "HOBBIES"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void getCatalog_invalidKind_returns400() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        mockMvc.perform(get("/api/lists/categories")
                        .param("kind", "invalid-kind")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid list kind."));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String toJsonArray(List<String> ids) {
        return "[" + ids.stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    private void awaitScopeLockWaiter() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            Integer waiters = jdbcTemplate.queryForObject("""
                    select count(*)
                    from pg_stat_activity
                    where wait_event_type = 'Lock'
                      and query ilike '%list_category_catalog_scope%'
                      and pid <> pg_backend_pid()
                    """, Integer.class);
            if (waiters != null && waiters > 0) {
                return;
            }
            sleepBriefly();
        }
        throw new AssertionError("Timed out waiting for a request blocked on the list category scope lock");
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for latch");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(25);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for lock contention", e);
        }
    }
}
