package com.familyhub.demo.integration;

import com.familyhub.demo.config.TestcontainersConfig;
import com.familyhub.demo.dto.CreateListCategoryRequest;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.ListKind;
import com.familyhub.demo.repository.FamilyRepository;
import com.familyhub.demo.repository.ListCategoryCatalogScopeRepository;
import com.familyhub.demo.repository.ListCategoryRepository;
import com.familyhub.demo.service.ListCategoryService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class ListIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private ListCategoryRepository listCategoryRepository;

    @Autowired
    private ListCategoryCatalogScopeRepository scopeRepository;

    @Autowired
    private ListCategoryService listCategoryService;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String uniqueUsername() {
        return "lists" + System.nanoTime();
    }

    private String registerAndGetToken(String username) throws Exception {
        String authBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "TestPassword123!",
                                  "familyName": "Lists Family",
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

    // -------------------------------------------------------------------------
    // Original end-to-end round-trip test
    // -------------------------------------------------------------------------

    @Test
    void listsFlow_roundTripsThroughAuthenticatedApi() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        mockMvc.perform(get("/api/lists/preferences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.showCompletedByDefault").value(true));

        mockMvc.perform(patch("/api/lists/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "showCompletedByDefault": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.showCompletedByDefault").value(false));

        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Trader Joe's Run",
                                  "kind": "grocery"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.kind").value("grocery"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String listId = JsonPath.read(listBody, "$.data.id");

        mockMvc.perform(get("/api/lists")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(listId))
                .andExpect(jsonPath("$.data[0].totalItems").value(0))
                .andExpect(jsonPath("$.data[0].completedItems").value(0));

        mockMvc.perform(patch("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryDisplayMode": "flat",
                                  "showCompletedOverride": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryDisplayMode").value("flat"))
                .andExpect(jsonPath("$.data.showCompletedOverride").value(true));

        mockMvc.perform(patch("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryDisplayMode": "flat",
                                  "showCompletedOverride": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.showCompletedOverride").value(nullValue()));

        String detailBody = mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0].name").value("Produce"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String produceCategoryId = JsonPath.read(detailBody, "$.data.categories[0].id");

        String bananasBody = mockMvc.perform(post("/api/lists/{id}/items", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Bananas",
                                  "categoryId": "%s"
                                }
                                """.formatted(produceCategoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.text").value("Bananas"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String bananasItemId = JsonPath.read(bananasBody, "$.data.id");

        String yogurtBody = mockMvc.perform(post("/api/lists/{id}/items", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Greek yogurt"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.text").value("Greek yogurt"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String yogurtItemId = JsonPath.read(yogurtBody, "$.data.id");

        mockMvc.perform(patch("/api/lists/{listId}/items/{itemId}", listId, bananasItemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Bananas",
                                  "completed": true,
                                  "categoryId": "%s"
                                }
                                """.formatted(produceCategoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        mockMvc.perform(delete("/api/lists/{listId}/items/{itemId}", listId, yogurtItemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/lists/{id}/clear-completed", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.removedCount").value(1));

        mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    // -------------------------------------------------------------------------
    // Bulk append: ordering + persistence, generic (non-grocery) support, rollback
    // -------------------------------------------------------------------------

    @Test
    void bulkAppend_ordersPersistsIsGeneric_andRollsBackOnBadCategory() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        // Grocery list (seeded categories → grouped).
        String groceryBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Groceries", "kind": "grocery"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String groceryListId = JsonPath.read(groceryBody, "$.data.id");

        // To-do list, plus one of its seeded TODO categories (wrong kind for the grocery list).
        String todoBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Chores", "kind": "to-do"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String todoListId = JsonPath.read(todoBody, "$.data.id");

        String todoDetail = mockMvc.perform(get("/api/lists/{id}", todoListId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String todoCategoryId = JsonPath.read(todoDetail, "$.data.categories[0].id");

        // Append two items to the grocery list. Strict request order is asserted on the RESPONSE.
        mockMvc.perform(post("/api/lists/{id}/items/bulk", groceryListId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "items": [ { "text": "2 chicken breasts" }, { "text": "1 tbsp olive oil" } ] }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].text").value("2 chicken breasts"))
                .andExpect(jsonPath("$.data[1].text").value("1 tbsp olive oil"));

        // GET-back asserts persistence by MEMBERSHIP/count, not strict order: same-batch items tie on
        // the non-unique createdAt, and @OrderBy("createdAt ASC, id ASC") only guarantees a stable
        // (deterministic) read order, not the original insertion order.
        mockMvc.perform(get("/api/lists/{id}", groceryListId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[*].text",
                        containsInAnyOrder("2 chicken breasts", "1 tbsp olive oil")));

        // Generic proof: the same endpoint appends to a to-do list (no grocery coupling).
        mockMvc.perform(post("/api/lists/{id}/items/bulk", todoListId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "items": [ { "text": "call plumber" } ] }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].text").value("call plumber"));

        // Rollback proof: a wrong-kind (TODO) category in the second item writes nothing.
        mockMvc.perform(post("/api/lists/{id}/items/bulk", groceryListId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "items": [ { "text": "should not persist" }, { "text": "bad", "categoryId": "%s" } ] }
                                """.formatted(todoCategoryId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/lists/{id}", groceryListId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2)); // still only the first two
    }

    @Test
    void bulkAppend_listNotInFamily_returns404() throws Exception {
        // Family A creates a grocery list.
        String tokenA = registerAndGetToken(uniqueUsername());
        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "A Groceries", "kind": "grocery"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String listId = JsonPath.read(listBody, "$.data.id");

        // Family B tries to bulk-append to A's list → 404 (cross-family not revealed).
        String tokenB = registerAndGetToken(uniqueUsername());
        mockMvc.perform(post("/api/lists/{id}/items/bulk", listId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "items": [ { "text": "milk" } ] }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void bulkAppend_ontoNonEmptyList_appendsAfterExistingItems() throws Exception {
        String token = registerAndGetToken(uniqueUsername());

        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Groceries", "kind": "grocery"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String listId = JsonPath.read(listBody, "$.data.id");

        // Pre-existing single item so the bulk append runs against a non-zero existingCount.
        mockMvc.perform(post("/api/lists/{id}/items", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "existing milk"}
                                """))
                .andExpect(status().isCreated());

        // (a) The bulk RESPONSE contains ONLY the newly-created items, in request order —
        //     the pre-existing item is not echoed back, proving the subList(existingCount, size) offset.
        mockMvc.perform(post("/api/lists/{id}/items/bulk", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "items": [ { "text": "2 chicken breasts" }, { "text": "1 tbsp olive oil" } ] }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].text").value("2 chicken breasts"))
                .andExpect(jsonPath("$.data[1].text").value("1 tbsp olive oil"));

        // (b) GET-back contains the pre-existing item PLUS the two new ones (membership/count).
        mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[*].text",
                        containsInAnyOrder("existing milk", "2 chicken breasts", "1 tbsp olive oil")));
    }

    // -------------------------------------------------------------------------
    // Registration seeds catalog scopes and starter categories
    // -------------------------------------------------------------------------

    @Test
    void registration_createsCatalogScopesForAllKinds() throws Exception {
        String username = uniqueUsername();
        registerAndGetToken(username);

        Family family = familyRepository.findByUsername(username).orElseThrow();
        long familyScopeCount = scopeRepository.findAll().stream()
                .filter(s -> s.getFamily().getId().equals(family.getId()))
                .count();
        assertThat(familyScopeCount).isEqualTo(3L);
    }

    @Test
    void registration_scopeKindsAreGroceryTodoGeneral() throws Exception {
        String username = uniqueUsername();
        registerAndGetToken(username);

        Family family = familyRepository.findByUsername(username).orElseThrow();
        var kinds = scopeRepository.findAll().stream()
                .filter(s -> s.getFamily().getId().equals(family.getId()))
                .map(s -> s.getKind())
                .toList();
        assertThat(kinds).containsExactlyInAnyOrder(ListKind.GROCERY, ListKind.TODO, ListKind.GENERAL);
    }

    @Test
    void registration_createsGroceryAndTodoStarterCategories_noGeneralStarters() throws Exception {
        String username = uniqueUsername();
        registerAndGetToken(username);

        Family family = familyRepository.findByUsername(username).orElseThrow();

        long groceryCount = listCategoryRepository.countByFamilyAndKind(family, ListKind.GROCERY);
        long todoCount = listCategoryRepository.countByFamilyAndKind(family, ListKind.TODO);
        long generalCount = listCategoryRepository.countByFamilyAndKind(family, ListKind.GENERAL);

        assertThat(groceryCount).isEqualTo(5L);  // Produce, Dairy, Pantry, Frozen, Household
        assertThat(todoCount).isEqualTo(3L);      // Urgent, Soon, Later
        assertThat(generalCount).isEqualTo(0L);   // No General starters
    }

    @Test
    void registration_isOneTime_loginDoesNotReseed() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        // Get a grocery list so we can see the seeded categories
        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Test List", "kind": "grocery"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String listId = JsonPath.read(listBody, "$.data.id");

        // Get the first category ID
        String detailBody = mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String catId = JsonPath.read(detailBody, "$.data.categories[0].id");
        String originalName = JsonPath.read(detailBody, "$.data.categories[0].name");

        // Rename via service (category rename via service; no controller yet)
        listCategoryService.rename(
                java.util.UUID.fromString(catId),
                new com.familyhub.demo.dto.RenameListCategoryRequest("Fresh Produce"),
                family
        );

        // Login again (triggers no reseed)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "TestPassword123!"}
                                """.formatted(username)))
                .andExpect(status().isOk());

        // Category still has the renamed name — no reseed happened
        mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0].name").value("Fresh Produce"));
    }

    // -------------------------------------------------------------------------
    // Grocery/To-do list creation display mode — based on catalog state
    // -------------------------------------------------------------------------

    @Test
    void createGroceryList_withSeededCategories_isGrouped() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        // Registration seeded Grocery categories → new grocery list should be GROUPED
        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "My Groceries", "kind": "grocery"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(listBody, "$.data.categoryDisplayMode")).isEqualTo("grouped");
    }

    @Test
    void createGeneralList_alwaysFlat() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Movies", "kind": "general"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(listBody, "$.data.categoryDisplayMode")).isEqualTo("flat");
    }

    // -------------------------------------------------------------------------
    // updateList GROUPED — empty-catalog 409 for all kinds
    // -------------------------------------------------------------------------

    @Test
    void updateList_groupedPatch_emptyCatalogGeneral_returns409() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        // Create a general list (no categories exist for GENERAL)
        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Notes", "kind": "general"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String listId = JsonPath.read(listBody, "$.data.id");

        // GROUPED PATCH with empty catalog → 409
        mockMvc.perform(patch("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryDisplayMode": "grouped"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void updateList_groupedPatch_generalWithCategories_succeeds() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        // Create a General list
        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Notes", "kind": "general"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String listId = JsonPath.read(listBody, "$.data.id");

        // Add a category to GENERAL via service
        listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Documents"), family);

        // Now GROUPED PATCH should succeed
        mockMvc.perform(patch("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryDisplayMode": "grouped"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryDisplayMode").value("grouped"));
    }

    // -------------------------------------------------------------------------
    // General item create/update accepts matching-kind category
    // -------------------------------------------------------------------------

    @Test
    void createItem_generalList_withGeneralCategory_succeeds() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        // Create General list
        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Movies", "kind": "general"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String listId = JsonPath.read(listBody, "$.data.id");

        // Add General category via service
        var catEntry = listCategoryService.create(
                new CreateListCategoryRequest(ListKind.GENERAL, "Action"), family);
        String catId = catEntry.id().toString();

        // Create item with category → should succeed
        mockMvc.perform(post("/api/lists/{id}/items", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Die Hard", "categoryId": "%s"}
                                """.formatted(catId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.text").value("Die Hard"));
    }

    @Test
    void createItem_generalList_withGroceryCategory_returns400() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        // Get a Grocery category from the seeded ones
        String groceryListBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Groceries", "kind": "grocery"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String groceryListId = JsonPath.read(groceryListBody, "$.data.id");

        String detailBody = mockMvc.perform(get("/api/lists/{id}", groceryListId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String groceryCatId = JsonPath.read(detailBody, "$.data.categories[0].id");

        // Create General list
        String generalListBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Notes", "kind": "general"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String generalListId = JsonPath.read(generalListBody, "$.data.id");

        // Try to assign a Grocery category to a General list item → 400
        mockMvc.perform(post("/api/lists/{id}/items", generalListId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Apples", "categoryId": "%s"}
                                """.formatted(groceryCatId)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // List detail exposes categories for ALL kinds, no seeded field
    // -------------------------------------------------------------------------

    @Test
    void listDetail_generalKind_exposesCategoriesWhenPresent() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        // Create General list
        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Notes", "kind": "general"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String listId = JsonPath.read(listBody, "$.data.id");

        // Add General category via service
        listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Documents"), family);

        // List detail should include the category
        mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0].name").value("Documents"))
                .andExpect(jsonPath("$.data.categories[0].id").isNotEmpty())
                .andExpect(jsonPath("$.data.categories[0].sortOrder").isNumber());
    }

    @Test
    void listDetail_categories_doNotHaveSeededField() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        // Grocery list with seeded categories from registration
        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "My Groceries", "kind": "grocery"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String listId = JsonPath.read(listBody, "$.data.id");

        // seeded field must NOT appear in the response
        mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0]").exists())
                .andExpect(jsonPath("$.data.categories[0].seeded").doesNotExist());
    }

    // -------------------------------------------------------------------------
    // Sequential race outcomes: delete-then-operation
    // -------------------------------------------------------------------------

    @Test
    void deleteLastCategory_thenGroupedPatch_returns409_andListIsFlat() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        // Create a General list
        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Notes", "kind": "general"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String listId = JsonPath.read(listBody, "$.data.id");

        // Add one category and switch list to GROUPED
        var catEntry = listCategoryService.create(
                new CreateListCategoryRequest(ListKind.GENERAL, "Documents"), family);

        mockMvc.perform(patch("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryDisplayMode": "grouped"}
                                """))
                .andExpect(status().isOk());

        // Delete the only category (service flattens grouped lists)
        listCategoryService.delete(catEntry.id(), family);

        // GROUPED PATCH must return 409 (empty catalog)
        mockMvc.perform(patch("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryDisplayMode": "grouped"}
                                """))
                .andExpect(status().isConflict());

        // List must now be flat (flattened by the delete)
        mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryDisplayMode").value("flat"));
    }

    @Test
    void deleteLastCategory_thenItemAssignment_returns404_andItemUnchanged() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        // Create a General list
        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Notes", "kind": "general"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String listId = JsonPath.read(listBody, "$.data.id");

        // Add a category
        var catEntry = listCategoryService.create(
                new CreateListCategoryRequest(ListKind.GENERAL, "Documents"), family);
        String catId = catEntry.id().toString();

        // Add an item (no category)
        String itemBody = mockMvc.perform(post("/api/lists/{id}/items", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Meeting notes"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String itemId = JsonPath.read(itemBody, "$.data.id");

        // Delete the category
        listCategoryService.delete(catEntry.id(), family);

        // Try to assign the now-deleted category to the item → 404
        mockMvc.perform(patch("/api/lists/{listId}/items/{itemId}", listId, itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Meeting notes", "completed": false, "categoryId": "%s"}
                                """.formatted(catId)))
                .andExpect(status().isNotFound());

        // Item should be unchanged (null category)
        mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].categoryId").value(nullValue()));
    }

    // -------------------------------------------------------------------------
    // Display-mode stability: creating/recreating a category must NOT change
    // existing lists' display modes (spec invariants D5/D6).
    // -------------------------------------------------------------------------

    @Test
    void creatingFirstCategory_doesNotRegroupExistingList() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        // Create a General list — it starts FLAT (no General categories exist)
        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Notes", "kind": "general"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.categoryDisplayMode").value("flat"))
                .andReturn().getResponse().getContentAsString();
        String listId = JsonPath.read(listBody, "$.data.id");

        // Create the FIRST General category
        listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Documents"), family);

        // Existing list must STILL be flat — creating a category does not auto-group existing lists
        mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryDisplayMode").value("flat"));
    }

    @Test
    void recreatingCategory_doesNotRegroupExistingFlattenedList() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        // Grocery list is GROUPED after registration (seeded grocery categories exist)
        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Groceries", "kind": "grocery"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.categoryDisplayMode").value("grouped"))
                .andReturn().getResponse().getContentAsString();
        String listId = JsonPath.read(listBody, "$.data.id");

        // Delete EVERY grocery category; the final delete flattens grouped lists
        var grocery = listCategoryRepository.findByFamilyAndKindOrderBySortOrderAsc(family, ListKind.GROCERY);
        for (var cat : grocery) {
            listCategoryService.delete(cat.getId(), family);
        }

        // List is now flat
        mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryDisplayMode").value("flat"));

        // Recreate a grocery category
        listCategoryService.create(new CreateListCategoryRequest(ListKind.GROCERY, "Produce"), family);

        // The previously-flattened list must STILL be flat — recreating does not regroup it
        mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryDisplayMode").value("flat"));
    }

    // -------------------------------------------------------------------------
    // updateItem with null categoryId clears an existing assignment
    // (PATCH-replace semantics: null selects "Uncategorized").
    // -------------------------------------------------------------------------

    @Test
    void updateItem_nullCategoryId_clearsExistingCategory() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        // Grocery list with seeded categories
        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Groceries", "kind": "grocery"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String listId = JsonPath.read(listBody, "$.data.id");

        String detailBody = mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String produceCategoryId = JsonPath.read(detailBody, "$.data.categories[0].id");

        // Create an item WITH a category
        String itemBody = mockMvc.perform(post("/api/lists/{id}/items", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Bananas", "categoryId": "%s"}
                                """.formatted(produceCategoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.categoryId").value(produceCategoryId))
                .andReturn().getResponse().getContentAsString();
        String itemId = JsonPath.read(itemBody, "$.data.id");

        // PATCH the item with categoryId: null → clears the assignment
        mockMvc.perform(patch("/api/lists/{listId}/items/{itemId}", listId, itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Bananas", "completed": false, "categoryId": null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryId").value(nullValue()));

        // Persisted item now has a null category
        mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].categoryId").value(nullValue()));
    }

    // -------------------------------------------------------------------------
    // updateItem validates the category BEFORE mutating the item: a bad category
    // rolls the whole request back, so a changed text is never persisted.
    // -------------------------------------------------------------------------

    @Test
    void updateItem_invalidCategory_returns404_andDoesNotPersistTextMutation() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);

        // Grocery list (seeded categories → grouped); grab a real category for the initial item.
        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Groceries", "kind": "grocery"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String listId = JsonPath.read(listBody, "$.data.id");

        String detailBody = mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String produceCategoryId = JsonPath.read(detailBody, "$.data.categories[0].id");

        String itemBody = mockMvc.perform(post("/api/lists/{id}/items", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Original", "categoryId": "%s"}
                                """.formatted(produceCategoryId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String itemId = JsonPath.read(itemBody, "$.data.id");

        // PATCH changes text + completion AND points at a non-existent category. The 404 must roll the
        // whole request back: category resolution runs before the item is mutated, so nothing persists.
        UUID missingCategoryId = UUID.randomUUID();
        mockMvc.perform(patch("/api/lists/{listId}/items/{itemId}", listId, itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Changed", "completed": true, "categoryId": "%s"}
                                """.formatted(missingCategoryId)))
                .andExpect(status().isNotFound());

        // The item is unchanged: original text, not completed, original category.
        mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].text").value("Original"))
                .andExpect(jsonPath("$.data.items[0].completed").value(false))
                .andExpect(jsonPath("$.data.items[0].categoryId").value(produceCategoryId));
    }

    // -------------------------------------------------------------------------
    // Lock-serialized contention: a grouped PATCH blocked behind an in-flight
    // final-category delete observes the post-commit empty catalog and returns 409.
    // -------------------------------------------------------------------------

    @Test
    void deleteLastCategory_concurrentGroupedPatch_lockSerialized_returns409_andListFlat() throws Exception {
        String username = uniqueUsername();
        String token = registerAndGetToken(username);
        Family family = familyRepository.findByUsername(username).orElseThrow();

        String listBody = mockMvc.perform(post("/api/lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Notes", "kind": "general"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String listId = JsonPath.read(listBody, "$.data.id");

        var cat = listCategoryService.create(new CreateListCategoryRequest(ListKind.GENERAL, "Solo"), family);

        mockMvc.perform(patch("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryDisplayMode": "grouped"}
                                """))
                .andExpect(status().isOk());

        TransactionTemplate txTemplate = new TransactionTemplate(txManager);
        CountDownLatch aHoldsLock = new CountDownLatch(1);
        CountDownLatch aMayCommit = new CountDownLatch(1);
        CountDownLatch bDone = new CountDownLatch(1);
        AtomicInteger bStatus = new AtomicInteger(-1);

        // Thread A deletes the only category inside an open transaction, holding the (family, GENERAL)
        // scope lock and (uncommitted) flattening the grouped list.
        Thread a = new Thread(() -> txTemplate.executeWithoutResult(status -> {
            listCategoryService.delete(cat.id(), family);
            aHoldsLock.countDown();
            awaitLatch(aMayCommit);
        }));

        // Thread B asks to group the list. It must block on the scope lock, then see the now-empty
        // catalog after A commits and return 409.
        Thread b = new Thread(() -> {
            try {
                bStatus.set(mockMvc.perform(patch("/api/lists/{id}", listId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"categoryDisplayMode": "grouped"}
                                        """))
                        .andReturn().getResponse().getStatus());
            } catch (Exception ex) {
                bStatus.set(-2);
            } finally {
                bDone.countDown();
            }
        });

        a.start();
        awaitLatch(aHoldsLock);   // A holds the scope lock with its final-delete uncommitted
        b.start();                // B contends for the same scope lock and blocks
        try {
            awaitScopeLockWaiter();
            assertThat(bDone.await(250, TimeUnit.MILLISECONDS))
                    .as("grouped PATCH must remain blocked while the final delete transaction holds the scope lock")
                    .isFalse();
        } finally {
            aMayCommit.countDown();   // let A commit and release the lock
        }
        boolean bFinished = bDone.await(20, TimeUnit.SECONDS);
        a.join(5000);
        b.join(5000);

        assertThat(bFinished).isTrue();
        assertThat(bStatus.get())
                .as("grouped PATCH serialized behind the final delete sees an empty catalog")
                .isEqualTo(409);

        // The list was flattened by the committed delete and stays flat.
        mockMvc.perform(get("/api/lists/{id}", listId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryDisplayMode").value("flat"));
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
