package com.familyhub.demo.service;

import java.util.List;

public record ImportedRecipe(
        String title,
        String imageUrl,
        List<String> ingredients,
        List<String> instructions,
        String note,
        String sourceUrl,
        List<String> tags,
        boolean favorite
) {
}
