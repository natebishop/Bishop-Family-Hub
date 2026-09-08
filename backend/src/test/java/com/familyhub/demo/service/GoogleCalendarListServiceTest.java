package com.familyhub.demo.service;

import com.familyhub.demo.dto.GoogleCalendarInfo;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarListServiceTest {

    @Mock
    private GoogleCredentialService credentialService;

    @Test
    void listCalendars_mapsResponseCorrectly() {
        UUID memberId = UUID.randomUUID();

        String jsonResponse = """
                {
                  "kind": "calendar#calendarList",
                  "items": [
                    { "id": "primary", "summary": "Joe's Calendar", "primary": true },
                    { "id": "work@group.calendar.google.com", "summary": "Work", "primary": false },
                    { "id": "family@group.calendar.google.com", "summary": "Family" }
                  ]
                }
                """;

        HttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.setContentType("application/json");
                        response.setContent(jsonResponse);
                        return response;
                    }
                };
            }
        };

        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setTransport(transport)
                .setJsonFactory(jsonFactory)
                .build();
        credential.setAccessToken("fake-token");

        when(credentialService.getCredential(memberId)).thenReturn(credential);
        when(credentialService.getHttpTransport()).thenReturn(transport);
        when(credentialService.getJsonFactory()).thenReturn(jsonFactory);

        GoogleCalendarListService service = new GoogleCalendarListService(credentialService);
        List<GoogleCalendarInfo> calendars = service.listCalendars(memberId);

        assertThat(calendars).hasSize(3);

        GoogleCalendarInfo primary = calendars.stream()
                .filter(c -> c.id().equals("primary"))
                .findFirst().orElseThrow();
        assertThat(primary.name()).isEqualTo("Joe's Calendar");
        assertThat(primary.primary()).isTrue();

        GoogleCalendarInfo work = calendars.stream()
                .filter(c -> c.id().equals("work@group.calendar.google.com"))
                .findFirst().orElseThrow();
        assertThat(work.name()).isEqualTo("Work");
        assertThat(work.primary()).isFalse();

        GoogleCalendarInfo family = calendars.stream()
                .filter(c -> c.id().equals("family@group.calendar.google.com"))
                .findFirst().orElseThrow();
        assertThat(family.name()).isEqualTo("Family");
        assertThat(family.primary()).isFalse();
    }

    @Test
    void listCalendars_emptyResponse_returnsEmptyList() {
        UUID memberId = UUID.randomUUID();

        String jsonResponse = """
                {
                  "kind": "calendar#calendarList"
                }
                """;

        HttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() {
                        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                        response.setContentType("application/json");
                        response.setContent(jsonResponse);
                        return response;
                    }
                };
            }
        };

        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setTransport(transport)
                .setJsonFactory(jsonFactory)
                .build();
        credential.setAccessToken("fake-token");

        when(credentialService.getCredential(memberId)).thenReturn(credential);
        when(credentialService.getHttpTransport()).thenReturn(transport);
        when(credentialService.getJsonFactory()).thenReturn(jsonFactory);

        GoogleCalendarListService service = new GoogleCalendarListService(credentialService);
        List<GoogleCalendarInfo> calendars = service.listCalendars(memberId);

        assertThat(calendars).isEmpty();
    }
}
