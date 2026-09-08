package com.familyhub.demo.mapper;

import com.familyhub.demo.dto.RecipeDetailResponse;
import com.familyhub.demo.dto.RecipeSummaryResponse;
import com.familyhub.demo.model.Recipe;
import com.familyhub.demo.model.RecipeIngredient;
import com.familyhub.demo.model.RecipeInstruction;
import com.familyhub.demo.model.RecipeTag;

public final class RecipeMapper {
    private RecipeMapper() {
    }

    public static RecipeSummaryResponse toSummaryDto(Recipe recipe) {
        return new RecipeSummaryResponse(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getImageUrl(),
                recipe.isFavorite(),
                recipe.getTags().stream().map(RecipeTag::getName).toList(),
                recipe.getUpdatedAt()
        );
    }

    public static RecipeDetailResponse toDetailDto(Recipe recipe) {
        return new RecipeDetailResponse(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getImageUrl(),
                recipe.getIngredients().stream().map(RecipeIngredient::getText).toList(),
                recipe.getInstructions().stream().map(RecipeInstruction::getText).toList(),
                recipe.getNote(),
                recipe.getSourceUrl(),
                recipe.getTags().stream().map(RecipeTag::getName).toList(),
                recipe.isFavorite(),
                recipe.getUpdatedAt()
        );
    }
}
