package com.familyhub.demo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum MealEntrySourceType {
    RECIPE("recipe"),
    QUICK("quick");

    private final String wireValue;

    MealEntrySourceType(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    @JsonCreator
    public static MealEntrySourceType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Meal entry source type is required.");
        }
        return Arrays.stream(values())
                .filter(type -> type.wireValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown meal entry source type: " + value));
    }
}
