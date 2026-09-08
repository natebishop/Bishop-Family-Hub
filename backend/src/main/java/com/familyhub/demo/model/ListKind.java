package com.familyhub.demo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ListKind {
    GROCERY("grocery"),
    TODO("to-do"),
    GENERAL("general");

    private final String wireValue;

    ListKind(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    @JsonCreator
    public static ListKind fromValue(String value) {
        return Arrays.stream(values())
                .filter(kind -> kind.wireValue.equalsIgnoreCase(value) || kind.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown list kind: " + value));
    }
}
