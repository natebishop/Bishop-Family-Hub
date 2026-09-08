package com.familyhub.demo.controller;

import com.familyhub.demo.dto.ApiResponse;
import com.familyhub.demo.dto.CreateRecipeRequest;
import com.familyhub.demo.dto.ImportRecipeRequest;
import com.familyhub.demo.dto.RecipeDetailResponse;
import com.familyhub.demo.dto.RecipeSummaryResponse;
import com.familyhub.demo.dto.UpdateRecipeRequest;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {
    private final RecipeService recipeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecipeSummaryResponse>>> getRecipes(@AuthenticationPrincipal Family family) {
        return ResponseEntity.ok(new ApiResponse<>(recipeService.getRecipes(family), ""));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipeDetailResponse>> getRecipe(
            @PathVariable UUID id,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(new ApiResponse<>(recipeService.getRecipe(id, family), ""));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecipeDetailResponse>> createRecipe(
            @Valid @RequestBody CreateRecipeRequest request,
            @AuthenticationPrincipal Family family
    ) {
        RecipeDetailResponse response = recipeService.createRecipe(request, family);
        return ResponseEntity.created(URI.create("/api/recipes/" + response.id()))
                .body(new ApiResponse<>(response, "Recipe created successfully"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipeDetailResponse>> updateRecipe(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecipeRequest request,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(recipeService.updateRecipe(id, request, family), "Recipe updated successfully")
        );
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<RecipeDetailResponse>> importRecipe(
            @Valid @RequestBody ImportRecipeRequest request,
            @AuthenticationPrincipal Family family
    ) {
        RecipeDetailResponse response = recipeService.importRecipe(request, family);
        return ResponseEntity.created(URI.create("/api/recipes/" + response.id()))
                .body(new ApiResponse<>(response, "Recipe imported successfully"));
    }
}
