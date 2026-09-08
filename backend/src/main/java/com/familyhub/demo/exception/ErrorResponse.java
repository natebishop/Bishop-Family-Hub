package com.familyhub.demo.exception;


import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        int httpStatus,
        String path,
        String message,
        Instant timeStamp,
        List<ValidationError> errors
) {

    public ErrorResponse(int httpStatus, String path, String message) {
        this(httpStatus, path, message, Instant.now(), null);
    }

    public ErrorResponse(int httpStatus, String path, String message, List<ValidationError> errors) {
        this(httpStatus, path, message, Instant.now(), errors);
    }

    public record ValidationError(String field, String message) {}
}

