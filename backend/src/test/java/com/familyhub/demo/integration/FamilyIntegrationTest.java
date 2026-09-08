package com.familyhub.demo.integration;

import com.familyhub.demo.config.TestcontainersConfig;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.repository.FamilyRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class FamilyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FamilyRepository familyRepository;

    private String uniqueUsername() {
        return "family" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
    }

    private String registerJson(String username) {
        return """
                {
                    "username": "%s",
                    "password": "password123",
                    "familyName": "Timezone Family",
                    "members": [
                        { "name": "Mom", "color": "coral", "email": "mom@test.com" }
                    ]
                }
                """.formatted(username);
    }

    private String registerJson(String username, String timezone) {
        return """
                {
                    "username": "%s",
                    "password": "password123",
                    "familyName": "Timezone Family",
                    "members": [
                        { "name": "Mom", "color": "coral", "email": "mom@test.com" }
                    ],
                    "timezone": "%s"
                }
                """.formatted(username, timezone);
    }

    private String register(String registerJson) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void getFamily_returnsStoredTimezone() throws Exception {
        String body = register(registerJson(uniqueUsername(), "Asia/Tokyo"));
        String token = JsonPath.read(body, "$.data.token");

        mockMvc.perform(get("/api/family")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timezone").value("Asia/Tokyo"));
    }

    @Test
    void getFamily_legacyInvalidStoredTimezone_returnsDefault() throws Exception {
        String body = register(registerJson(uniqueUsername()));
        String token = JsonPath.read(body, "$.data.token");
        UUID familyId = UUID.fromString(JsonPath.read(body, "$.data.family.id"));

        // Simulate a legacy row whose stored timezone is not a valid IANA zone
        Family family = familyRepository.findById(familyId).orElseThrow();
        family.setTimezone("Not/AZone");
        familyRepository.save(family);

        mockMvc.perform(get("/api/family")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timezone").value("America/Los_Angeles"));
    }

    @Test
    void updateFamily_validTimezone_roundTrips() throws Exception {
        String body = register(registerJson(uniqueUsername()));
        String token = JsonPath.read(body, "$.data.token");

        mockMvc.perform(put("/api/family")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "timezone": "America/New_York" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timezone").value("America/New_York"));

        mockMvc.perform(get("/api/family")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timezone").value("America/New_York"));
    }

    @Test
    void updateFamily_invalidTimezone_returns400AndPersistsNothing() throws Exception {
        String body = register(registerJson(uniqueUsername(), "America/New_York"));
        String token = JsonPath.read(body, "$.data.token");

        mockMvc.perform(put("/api/family")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "timezone": "Mars/Olympus" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Timezone must be a valid IANA timezone."));

        // Stored value untouched: a persisted invalid zone would resolve to the
        // default, not America/New_York, so this catches accidental writes
        mockMvc.perform(get("/api/family")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timezone").value("America/New_York"));
    }

    @Test
    void updateFamily_invalidTimezoneWithOtherFields_persistsNothing() throws Exception {
        String body = register(registerJson(uniqueUsername(), "America/New_York"));
        String token = JsonPath.read(body, "$.data.token");

        // Name is applied to the managed entity before timezone validation
        // throws, so this pins the transactional rollback: neither field
        // may survive a request rejected for an invalid timezone
        mockMvc.perform(put("/api/family")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Should Not Persist", "timezone": "Mars/Olympus" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Timezone must be a valid IANA timezone."));

        mockMvc.perform(get("/api/family")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Timezone Family"))
                .andExpect(jsonPath("$.data.timezone").value("America/New_York"));
    }

    @Test
    void updateFamily_omittedTimezone_leavesStoredValueUnchanged() throws Exception {
        String body = register(registerJson(uniqueUsername(), "America/New_York"));
        String token = JsonPath.read(body, "$.data.token");

        mockMvc.perform(put("/api/family")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Renamed Family" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Renamed Family"))
                .andExpect(jsonPath("$.data.timezone").value("America/New_York"));

        mockMvc.perform(get("/api/family")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Renamed Family"))
                .andExpect(jsonPath("$.data.timezone").value("America/New_York"));
    }
}
