package com.familyhub.demo.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, UUID uuid) {
        super("%s not found: %s".formatted(resourceName, uuid));
    }
}
