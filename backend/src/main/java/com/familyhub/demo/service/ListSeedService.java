package com.familyhub.demo.service;

import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.ListCategory;
import com.familyhub.demo.model.ListCategoryCatalogScope;
import com.familyhub.demo.model.ListKind;
import com.familyhub.demo.model.ListPreferences;
import com.familyhub.demo.repository.ListCategoryCatalogScopeRepository;
import com.familyhub.demo.repository.ListCategoryRepository;
import com.familyhub.demo.repository.ListPreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ListSeedService {

    private static final Map<ListKind, List<String>> STARTER_CATEGORY_NAMES = Map.of(
            ListKind.GROCERY, List.of("Produce", "Dairy", "Pantry", "Frozen", "Household"),
            ListKind.TODO, List.of("Urgent", "Soon", "Later")
    );

    private final ListPreferencesRepository listPreferencesRepository;
    private final ListCategoryRepository listCategoryRepository;
    private final ListCategoryCatalogScopeRepository scopeRepository;

    @Transactional
    public void seedDefaultsForFamily(Family family) {
        ensurePreferencesExist(family);
        createCatalogScopes(family);
        createStarterCategories(family);
    }

    private void ensurePreferencesExist(Family family) {
        if (listPreferencesRepository.findByFamily(family).isPresent()) {
            return;
        }

        ListPreferences preferences = new ListPreferences();
        preferences.setFamily(family);
        preferences.setShowCompletedByDefault(true);
        listPreferencesRepository.save(preferences);
    }

    private void createCatalogScopes(Family family) {
        List<ListCategoryCatalogScope> scopes = new ArrayList<>();
        for (ListKind kind : ListKind.values()) {
            ListCategoryCatalogScope scope = new ListCategoryCatalogScope();
            scope.setFamily(family);
            scope.setKind(kind);
            scopes.add(scope);
        }
        scopeRepository.saveAll(scopes);
        scopeRepository.flush();
    }

    private void createStarterCategories(Family family) {
        List<ListCategory> categories = new ArrayList<>();
        STARTER_CATEGORY_NAMES.forEach((kind, names) -> {
            for (int i = 0; i < names.size(); i++) {
                ListCategory category = new ListCategory();
                category.setFamily(family);
                category.setKind(kind);
                category.setName(names.get(i));
                category.setSortOrder(i);
                categories.add(category);
            }
        });
        if (!categories.isEmpty()) {
            listCategoryRepository.saveAll(categories);
        }
    }
}
