package com.familyhub.demo.controller;

import com.familyhub.demo.config.SecurityConfig;
import com.familyhub.demo.dto.AuthResponse;
import com.familyhub.demo.dto.FamilyResponse;
import com.familyhub.demo.dto.UsernameCheckResponse;
import com.familyhub.demo.exception.InvalidCredentialException;
import com.familyhub.demo.exception.UsernameAlreadyExists;
import com.familyhub.demo.security.JwtAuthenticationEntryPoint;
import com.familyhub.demo.security.JwtAuthenticationFilter;
import com.familyhub.demo.service.AuthService;
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
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    FamilyService familyService;

    @MockitoBean
    AuthService authService;

    private AuthResponse sampleAuthResponse() {
        return new AuthResponse("jwt-token", new FamilyResponse(
                FAMILY_ID, "Test Family", "America/Los_Angeles", List.of(), LocalDateTime.of(2025, 1, 1, 0, 0)
        ));
    }

    @Test
    void register_success_returns200WithToken() throws Exception {
        given(authService.register(any())).willReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "testfamily",
                                    "password": "password123",
                                    "familyName": "Test Family",
                                    "members": [{"name": "Mom", "color": "coral"}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.message").value("Register successful"));
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        given(authService.register(any())).willThrow(new UsernameAlreadyExists("testfamily"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "testfamily",
                                    "password": "password123",
                                    "familyName": "Test Family",
                                    "members": [{"name": "Mom", "color": "coral"}]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username testfamily already exists."));
    }

    @Test
    void register_shortUsername_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "ab",
                                    "password": "password123",
                                    "familyName": "Test Family",
                                    "members": [{"name": "Mom", "color": "coral"}]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'username')]").exists());
    }

    @Test
    void register_shortPassword_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "testfamily",
                                    "password": "short",
                                    "familyName": "Test Family",
                                    "members": [{"name": "Mom", "color": "coral"}]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists());
    }

    @Test
    void register_emptyFamilyName_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "testfamily",
                                    "password": "password123",
                                    "familyName": "",
                                    "members": [{"name": "Mom", "color": "coral"}]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'familyName')]").exists());
    }

    @Test
    void register_emptyMembers_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "testfamily",
                                    "password": "password123",
                                    "familyName": "Test Family",
                                    "members": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'members')]").exists());
    }

    @Test
    void login_success_returns200WithToken() throws Exception {
        given(authService.login(any())).willReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "testfamily",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        given(authService.login(any())).willThrow(new InvalidCredentialException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "testfamily",
                                    "password": "wrongpassword"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void checkUsername_returns200WithAvailability() throws Exception {
        given(authService.checkUsername("testfamily")).willReturn(new UsernameCheckResponse(true));

        mockMvc.perform(get("/api/auth/check-username")
                        .param("username", "testfamily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.message").value("Username check"));
    }
}
