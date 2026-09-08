package com.familyhub.demo.controller;

import com.familyhub.demo.dto.ApiResponse;
import com.familyhub.demo.dto.CategoryDeleteResult;
import com.familyhub.demo.dto.CreateListCategoryRequest;
import com.familyhub.demo.dto.ListCategoryCatalogResponse;
import com.familyhub.demo.dto.ListCategoryManagementEntry;
import com.familyhub.demo.dto.RenameListCategoryRequest;
import com.familyhub.demo.dto.ReorderListCategoriesRequest;
import com.familyhub.demo.exception.BadRequestException;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.ListKind;
import com.familyhub.demo.service.ListCategoryService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/lists/categories")
@RequiredArgsConstructor
public class ListCategoryController {

    private final ListCategoryService listCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<ListCategoryCatalogResponse>> getCatalog(
            @RequestParam("kind") String kind,
            @AuthenticationPrincipal Family family
    ) {
        ListKind listKind;
        try {
            listKind = ListKind.fromValue(kind);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid list kind.");
        }
        return ResponseEntity.ok(
                new ApiResponse<>(listCategoryService.getCatalog(listKind, family), "Categories retrieved successfully")
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ListCategoryManagementEntry>> createCategory(
            @Valid @RequestBody CreateListCategoryRequest request,
            @AuthenticationPrincipal Family family
    ) {
        ListCategoryManagementEntry entry = listCategoryService.create(request, family);
        return ResponseEntity.created(URI.create("/api/lists/categories/" + entry.id()))
                .body(new ApiResponse<>(entry, "Category created successfully"));
    }

    @PutMapping("/order")
    public ResponseEntity<ApiResponse<ListCategoryCatalogResponse>> reorderCategories(
            @Valid @RequestBody ReorderListCategoriesRequest request,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(listCategoryService.reorder(request, family), "Category order updated successfully")
        );
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<ListCategoryManagementEntry>> renameCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody RenameListCategoryRequest request,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(listCategoryService.rename(categoryId, request, family), "Category renamed successfully")
        );
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryDeleteResult>> deleteCategory(
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(listCategoryService.delete(categoryId, family), "Category deleted successfully")
        );
    }
}
