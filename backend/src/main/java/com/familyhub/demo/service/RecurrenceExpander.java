package com.familyhub.demo.service;

import com.familyhub.demo.dto.CalendarEventResponse;
import com.familyhub.demo.mapper.CalendarEventMapper;
import com.familyhub.demo.model.CalendarEvent;
import net.fortuna.ical4j.model.Recur;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class RecurrenceExpander {

    public List<CalendarEventResponse> expand(
            CalendarEvent parent,
            LocalDate rangeStart,
            LocalDate rangeEnd,
            Map<LocalDate, CalendarEvent> exceptions
    ) {
        Recur<LocalDate> recur = new Recur<>(parent.getRecurrenceRule());
        List<LocalDate> dates = recur.getDates(parent.getDate(), rangeStart, rangeEnd);

        // Filter out EXDATE dates (excluded by the recurrence rule source, e.g., Google Calendar)
        Set<LocalDate> excludedDates = parseExdates(parent.getExdates());

        List<CalendarEventResponse> results = new ArrayList<>();
        for (LocalDate date : dates) {
            if (excludedDates.contains(date)) {
                continue;
            }
            CalendarEvent exception = exceptions.get(date);
            if (exception != null) {
                if (exception.isCancelled()) {
                    continue; // Skip cancelled instances
                }
                // Use edited exception data
                results.add(CalendarEventMapper.toDto(exception));
            } else {
                // Virtual instance from parent
                results.add(CalendarEventMapper.toInstanceResponse(parent, date));
            }
        }
        return results;
    }

    private Set<LocalDate> parseExdates(String exdates) {
        if (exdates == null || exdates.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(exdates.split(","))
                .map(LocalDate::parse)
                .collect(Collectors.toSet());
    }
}
