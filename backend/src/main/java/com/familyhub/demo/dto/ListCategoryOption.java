package com.familyhub.demo.dto;

import com.familyhub.demo.model.ListKind;

import java.util.UUID;

public record ListCategoryOption(UUID id, ListKind kind, String name, int sortOrder) {}
