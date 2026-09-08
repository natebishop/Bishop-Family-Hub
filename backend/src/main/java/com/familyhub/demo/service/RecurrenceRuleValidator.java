package com.familyhub.demo.service;

import com.familyhub.demo.exception.BadRequestException;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.transform.recurrence.Frequency;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public class RecurrenceRuleValidator {

    private static final Set<Frequency> BLOCKED_FREQUENCIES = Set.of(Frequency.YEARLY);
    private static final Set<String> BLOCKED_PARTS = Set.of("COUNT", "BYSETPOS");

    public void validate(String rrule) {
        if (rrule == null || rrule.isBlank()) {
            throw new BadRequestException("Recurrence rule must not be empty");
        }

        // Check for blocked parts before parsing
        String upper = rrule.toUpperCase();
        for (String blocked : BLOCKED_PARTS) {
            if (upper.contains(blocked + "=")) {
                throw new BadRequestException("Recurrence rule must not contain " + blocked);
            }
        }

        Recur<LocalDate> recur;
        try {
            recur = new Recur<>(rrule);
        } catch (Exception e) {
            throw new BadRequestException("Invalid recurrence rule: " + rrule);
        }

        Frequency frequency = recur.getFrequency();
        if (frequency == null) {
            throw new BadRequestException("Recurrence rule must specify a frequency");
        }
        if (BLOCKED_FREQUENCIES.contains(frequency)) {
            throw new BadRequestException("Recurrence rule must not use " + frequency + " frequency");
        }
    }
}
