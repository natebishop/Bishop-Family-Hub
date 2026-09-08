package com.familyhub.demo.service;

import com.familyhub.demo.exception.BadRequestException;

import java.time.DateTimeException;
import java.time.ZoneId;

public final class FamilyTimezoneResolver {
    static final String DEFAULT_FAMILY_TIMEZONE = "America/Los_Angeles";
    static final String INVALID_TIMEZONE_MESSAGE = "Timezone must be a valid IANA timezone.";

    private FamilyTimezoneResolver() {
    }

    static String normalizeRequestedTimezone(String timezone) {
        String candidate = normalize(timezone);
        if (candidate == null) {
            return DEFAULT_FAMILY_TIMEZONE;
        }

        try {
            return ZoneId.of(candidate).getId();
        } catch (DateTimeException ex) {
            throw new BadRequestException(INVALID_TIMEZONE_MESSAGE);
        }
    }

    public static String resolveStoredTimezoneOrDefault(String timezone) {
        String candidate = normalize(timezone);
        if (candidate == null) {
            return DEFAULT_FAMILY_TIMEZONE;
        }

        try {
            return ZoneId.of(candidate).getId();
        } catch (DateTimeException ex) {
            return DEFAULT_FAMILY_TIMEZONE;
        }
    }

    private static String normalize(String timezone) {
        if (timezone == null) {
            return null;
        }

        String trimmed = timezone.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
