package com.familyhub.demo.controller;

import com.familyhub.demo.dto.*;
import com.familyhub.demo.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;


    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok().body(new ApiResponse<>(response, "Register successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse loginResponse = authService.login(loginRequest);
        return ResponseEntity.ok().body(new ApiResponse<>(loginResponse, "Login successful"));
    }

    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<UsernameCheckResponse>> checkUsername(
            @RequestParam(name = "username", required = true) String username) {

       return ResponseEntity.ok(new ApiResponse<>(authService.checkUsername(username), "Username check"));
    }
}
