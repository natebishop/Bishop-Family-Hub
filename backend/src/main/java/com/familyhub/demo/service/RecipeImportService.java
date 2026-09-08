package com.familyhub.demo.service;

import com.familyhub.demo.exception.BadRequestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class RecipeImportService {
    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_RESPONSE_BYTES = 1_000_000;
    private static final String IMPORT_FAILURE_MESSAGE = "Could not import recipe.";

    private final ObjectMapper objectMapper;
    private final RecipePageFetcher pageFetcher;
    private final RecipeImportNetworkGuard networkGuard;

    public RecipeImportService(
            ObjectMapper objectMapper,
            RecipePageFetcher pageFetcher,
            RecipeImportNetworkGuard networkGuard
    ) {
        this.objectMapper = objectMapper;
        this.pageFetcher = pageFetcher;
        this.networkGuard = networkGuard;
    }

    public ImportedRecipe importFromUrl(String url) {
        try {
            URI uri = networkGuard.validatePublicHttpUri(url);
            for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
                FetchedRecipePage response = pageFetcher.fetch(uri);
                int statusCode = response.statusCode();
                if (isRedirect(statusCode)) {
                    if (redirects == MAX_REDIRECTS) {
                        throw importFailure();
                    }
                    uri = networkGuard.validatePublicHttpUri(resolveRedirect(uri, response));
                    continue;
                }
                if (statusCode < 200 || statusCode >= 300) {
                    throw importFailure();
                }

                String html = readCappedBody(response.body());
                return parseHtml(uri.toString(), html);
            }
            throw importFailure();
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw importFailure();
        }
    }

    public ImportedRecipe parseHtml(String sourceUrl, String html) {
        Document document = Jsoup.parse(html, sourceUrl);
        for (Element script : document.select("script[type=application/ld+json]")) {
            try {
                Optional<JsonNode> recipeNode = findRecipeNode(objectMapper.readTree(script.data()));
                if (recipeNode.isPresent()) {
                    return toImportedRecipe(sourceUrl, document, recipeNode.get());
                }
            } catch (JacksonException ignored) {
                // Recipe pages often include several JSON-LD blocks. Ignore malformed blocks and keep looking.
            }
        }
        throw importFailure();
    }

    private String readCappedBody(String body) {
        if (body == null || body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_RESPONSE_BYTES) {
            throw importFailure();
        }
        return body;
    }

    private URI resolveRedirect(URI currentUri, FetchedRecipePage response) {
        String location = response.redirectLocation();
        if (location == null || location.isBlank()) {
            throw importFailure();
        }
        return currentUri.resolve(location);
    }

    private Optional<JsonNode> findRecipeNode(JsonNode root) {
        if (isRecipe(root)) {
            return Optional.of(root);
        }
        if (root.isArray()) {
            return StreamSupport.stream(root.spliterator(), false)
                    .map(this::findRecipeNode)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
        }
        JsonNode graph = root.path("@graph");
        if (graph.isArray()) {
            return StreamSupport.stream(graph.spliterator(), false)
                    .filter(this::isRecipe)
                    .findFirst();
        }
        return Optional.empty();
    }

    private boolean isRecipe(JsonNode node) {
        JsonNode type = node.path("@type");
        if (type.isTextual()) {
            return "Recipe".equalsIgnoreCase(type.asText());
        }
        if (type.isArray()) {
            return StreamSupport.stream(type.spliterator(), false)
                    .anyMatch(candidate -> "Recipe".equalsIgnoreCase(candidate.asText()));
        }
        return false;
    }

    private ImportedRecipe toImportedRecipe(String sourceUrl, Document document, JsonNode recipeNode) {
        String title = textValue(recipeNode.path("name"));
        if (title == null || title.isBlank()) {
            title = document.title();
        }
        if (title == null || title.isBlank()) {
            throw importFailure();
        }

        return new ImportedRecipe(
                title.trim(),
                resolveAgainstBase(sourceUrl, firstImage(recipeNode.path("image"))),
                stringList(recipeNode.path("recipeIngredient")),
                instructionList(recipeNode.path("recipeInstructions")),
                null,
                sourceUrl,
                List.of(),
                false
        );
    }

    /**
     * Resolves a possibly relative or protocol-relative URL (e.g. {@code //cdn/x.jpg}, {@code /img/x.jpg})
     * against the page's source URL so it becomes absolute. Returns {@code null} when the value is blank
     * or cannot be resolved to an absolute URL; the write path then drops unusable values instead of
     * failing the whole import.
     */
    private String resolveAgainstBase(String sourceUrl, String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        try {
            URI resolved = URI.create(sourceUrl).resolve(rawUrl.trim());
            return resolved.isAbsolute() ? resolved.toString() : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String firstImage(JsonNode imageNode) {
        if (imageNode.isMissingNode() || imageNode.isNull()) {
            return null;
        }
        if (imageNode.isTextual()) {
            return blankToNull(imageNode.asText());
        }
        if (imageNode.isArray() && !imageNode.isEmpty()) {
            return firstImage(imageNode.get(0));
        }
        if (imageNode.isObject()) {
            String url = textValue(imageNode.path("url"));
            if (url == null) {
                url = textValue(imageNode.path("contentUrl"));
            }
            return blankToNull(url);
        }
        return null;
    }

    private List<String> stringList(JsonNode node) {
        if (!node.isArray()) {
            String value = textValue(node);
            return value == null ? List.of() : List.of(value);
        }

        List<String> values = new ArrayList<>();
        for (JsonNode element : node) {
            String value = textValue(element);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private List<String> instructionList(JsonNode node) {
        if (!node.isArray()) {
            String value = instructionText(node);
            return value == null ? List.of() : List.of(value);
        }

        List<String> values = new ArrayList<>();
        for (JsonNode element : node) {
            if (element.path("itemListElement").isArray()) {
                values.addAll(instructionList(element.path("itemListElement")));
            } else {
                String value = instructionText(element);
                if (value != null) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private String instructionText(JsonNode node) {
        if (node.isTextual()) {
            return blankToNull(node.asText());
        }
        if (node.isObject()) {
            String text = textValue(node.path("text"));
            if (text == null) {
                text = textValue(node.path("name"));
            }
            return blankToNull(text);
        }
        return null;
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return blankToNull(node.asText());
        }
        return null;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == 301
                || statusCode == 302
                || statusCode == 303
                || statusCode == 307
                || statusCode == 308;
    }

    private static BadRequestException importFailure() {
        return new BadRequestException(IMPORT_FAILURE_MESSAGE);
    }
}
