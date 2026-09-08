package com.familyhub.demo.service;

import com.familyhub.demo.dto.AuthResponse;
import com.familyhub.demo.dto.LoginRequest;
import com.familyhub.demo.dto.RegisterRequest;
import com.familyhub.demo.dto.UsernameCheckResponse;
import com.familyhub.demo.exception.BadRequestException;
import com.familyhub.demo.exception.InvalidCredentialException;
import com.familyhub.demo.exception.UsernameAlreadyExists;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.repository.FamilyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static com.familyhub.demo.TestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private ListSeedService listSeedService;

    @InjectMocks
    private AuthService authService;

    private Family family;

    @BeforeEach
    void setUp() {
        family = createFamily();
        family.setFamilyMembers(List.of());
    }

    @Test
    void register_success_savesAndReturnsToken() {
        RegisterRequest request = createRegisterRequest();
        when(familyRepository.existsByUsername(request.username())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(familyRepository.saveAndFlush(any(Family.class))).thenReturn(family);
        when(jwtService.generateToken(family)).thenReturn("jwt-token");

        AuthResponse result = authService.register(request);

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.family().id()).isEqualTo(FAMILY_ID);
    }

    @Test
    void register_success_seedsListsModuleDefaults() {
        RegisterRequest request = createRegisterRequest();
        when(familyRepository.existsByUsername(request.username())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(familyRepository.saveAndFlush(any(Family.class))).thenReturn(family);
        when(jwtService.generateToken(family)).thenReturn("jwt-token");

        authService.register(request);

        verify(listSeedService).seedDefaultsForFamily(family);
    }

    @Test
    void register_withExplicitTimezone_persistsTimezone() {
        RegisterRequest request = new RegisterRequest(
                "smithfamily",
                "password123",
                "Smith Family",
                List.of(createFamilyMemberRequest()),
                "America/Chicago"
        );
        when(familyRepository.existsByUsername(request.username())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(familyRepository.saveAndFlush(any(Family.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(Family.class))).thenReturn("jwt-token");

        authService.register(request);

        verify(familyRepository).saveAndFlush(argThat(saved ->
                "America/Chicago".equals(saved.getTimezone())
        ));
    }

    @Test
    void register_withoutTimezone_fallsBackToPacificDefault() {
        RegisterRequest request = new RegisterRequest(
                "smithfamily",
                "password123",
                "Smith Family",
                List.of(createFamilyMemberRequest()),
                null
        );
        when(familyRepository.existsByUsername(request.username())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(familyRepository.saveAndFlush(any(Family.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(Family.class))).thenReturn("jwt-token");

        authService.register(request);

        verify(familyRepository).saveAndFlush(argThat(saved ->
                "America/Los_Angeles".equals(saved.getTimezone())
        ));
    }

    @Test
    void register_withInvalidTimezone_throwsBadRequest() {
        RegisterRequest request = new RegisterRequest(
                "smithfamily",
                "password123",
                "Smith Family",
                List.of(createFamilyMemberRequest()),
                "Mars/Olympus"
        );
        when(familyRepository.existsByUsername(request.username())).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Timezone must be a valid IANA timezone.");
    }

    @Test
    void register_duplicateUsername_throwsUsernameAlreadyExists() {
        RegisterRequest request = createRegisterRequest();
        when(familyRepository.existsByUsername(request.username())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UsernameAlreadyExists.class);
    }

    @Test
    void login_success_returnsToken() {
        LoginRequest request = createLoginRequest();
        when(familyRepository.findByUsername(request.username())).thenReturn(Optional.of(family));
        when(passwordEncoder.matches(request.password(), family.getPassword())).thenReturn(true);
        when(jwtService.generateToken(family)).thenReturn("jwt-token");

        AuthResponse result = authService.login(request);

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.family().id()).isEqualTo(FAMILY_ID);
    }

    @Test
    void login_invalidPassword_throwsInvalidCredential() {
        LoginRequest request = createLoginRequest();
        when(familyRepository.findByUsername(request.username())).thenReturn(Optional.of(family));
        when(passwordEncoder.matches(request.password(), family.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialException.class);
    }

    @Test
    void login_unknownUsername_throwsInvalidCredential() {
        LoginRequest request = createLoginRequest();
        when(familyRepository.findByUsername(request.username())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialException.class);
    }

    @Test
    void checkUsername_available_returnsTrue() {
        when(familyRepository.existsByUsername("newuser")).thenReturn(false);

        UsernameCheckResponse result = authService.checkUsername("newuser");

        assertThat(result.available()).isTrue();
    }

    @Test
    void checkUsername_taken_returnsFalse() {
        when(familyRepository.existsByUsername("testfamily")).thenReturn(true);

        UsernameCheckResponse result = authService.checkUsername("testfamily");

        assertThat(result.available()).isFalse();
    }
}
