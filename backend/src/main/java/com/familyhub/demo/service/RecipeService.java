package com.familyhub.demo.service;

import com.familyhub.demo.dto.CreateRecipeRequest;
import com.familyhub.demo.dto.ImportRecipeRequest;
import com.familyhub.demo.dto.RecipeDetailResponse;
import com.familyhub.demo.dto.RecipeSummaryResponse;
import com.familyhub.demo.dto.UpdateRecipeRequest;
import com.familyhub.demo.exception.BadRequestException;
import com.familyhub.demo.exception.ResourceNotFoundException;
import com.familyhub.demo.mapper.RecipeMapper;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.Recipe;
import com.familyhub.demo.model.RecipeIngredient;
import com.familyhub.demo.model.RecipeInstruction;
import com.familyhub.demo.model.RecipeTag;
import com.familyhub.demo.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final RecipeImportService recipeImportService;

    public List<RecipeSummaryResponse> getRecipes(Family family) {
        return recipeRepository.findByFamilyForSummary(family)
                .stream()
                .map(RecipeMapper::toSummaryDto)
                .toList();
    }

    public RecipeDetailResponse getRecipe(UUID id, Family family) {
        return RecipeMapper.toDetailDto(getRecipeOrThrow(id, family));
    }

    @Transactional
    public RecipeDetailResponse createRecipe(CreateRecipeRequest request, Family family) {
        Recipe recipe = new Recipe();
        recipe.setFamily(family);
        recipe.setTitle(RecipeFieldValidator.requiredTitle(request.title()));
        recipe.setImageUrl(RecipeFieldValidator.optionalHttpUrl(request.imageUrl(), "Recipe image URL"));
        recipe.setNote(RecipeFieldValidator.optionalText(request.note()));
        recipe.setSourceUrl(RecipeFieldValidator.optionalHttpUrl(request.sourceUrl(), "Recipe source URL"));
        recipe.setFavorite(Boolean.TRUE.equals(request.favorite()));
        replaceIngredients(recipe, request.ingredients());
        replaceInstructions(recipe, request.instructions());
        replaceTags(recipe, request.tags());

        return RecipeMapper.toDetailDto(recipeRepository.saveAndFlush(recipe));
    }

    @Transactional
    public RecipeDetailResponse updateRecipe(UUID id, UpdateRecipeRequest request, Family family) {
        Recipe recipe = getRecipeOrThrow(id, family);
        if (request.hasTitle()) {
            recipe.setTitle(RecipeFieldValidator.requiredTitle(request.title()));
        }
        if (request.hasImageUrl()) {
            recipe.setImageUrl(RecipeFieldValidator.optionalHttpUrl(request.imageUrl(), "Recipe image URL"));
        }
        if (request.hasIngredients()) {
            replaceIngredients(recipe, request.ingredients());
        }
        if (request.hasInstructions()) {
            replaceInstructions(recipe, request.instructions());
        }
        if (request.hasNote()) {
            recipe.setNote(RecipeFieldValidator.optionalText(request.note()));
        }
        if (request.hasSourceUrl()) {
            recipe.setSourceUrl(RecipeFieldValidator.optionalHttpUrl(request.sourceUrl(), "Recipe source URL"));
        }
        if (request.hasTags()) {
            replaceTags(recipe, request.tags());
        }
        if (request.hasFavorite()) {
            recipe.setFavorite(Boolean.TRUE.equals(request.favorite()));
        }

        return RecipeMapper.toDetailDto(recipeRepository.saveAndFlush(recipe));
    }

    @Transactional
    public RecipeDetailResponse importRecipe(ImportRecipeRequest request, Family family) {
        Recipe recipe = new Recipe();
        try {
            ImportedRecipe imported = recipeImportService.importFromUrl(request.url().trim());
            recipe.setFamily(family);
            recipe.setTitle(RecipeFieldValidator.requiredTitle(imported.title()));
            recipe.setImageUrl(RecipeFieldValidator.optionalHttpUrlOrNull(imported.imageUrl()));
            recipe.setNote(RecipeFieldValidator.optionalText(imported.note()));
            recipe.setSourceUrl(RecipeFieldValidator.optionalHttpUrlOrNull(imported.sourceUrl()));
            recipe.setFavorite(imported.favorite());
            replaceIngredients(recipe, imported.ingredients());
            replaceInstructions(recipe, imported.instructions());
            replaceTags(recipe, imported.tags());
        } catch (BadRequestException ex) {
            throw importFailure();
        }

        return RecipeMapper.toDetailDto(recipeRepository.saveAndFlush(recipe));
    }

    private Recipe getRecipeOrThrow(UUID id, Family family) {
        return recipeRepository.findByIdAndFamily(id, family)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", id));
    }

    private void replaceIngredients(Recipe recipe, List<String> values) {
        recipe.getIngredients().clear();
        List<String> normalized = RecipeFieldValidator.normalizedIngredients(values);
        for (int i = 0; i < normalized.size(); i++) {
            RecipeIngredient ingredient = new RecipeIngredient();
            ingredient.setRecipe(recipe);
            ingredient.setSortOrder(i);
            ingredient.setText(normalized.get(i));
            recipe.getIngredients().add(ingredient);
        }
    }

    private void replaceInstructions(Recipe recipe, List<String> values) {
        recipe.getInstructions().clear();
        List<String> normalized = RecipeFieldValidator.normalizedInstructions(values);
        for (int i = 0; i < normalized.size(); i++) {
            RecipeInstruction instruction = new RecipeInstruction();
            instruction.setRecipe(recipe);
            instruction.setSortOrder(i);
            instruction.setText(normalized.get(i));
            recipe.getInstructions().add(instruction);
        }
    }

    private void replaceTags(Recipe recipe, List<String> values) {
        recipe.getTags().clear();
        List<String> normalized = RecipeFieldValidator.normalizedTags(values);
        for (int i = 0; i < normalized.size(); i++) {
            RecipeTag tag = new RecipeTag();
            tag.setRecipe(recipe);
            tag.setSortOrder(i);
            tag.setName(normalized.get(i));
            recipe.getTags().add(tag);
        }
    }

    private static BadRequestException importFailure() {
        return new BadRequestException("Could not import recipe.");
    }
}
