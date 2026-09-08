package com.familyhub.demo.controller;

import com.familyhub.demo.config.SecurityConfig;
import com.familyhub.demo.dto.CategoryDeleteResult;
import com.familyhub.demo.dto.ListCategoryCatalogResponse;
import com.familyhub.demo.dto.ListCategoryManagementEntry;
import com.familyhub.demo.model.ListKind;
import com.familyhub.demo.security.JwtAuthenticationEntryPoint;
import com.familyhub.demo.security.JwtAuthenticationFilter;
import com.familyhub.demo.security.WithMockFamily;
import com.familyhub.demo.service.FamilyService;
import com.familyhub.demo.service.JwtService;
import com.familyhub.demo.service.ListCategoryService;
import com.familyhub.demo.exception.ConflictException;
import com.familyhub.demo.exception.ResourceNotFoundException;
import com.familyhub.demo.model.Family;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.familyhub.demo.TestDataFactory.LIST_CATEGORY_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ListCategoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class ListCategoryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    FamilyService familyService;

    @MockitoBean
    ListCategoryService listCategoryService;

    // -------------------------------------------------------------------------
    // GET /api/lists/categories?kind=...
    // -------------------------------------------------------------------------

    @Test
    @WithMockFamily
    void getCatalog_grocery_returns200() throws Exception {
        ListCategoryCatalogResponse response = sampleCatalog(ListKind.GROCERY);
        given(listCategoryService.getCatalog(eq(ListKind.GROCERY), any(Family.class))).willReturn(response);

        mockMvc.perform(get("/api/lists/categories").param("kind", "grocery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kind").value("grocery"))
                .andExpect(jsonPath("$.data.categories[0].name").value("Produce"));
    }

    @Test
    @WithMockFamily
    void getCatalog_todo_returns200() throws Exception {
        ListCategoryCatalogResponse response = sampleCatalog(ListKind.TODO);
        given(listCategoryService.getCatalog(eq(ListKind.TODO), any(Family.class))).willReturn(response);

        mockMvc.perform(get("/api/lists/categories").param("kind", "to-do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kind").value("to-do"));
    }

    @Test
    @WithMockFamily
    void getCatalog_general_returns200() throws Exception {
        ListCategoryCatalogResponse response = sampleCatalog(ListKind.GENERAL);
        given(listCategoryService.getCatalog(eq(ListKind.GENERAL), any(Family.class))).willReturn(response);

        mockMvc.perform(get("/api/lists/categories").param("kind", "general"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kind").value("general"));
    }

    @Test
    @WithMockFamily
    void getCatalog_missingKind_returns400() throws Exception {
        mockMvc.perform(get("/api/lists/categories"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockFamily
    void getCatalog_invalidKind_returns400() throws Exception {
        mockMvc.perform(get("/api/lists/categories").param("kind", "foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid list kind."));
    }

    // -------------------------------------------------------------------------
    // POST /api/lists/categories
    // -------------------------------------------------------------------------

    @Test
    @WithMockFamily
    void createCategory_returns201WithLocation() throws Exception {
        ListCategoryManagementEntry entry = sampleManagementEntry();
        given(listCategoryService.create(any(), any(Family.class))).willReturn(entry);

        mockMvc.perform(post("/api/lists/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind": "grocery",
                                  "name": "Produce"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/lists/categories/" + LIST_CATEGORY_ID))
                .andExpect(jsonPath("$.data.id").value(LIST_CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("Produce"))
                .andExpect(jsonPath("$.message").value("Category created successfully"));
    }

    @Test
    @WithMockFamily
    void createCategory_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/lists/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind": "grocery",
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockFamily
    void createCategory_missingKind_returns400() throws Exception {
        mockMvc.perform(post("/api/lists/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Produce"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockFamily
    void createCategory_invalidKindInBody_returns400() throws Exception {
        // An unknown kind in the request body fails Jackson enum binding (HttpMessageNotReadable),
        // which must surface as 400 — not 500 via the generic handler.
        mockMvc.perform(post("/api/lists/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind": "banana",
                                  "name": "Produce"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockFamily
    void createCategory_nameWithinLimitAfterTrim_returns201() throws Exception {
        given(listCategoryService.create(any(), any(Family.class))).willReturn(sampleManagementEntry());

        // 100 visible chars wrapped in whitespace: 102 raw but 100 after trim, so it satisfies the
        // "trimmed name at most 100 chars" contract and must not be rejected by @Size.
        String paddedName = "  " + "A".repeat(100) + "  ";
        mockMvc.perform(post("/api/lists/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"grocery\",\"name\":\"" + paddedName + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockFamily
    void createCategory_nameOver100AfterTrim_returns400() throws Exception {
        String tooLong = "A".repeat(101);
        mockMvc.perform(post("/api/lists/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"grocery\",\"name\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockFamily
    void createCategory_conflictException_returns409() throws Exception {
        given(listCategoryService.create(any(), any(Family.class)))
                .willThrow(new ConflictException("A category with this name already exists."));

        mockMvc.perform(post("/api/lists/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind": "grocery",
                                  "name": "Produce"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.httpStatus").value(409))
                .andExpect(jsonPath("$.message").value("A category with this name already exists."));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/lists/categories/{categoryId}
    // -------------------------------------------------------------------------

    @Test
    @WithMockFamily
    void renameCategory_returns200() throws Exception {
        ListCategoryManagementEntry entry = sampleManagementEntry();
        given(listCategoryService.rename(eq(LIST_CATEGORY_ID), any(), any(Family.class))).willReturn(entry);

        mockMvc.perform(patch("/api/lists/categories/{id}", LIST_CATEGORY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Fresh Produce"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(LIST_CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.message").value("Category renamed successfully"));
    }

    @Test
    @WithMockFamily
    void renameCategory_blankName_returns400() throws Exception {
        mockMvc.perform(patch("/api/lists/categories/{id}", LIST_CATEGORY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockFamily
    void renameCategory_notFound_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        given(listCategoryService.rename(eq(unknownId), any(), any(Family.class)))
                .willThrow(new ResourceNotFoundException("List Category", unknownId));

        mockMvc.perform(patch("/api/lists/categories/{id}", unknownId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Does Not Matter"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.httpStatus").value(404));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/lists/categories/{categoryId}
    // -------------------------------------------------------------------------

    @Test
    @WithMockFamily
    void deleteCategory_returns200WithDeleteResult() throws Exception {
        given(listCategoryService.delete(eq(LIST_CATEGORY_ID), any(Family.class)))
                .willReturn(new CategoryDeleteResult(3L, 1L));

        mockMvc.perform(delete("/api/lists/categories/{id}", LIST_CATEGORY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uncategorizedItemCount").value(3))
                .andExpect(jsonPath("$.data.flattenedListCount").value(1))
                .andExpect(jsonPath("$.message").value("Category deleted successfully"));
    }

    @Test
    @WithMockFamily
    void deleteCategory_notFound_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        given(listCategoryService.delete(eq(unknownId), any(Family.class)))
                .willThrow(new ResourceNotFoundException("List Category", unknownId));

        mockMvc.perform(delete("/api/lists/categories/{id}", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.httpStatus").value(404));
    }

    // -------------------------------------------------------------------------
    // PUT /api/lists/categories/order
    // -------------------------------------------------------------------------

    @Test
    @WithMockFamily
    void reorderCategories_returns200() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ListCategoryCatalogResponse response = new ListCategoryCatalogResponse(
                ListKind.GROCERY, 2L,
                List.of(
                        new ListCategoryManagementEntry(id2, ListKind.GROCERY, "Dairy", 0, 1L),
                        new ListCategoryManagementEntry(id1, ListKind.GROCERY, "Produce", 1, 2L)
                )
        );
        given(listCategoryService.reorder(any(), any(Family.class))).willReturn(response);

        mockMvc.perform(put("/api/lists/categories/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind": "grocery",
                                  "expectedCategoryIds": ["%s", "%s"],
                                  "categoryIds": ["%s", "%s"]
                                }
                                """.formatted(id1, id2, id2, id1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kind").value("grocery"))
                .andExpect(jsonPath("$.data.categories[0].name").value("Dairy"))
                .andExpect(jsonPath("$.message").value("Category order updated successfully"));
    }

    @Test
    @WithMockFamily
    void reorderCategories_conflictStaleBaseline_returns409() throws Exception {
        given(listCategoryService.reorder(any(), any(Family.class)))
                .willThrow(new ConflictException("Categories changed since you last loaded them."));

        UUID id1 = UUID.randomUUID();
        mockMvc.perform(put("/api/lists/categories/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind": "grocery",
                                  "expectedCategoryIds": ["%s"],
                                  "categoryIds": ["%s"]
                                }
                                """.formatted(id1, id1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.httpStatus").value(409));
    }

    @Test
    @WithMockFamily
    void reorderCategories_missingKind_returns400() throws Exception {
        UUID id1 = UUID.randomUUID();
        mockMvc.perform(put("/api/lists/categories/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedCategoryIds": ["%s"],
                                  "categoryIds": ["%s"]
                                }
                                """.formatted(id1, id1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockFamily
    void reorderCategories_invalidKindInBody_returns400() throws Exception {
        // Unknown kind in the body must fail enum binding as 400, not 500.
        UUID id1 = UUID.randomUUID();
        mockMvc.perform(put("/api/lists/categories/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind": "banana",
                                  "expectedCategoryIds": ["%s"],
                                  "categoryIds": ["%s"]
                                }
                                """.formatted(id1, id1)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Unauthenticated → 401
    // -------------------------------------------------------------------------

    @Test
    void getCatalog_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/lists/categories").param("kind", "grocery"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.httpStatus").value(401));
    }

    @Test
    void createCategory_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/lists/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kind":"grocery","name":"Produce"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ListCategoryCatalogResponse sampleCatalog(ListKind kind) {
        return new ListCategoryCatalogResponse(
                kind, 2L,
                List.of(new ListCategoryManagementEntry(LIST_CATEGORY_ID, kind, "Produce", 0, 5L))
        );
    }

    private ListCategoryManagementEntry sampleManagementEntry() {
        return new ListCategoryManagementEntry(LIST_CATEGORY_ID, ListKind.GROCERY, "Produce", 0, 5L);
    }
}
