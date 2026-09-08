package com.familyhub.demo.controller;

import com.familyhub.demo.config.SecurityConfig;
import com.familyhub.demo.dto.ChoreBoardItemResponse;
import com.familyhub.demo.dto.ChoreBoardResponse;
import com.familyhub.demo.dto.ChoreCurrentPeriodStateResponse;
import com.familyhub.demo.dto.ChoreScopeBoardResponse;
import com.familyhub.demo.dto.ChoreTemplateResponse;
import com.familyhub.demo.exception.BadRequestException;
import com.familyhub.demo.model.ChoreCadence;
import com.familyhub.demo.model.ChoreScope;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.security.JwtAuthenticationEntryPoint;
import com.familyhub.demo.security.JwtAuthenticationFilter;
import com.familyhub.demo.security.WithMockFamily;
import com.familyhub.demo.service.ChoreService;
import com.familyhub.demo.service.FamilyService;
import com.familyhub.demo.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.familyhub.demo.TestDataFactory.CHORE_TEMPLATE_ID;
import static com.familyhub.demo.TestDataFactory.MEMBER_ID;
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

@WebMvcTest(ChoreController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class ChoreControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    FamilyService familyService;

    @MockitoBean
    ChoreService choreService;

    @Test
    @WithMockFamily
    void getBoard_returnsTodayWeekAndMonth() throws Exception {
        ChoreBoardResponse board = new ChoreBoardResponse(
                "America/Los_Angeles",
                new ChoreScopeBoardResponse(
                        ChoreScope.TODAY,
                        LocalDate.of(2026, 5, 17),
                        LocalDate.of(2026, 5, 17),
                        new ChoreScopeBoardResponse.Summary(1, 0, 1),
                        List.of()
                ),
                new ChoreScopeBoardResponse(
                        ChoreScope.THIS_WEEK,
                        LocalDate.of(2026, 5, 17),
                        LocalDate.of(2026, 5, 23),
                        new ChoreScopeBoardResponse.Summary(1, 0, 1),
                        List.of()
                ),
                new ChoreScopeBoardResponse(
                        ChoreScope.THIS_MONTH,
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 31),
                        new ChoreScopeBoardResponse.Summary(1, 0, 1),
                        List.of()
                )
        );
        given(choreService.getBoard(any(Family.class))).willReturn(board);

        mockMvc.perform(get("/api/chores/board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timezone").value("America/Los_Angeles"))
                .andExpect(jsonPath("$.data.today.scope").value("TODAY"))
                .andExpect(jsonPath("$.data.thisWeek.scope").value("THIS_WEEK"))
                .andExpect(jsonPath("$.data.thisMonth.scope").value("THIS_MONTH"));
    }

    @Test
    @WithMockFamily
    void createTemplate_returns201WithLocationHeader() throws Exception {
        given(choreService.createTemplate(any(), any(Family.class)))
                .willReturn(sampleTemplateResponse(false));

        mockMvc.perform(post("/api/chores/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Brush teeth",
                                    "assignedToMemberId": "00000000-0000-0000-0000-000000000002",
                                    "cadence": "DAILY",
                                    "activeFrom": "2026-05-17"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/chores/templates/" + CHORE_TEMPLATE_ID))
                .andExpect(jsonPath("$.data.cadence").value("DAILY"))
                .andExpect(jsonPath("$.message").value("Chore template created successfully"));
    }

    @Test
    @WithMockFamily
    void createTemplate_missingActiveFrom_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/chores/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Brush teeth",
                                    "assignedToMemberId": "00000000-0000-0000-0000-000000000002",
                                    "cadence": "DAILY"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'activeFrom')]").exists());
    }

    @Test
    @WithMockFamily
    void updateTemplate_archive_returns200() throws Exception {
        given(choreService.updateTemplate(eq(CHORE_TEMPLATE_ID), any(), any(Family.class)))
                .willReturn(sampleTemplateResponse(true));

        mockMvc.perform(patch("/api/chores/templates/{id}", CHORE_TEMPLATE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"archived": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.archived").value(true))
                .andExpect(jsonPath("$.message").value("Chore template updated successfully"));
    }

    @Test
    @WithMockFamily
    void completeCurrentPeriod_staleRequest_returns400() throws Exception {
        given(choreService.completeCurrentPeriod(eq(CHORE_TEMPLATE_ID), any(), any(Family.class)))
                .willThrow(new BadRequestException("Chore period is stale. Refresh and try again."));

        mockMvc.perform(put("/api/chores/templates/{id}/current-period-completion", CHORE_TEMPLATE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scope": "THIS_WEEK", "periodStartDate": "2026-05-04"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Chore period is stale. Refresh and try again."));
    }

    @Test
    @WithMockFamily
    void uncompleteCurrentPeriod_returns200() throws Exception {
        given(choreService.uncompleteCurrentPeriod(eq(CHORE_TEMPLATE_ID), any(), any(Family.class)))
                .willReturn(new ChoreCurrentPeriodStateResponse(
                        ChoreScope.TODAY,
                        LocalDate.of(2026, 5, 17),
                        LocalDate.of(2026, 5, 17),
                        new ChoreBoardItemResponse(
                                CHORE_TEMPLATE_ID,
                                "Brush teeth",
                                ChoreCadence.DAILY,
                                MEMBER_ID,
                                false,
                                null
                        )
                ));

        mockMvc.perform(delete("/api/chores/templates/{id}/current-period-completion", CHORE_TEMPLATE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scope": "TODAY", "periodStartDate": "2026-05-17"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.item.completed").value(false));
    }

    @Test
    void getBoard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/chores/board"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.httpStatus").value(401));
    }

    private ChoreTemplateResponse sampleTemplateResponse(boolean archived) {
        return new ChoreTemplateResponse(
                CHORE_TEMPLATE_ID,
                "Brush teeth",
                MEMBER_ID,
                ChoreCadence.DAILY,
                LocalDate.of(2026, 5, 17),
                archived,
                LocalDateTime.of(2026, 5, 17, 8, 0),
                LocalDateTime.of(2026, 5, 17, 9, 0)
        );
    }
}
