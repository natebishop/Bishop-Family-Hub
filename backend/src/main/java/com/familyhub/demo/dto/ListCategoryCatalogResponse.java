package com.familyhub.demo.dto;

import com.familyhub.demo.model.ListKind;

import java.util.List;

public record ListCategoryCatalogResponse(
        ListKind kind, long groupedListCount, List<ListCategoryManagementEntry> categories
) {}
