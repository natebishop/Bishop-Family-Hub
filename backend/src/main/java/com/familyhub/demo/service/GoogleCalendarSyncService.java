package com.familyhub.demo.service;

import com.familyhub.demo.model.CalendarEvent;
import com.familyhub.demo.model.EventSource;
import com.familyhub.demo.model.GoogleSyncedCalendar;
import com.familyhub.demo.repository.CalendarEventRepository;
import com.familyhub.demo.repository.GoogleSyncedCalendarRepository;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.familyhub.demo.event.SyncRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarSyncService {

    private final CalendarEventRepository calendarEventRepository;
    private final GoogleSyncedCalendarRepository syncedCalendarRepository;
    private final GoogleEventMapper googleEventMapper;
    private final GoogleCredentialService credentialService;

    @Lazy
    @Autowired
    private GoogleCalendarSyncService self;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSyncRequested(SyncRequestedEvent event) {
        self.syncMember(event.memberId());
    }

    @Async
    public void syncMember(UUID memberId) {
        log.info("Starting async sync for member {}", memberId);
        List<GoogleSyncedCalendar> calendars = syncedCalendarRepository.findByMemberIdAndEnabledTrue(memberId);
        if (calendars.isEmpty()) {
            log.info("No enabled calendars for member {}, skipping sync", memberId);
            return;
        }

        Calendar calendarClient = buildCalendarClient(memberId);

        // DD-1: Per-calendar isolation — each calendar syncs independently.
        // fullSync uses deleteBySyncedCalendarAndSource (per-calendar scope),
        // so a failure on one calendar cannot affect another's committed data.
        // The abort-all pattern was originally for member-wide deletes via
        // deleteByMemberAndSource, which is no longer used in sync.
        for (GoogleSyncedCalendar cal : calendars) {
            try {
                if (cal.getSyncToken() != null) {
                    incrementalSync(cal, calendarClient);
                } else {
                    fullSync(cal, calendarClient);
                }
            } catch (Exception e) {
                log.error("Sync failed for calendar {} (member {}): {}",
                        cal.getGoogleCalendarId(), memberId, e.getMessage());
            }
        }
    }

    private void incrementalSync(GoogleSyncedCalendar syncedCal, Calendar calendarClient) throws IOException {
        try {
            List<Event> changes = fetchIncrementalEvents(syncedCal, calendarClient);
            self.persistIncrementalChanges(syncedCal, changes);
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 410) {
                log.warn("Sync token expired for calendar {} — falling back to full sync",
                        syncedCal.getGoogleCalendarId());
                syncedCal.setSyncToken(null);
                fullSync(syncedCal, calendarClient);
            } else {
                throw e;
            }
        }
    }

    /**
     * Builds a Google Calendar API client for the given member.
     * Package-private for test spying.
     */
    Calendar buildCalendarClient(UUID memberId) {
        Credential credential = credentialService.getCredential(memberId);
        return new Calendar.Builder(
                credentialService.getHttpTransport(),
                credentialService.getJsonFactory(),
                credential)
                .setApplicationName("FamilyHub")
                .build();
    }

    /**
     * Full sync for a single calendar: fetches all events from Google, then persists
     * via a separate transactional method. Used for initial sync or 410 Gone fallback.
     */
    public void fullSync(GoogleSyncedCalendar syncedCal, Calendar calendarClient) {
        try {
            List<Event> allEvents = fetchAllEvents(syncedCal, calendarClient);
            self.persistFullSync(syncedCal, allEvents);
        } catch (IOException e) {
            log.error("Failed to sync calendar {} for member {}: {}",
                    syncedCal.getGoogleCalendarId(), syncedCal.getMember().getId(), e.getMessage());
            throw new RuntimeException("Google Calendar sync failed", e);
        }
    }

    /**
     * Persists a full sync: deletes existing Google events for this calendar,
     * then re-inserts from scratch. Called via self-proxy to ensure @Transactional is active.
     */
    @Transactional
    public void persistFullSync(GoogleSyncedCalendar syncedCal, List<Event> allEvents) {
        Optional<GoogleSyncedCalendar> activeCalendar = syncedCalendarRepository
                .findEnabledForUpdate(syncedCal.getId());
        if (activeCalendar.isEmpty()) {
            // A user may have deselected this calendar while Google was being
            // fetched. Clean up any old cache and do not write the fetched data.
            calendarEventRepository.deleteBySyncedCalendarAndSource(syncedCal, EventSource.GOOGLE);
            return;
        }

        GoogleSyncedCalendar calendar = activeCalendar.get();
        calendarEventRepository.deleteBySyncedCalendarAndSource(calendar, EventSource.GOOGLE);
        // The derived delete is executed in the current persistence context. Flush it
        // before inserting the fresh rows so reused Google IDs do not hit the unique index.
        calendarEventRepository.flush();
        saveGoogleEvents(allEvents, calendar);
        calendar.setSyncToken(syncedCal.getSyncToken());
        calendar.setLastSyncedAt(Instant.now());
        syncedCalendarRepository.save(calendar);
    }

    /**
     * Saves Google events: parents/regular first (flush), then exceptions.
     * Cancelled non-recurring events and orphaned exceptions are skipped.
     */
    private void saveGoogleEvents(List<Event> allEvents, GoogleSyncedCalendar syncedCal) {
        List<Event> parentsAndRegular = allEvents.stream()
                .filter(e -> e.getRecurringEventId() == null)
                .filter(e -> !"cancelled".equals(e.getStatus()))
                .toList();

        List<Event> exceptions = allEvents.stream()
                .filter(e -> e.getRecurringEventId() != null)
                .toList();

        List<CalendarEvent> parentEntities = new ArrayList<>();
        for (Event event : parentsAndRegular) {
            parentEntities.add(googleEventMapper.toEntity(event, syncedCal));
        }
        calendarEventRepository.saveAll(parentEntities);
        calendarEventRepository.flush();

        for (Event exception : exceptions) {
            String googleParentId = exception.getRecurringEventId();
            Optional<CalendarEvent> parentEntity = calendarEventRepository
                    .findBySyncedCalendarAndGoogleEventId(syncedCal, googleParentId);

            if (parentEntity.isEmpty()) {
                log.warn("Orphaned exception {} — parent {} not found, skipping",
                        exception.getId(), googleParentId);
                continue;
            }

            CalendarEvent exceptionEntity = googleEventMapper.toExceptionEntity(
                    exception, syncedCal, parentEntity.get());
            calendarEventRepository.save(exceptionEntity);
        }
    }

    /**
     * Fetches only events that changed since last sync using Google's sync token.
     * Does NOT use setSingleEvents or setMaxResults — incompatible with sync tokens.
     * Package-private for test access.
     */
    List<Event> fetchIncrementalEvents(GoogleSyncedCalendar syncedCal, Calendar calendarClient) throws IOException {
        String pageToken = null;
        List<Event> changedEvents = new ArrayList<>();

        do {
            Events response = calendarClient.events().list(syncedCal.getGoogleCalendarId())
                    .setSyncToken(syncedCal.getSyncToken())
                    .setTimeZone(familyTimezone(syncedCal))
                    .setPageToken(pageToken)
                    .execute();

            if (response.getItems() != null) {
                changedEvents.addAll(response.getItems());
            }
            pageToken = response.getNextPageToken();

            if (pageToken == null && response.getNextSyncToken() != null) {
                syncedCal.setSyncToken(response.getNextSyncToken());
            }
        } while (pageToken != null);

        return changedEvents;
    }

    private String familyTimezone(GoogleSyncedCalendar syncedCal) {
        return FamilyTimezoneResolver.resolveStoredTimezoneOrDefault(
                syncedCal.getMember().getFamily().getTimezone());
    }

    /**
     * Persists incremental changes: upserts new/updated events, deletes cancelled ones.
     * Parents/regular events processed before exceptions for FK resolution.
     * Called via self-proxy to ensure @Transactional is active.
     */
    @Transactional
    public void persistIncrementalChanges(GoogleSyncedCalendar syncedCal, List<Event> changedEvents) {
        Optional<GoogleSyncedCalendar> activeCalendar = syncedCalendarRepository
                .findEnabledForUpdate(syncedCal.getId());
        if (activeCalendar.isEmpty()) {
            calendarEventRepository.deleteBySyncedCalendarAndSource(syncedCal, EventSource.GOOGLE);
            return;
        }

        GoogleSyncedCalendar calendar = activeCalendar.get();
        List<Event> parentChanges = changedEvents.stream()
                .filter(e -> e.getRecurringEventId() == null)
                .toList();
        List<Event> exceptionChanges = changedEvents.stream()
                .filter(e -> e.getRecurringEventId() != null)
                .toList();

        for (Event event : parentChanges) {
            if ("cancelled".equals(event.getStatus())) {
                calendarEventRepository.deleteBySyncedCalendarAndGoogleEventId(calendar, event.getId());
            } else {
                upsertEvent(event, calendar);
            }
        }

        for (Event event : exceptionChanges) {
            upsertException(event, calendar);
        }

        calendar.setSyncToken(syncedCal.getSyncToken());
        calendar.setLastSyncedAt(Instant.now());
        syncedCalendarRepository.save(calendar);
    }

    private void upsertEvent(Event googleEvent, GoogleSyncedCalendar syncedCal) {
        CalendarEvent entity = googleEventMapper.toEntity(googleEvent, syncedCal);
        calendarEventRepository.findBySyncedCalendarAndGoogleEventId(syncedCal, googleEvent.getId())
                .ifPresentOrElse(
                        existing -> {
                            updateExistingEvent(existing, entity);
                            calendarEventRepository.save(existing);
                        },
                        () -> calendarEventRepository.save(entity)
                );
    }

    private void upsertException(Event googleEvent, GoogleSyncedCalendar syncedCal) {
        String googleParentId = googleEvent.getRecurringEventId();
        Optional<CalendarEvent> parentOpt = calendarEventRepository
                .findBySyncedCalendarAndGoogleEventId(syncedCal, googleParentId);

        if (parentOpt.isEmpty()) {
            log.warn("Orphaned exception {} — parent {} not found, skipping",
                    googleEvent.getId(), googleParentId);
            return;
        }

        CalendarEvent entity = googleEventMapper.toExceptionEntity(googleEvent, syncedCal, parentOpt.get());
        calendarEventRepository.findBySyncedCalendarAndGoogleEventId(syncedCal, googleEvent.getId())
                .ifPresentOrElse(
                        existing -> {
                            updateExistingEvent(existing, entity);
                            calendarEventRepository.save(existing);
                        },
                        () -> calendarEventRepository.save(entity)
                );
    }

    /**
     * Copies mutable fields from updated entity onto existing entity.
     * Preserves: id, googleEventId, source, member, family, syncedCalendar, recurringEvent, originalDate.
     * DD-3: Field list must stay in sync with CalendarEvent entity changes.
     */
    void updateExistingEvent(CalendarEvent existing, CalendarEvent updated) {
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setLocation(updated.getLocation());
        existing.setDate(updated.getDate());
        existing.setEndDate(updated.getEndDate());
        existing.setStartTime(updated.getStartTime());
        existing.setEndTime(updated.getEndTime());
        existing.setAllDay(updated.isAllDay());
        existing.setHtmlLink(updated.getHtmlLink());
        existing.setEtag(updated.getEtag());
        existing.setGoogleUpdatedAt(updated.getGoogleUpdatedAt());
        existing.setRecurrenceRule(updated.getRecurrenceRule());
        existing.setExdates(updated.getExdates());
        existing.setCancelled(updated.isCancelled());
    }

    private List<Event> fetchAllEvents(GoogleSyncedCalendar syncedCal, Calendar calendarClient) throws IOException {
        String pageToken = null;
        List<Event> allEvents = new ArrayList<>();

        do {
            Events response = calendarClient.events().list(syncedCal.getGoogleCalendarId())
                    .setSingleEvents(false)
                    .setMaxResults(250)
                    .setTimeZone(familyTimezone(syncedCal))
                    .setPageToken(pageToken)
                    .execute();

            if (response.getItems() != null) {
                allEvents.addAll(response.getItems());
            }
            pageToken = response.getNextPageToken();

            if (pageToken == null && response.getNextSyncToken() != null) {
                syncedCal.setSyncToken(response.getNextSyncToken());
            }
        } while (pageToken != null);

        return allEvents;
    }

}
