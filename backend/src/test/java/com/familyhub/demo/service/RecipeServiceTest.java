package com.familyhub.demo.service;

import com.familyhub.demo.dto.CreateRecipeRequest;
import com.familyhub.demo.dto.ImportRecipeRequest;
import com.familyhub.demo.dto.RecipeDetailResponse;
import com.familyhub.demo.dto.RecipeSummaryResponse;
import com.familyhub.demo.dto.UpdateRecipeRequest;
import com.familyhub.demo.exception.BadRequestException;
import com.familyhub.demo.exception.ResourceNotFoundException;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.Recipe;
import com.familyhub.demo.model.RecipeIngredient;
import com.familyhub.demo.model.RecipeInstruction;
import com.familyhub.demo.model.RecipeTag;
import com.familyhub.demo.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.familyhub.demo.TestDataFactory.RECIPE_ID;
import static com.familyhub.demo.TestDataFactory.createFamily;
import static com.familyhub.demo.TestDataFactory.createOtherFamily;
import static com.familyhub.demo.TestDataFactory.createRecipe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private RecipeImportService recipeImportService;

    @InjectMocks
    private RecipeService recipeService;

    private Family family;
    private Family otherFamily;

    @BeforeEach
    void setUp() {
        family = createFamily();
        otherFamily = createOtherFamily();
    }

    @Test
    void createRecipe_requiresOnlyTitle() {
        when(recipeRepository.saveAndFlush(any(Recipe.class))).thenAnswer(invocation -> savedRecipe(invocation.getArgument(0)));

        RecipeDetailResponse response = recipeService.createRecipe(
                new CreateRecipeRequest("Quick Oats", null, null, null, null, null, null, null),
                family
        );

        assertThat(response.title()).isEqualTo("Quick Oats");
        assertThat(response.imageUrl()).isNull();
        assertThat(response.ingredients()).isEmpty();
        assertThat(response.instructions()).isEmpty();
        assertThat(response.note()).isNull();
        assertThat(response.sourceUrl()).isNull();
        assertThat(response.tags()).isEmpty();
        assertThat(response.favorite()).isFalse();
    }

    @Test
    void createRecipe_persistsOrderedIngredientsInstructionsTagsAndFavoriteState() {
        when(recipeRepository.saveAndFlush(any(Recipe.class))).thenAnswer(invocation -> savedRecipe(invocation.getArgument(0)));

        RecipeDetailResponse response = recipeService.createRecipe(
                new CreateRecipeRequest(
                        "Sheet Pan Gnocchi",
                        "https://cdn.example.com/gnocchi.jpg",
                        List.of("1 lb shelf-stable gnocchi", "2 cups cherry tomatoes"),
                        List.of("Heat oven to 425F", "Roast for 20 minutes"),
                        "Weeknight favorite",
                        "https://example.com/gnocchi",
                        List.of("dinner", "weeknight"),
                        true
                ),
                family
        );

        assertThat(response.title()).isEqualTo("Sheet Pan Gnocchi");
        assertThat(response.favorite()).isTrue();
        assertThat(response.ingredients()).containsExactly("1 lb shelf-stable gnocchi", "2 cups cherry tomatoes");
        assertThat(response.instructions()).containsExactly("Heat oven to 425F", "Roast for 20 minutes");
        assertThat(response.tags()).containsExactly("dinner", "weeknight");

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getFamily()).isEqualTo(family);
        assertThat(captor.getValue().getIngredients()).extracting(RecipeIngredient::getSortOrder)
                .containsExactly(0, 1);
        assertThat(captor.getValue().getInstructions()).extracting(RecipeInstruction::getSortOrder)
                .containsExactly(0, 1);
        assertThat(captor.getValue().getTags()).extracting(RecipeTag::getSortOrder)
                .containsExactly(0, 1);
    }

    @Test
    void getRecipes_returnsRecentRecipesForFamilyOnly() {
        Recipe newest = createRecipe(family, "Newest Soup");
        newest.setUpdatedAt(LocalDateTime.of(2026, 6, 2, 10, 0));
        Recipe oldest = createRecipe(family, "Oldest Soup");
        oldest.setUpdatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        when(recipeRepository.findByFamilyForSummary(family)).thenReturn(List.of(newest, oldest));

        List<RecipeSummaryResponse> recipes = recipeService.getRecipes(family);

        assertThat(recipes).extracting(RecipeSummaryResponse::title)
                .containsExactly("Newest Soup", "Oldest Soup");
        verify(recipeRepository).findByFamilyForSummary(family);
    }

    @Test
    void getRecipe_usesFamilyScopedLookup() {
        when(recipeRepository.findByIdAndFamily(RECIPE_ID, otherFamily)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.getRecipe(RECIPE_ID, otherFamily))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRecipe_canEditRecipeFieldsAndPreserveOrder() {
        Recipe recipe = createRecipe(family, "Old Soup");
        when(recipeRepository.findByIdAndFamily(RECIPE_ID, family)).thenReturn(Optional.of(recipe));
        when(recipeRepository.saveAndFlush(recipe)).thenAnswer(invocation -> savedRecipe(invocation.getArgument(0)));

        RecipeDetailResponse response = recipeService.updateRecipe(
                RECIPE_ID,
                updateRequest(
                        "New Soup",
                        "https://cdn.example.com/soup.jpg",
                        List.of("1 cup broth", "1 cup noodles"),
                        List.of("Simmer", "Serve"),
                        "Updated note",
                        "https://example.com/soup",
                        List.of("lunch", "cozy"),
                        true
                ),
                family
        );

        assertThat(response.title()).isEqualTo("New Soup");
        assertThat(response.imageUrl()).isEqualTo("https://cdn.example.com/soup.jpg");
        assertThat(response.ingredients()).containsExactly("1 cup broth", "1 cup noodles");
        assertThat(response.instructions()).containsExactly("Simmer", "Serve");
        assertThat(response.note()).isEqualTo("Updated note");
        assertThat(response.sourceUrl()).isEqualTo("https://example.com/soup");
        assertThat(response.tags()).containsExactly("lunch", "cozy");
        assertThat(response.favorite()).isTrue();
    }

    @Test
    void updateRecipe_canToggleFavoriteWithoutReplacingOtherFields() {
        Recipe recipe = createRecipe(family, "Keep Soup");
        recipe.setFavorite(false);
        addIngredient(recipe, 0, "1 cup broth");
        when(recipeRepository.findByIdAndFamily(RECIPE_ID, family)).thenReturn(Optional.of(recipe));
        when(recipeRepository.saveAndFlush(recipe)).thenAnswer(invocation -> savedRecipe(invocation.getArgument(0)));

        RecipeDetailResponse response = recipeService.updateRecipe(
                RECIPE_ID,
                favoriteOnlyRequest(true),
                family
        );

        assertThat(response.title()).isEqualTo("Keep Soup");
        assertThat(response.ingredients()).containsExactly("1 cup broth");
        assertThat(response.favorite()).isTrue();
    }

    @Test
    void updateRecipe_canClearNullableFieldsWithExplicitNullSetters() {
        Recipe recipe = createRecipe(family, "Keep Soup");
        recipe.setImageUrl("https://cdn.example.com/soup.jpg");
        recipe.setNote("Old note");
        recipe.setSourceUrl("https://example.com/soup");
        when(recipeRepository.findByIdAndFamily(RECIPE_ID, family)).thenReturn(Optional.of(recipe));
        when(recipeRepository.saveAndFlush(recipe)).thenAnswer(invocation -> savedRecipe(invocation.getArgument(0)));

        UpdateRecipeRequest request = new UpdateRecipeRequest();
        request.setImageUrl(null);
        request.setNote(null);
        request.setSourceUrl(null);

        RecipeDetailResponse response = recipeService.updateRecipe(RECIPE_ID, request, family);

        assertThat(response.imageUrl()).isNull();
        assertThat(response.note()).isNull();
        assertThat(response.sourceUrl()).isNull();
    }

    @Test
    void createRecipe_rejectsFieldsThatWouldViolateRecipeConstraints() {
        CreateRecipeRequest request = new CreateRecipeRequest(
                "Sheet Pan Gnocchi",
                "not a url",
                List.of("x".repeat(501)),
                List.of("Heat oven"),
                null,
                "https://example.com/gnocchi",
                List.of("dinner"),
                false
        );

        assertThatThrownBy(() -> recipeService.createRecipe(request, family))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Recipe image URL must be a valid http or https URL");
    }

    @Test
    void updateRecipe_rejectsTagsThatWouldViolateRecipeConstraints() {
        Recipe recipe = createRecipe(family, "Keep Soup");
        when(recipeRepository.findByIdAndFamily(RECIPE_ID, family)).thenReturn(Optional.of(recipe));

        UpdateRecipeRequest request = new UpdateRecipeRequest();
        request.setTags(List.of("x".repeat(61)));

        assertThatThrownBy(() -> recipeService.updateRecipe(RECIPE_ID, request, family))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Recipe tag must be 60 characters or less");
    }

    @Test
    void importRecipe_failsClosedWhenImportedRecipeWouldViolateConstraints() {
        when(recipeImportService.importFromUrl("https://example.com/tacos"))
                .thenReturn(new ImportedRecipe(
                        "x".repeat(161),
                        null,
                        List.of("1 lb beef"),
                        List.of("Brown beef"),
                        null,
                        "https://example.com/tacos",
                        List.of(),
                        false
                ));

        assertThatThrownBy(() -> recipeService.importRecipe(new ImportRecipeRequest("https://example.com/tacos"), family))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Could not import recipe");
    }

    @Test
    void importRecipe_autoSavesImportedRecipeForFamily() {
        when(recipeImportService.importFromUrl("https://example.com/tacos"))
                .thenReturn(new ImportedRecipe(
                        "Weeknight Tacos",
                        "https://cdn.example.com/tacos.jpg",
                        List.of("1 lb beef", "8 tortillas"),
                        List.of("Brown beef", "Serve in tortillas"),
                        null,
                        "https://example.com/tacos",
                        List.of("dinner"),
                        false
                ));
        when(recipeRepository.saveAndFlush(any(Recipe.class))).thenAnswer(invocation -> savedRecipe(invocation.getArgument(0)));

        RecipeDetailResponse response = recipeService.importRecipe(
                new ImportRecipeRequest("https://example.com/tacos"),
                family
        );

        assertThat(response.title()).isEqualTo("Weeknight Tacos");
        assertThat(response.ingredients()).containsExactly("1 lb beef", "8 tortillas");
        assertThat(response.sourceUrl()).isEqualTo("https://example.com/tacos");

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getFamily()).isEqualTo(family);
    }

    @Test
    void importRecipe_dropsUnusableImageUrlButStillSaves() {
        when(recipeImportService.importFromUrl("https://example.com/tacos"))
                .thenReturn(new ImportedRecipe(
                        "Weeknight Tacos",
                        "data:image/png;base64,AAAA",
                        List.of("1 lb beef"),
                        List.of("Brown beef"),
                        null,
                        "https://example.com/tacos",
                        List.of(),
                        false
                ));
        when(recipeRepository.saveAndFlush(any(Recipe.class))).thenAnswer(invocation -> savedRecipe(invocation.getArgument(0)));

        RecipeDetailResponse response = recipeService.importRecipe(
                new ImportRecipeRequest("https://example.com/tacos"),
                family
        );

        assertThat(response.title()).isEqualTo("Weeknight Tacos");

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getImageUrl()).isNull();
        assertThat(captor.getValue().getIngredients()).hasSize(1);
    }

    private Recipe savedRecipe(Recipe recipe) {
        recipe.setId(RECIPE_ID);
        recipe.setCreatedAt(LocalDateTime.of(2026, 6, 2, 9, 0));
        recipe.setUpdatedAt(LocalDateTime.of(2026, 6, 2, 9, 0));
        return recipe;
    }

    private void addIngredient(Recipe recipe, int sortOrder, String text) {
        RecipeIngredient ingredient = new RecipeIngredient();
        ingredient.setRecipe(recipe);
        ingredient.setSortOrder(sortOrder);
        ingredient.setText(text);
        recipe.getIngredients().add(ingredient);
    }

    private UpdateRecipeRequest favoriteOnlyRequest(boolean favorite) {
        UpdateRecipeRequest request = new UpdateRecipeRequest();
        request.setFavorite(favorite);
        return request;
    }

    private UpdateRecipeRequest updateRequest(
            String title,
            String imageUrl,
            List<String> ingredients,
            List<String> instructions,
            String note,
            String sourceUrl,
            List<String> tags,
            Boolean favorite
    ) {
        UpdateRecipeRequest request = new UpdateRecipeRequest();
        request.setTitle(title);
        request.setImageUrl(imageUrl);
        request.setIngredients(ingredients);
        request.setInstructions(instructions);
        request.setNote(note);
        request.setSourceUrl(sourceUrl);
        request.setTags(tags);
        request.setFavorite(favorite);
        return request;
    }
}
