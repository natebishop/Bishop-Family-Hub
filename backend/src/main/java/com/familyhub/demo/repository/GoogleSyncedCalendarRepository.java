package com.familyhub.demo.repository;

import com.familyhub.demo.model.GoogleSyncedCalendar;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoogleSyncedCalendarRepository extends JpaRepository<GoogleSyncedCalendar, UUID> {
    List<GoogleSyncedCalendar> findByMemberId(UUID memberId);

    List<GoogleSyncedCalendar> findByMemberIdAndEnabledTrue(UUID memberId);

    Optional<GoogleSyncedCalendar> findByMemberIdAndGoogleCalendarId(UUID memberId, String googleCalendarId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sc FROM GoogleSyncedCalendar sc WHERE sc.id = :id AND sc.enabled = true")
    Optional<GoogleSyncedCalendar> findEnabledForUpdate(@Param("id") UUID id);

    void deleteByMemberId(UUID memberId);
}
