package com.familyhub.demo.controller;

import com.familyhub.demo.dto.*;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.service.ListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lists")
@RequiredArgsConstructor
public class ListController {
    private final ListService listService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ListSummaryResponse>>> getLists(@AuthenticationPrincipal Family family) {
        return ResponseEntity.ok(new ApiResponse<>(listService.getLists(family), ""));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ListDetailResponse>> createList(
            @Valid @RequestBody CreateListRequest request,
            @AuthenticationPrincipal Family family
    ) {
        ListDetailResponse response = listService.createList(request, family);
        return ResponseEntity.created(URI.create("/api/lists/" + response.id()))
                .body(new ApiResponse<>(response, "List created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ListDetailResponse>> getList(
            @PathVariable UUID id,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(new ApiResponse<>(listService.getList(id, family), ""));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ListDetailResponse>> updateList(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateListRequest request,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(new ApiResponse<>(listService.updateList(id, request, family), "List updated successfully"));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<ApiResponse<ListItemResponse>> createItem(
            @PathVariable UUID id,
            @Valid @RequestBody CreateListItemRequest request,
            @AuthenticationPrincipal Family family
    ) {
        ListItemResponse response = listService.createItem(id, request, family);
        return ResponseEntity.created(URI.create("/api/lists/" + id + "/items/" + response.id()))
                .body(new ApiResponse<>(response, "List item created successfully"));
    }

    @PostMapping("/{id}/items/bulk")
    public ResponseEntity<ApiResponse<List<ListItemResponse>>> createItemsBulk(
            @PathVariable UUID id,
            @Valid @RequestBody BulkCreateListItemsRequest request,
            @AuthenticationPrincipal Family family
    ) {
        List<ListItemResponse> response = listService.createItemsBulk(id, request, family);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(response, "List items added successfully"));
    }

    @PatchMapping("/{listId}/items/{itemId}")
    public ResponseEntity<ApiResponse<ListItemResponse>> updateItem(
            @PathVariable UUID listId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateListItemRequest request,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(listService.updateItem(listId, itemId, request, family), "List item updated successfully")
        );
    }

    @DeleteMapping("/{listId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable UUID listId,
            @PathVariable UUID itemId,
            @AuthenticationPrincipal Family family
    ) {
        listService.deleteItem(listId, itemId, family);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/clear-completed")
    public ResponseEntity<ApiResponse<ClearCompletedResponse>> clearCompleted(
            @PathVariable UUID id,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(listService.clearCompleted(id, family), "Completed items removed successfully")
        );
    }

    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<ListPreferencesResponse>> getPreferences(@AuthenticationPrincipal Family family) {
        return ResponseEntity.ok(new ApiResponse<>(listService.getPreferences(family), ""));
    }

    @PatchMapping("/preferences")
    public ResponseEntity<ApiResponse<ListPreferencesResponse>> updatePreferences(
            @Valid @RequestBody UpdateListPreferencesRequest request,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(listService.updatePreferences(request, family), "List preferences updated successfully")
        );
    }
}
