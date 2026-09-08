package com.familyhub.demo.mapper;

import com.familyhub.demo.dto.CalendarEventRequest;
import com.familyhub.demo.dto.CalendarEventResponse;
import com.familyhub.demo.exception.BadRequestException;
import com.familyhub.demo.model.CalendarEvent;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.FamilyMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static com.familyhub.demo.TestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalendarEventMapperTest {

    private Family family;
    private FamilyMember member;

    @BeforeEach
    void setUp() {
        family = createFamily();
        family.setFamilyMembers(List.of());
        member = createFamilyMember(family);
    }

    @Test
    void toEntity_parsesTimeCorrectly() {
        CalendarEventRequest request = new CalendarEventRequest(
                "Event", "2:00 PM", "3:00 PM",
                LocalDate.of(2025, 6, 15), MEMBER_ID, false, null, null, null, null);

        CalendarEvent entity = CalendarEventMapper.toEntity(request, family, member);

        assertThat(entity.getStartTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(entity.getEndTime()).isEqualTo(LocalTime.of(15, 0));
    }

    @Test
    void toEntity_allDayEvent_setsFlag() {
        CalendarEventRequest request = new CalendarEventRequest(
                "Birthday", "12:00 AM", "12:00 AM",
                LocalDate.of(2025, 6, 15), MEMBER_ID, true, null, null, null, null);

        CalendarEvent entity = CalendarEventMapper.toEntity(request, family, member);

        assertThat(entity.isAllDay()).isTrue();
    }

    @Test
    void toEntity_nullIsAllDay_defaultsToFalse() {
        CalendarEventRequest request = new CalendarEventRequest(
                "Event", "9:00 AM", "10:00 AM",
                LocalDate.of(2025, 6, 15), MEMBER_ID, null, null, null, null, null);

        CalendarEvent entity = CalendarEventMapper.toEntity(request, family, member);

        assertThat(entity.isAllDay()).isFalse();
    }

    @Test
    void toEntity_invalidTimeString_throwsBadRequest() {
        CalendarEventRequest request = new CalendarEventRequest(
                "Event", "not-a-time", "10:00 AM",
                LocalDate.of(2025, 6, 15), MEMBER_ID, false, null, null, null, null);

        assertThatThrownBy(() -> CalendarEventMapper.toEntity(request, family, member))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not-a-time");
    }

    @Test
    void toDto_formatsTimeCorrectly() {
        CalendarEvent event = createCalendarEvent(family, member);
        event.setStartTime(LocalTime.of(9, 0));

        CalendarEventResponse response = CalendarEventMapper.toDto(event);

        assertThat(response.startTime()).isEqualTo("9:00 AM");
    }

    @Test
    void toDto_mapsAllFields() {
        CalendarEvent event = createCalendarEvent(family, member);

        CalendarEventResponse response = CalendarEventMapper.toDto(event);

        assertThat(response.id()).isEqualTo(EVENT_ID);
        assertThat(response.title()).isEqualTo("Test Event");
        assertThat(response.startTime()).isEqualTo("9:00 AM");
        assertThat(response.endTime()).isEqualTo("10:00 AM");
        assertThat(response.date()).isEqualTo(LocalDate.of(2025, 6, 15));
        assertThat(response.memberId()).isEqualTo(MEMBER_ID);
        assertThat(response.isAllDay()).isFalse();
        assertThat(response.location()).isEqualTo("Test Location");
    }
}
