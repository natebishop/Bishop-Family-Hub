package com.familyhub.demo.controller;

import com.familyhub.demo.config.SecurityConfig;
import com.familyhub.demo.dto.FamilyMemberResponse;
import com.familyhub.demo.exception.BadRequestException;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.FamilyColor;
import com.familyhub.demo.security.JwtAuthenticationEntryPoint;
import com.familyhub.demo.security.JwtAuthenticationFilter;
import com.familyhub.demo.security.WithMockFamily;
import com.familyhub.demo.service.FamilyMemberService;
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

import java.util.List;

import static com.familyhub.demo.TestDataFactory.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FamilyMemberController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class FamilyMemberControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    FamilyService familyService;

    @MockitoBean
    FamilyMemberService familyMemberService;

    private FamilyMemberResponse sampleMemberResponse() {
        return new FamilyMemberResponse(MEMBER_ID, "Mom", FamilyColor.CORAL, "mom@test.com", null);
    }

    private static final String MEMBER_JSON = """
            {"name": "Mom", "color": "coral"}
            """;

    @Test
    @WithMockFamily
    void getMembers_returns200() throws Exception {
        given(familyMemberService.findAllMembers(any(Family.class)))
                .willReturn(List.of(sampleMemberResponse()));

        mockMvc.perform(get("/api/family/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Mom"))
                .andExpect(jsonPath("$.message").value("Get members"));
    }

    @Test
    @WithMockFamily
    void getMemberById_returns200() throws Exception {
        given(familyMemberService.findById(any(Family.class), eq(MEMBER_ID)))
                .willReturn(sampleMemberResponse());

        mockMvc.perform(get("/api/family/members/{id}", MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Mom"))
                .andExpect(jsonPath("$.message").value("Get member by id"));
    }

    @Test
    @WithMockFamily
    void addMember_returns201WithLocationHeader() throws Exception {
        given(familyMemberService.addFamilyMember(any(Family.class), any()))
                .willReturn(sampleMemberResponse());

        mockMvc.perform(post("/api/family/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MEMBER_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/family/members/" + MEMBER_ID))
                .andExpect(jsonPath("$.data.name").value("Mom"))
                .andExpect(jsonPath("$.message").value("Family Member Added"));
    }

    @Test
    @WithMockFamily
    void updateMember_returns200() throws Exception {
        given(familyMemberService.updateFamilyMember(any(Family.class), eq(MEMBER_ID), any()))
                .willReturn(sampleMemberResponse());

        mockMvc.perform(put("/api/family/members/{id}", MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MEMBER_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Mom"))
                .andExpect(jsonPath("$.message").value("Family Member Updated"));
    }

    @Test
    @WithMockFamily
    void deleteMember_returns204() throws Exception {
        willDoNothing().given(familyMemberService).deleteFamilyMember(any(Family.class), eq(MEMBER_ID));

        mockMvc.perform(delete("/api/family/members/{id}", MEMBER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockFamily
    void deleteMember_withActiveRecurringChores_returns400() throws Exception {
        givenDeleteMemberThrows(new BadRequestException(
                "Reassign or archive this member's recurring chores before deleting them."
        ));

        mockMvc.perform(delete("/api/family/members/{id}", MEMBER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Reassign or archive this member's recurring chores before deleting them."));
    }

    @Test
    void getMembers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/family/members"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.httpStatus").value(401));
    }

    private void givenDeleteMemberThrows(RuntimeException exception) {
        org.mockito.BDDMockito.willThrow(exception)
                .given(familyMemberService)
                .deleteFamilyMember(any(Family.class), eq(MEMBER_ID));
    }
}
