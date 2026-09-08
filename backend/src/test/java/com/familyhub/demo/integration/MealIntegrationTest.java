package com.familyhub.demo.integration;

import com.familyhub.demo.config.TestcontainersConfig;
import com.familyhub.demo.repository.RecipeRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

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
class MealIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecipeRepository recipeRepository;

    @Test
    void mealsFlow_roundTripsThroughAuthenticatedApiAndRealPersistence() throws Exception {
        String token = registerAndToken(uniqueUsername(), "Meals Family");
        String otherToken = registerAndToken(uniqueUsername(), "Other Meals Family");

        String createBatchRecipeBody = mockMvc.perform(post("/api/recipes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Batch Enchiladas",
                                  "imageUrl": "https://cdn.example.com/enchiladas.jpg",
                                  "note": "Freeze half"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String batchRecipeId = JsonPath.read(createBatchRecipeBody, "$.data.id");

        mockMvc.perform(post("/api/meals/plans")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-14",
                                  "slots": [
                                    {
                                      "dayIndex": 1,
                                      "mealType": "dinner",
                                      "primary": {
                                        "sourceType": "recipe",
                                        "recipeId": "%s",
                                        "title": null,
                                        "imageUrl": null,
                                        "note": null
                                      },
                                      "extras": [],
                                      "note": null
                                    }
                                  ]
                                }
                                """.formatted(batchRecipeId)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/meals/plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-14",
                                  "slots": [
                                    {
                                      "dayIndex": 0,
                                      "mealType": "dinner",
                                      "primary": {
                                        "sourceType": "quick",
                                        "recipeId": null,
                                        "title": "Sheet Pan Chicken",
                                        "imageUrl": null,
                                        "note": "Use peppers"
                                      },
                                      "extras": [
                                        {
                                          "sourceType": "quick",
                                          "recipeId": null,
                                          "title": "Rice",
                                          "imageUrl": null,
                                          "note": null
                                        }
                                      ],
                                      "note": "Batch Monday dinner"
                                    },
                                    {
                                      "dayIndex": 1,
                                      "mealType": "dinner",
                                      "primary": {
                                        "sourceType": "recipe",
                                        "recipeId": "%s",
                                        "title": null,
                                        "imageUrl": null,
                                        "note": null
                                      },
                                      "extras": [],
                                      "note": null
                                    }
                                  ]
                                }
                                """.formatted(batchRecipeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Meal plan saved successfully"))
                .andExpect(jsonPath("$.data.weekStartDate").value("2026-06-14"))
                .andExpect(jsonPath("$.data.days.length()").value(7))
                .andExpect(jsonPath("$.data.days[0].slots[0].mealType").value("breakfast"))
                .andExpect(jsonPath("$.data.days[0].slots[1].mealType").value("lunch"))
                .andExpect(jsonPath("$.data.days[0].slots[2].mealType").value("dinner"))
                .andExpect(jsonPath("$.data.days[0].slots[2].primary.sourceType").value("quick"))
                .andExpect(jsonPath("$.data.days[0].slots[2].primary.title").value("Sheet Pan Chicken"))
                .andExpect(jsonPath("$.data.days[0].slots[2].primary.note").value("Use peppers"))
                .andExpect(jsonPath("$.data.days[0].slots[2].extras[0].title").value("Rice"))
                .andExpect(jsonPath("$.data.days[0].slots[2].note").value("Batch Monday dinner"))
                .andExpect(jsonPath("$.data.days[1].slots[2].primary.sourceType").value("recipe"))
                .andExpect(jsonPath("$.data.days[1].slots[2].primary.title").value("Batch Enchiladas"))
                .andExpect(jsonPath("$.data.days[1].slots[2].primary.imageUrl").value("https://cdn.example.com/enchiladas.jpg"))
                .andExpect(jsonPath("$.data.days[1].slots[2].primary.note").value("Freeze half"));

        mockMvc.perform(get("/api/meals/board")
                        .header("Authorization", "Bearer " + otherToken)
                        .param("weekStartDate", "2026-06-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[0].slots[2].primary").doesNotExist())
                .andExpect(jsonPath("$.data.days[1].slots[2].primary").doesNotExist());

        mockMvc.perform(post("/api/meals/plans")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-14",
                                  "slots": [
                                    {
                                      "dayIndex": 0,
                                      "mealType": "dinner",
                                      "primary": {
                                        "sourceType": "quick",
                                        "recipeId": null,
                                        "title": "Other Family Chili",
                                        "imageUrl": null,
                                        "note": null
                                      },
                                      "extras": [],
                                      "note": null
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[0].slots[2].primary.title").value("Other Family Chili"));

        mockMvc.perform(get("/api/meals/board")
                        .header("Authorization", "Bearer " + token)
                        .param("weekStartDate", "2026-06-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[0].slots[2].primary.title").value("Sheet Pan Chicken"))
                .andExpect(jsonPath("$.data.days[1].slots[2].primary.title").value("Batch Enchiladas"));

        mockMvc.perform(patch("/api/recipes/{id}", batchRecipeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Batch Enchiladas Updated",
                                  "imageUrl": "https://cdn.example.com/enchiladas-updated.jpg",
                                  "note": "Updated batch note"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/meals/board")
                        .header("Authorization", "Bearer " + token)
                        .param("weekStartDate", "2026-06-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[1].slots[2].primary.title").value("Batch Enchiladas"))
                .andExpect(jsonPath("$.data.days[1].slots[2].primary.imageUrl").value("https://cdn.example.com/enchiladas.jpg"))
                .andExpect(jsonPath("$.data.days[1].slots[2].primary.note").value("Freeze half"));

        mockMvc.perform(post("/api/meals/plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-14",
                                  "slots": [
                                    {
                                      "dayIndex": 0,
                                      "mealType": "dinner",
                                      "primary": {
                                        "sourceType": "quick",
                                        "recipeId": null,
                                        "title": "Should Not Replace",
                                        "imageUrl": null,
                                        "note": null
                                      },
                                      "extras": [],
                                      "note": null
                                    },
                                    {
                                      "dayIndex": 2,
                                      "mealType": "dinner",
                                      "primary": {
                                        "sourceType": "quick",
                                        "recipeId": null,
                                        "title": "Should Not Save",
                                        "imageUrl": null,
                                        "note": null
                                      },
                                      "extras": [],
                                      "note": null
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Some meal slots are no longer empty."));

        mockMvc.perform(get("/api/meals/board")
                        .header("Authorization", "Bearer " + token)
                        .param("weekStartDate", "2026-06-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[0].slots[2].primary.title").value("Sheet Pan Chicken"))
                .andExpect(jsonPath("$.data.days[2].slots[2].primary").doesNotExist());

        mockMvc.perform(get("/api/meals/board")
                        .header("Authorization", "Bearer " + token)
                        .param("weekStartDate", "2026-06-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days.length()").value(7))
                .andExpect(jsonPath("$.data.days[0].slots[0].mealType").value("breakfast"))
                .andExpect(jsonPath("$.data.days[0].slots[1].mealType").value("lunch"))
                .andExpect(jsonPath("$.data.days[0].slots[2].mealType").value("dinner"))
                .andExpect(jsonPath("$.data.days[0].slots[2].primary").doesNotExist());

        mockMvc.perform(put("/api/meals/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "dayIndex": 1,
                                  "mealType": "dinner",
                                  "primary": {
                                    "sourceType": "quick",
                                    "recipeId": null,
                                    "title": "Leftovers",
                                    "imageUrl": null,
                                    "note": "Use the glass containers"
                                  },
                                  "extras": [
                                    {
                                      "sourceType": "quick",
                                      "recipeId": null,
                                      "title": "Salad",
                                      "imageUrl": null,
                                      "note": null
                                    }
                                  ],
                                  "note": "Monday dinner note",
                                  "collisionMode": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primary.title").value("Leftovers"))
                .andExpect(jsonPath("$.data.extras[0].title").value("Salad"));

        String createRecipeBody = mockMvc.perform(post("/api/recipes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Original Tacos",
                                  "imageUrl": "https://cdn.example.com/tacos.jpg",
                                  "note": "Use corn tortillas"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String recipeId = JsonPath.read(createRecipeBody, "$.data.id");

        mockMvc.perform(put("/api/meals/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "dayIndex": 2,
                                  "mealType": "lunch",
                                  "primary": {
                                    "sourceType": "recipe",
                                    "recipeId": "%s",
                                    "title": null,
                                    "imageUrl": null,
                                    "note": null
                                  },
                                  "extras": [],
                                  "note": null,
                                  "collisionMode": null
                                }
                                """.formatted(recipeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primary.sourceType").value("recipe"))
                .andExpect(jsonPath("$.data.primary.title").value("Original Tacos"))
                .andExpect(jsonPath("$.data.primary.imageUrl").value("https://cdn.example.com/tacos.jpg"));

        mockMvc.perform(patch("/api/recipes/{id}", recipeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Tacos",
                                  "imageUrl": "https://cdn.example.com/updated.jpg",
                                  "note": "Updated note"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/meals/board")
                        .header("Authorization", "Bearer " + token)
                        .param("weekStartDate", "2026-06-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[2].slots[1].primary.title").value("Original Tacos"))
                .andExpect(jsonPath("$.data.days[2].slots[1].primary.imageUrl").value("https://cdn.example.com/tacos.jpg"))
                .andExpect(jsonPath("$.data.days[2].slots[1].primary.note").value("Use corn tortillas"));

        mockMvc.perform(put("/api/meals/slots")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "dayIndex": 0,
                                  "mealType": "dinner",
                                  "primary": {
                                    "sourceType": "recipe",
                                    "recipeId": "%s",
                                    "title": null,
                                    "imageUrl": null,
                                    "note": null
                                  },
                                  "extras": [],
                                  "note": null,
                                  "collisionMode": null
                                }
                                """.formatted(recipeId)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/meals/slots/move")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceWeekStartDate": "2026-06-07",
                                  "sourceDayIndex": 1,
                                  "sourceMealType": "dinner",
                                  "destinationWeekStartDate": "2026-06-07",
                                  "destinationDayIndex": 3,
                                  "destinationMealType": "dinner",
                                  "collisionMode": "replace_primary"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[1].slots[2].primary").doesNotExist())
                .andExpect(jsonPath("$.data.days[3].slots[2].primary.title").value("Leftovers"))
                .andExpect(jsonPath("$.data.days[3].slots[2].extras[0].title").value("Salad"))
                .andExpect(jsonPath("$.data.days[3].slots[2].note").value("Monday dinner note"));

        mockMvc.perform(post("/api/meals/slots/duplicate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceWeekStartDate": "2026-06-07",
                                  "sourceDayIndex": 3,
                                  "sourceMealType": "dinner",
                                  "destinationWeekStartDate": "2026-06-07",
                                  "destinationDayIndex": 4,
                                  "destinationMealType": "dinner",
                                  "collisionMode": "replace_primary"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[3].slots[2].primary.title").value("Leftovers"))
                .andExpect(jsonPath("$.data.days[4].slots[2].primary.title").value("Leftovers"))
                .andExpect(jsonPath("$.data.days[4].slots[2].extras[0].title").value("Salad"));

        mockMvc.perform(put("/api/meals/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "dayIndex": 5,
                                  "mealType": "dinner",
                                  "primary": {
                                    "sourceType": "quick",
                                    "recipeId": null,
                                    "title": "Pizza",
                                    "imageUrl": null,
                                    "note": null
                                  },
                                  "extras": [],
                                  "note": null,
                                  "collisionMode": null
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/meals/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "dayIndex": 5,
                                  "mealType": "dinner",
                                  "primary": {
                                    "sourceType": "quick",
                                    "recipeId": null,
                                    "title": "No Collision Mode",
                                    "imageUrl": null,
                                    "note": null
                                  },
                                  "extras": [],
                                  "note": null,
                                  "collisionMode": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/meals/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "dayIndex": 5,
                                  "mealType": "dinner",
                                  "primary": {
                                    "sourceType": "quick",
                                    "recipeId": null,
                                    "title": "Tacos",
                                    "imageUrl": null,
                                    "note": null
                                  },
                                  "extras": [],
                                  "note": null,
                                  "collisionMode": "replace_primary"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primary.title").value("Tacos"));

        mockMvc.perform(post("/api/meals/slots/duplicate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceWeekStartDate": "2026-06-07",
                                  "sourceDayIndex": 4,
                                  "sourceMealType": "dinner",
                                  "destinationWeekStartDate": "2026-06-07",
                                  "destinationDayIndex": 5,
                                  "destinationMealType": "dinner",
                                  "collisionMode": "add_as_extra"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[5].slots[2].primary.title").value("Tacos"))
                .andExpect(jsonPath("$.data.days[5].slots[2].extras[0].title").value("Leftovers"))
                .andExpect(jsonPath("$.data.days[5].slots[2].extras[1].title").value("Salad"));

        mockMvc.perform(delete("/api/meals/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "dayIndex": 5,
                                  "mealType": "dinner"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[5].slots[2].primary").doesNotExist());

        mockMvc.perform(delete("/api/meals/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "dayIndex": 5,
                                  "mealType": "dinner"
                                }
                                """))
                .andExpect(status().isNotFound());

        recipeRepository.deleteById(UUID.fromString(recipeId));
        recipeRepository.flush();

        String boardAfterDelete = mockMvc.perform(get("/api/meals/board")
                        .header("Authorization", "Bearer " + token)
                        .param("weekStartDate", "2026-06-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[2].slots[1].primary.title").value("Original Tacos"))
                .andExpect(jsonPath("$.data.days[2].slots[1].primary.recipeId").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(boardAfterDelete).contains("Original Tacos");
    }

    private String uniqueUsername() {
        return "m" + System.nanoTime();
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
