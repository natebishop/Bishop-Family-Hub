package com.familyhub.demo.controller;

import com.familyhub.demo.config.SecurityConfig;
import com.familyhub.demo.dto.MealBoardResponse;
import com.familyhub.demo.dto.MealDayResponse;
import com.familyhub.demo.dto.MealSlotEntryResponse;
import com.familyhub.demo.dto.MealSlotResponse;
import com.familyhub.demo.exception.BadRequestException;
import com.familyhub.demo.exception.ConflictException;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.MealEntrySourceType;
import com.familyhub.demo.model.MealSlotRole;
import com.familyhub.demo.model.MealType;
import com.familyhub.demo.security.JwtAuthenticationEntryPoint;
import com.familyhub.demo.security.JwtAuthenticationFilter;
import com.familyhub.demo.security.WithMockFamily;
import com.familyhub.demo.service.FamilyService;
import com.familyhub.demo.service.JwtService;
import com.familyhub.demo.service.MealService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MealController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class MealControllerTest {

    private static final LocalDate WEEK_START = LocalDate.of(2026, 6, 7);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    FamilyService familyService;

    @MockitoBean
    MealService mealService;

    @Test
    @WithMockFamily
    void getBoard_returnsSevenDayBoard() throws Exception {
        given(mealService.getBoard(eq(WEEK_START), any(Family.class))).willReturn(sampleBoard());

        mockMvc.perform(get("/api/meals/board")
                        .param("weekStartDate", "2026-06-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weekStartDate").value("2026-06-07"))
                .andExpect(jsonPath("$.data.days[0].slots[0].mealType").value("breakfast"))
                .andExpect(jsonPath("$.data.days[0].slots[2].primary.title").value("Leftovers"));
    }

    @Test
    @WithMockFamily
    void upsertSlot_returns200WithLowercaseWireValues() throws Exception {
        given(mealService.upsertSlot(any(), any(Family.class))).willReturn(filledDinnerSlot(0, "Leftovers"));

        mockMvc.perform(put("/api/meals/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "dayIndex": 0,
                                  "mealType": "dinner",
                                  "primary": {
                                    "sourceType": "quick",
                                    "recipeId": null,
                                    "title": "Leftovers",
                                    "imageUrl": null,
                                    "note": null
                                  },
                                  "extras": [],
                                  "note": null,
                                  "collisionMode": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Meal slot updated successfully"))
                .andExpect(jsonPath("$.data.mealType").value("dinner"))
                .andExpect(jsonPath("$.data.primary.role").value("primary"))
                .andExpect(jsonPath("$.data.primary.sourceType").value("quick"));
    }

    @Test
    @WithMockFamily
    void moveSlot_returns200() throws Exception {
        given(mealService.moveSlot(any(), any(Family.class))).willReturn(sampleBoard());

        mockMvc.perform(post("/api/meals/slots/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceWeekStartDate": "2026-06-07",
                                  "sourceDayIndex": 0,
                                  "sourceMealType": "dinner",
                                  "destinationWeekStartDate": "2026-06-07",
                                  "destinationDayIndex": 1,
                                  "destinationMealType": "dinner",
                                  "collisionMode": "replace_primary"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Meal slot moved successfully"));
    }

    @Test
    @WithMockFamily
    void duplicateSlot_returns200() throws Exception {
        given(mealService.duplicateSlot(any(), any(Family.class))).willReturn(sampleBoard());

        mockMvc.perform(post("/api/meals/slots/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceWeekStartDate": "2026-06-07",
                                  "sourceDayIndex": 0,
                                  "sourceMealType": "dinner",
                                  "destinationWeekStartDate": "2026-06-07",
                                  "destinationDayIndex": 1,
                                  "destinationMealType": "dinner",
                                  "collisionMode": "replace_primary"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Meal slot duplicated successfully"));
    }

    @Test
    @WithMockFamily
    void savePlan_returnsUpdatedBoard() throws Exception {
        given(mealService.savePlan(any(), any(Family.class))).willReturn(sampleBoard());

        mockMvc.perform(post("/api/meals/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "slots": [
                                    {
                                      "dayIndex": 0,
                                      "mealType": "dinner",
                                      "primary": {
                                        "sourceType": "quick",
                                        "recipeId": null,
                                        "title": "Leftovers",
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
                .andExpect(jsonPath("$.message").value("Meal plan saved successfully"))
                .andExpect(jsonPath("$.data.weekStartDate").value("2026-06-07"))
                .andExpect(jsonPath("$.data.days[0].slots[2].primary.title").value("Leftovers"));
    }

    @Test
    @WithMockFamily
    void savePlan_conflictReturns409() throws Exception {
        given(mealService.savePlan(any(), any(Family.class)))
                .willThrow(new ConflictException("Some meal slots are no longer empty."));

        mockMvc.perform(post("/api/meals/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "slots": [
                                    {
                                      "dayIndex": 0,
                                      "mealType": "dinner",
                                      "primary": {
                                        "sourceType": "quick",
                                        "recipeId": null,
                                        "title": "Leftovers",
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
    }

    @Test
    @WithMockFamily
    void upsertSlot_missingCollisionModeForOccupiedSlotReturns400() throws Exception {
        given(mealService.upsertSlot(any(), any(Family.class))).willThrow(new BadRequestException(
                "Collision mode is required when target slot already has a primary meal."
        ));

        mockMvc.perform(put("/api/meals/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "dayIndex": 0,
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
                                  "collisionMode": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Collision mode is required when target slot already has a primary meal."));
    }

    @Test
    @WithMockFamily
    void duplicateSlot_missingCollisionModeReturns400() throws Exception {
        mockMvc.perform(post("/api/meals/slots/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceWeekStartDate": "2026-06-07",
                                  "sourceDayIndex": 0,
                                  "sourceMealType": "dinner",
                                  "destinationWeekStartDate": "2026-06-07",
                                  "destinationDayIndex": 1,
                                  "destinationMealType": "dinner"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bad Request"));
    }

    @Test
    @WithMockFamily
    void duplicateSlot_invalidCollisionModeReturns400() throws Exception {
        mockMvc.perform(post("/api/meals/slots/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceWeekStartDate": "2026-06-07",
                                  "sourceDayIndex": 0,
                                  "sourceMealType": "dinner",
                                  "destinationWeekStartDate": "2026-06-07",
                                  "destinationDayIndex": 1,
                                  "destinationMealType": "dinner",
                                  "collisionMode": "overwrite"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockFamily
    void removeSlot_returns200() throws Exception {
        given(mealService.removeSlot(any(), any(Family.class))).willReturn(sampleBoard());

        mockMvc.perform(delete("/api/meals/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "dayIndex": 0,
                                  "mealType": "dinner"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Meal slot removed successfully"));
    }

    @Test
    @WithMockFamily
    void removeSlot_missingDayIndexReturns400() throws Exception {
        mockMvc.perform(delete("/api/meals/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "mealType": "dinner"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bad Request"));
    }

    @Test
    @WithMockFamily
    void upsertSlot_missingDayIndexReturns400() throws Exception {
        mockMvc.perform(put("/api/meals/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "mealType": "dinner",
                                  "primary": {
                                    "sourceType": "quick",
                                    "recipeId": null,
                                    "title": "Leftovers",
                                    "imageUrl": null,
                                    "note": null
                                  },
                                  "extras": [],
                                  "note": null,
                                  "collisionMode": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bad Request"));
    }

    @Test
    @WithMockFamily
    void upsertSlot_dayIndexOutOfRangeReturns400() throws Exception {
        mockMvc.perform(put("/api/meals/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "dayIndex": 7,
                                  "mealType": "dinner",
                                  "primary": {
                                    "sourceType": "quick",
                                    "recipeId": null,
                                    "title": "Leftovers",
                                    "imageUrl": null,
                                    "note": null
                                  },
                                  "extras": [],
                                  "note": null,
                                  "collisionMode": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bad Request"));
    }

    @Test
    @WithMockFamily
    void moveSlot_missingSourceDayIndexReturns400() throws Exception {
        mockMvc.perform(post("/api/meals/slots/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceWeekStartDate": "2026-06-07",
                                  "sourceMealType": "dinner",
                                  "destinationWeekStartDate": "2026-06-07",
                                  "destinationDayIndex": 1,
                                  "destinationMealType": "dinner",
                                  "collisionMode": "replace_primary"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bad Request"));
    }

    @Test
    @WithMockFamily
    void upsertSlot_dataIntegrityViolationReturns409() throws Exception {
        given(mealService.upsertSlot(any(), any(Family.class)))
                .willThrow(new DataIntegrityViolationException("duplicate slot"));

        mockMvc.perform(put("/api/meals/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "dayIndex": 0,
                                  "mealType": "dinner",
                                  "primary": {
                                    "sourceType": "quick",
                                    "recipeId": null,
                                    "title": "Leftovers",
                                    "imageUrl": null,
                                    "note": null
                                  },
                                  "extras": [],
                                  "note": null,
                                  "collisionMode": null
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockFamily
    void upsertSlot_optimisticLockConflictReturns409() throws Exception {
        given(mealService.upsertSlot(any(), any(Family.class)))
                .willThrow(new OptimisticLockingFailureException("stale meal slot"));

        mockMvc.perform(put("/api/meals/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "dayIndex": 0,
                                  "mealType": "dinner",
                                  "primary": {
                                    "sourceType": "quick",
                                    "recipeId": null,
                                    "title": "Leftovers",
                                    "imageUrl": null,
                                    "note": null
                                  },
                                  "extras": [],
                                  "note": null,
                                  "collisionMode": null
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockFamily
    void upsertSlot_uppercaseEnumValueReturns400() throws Exception {
        mockMvc.perform(put("/api/meals/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weekStartDate": "2026-06-07",
                                  "dayIndex": 0,
                                  "mealType": "DINNER",
                                  "primary": {
                                    "sourceType": "quick",
                                    "recipeId": null,
                                    "title": "Leftovers",
                                    "imageUrl": null,
                                    "note": null
                                  },
                                  "extras": [],
                                  "note": null,
                                  "collisionMode": null
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private MealBoardResponse sampleBoard() {
        return new MealBoardResponse(
                WEEK_START,
                List.of(new MealDayResponse(
                        WEEK_START,
                        0,
                        List.of(
                                emptySlot(MealType.BREAKFAST),
                                emptySlot(MealType.LUNCH),
                                filledDinnerSlot(0, "Leftovers")
                        )
                ))
        );
    }

    private MealSlotResponse emptySlot(MealType mealType) {
        return new MealSlotResponse(null, WEEK_START, 0, mealType, null, List.of(), null);
    }

    private MealSlotResponse filledDinnerSlot(int dayIndex, String title) {
        return new MealSlotResponse(
                UUID.randomUUID(),
                WEEK_START,
                dayIndex,
                MealType.DINNER,
                new MealSlotEntryResponse(
                        UUID.randomUUID(),
                        MealSlotRole.PRIMARY,
                        MealEntrySourceType.QUICK,
                        null,
                        title,
                        null,
                        null
                ),
                List.of(),
                null
        );
    }
}
