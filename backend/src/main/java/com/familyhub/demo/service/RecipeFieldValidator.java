package com.familyhub.demo.service;

import com.familyhub.demo.exception.BadRequestException;
import com.familyhub.demo.model.RecipeConstraints;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

final class RecipeFieldValidator {
    private RecipeFieldValidator() {
    }

    static String requiredTitle(String value) {
        String normalized = optionalText(value);
        if (normalized == null) {
            throw new BadRequestException("Recipe title is required.");
        }
        validateMaxLength(normalized, RecipeConstraints.TITLE_MAX_LENGTH, "Recipe title");
        return normalized;
    }

    static String optionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    static String optionalHttpUrl(String value, String fieldLabel) {
        String normalized = optionalText(value);
        if (normalized == null) {
            return null;
        }

        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if ((!scheme.equals("http") && !scheme.equals("https")) || uri.getHost() == null || uri.getHost().isBlank()) {
                throw invalidUrl(fieldLabel);
            }
            return normalized;
        } catch (URISyntaxException | IllegalArgumentException ex) {
            throw invalidUrl(fieldLabel);
        }
    }

    /**
     * Lenient variant for imported content: returns the normalized http/https URL, or {@code null}
     * when the value is absent or not a usable http/https URL. Unlike {@link #optionalHttpUrl},
     * this never throws, so a cosmetic field (e.g. an unusable image URL) cannot fail the whole import.
     */
    static String optionalHttpUrlOrNull(String value) {
        try {
            return optionalHttpUrl(value, "Recipe URL");
        } catch (BadRequestException ex) {
            return null;
        }
    }

    static List<String> normalizedIngredients(List<String> values) {
        return normalizedList(values).stream()
                .peek(value -> validateMaxLength(value, RecipeConstraints.INGREDIENT_MAX_LENGTH, "Recipe ingredient"))
                .toList();
    }

    static List<String> normalizedInstructions(List<String> values) {
        return normalizedList(values);
    }

    static List<String> normalizedTags(List<String> values) {
        return new LinkedHashSet<>(normalizedList(values)).stream()
                .peek(value -> validateMaxLength(value, RecipeConstraints.TAG_MAX_LENGTH, "Recipe tag"))
                .toList();
    }

    private static List<String> normalizedList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(RecipeFieldValidator::optionalText)
                .filter(value -> value != null)
                .toList();
    }

    private static void validateMaxLength(String value, int maxLength, String fieldLabel) {
        if (value.length() > maxLength) {
            throw new BadRequestException(fieldLabel + " must be " + maxLength + " characters or less.");
        }
    }

    private static BadRequestException invalidUrl(String fieldLabel) {
        return new BadRequestException(fieldLabel + " must be a valid http or https URL.");
    }
}
