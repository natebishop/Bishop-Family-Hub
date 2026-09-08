package com.familyhub.demo.controller;

import com.familyhub.demo.config.SecurityConfig;
import com.familyhub.demo.dto.FamilyMemberResponse;
import com.familyhub.demo.dto.FamilyResponse;
import com.familyhub.demo.model.FamilyColor;
import com.familyhub.demo.security.JwtAuthenticationEntryPoint;
import com.familyhub.demo.security.JwtAuthenticationFilter;
import com.familyhub.demo.security.WithMockFamily;
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

import java.time.LocalDateTime;
import java.util.List;

import static com.familyhub.demo.TestDataFactory.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FamilyController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class FamilyControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    FamilyService familyService;

    private FamilyResponse sampleFamilyResponse() {
        return new FamilyResponse(
                FAMILY_ID,
                "Test Family",
                "America/Los_Angeles",
                List.of(new FamilyMemberResponse(
                        MEMBER_ID,
                        "Mom", FamilyColor.CORAL, "mom@test.com", null
                )),
                LocalDateTime.of(2025, 1, 1, 0, 0)
        );
    }

    @Test
    @WithMockFamily
    void getFamily_authenticated_returns200() throws Exception {
        given(familyService.findFamilyResponse(FAMILY_ID)).willReturn(sampleFamilyResponse());

        mockMvc.perform(get("/api/family"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Test Family"))
                .andExpect(jsonPath("$.message").value("Family Found"));
    }

    @Test
    void getFamily_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/family"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.httpStatus").value(401));
    }

    @Test
    @WithMockFamily
    void updateFamily_returns200() throws Exception {
        given(familyService.updateFamily(eq(FAMILY_ID), any())).willReturn(sampleFamilyResponse());

        mockMvc.perform(put("/api/family")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Updated Family", "username": "testfamily"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Test Family"))
                .andExpect(jsonPath("$.message").value("Update"));
    }

    @Test
    @WithMockFamily
    void deleteFamily_returns204() throws Exception {
        willDoNothing().given(familyService).deleteFamily(FAMILY_ID);

        mockMvc.perform(delete("/api/family"))
                .andExpect(status().isNoContent());
    }
}
