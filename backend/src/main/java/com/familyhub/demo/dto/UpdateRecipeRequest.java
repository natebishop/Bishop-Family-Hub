package com.familyhub.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.familyhub.demo.model.RecipeConstraints;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class UpdateRecipeRequest {

    @Size(max = RecipeConstraints.TITLE_MAX_LENGTH, message = "Recipe title must be 160 characters or less")
    private String title;

    private String imageUrl;

    private List<@NotBlank @Size(max = RecipeConstraints.INGREDIENT_MAX_LENGTH) String> ingredients;

    private List<String> instructions;

    private String note;

    private String sourceUrl;

    private List<@NotBlank @Size(max = RecipeConstraints.TAG_MAX_LENGTH) String> tags;

    private Boolean favorite;

    private boolean titleSet;
    private boolean imageUrlSet;
    private boolean ingredientsSet;
    private boolean instructionsSet;
    private boolean noteSet;
    private boolean sourceUrlSet;
    private boolean tagsSet;
    private boolean favoriteSet;

    public UpdateRecipeRequest() {
    }

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.titleSet = true;
    }

    public String imageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        this.imageUrlSet = true;
    }

    public List<String> ingredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
        this.ingredientsSet = true;
    }

    public List<String> instructions() {
        return instructions;
    }

    public void setInstructions(List<String> instructions) {
        this.instructions = instructions;
        this.instructionsSet = true;
    }

    public String note() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
        this.noteSet = true;
    }

    public String sourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
        this.sourceUrlSet = true;
    }

    public List<String> tags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
        this.tagsSet = true;
    }

    public Boolean favorite() {
        return favorite;
    }

    public void setFavorite(Boolean favorite) {
        this.favorite = favorite;
        this.favoriteSet = true;
    }

    @JsonIgnore
    public boolean hasTitle() {
        return titleSet;
    }

    @JsonIgnore
    public boolean hasImageUrl() {
        return imageUrlSet;
    }

    @JsonIgnore
    public boolean hasIngredients() {
        return ingredientsSet;
    }

    @JsonIgnore
    public boolean hasInstructions() {
        return instructionsSet;
    }

    @JsonIgnore
    public boolean hasNote() {
        return noteSet;
    }

    @JsonIgnore
    public boolean hasSourceUrl() {
        return sourceUrlSet;
    }

    @JsonIgnore
    public boolean hasTags() {
        return tagsSet;
    }

    @JsonIgnore
    public boolean hasFavorite() {
        return favoriteSet;
    }
}
