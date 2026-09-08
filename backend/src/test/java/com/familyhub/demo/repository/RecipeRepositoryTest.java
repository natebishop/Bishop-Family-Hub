package com.familyhub.demo.repository;

import com.familyhub.demo.config.TestcontainersConfig;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.Recipe;
import com.familyhub.demo.model.RecipeIngredient;
import com.familyhub.demo.model.RecipeInstruction;
import com.familyhub.demo.model.RecipeTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RecipeRepositoryTest {

    @Autowired
    RecipeRepository recipeRepository;

    @Autowired
    TestEntityManager entityManager;

    private Family family;
    private Family otherFamily;

    @BeforeEach
    void setUp() {
        family = persistFamily("recipes-family");
        otherFamily = persistFamily("other-recipes-family");
    }

    @Test
    void saveRecipe_persistsOrderedIngredientsInstructionsAndTags() {
        Recipe recipe = recipeFor(family, "Sheet Pan Gnocchi");
        recipe.getIngredients().add(ingredient(recipe, 0, "1 lb shelf-stable gnocchi"));
        recipe.getIngredients().add(ingredient(recipe, 1, "2 cups cherry tomatoes"));
        recipe.getInstructions().add(instruction(recipe, 0, "Heat oven to 425F"));
        recipe.getInstructions().add(instruction(recipe, 1, "Roast for 20 minutes"));
        recipe.getTags().add(tag(recipe, 0, "dinner"));
        recipe.getTags().add(tag(recipe, 1, "weeknight"));

        UUID id = recipeRepository.saveAndFlush(recipe).getId();
        entityManager.clear();

        Recipe saved = recipeRepository.findByIdAndFamily(id, family).orElseThrow();

        assertThat(saved.getIngredients()).extracting(RecipeIngredient::getText)
                .containsExactly("1 lb shelf-stable gnocchi", "2 cups cherry tomatoes");
        assertThat(saved.getInstructions()).extracting(RecipeInstruction::getText)
                .containsExactly("Heat oven to 425F", "Roast for 20 minutes");
        assertThat(saved.getTags()).extracting(RecipeTag::getName)
                .containsExactly("dinner", "weeknight");
    }

    @Test
    void findByIdAndFamily_doesNotReturnAnotherFamiliesRecipe() {
        Recipe otherFamilyRecipe = recipeRepository.saveAndFlush(recipeFor(otherFamily, "Other Soup"));

        assertThat(recipeRepository.findByIdAndFamily(otherFamilyRecipe.getId(), family)).isEmpty();
    }

    @Test
    void findByFamilyForSummary_keepsFamilyScopeAndTagOrder() {
        Recipe familyRecipe = recipeFor(family, "Tag Soup");
        familyRecipe.getTags().add(tag(familyRecipe, 0, "dinner"));
        familyRecipe.getTags().add(tag(familyRecipe, 1, "quick"));
        recipeRepository.saveAndFlush(familyRecipe);
        recipeRepository.saveAndFlush(recipeFor(otherFamily, "Other Soup"));
        entityManager.clear();

        java.util.List<Recipe> recipes = recipeRepository.findByFamilyForSummary(family);

        assertThat(recipes).extracting(Recipe::getTitle)
                .contains("Tag Soup")
                .doesNotContain("Other Soup");
        Recipe saved = recipes.stream()
                .filter(recipe -> recipe.getTitle().equals("Tag Soup"))
                .findFirst()
                .orElseThrow();
        assertThat(saved.getTags()).extracting(RecipeTag::getName)
                .containsExactly("dinner", "quick");
    }

    private Family persistFamily(String username) {
        Family savedFamily = new Family();
        savedFamily.setName("Family " + username);
        savedFamily.setUsername(username);
        savedFamily.setPasswordHash("$2a$10$dummyhashfortesting");
        return entityManager.persistFlushFind(savedFamily);
    }

    private Recipe recipeFor(Family owner, String title) {
        Recipe recipe = new Recipe();
        recipe.setFamily(owner);
        recipe.setTitle(title);
        return recipe;
    }

    private RecipeIngredient ingredient(Recipe recipe, int sortOrder, String text) {
        RecipeIngredient ingredient = new RecipeIngredient();
        ingredient.setRecipe(recipe);
        ingredient.setSortOrder(sortOrder);
        ingredient.setText(text);
        return ingredient;
    }

    private RecipeInstruction instruction(Recipe recipe, int sortOrder, String text) {
        RecipeInstruction instruction = new RecipeInstruction();
        instruction.setRecipe(recipe);
        instruction.setSortOrder(sortOrder);
        instruction.setText(text);
        return instruction;
    }

    private RecipeTag tag(Recipe recipe, int sortOrder, String name) {
        RecipeTag tag = new RecipeTag();
        tag.setRecipe(recipe);
        tag.setSortOrder(sortOrder);
        tag.setName(name);
        return tag;
    }
}
