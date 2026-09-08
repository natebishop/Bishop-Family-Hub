package com.familyhub.demo.dto;

public record ChoreBoardResponse(
        String timezone,
        ChoreScopeBoardResponse today,
        ChoreScopeBoardResponse thisWeek,
        ChoreScopeBoardResponse thisMonth
) {
}
