package com.familyhub.demo.controller;

import com.familyhub.demo.dto.ApiResponse;
import com.familyhub.demo.dto.ChoreBoardResponse;
import com.familyhub.demo.dto.ChoreCurrentPeriodStateResponse;
import com.familyhub.demo.dto.ChoreTemplateResponse;
import com.familyhub.demo.dto.CreateChoreTemplateRequest;
import com.familyhub.demo.dto.UpdateChoreTemplateRequest;
import com.familyhub.demo.dto.UpdateCurrentPeriodCompletionRequest;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.service.ChoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/chores")
@RequiredArgsConstructor
public class ChoreController {
    private final ChoreService choreService;

    @GetMapping("/board")
    public ResponseEntity<ApiResponse<ChoreBoardResponse>> getBoard(@AuthenticationPrincipal Family family) {
        return ResponseEntity.ok(new ApiResponse<>(choreService.getBoard(family), ""));
    }

    @PostMapping("/templates")
    public ResponseEntity<ApiResponse<ChoreTemplateResponse>> createTemplate(
            @Valid @RequestBody CreateChoreTemplateRequest request,
            @AuthenticationPrincipal Family family
    ) {
        ChoreTemplateResponse response = choreService.createTemplate(request, family);

        return ResponseEntity.created(URI.create("/api/chores/templates/" + response.id()))
                .body(new ApiResponse<>(response, "Chore template created successfully"));
    }

    @PatchMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<ChoreTemplateResponse>> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateChoreTemplateRequest request,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                choreService.updateTemplate(id, request, family),
                "Chore template updated successfully"
        ));
    }

    @PutMapping("/templates/{id}/current-period-completion")
    public ResponseEntity<ApiResponse<ChoreCurrentPeriodStateResponse>> completeCurrentPeriod(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCurrentPeriodCompletionRequest request,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                choreService.completeCurrentPeriod(id, request, family),
                "Chore completion updated successfully"
        ));
    }

    @DeleteMapping("/templates/{id}/current-period-completion")
    public ResponseEntity<ApiResponse<ChoreCurrentPeriodStateResponse>> uncompleteCurrentPeriod(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCurrentPeriodCompletionRequest request,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                choreService.uncompleteCurrentPeriod(id, request, family),
                "Chore completion updated successfully"
        ));
    }
}
