package com.familyhub.demo.dto;

import java.time.Instant;
import java.util.List;

public record GoogleConnectionStatus(
        boolean connected,
        List<SyncedCalendarInfo> calendars
) {
    public record SyncedCalendarInfo(
            String id,
            String name,
            boolean enabled,
            Instant lastSyncedAt
    ) {}
}
