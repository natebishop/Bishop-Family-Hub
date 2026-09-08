package com.familyhub.demo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum MealCollisionMode {
    REPLACE_PRIMARY("replace_primary"),
    ADD_AS_EXTRA("add_as_extra");

    private final String wireValue;

    MealCollisionMode(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    @JsonCreator
    public static MealCollisionMode fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Meal collision mode is required.");
        }
        return Arrays.stream(values())
                .filter(mode -> mode.wireValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown meal collision mode: " + value));
    }
}
