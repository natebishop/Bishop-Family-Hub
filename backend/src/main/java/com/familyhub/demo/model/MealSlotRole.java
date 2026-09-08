package com.familyhub.demo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum MealSlotRole {
    PRIMARY("primary"),
    EXTRA("extra");

    private final String wireValue;

    MealSlotRole(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    @JsonCreator
    public static MealSlotRole fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Meal slot role is required.");
        }
        return Arrays.stream(values())
                .filter(role -> role.wireValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown meal slot role: " + value));
    }
}
