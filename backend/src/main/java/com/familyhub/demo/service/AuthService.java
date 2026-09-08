package com.familyhub.demo.service;

import com.familyhub.demo.dto.AuthResponse;
import com.familyhub.demo.dto.LoginRequest;
import com.familyhub.demo.dto.RegisterRequest;
import com.familyhub.demo.dto.UsernameCheckResponse;
import com.familyhub.demo.exception.InvalidCredentialException;
import com.familyhub.demo.exception.UsernameAlreadyExists;
import com.familyhub.demo.mapper.FamilyMapper;
import com.familyhub.demo.mapper.FamilyMemberMapper;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final FamilyRepository familyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ListSeedService listSeedService;

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        // Check if username already exists
        if (familyRepository.existsByUsername(registerRequest.username())) {
            throw new UsernameAlreadyExists(registerRequest.username());
        }

        // Hash password then save to db
        Family family = new Family();
        family.setName(registerRequest.familyName());
        family.setUsername(registerRequest.username());
        family.setPasswordHash(passwordEncoder.encode(registerRequest.password()));
        family.setTimezone(FamilyTimezoneResolver.normalizeRequestedTimezone(registerRequest.timezone()));
        family.setFamilyMembers(
                registerRequest.members().stream()
                        .map(request -> FamilyMemberMapper.toEntity(request, family))
                        .toList()
        );
        Family saved = familyRepository.saveAndFlush(family);
        listSeedService.seedDefaultsForFamily(saved);
        log.info("Family registered, familyId={}, username={}", saved.getId(), saved.getUsername());

        // Create JWT and return response
        String token = jwtService.generateToken(saved);

        return new AuthResponse(token, FamilyMapper.toDto(saved));
    }

    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        Family family = familyRepository.findByUsername(loginRequest.username())
                .orElse(null);

        if (family == null || !passwordEncoder.matches(loginRequest.password(), family.getPassword())) {
            throw new InvalidCredentialException("Invalid username or password.");
        }

        String token = jwtService.generateToken(family);
        log.info("Family logged in, familyId={}, username={}", family.getId(), family.getUsername());
        return new AuthResponse(token, FamilyMapper.toDto(family));
    }

    public UsernameCheckResponse checkUsername(String username) {
        return new UsernameCheckResponse(!familyRepository.existsByUsername(username));
    }
}
