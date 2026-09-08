package com.familyhub.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "google_synced_calendar",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "google_calendar_id"}))
public class GoogleSyncedCalendar {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "token_id", nullable = false)
    private GoogleOAuthToken token;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private FamilyMember member;

    @Column(name = "google_calendar_id", nullable = false)
    private String googleCalendarId;

    @Column(name = "calendar_name")
    private String calendarName;

    @Column(name = "sync_token", columnDefinition = "TEXT")
    private String syncToken;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
