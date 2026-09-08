package com.familyhub.demo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ListCategoryDisplayMode {
    GROUPED("grouped"),
    FLAT("flat");

    private final String wireValue;

    ListCategoryDisplayMode(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    @JsonCreator
    public static ListCategoryDisplayMode fromValue(String value) {
        return Arrays.stream(values())
                .filter(mode -> mode.wireValue.equalsIgnoreCase(value) || mode.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown list category display mode: " + value));
    }
}
