package com.familyhub.demo.service;

import com.familyhub.demo.dto.GoogleCalendarInfo;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarListService {
    private static final String APPLICATION_NAME = "FamilyHub";

    private final GoogleCredentialService credentialService;

    public List<GoogleCalendarInfo> listCalendars(UUID memberId) {
        Credential credential = credentialService.getCredential(memberId);

        Calendar calendarClient = new Calendar.Builder(
                credentialService.getHttpTransport(),
                credentialService.getJsonFactory(),
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        try {
            CalendarList calendarList = calendarClient.calendarList().list().execute();
            List<CalendarListEntry> items = calendarList.getItems();
            if (items == null) {
                return List.of();
            }
            return items.stream()
                    .map(this::toCalendarInfo)
                    .toList();
        } catch (IOException e) {
            log.error("Failed to list calendars for member {}: {}", memberId, e.getMessage());
            throw new RuntimeException("Failed to fetch Google calendars", e);
        }
    }

    private GoogleCalendarInfo toCalendarInfo(CalendarListEntry entry) {
        return new GoogleCalendarInfo(
                entry.getId(),
                entry.getSummary(),
                Boolean.TRUE.equals(entry.getPrimary())
        );
    }
}
