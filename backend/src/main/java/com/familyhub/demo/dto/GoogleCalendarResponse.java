package com.familyhub.demo.dto;

public record GoogleCalendarResponse(
        String id,
        String name,
        boolean primary,
        boolean enabled
) {}
