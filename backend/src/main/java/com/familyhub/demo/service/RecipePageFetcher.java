package com.familyhub.demo.service;

import java.io.IOException;
import java.net.URI;

public interface RecipePageFetcher {
    FetchedRecipePage fetch(URI uri) throws IOException;
}
