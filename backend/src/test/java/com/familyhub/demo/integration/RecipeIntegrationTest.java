package com.familyhub.demo.integration;

import com.familyhub.demo.config.TestcontainersConfig;
import com.familyhub.demo.service.ImportedRecipe;
import com.familyhub.demo.service.RecipeImportService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class RecipeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeImportService recipeImportService;

    private String uniqueUsername() {
        return "r" + System.nanoTime();
    }

    @Test
    void recipesFlow_roundTripsThroughAuthenticatedApiAndRealPersistence() throws Exception {
        String token = registerAndToken(uniqueUsername(), "Recipes Family");
        String otherToken = registerAndToken(uniqueUsername(), "Other Recipes Family");

        String createBody = mockMvc.perform(post("/api/recipes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Quick Oats"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Quick Oats"))
                .andExpect(jsonPath("$.data.favorite").value(false))
                .andExpect(jsonPath("$.data.ingredients").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String recipeId = JsonPath.read(createBody, "$.data.id");

        mockMvc.perform(post("/api/recipes")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Other Soup"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Other Soup"));

        mockMvc.perform(get("/api/recipes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(recipeId))
                .andExpect(jsonPath("$.data[0].title").value("Quick Oats"))
                .andExpect(jsonPath("$.data[0].favorite").value(false));

        String listBody = mockMvc.perform(get("/api/recipes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(listBody).contains("Quick Oats");
        assertThat(listBody).doesNotContain("Other Soup");

        mockMvc.perform(get("/api/recipes/{id}", recipeId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/recipes/{id}", recipeId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "favorite": true
                                }
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/recipes/{id}", recipeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Better Oats",
                                  "imageUrl": "https://cdn.example.com/oats.jpg",
                                  "ingredients": ["1 cup oats", "2 cups milk"],
                                  "instructions": ["Combine", "Simmer"],
                                  "note": "Breakfast standby",
                                  "sourceUrl": "https://example.com/oats",
                                  "tags": ["breakfast", "quick"],
                                  "favorite": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Better Oats"))
                .andExpect(jsonPath("$.data.ingredients[0]").value("1 cup oats"))
                .andExpect(jsonPath("$.data.ingredients[1]").value("2 cups milk"))
                .andExpect(jsonPath("$.data.instructions[0]").value("Combine"))
                .andExpect(jsonPath("$.data.instructions[1]").value("Simmer"))
                .andExpect(jsonPath("$.data.tags[0]").value("breakfast"))
                .andExpect(jsonPath("$.data.tags[1]").value("quick"))
                .andExpect(jsonPath("$.data.favorite").value(true));

        mockMvc.perform(patch("/api/recipes/{id}", recipeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "favorite": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Better Oats"))
                .andExpect(jsonPath("$.data.favorite").value(false));

        given(recipeImportService.importFromUrl("https://example.com/tacos"))
                .willReturn(new ImportedRecipe(
                        "Weeknight Tacos",
                        "https://cdn.example.com/tacos.jpg",
                        List.of("1 lb beef", "8 tortillas"),
                        List.of("Brown beef", "Serve in tortillas"),
                        null,
                        "https://example.com/tacos",
                        List.of("dinner"),
                        false
                ));

        String importBody = mockMvc.perform(post("/api/recipes/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com/tacos"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Weeknight Tacos"))
                .andExpect(jsonPath("$.data.ingredients[0]").value("1 lb beef"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String importedRecipeId = JsonPath.read(importBody, "$.data.id");

        mockMvc.perform(get("/api/recipes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(importedRecipeId))
                .andExpect(jsonPath("$.data[0].tags[0]").value("dinner"));
    }

    private String registerAndToken(String username, String familyName) throws Exception {
        String authBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "TestPassword123!",
                                  "familyName": "%s",
                                  "members": [
                                    { "name": "Alice", "color": "coral" }
                                  ]
                                }
                                """.formatted(username, familyName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(authBody, "$.data.token");
    }
}
