package com.familyhub.demo.repository;

import com.familyhub.demo.model.GoogleSyncedCalendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoogleSyncedCalendarRepository extends JpaRepository<GoogleSyncedCalendar, UUID> {
    List<GoogleSyncedCalendar> findByMemberId(UUID memberId);

    List<GoogleSyncedCalendar> findByMemberIdAndEnabledTrue(UUID memberId);

    Optional<GoogleSyncedCalendar> findByMemberIdAndGoogleCalendarId(UUID memberId, String googleCalendarId);

    void deleteByMemberId(UUID memberId);
}
