package com.familyhub.demo.service;

import com.familyhub.demo.config.GoogleOAuthConfig;
import com.familyhub.demo.exception.BadRequestException;
import com.familyhub.demo.model.FamilyMember;
import com.familyhub.demo.model.GoogleOAuthToken;
import com.familyhub.demo.repository.GoogleOAuthTokenRepository;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.testing.http.MockHttpTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleCredentialServiceTest {

    @Mock
    private GoogleOAuthTokenRepository tokenRepository;

    @Mock
    private TokenEncryptionService encryptionService;

    private GoogleOAuthConfig config;
    private GoogleCredentialService credentialService;

    @BeforeEach
    void setUp() {
        config = new GoogleOAuthConfig();
        config.setClientId("test-client-id");
        config.setClientSecret("test-client-secret");

        HttpTransport transport = new MockHttpTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        credentialService = new GoogleCredentialService(
                tokenRepository, encryptionService, config, transport, jsonFactory);
    }

    @Test
    void getCredential_validToken_returnsCredential() {
        UUID memberId = UUID.randomUUID();
        GoogleOAuthToken token = createToken(memberId, Instant.now().plusSeconds(3600));

        when(tokenRepository.findByMemberId(memberId)).thenReturn(Optional.of(token));
        when(encryptionService.decrypt("encrypted-access")).thenReturn("plain-access");
        when(encryptionService.decrypt("encrypted-refresh")).thenReturn("plain-refresh");

        Credential credential = credentialService.getCredential(memberId);

        assertThat(credential.getAccessToken()).isEqualTo("plain-access");
        assertThat(credential.getRefreshToken()).isEqualTo("plain-refresh");
    }

    @Test
    void getCredential_noToken_throwsBadRequest() {
        UUID memberId = UUID.randomUUID();
        when(tokenRepository.findByMemberId(memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> credentialService.getCredential(memberId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not connected");
    }

    private GoogleOAuthToken createToken(UUID memberId, Instant expiry) {
        FamilyMember member = new FamilyMember();
        member.setId(memberId);

        GoogleOAuthToken token = new GoogleOAuthToken();
        token.setId(UUID.randomUUID());
        token.setMember(member);
        token.setAccessToken("encrypted-access");
        token.setRefreshToken("encrypted-refresh");
        token.setTokenExpiry(expiry);
        token.setScope("calendar.events.readonly");
        return token;
    }
}
