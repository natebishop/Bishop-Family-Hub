package com.familyhub.demo.dto;

public record ApiResponse<T>(T data, String message) {
}