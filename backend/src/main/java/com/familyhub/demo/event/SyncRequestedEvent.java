package com.familyhub.demo.event;

import java.util.UUID;

/**
 * Published after calendar selections are committed.
 * Listened to by GoogleCalendarSyncService to trigger async sync.
 */
public record SyncRequestedEvent(UUID memberId) {}
