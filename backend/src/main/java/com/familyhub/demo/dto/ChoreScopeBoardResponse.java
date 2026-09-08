package com.familyhub.demo.dto;

import com.familyhub.demo.model.ChoreScope;

import java.time.LocalDate;
import java.util.List;

public record ChoreScopeBoardResponse(
        ChoreScope scope,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        Summary summary,
        List<ChoreAssigneeGroupResponse> assignees
) {
    public record Summary(int total, int completed, int remaining) {
    }
}
