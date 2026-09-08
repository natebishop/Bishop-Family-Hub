package com.familyhub.demo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FamilyColor {
    CORAL,
    TEAL,
    GREEN,
    PURPLE,
    YELLOW,
    PINK,
    ORANGE;

    // Outbound values turn to lowercase: CORAL -> coral
    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }

    // Inbound values become uppercase, matching Java Enum convention
    @JsonCreator
    public static FamilyColor fromValue(String value) {
        return FamilyColor.valueOf(value.toUpperCase());
    }
}
