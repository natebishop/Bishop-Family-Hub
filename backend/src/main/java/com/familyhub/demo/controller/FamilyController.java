package com.familyhub.demo.controller;

import com.familyhub.demo.dto.ApiResponse;
import com.familyhub.demo.dto.FamilyRequest;
import com.familyhub.demo.dto.FamilyResponse;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.service.FamilyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FamilyController {
    private final FamilyService familyService;

    @GetMapping("/family")
    ResponseEntity<ApiResponse<FamilyResponse>> findFamilyById(@AuthenticationPrincipal Family family) {
        FamilyResponse response = familyService.findFamilyResponse(family.getId());
        return ResponseEntity.ok(new ApiResponse<>(response, "Family Found"));
    }

    @PutMapping("/family")
    ResponseEntity<ApiResponse<FamilyResponse>> updateFamily(@AuthenticationPrincipal Family family, @RequestBody @Valid FamilyRequest update) {
        FamilyResponse response = familyService.updateFamily(family.getId(), update);
        return ResponseEntity.ok(new ApiResponse<>(response, "Update"));
    }

    @DeleteMapping("/family")
    ResponseEntity<Void> deleteFamily(@AuthenticationPrincipal Family family) {
        familyService.deleteFamily(family.getId());
        return ResponseEntity.noContent().build();
    }
}
