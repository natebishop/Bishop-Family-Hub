package com.familyhub.demo.dto;

import java.util.List;

public record ChoreAssigneeGroupResponse(
        FamilyMemberResponse member,
        Summary summary,
        List<ChoreBoardItemResponse> chores
) {
    public record Summary(int total, int completed, int remaining) {
    }
}
