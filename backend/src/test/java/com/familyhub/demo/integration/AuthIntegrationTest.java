package com.familyhub.demo.integration;

import com.familyhub.demo.config.TestcontainersConfig;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String uniqueUsername() {
        return "user" + System.nanoTime();
    }

    private String registerJson(String username) {
        return """
                {
                    "username": "%s",
                    "password": "password123",
                    "familyName": "Integration Family",
                    "members": [
                        { "name": "Mom", "color": "coral", "email": "mom@test.com" }
                    ]
                }
                """.formatted(username);
    }

    @Test
    void fullRegisterLoginFlow() throws Exception {
        String username = uniqueUsername();

        // Register
        String registerBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.family.name").value("Integration Family"))
                .andExpect(jsonPath("$.data.family.members[0].name").value("Mom"))
                .andReturn().getResponse().getContentAsString();

        String registerToken = JsonPath.read(registerBody, "$.data.token");

        // Login
        String loginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "%s", "password": "password123" }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String loginToken = JsonPath.read(loginBody, "$.data.token");

        // Use login token to access protected endpoint
        mockMvc.perform(get("/api/family")
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Integration Family"))
                .andExpect(jsonPath("$.data.members[0].name").value("Mom"));
    }

    @Test
    void register_invalidData_returns400() throws Exception {
        String payload = """
                {
                    "username": "ab",
                    "password": "short",
                    "familyName": "",
                    "members": []
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void register_invalidTimezone_returns400() throws Exception {
        String payload = """
                {
                    "username": "%s",
                    "password": "password123",
                    "familyName": "Integration Family",
                    "members": [
                        { "name": "Mom", "color": "coral", "email": "mom@test.com" }
                    ],
                    "timezone": "Mars/Olympus"
                }
                """.formatted(uniqueUsername());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Timezone must be a valid IANA timezone."));
    }

    @Test
    void protectedEndpoint_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/family"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.httpStatus").value(401));
    }

    @Test
    void protectedEndpoint_malformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/family")
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.httpStatus").value(401));
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        String username = uniqueUsername();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(username)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(username)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username " + username + " already exists."));
    }
}
