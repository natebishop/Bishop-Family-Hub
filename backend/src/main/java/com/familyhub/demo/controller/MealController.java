package com.familyhub.demo.controller;

import com.familyhub.demo.dto.ApiResponse;
import com.familyhub.demo.dto.DuplicateMealSlotRequest;
import com.familyhub.demo.dto.MealBoardResponse;
import com.familyhub.demo.dto.MealSlotResponse;
import com.familyhub.demo.dto.MoveMealSlotRequest;
import com.familyhub.demo.dto.RemoveMealSlotRequest;
import com.familyhub.demo.dto.SaveMealPlanRequest;
import com.familyhub.demo.dto.UpsertMealSlotRequest;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.service.MealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealController {
    private final MealService mealService;

    @GetMapping("/board")
    public ResponseEntity<ApiResponse<MealBoardResponse>> getBoard(
            @RequestParam LocalDate weekStartDate,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(new ApiResponse<>(mealService.getBoard(weekStartDate, family), ""));
    }

    @PutMapping("/slots")
    public ResponseEntity<ApiResponse<MealSlotResponse>> upsertSlot(
            @Valid @RequestBody UpsertMealSlotRequest request,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                mealService.upsertSlot(request, family),
                "Meal slot updated successfully"
        ));
    }

    @PostMapping("/plans")
    public ResponseEntity<ApiResponse<MealBoardResponse>> savePlan(
            @Valid @RequestBody SaveMealPlanRequest request,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                mealService.savePlan(request, family),
                "Meal plan saved successfully"
        ));
    }

    @PostMapping("/slots/move")
    public ResponseEntity<ApiResponse<MealBoardResponse>> moveSlot(
            @Valid @RequestBody MoveMealSlotRequest request,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                mealService.moveSlot(request, family),
                "Meal slot moved successfully"
        ));
    }

    @PostMapping("/slots/duplicate")
    public ResponseEntity<ApiResponse<MealBoardResponse>> duplicateSlot(
            @Valid @RequestBody DuplicateMealSlotRequest request,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                mealService.duplicateSlot(request, family),
                "Meal slot duplicated successfully"
        ));
    }

    @DeleteMapping("/slots")
    public ResponseEntity<ApiResponse<MealBoardResponse>> removeSlot(
            @Valid @RequestBody RemoveMealSlotRequest request,
            @AuthenticationPrincipal Family family
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                mealService.removeSlot(request, family),
                "Meal slot removed successfully"
        ));
    }
}
