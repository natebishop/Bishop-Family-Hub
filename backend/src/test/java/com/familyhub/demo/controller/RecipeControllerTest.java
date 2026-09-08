package com.familyhub.demo.controller;

import com.familyhub.demo.config.SecurityConfig;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.security.JwtAuthenticationEntryPoint;
import com.familyhub.demo.security.JwtAuthenticationFilter;
import com.familyhub.demo.security.WithMockFamily;
import com.familyhub.demo.service.FamilyService;
import com.familyhub.demo.service.JwtService;
import com.familyhub.demo.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.familyhub.demo.TestDataFactory.RECIPE_ID;
import static com.familyhub.demo.TestDataFactory.sampleRecipeDetailResponse;
import static com.familyhub.demo.TestDataFactory.sampleRecipeSummaryResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class RecipeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    FamilyService familyService;

    @MockitoBean
    RecipeService recipeService;

    @Test
    @WithMockFamily
    void getRecipes_returns200() throws Exception {
        given(recipeService.getRecipes(any(Family.class))).willReturn(List.of(sampleRecipeSummaryResponse()));

        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Sheet Pan Gnocchi"))
                .andExpect(jsonPath("$.data[0].favorite").value(true))
                .andExpect(jsonPath("$.data[0].tags[0]").value("dinner"));
    }

    @Test
    @WithMockFamily
    void getRecipe_returnsCookFirstDetail() throws Exception {
        given(recipeService.getRecipe(eq(RECIPE_ID), any(Family.class))).willReturn(sampleRecipeDetailResponse());

        mockMvc.perform(get("/api/recipes/{id}", RECIPE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUrl").value("https://cdn.example.com/gnocchi.jpg"))
                .andExpect(jsonPath("$.data.title").value("Sheet Pan Gnocchi"))
                .andExpect(jsonPath("$.data.ingredients[0]").value("1 lb shelf-stable gnocchi"))
                .andExpect(jsonPath("$.data.instructions[0]").value("Heat oven to 425F"))
                .andExpect(jsonPath("$.data.note").value("Weeknight favorite"))
                .andExpect(jsonPath("$.data.sourceUrl").value("https://example.com/gnocchi"))
                .andExpect(jsonPath("$.data.favorite").value(true));
    }

    @Test
    @WithMockFamily
    void createRecipe_returns201WithLocationHeader() throws Exception {
        given(recipeService.createRecipe(any(), any(Family.class))).willReturn(sampleRecipeDetailResponse());

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Sheet Pan Gnocchi"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/recipes/" + RECIPE_ID))
                .andExpect(jsonPath("$.message").value("Recipe created successfully"));
    }

    @Test
    @WithMockFamily
    void updateRecipe_returns200() throws Exception {
        given(recipeService.updateRecipe(eq(RECIPE_ID), any(), any(Family.class))).willReturn(sampleRecipeDetailResponse());

        mockMvc.perform(patch("/api/recipes/{id}", RECIPE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "favorite": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Recipe updated successfully"))
                .andExpect(jsonPath("$.data.favorite").value(true));
    }

    @Test
    @WithMockFamily
    void importRecipe_returns201() throws Exception {
        given(recipeService.importRecipe(any(), any(Family.class))).willReturn(sampleRecipeDetailResponse());

        mockMvc.perform(post("/api/recipes/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com/gnocchi"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/recipes/" + RECIPE_ID))
                .andExpect(jsonPath("$.message").value("Recipe imported successfully"))
                .andExpect(jsonPath("$.data.title").value("Sheet Pan Gnocchi"));
    }

    @Test
    void getRecipes_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.httpStatus").value(401));
    }
}
