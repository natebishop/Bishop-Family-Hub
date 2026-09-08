package com.familyhub.demo.service;

public record FetchedRecipePage(int statusCode, String body, String redirectLocation) {

    public static FetchedRecipePage ok(String body) {
        return new FetchedRecipePage(200, body, null);
    }

    public static FetchedRecipePage redirect(String location) {
        return new FetchedRecipePage(302, "", location);
    }

    public static FetchedRecipePage status(int statusCode) {
        return new FetchedRecipePage(statusCode, "", null);
    }
}
