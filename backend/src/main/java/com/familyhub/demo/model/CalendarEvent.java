package com.familyhub.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Getter
@Setter
public class CalendarEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private FamilyMember member;

    @ManyToOne
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    private LocalDate endDate;

    private boolean isAllDay;

    private String location;

    private String recurrenceRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_event_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CalendarEvent recurringEvent;

    private LocalDate originalDate;

    @Column(nullable = false)
    private boolean isCancelled;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EventSource source = EventSource.NATIVE;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "google_event_id", length = 1024)
    private String googleEventId;

    @Column(name = "html_link", length = 2048)
    private String htmlLink;

    @Column(length = 255)
    private String etag;

    @Column(name = "google_updated_at")
    private Instant googleUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "synced_calendar_id")
    private GoogleSyncedCalendar syncedCalendar;

    @Column(columnDefinition = "TEXT")
    private String exdates;
}
