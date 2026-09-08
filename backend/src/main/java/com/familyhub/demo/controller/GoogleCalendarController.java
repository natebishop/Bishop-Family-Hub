package com.familyhub.demo.controller;

import com.familyhub.demo.dto.ApiResponse;
import com.familyhub.demo.dto.CalendarSelectionRequest;
import com.familyhub.demo.dto.GoogleCalendarResponse;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.service.FamilyMemberService;
import com.familyhub.demo.service.GoogleCalendarSelectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/google/calendars")
@RequiredArgsConstructor
public class GoogleCalendarController {
    private final FamilyMemberService familyMemberService;
    private final GoogleCalendarSelectionService selectionService;

    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<List<GoogleCalendarResponse>>> listCalendars(
            @PathVariable UUID memberId,
            @AuthenticationPrincipal Family family) {
        familyMemberService.findById(family, memberId);

        List<GoogleCalendarResponse> calendars = selectionService.listCalendarsWithSelections(memberId);
        return ResponseEntity.ok(new ApiResponse<>(calendars, null));
    }

    @PutMapping("/{memberId}")
    public ResponseEntity<ApiResponse<List<GoogleCalendarResponse>>> updateSelection(
            @PathVariable UUID memberId,
            @Valid @RequestBody CalendarSelectionRequest request,
            @AuthenticationPrincipal Family family) {
        familyMemberService.findById(family, memberId);

        List<GoogleCalendarResponse> calendars = selectionService.updateCalendarSelections(memberId, request.calendarIds());
        return ResponseEntity.ok(new ApiResponse<>(calendars, "Calendar selection updated"));
    }
}
