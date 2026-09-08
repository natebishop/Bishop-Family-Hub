package com.familyhub.demo.dto;

import com.familyhub.demo.model.RecipeConstraints;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRecipeRequest(
        @NotBlank
        @Size(max = RecipeConstraints.TITLE_MAX_LENGTH, message = "Recipe title must be 160 characters or less")
        String title,

        String imageUrl,

        List<@NotBlank @Size(max = RecipeConstraints.INGREDIENT_MAX_LENGTH) String> ingredients,

        List<@NotBlank String> instructions,

        String note,

        String sourceUrl,

        List<@NotBlank @Size(max = RecipeConstraints.TAG_MAX_LENGTH) String> tags,

        Boolean favorite
) {
}
