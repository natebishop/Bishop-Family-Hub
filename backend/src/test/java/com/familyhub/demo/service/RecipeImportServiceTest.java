package com.familyhub.demo.service;

import com.familyhub.demo.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecipeImportServiceTest {

    private RecordingRecipePageFetcher pageFetcher;
    private RecipeImportService recipeImportService;

    @BeforeEach
    void setUp() {
        pageFetcher = new RecordingRecipePageFetcher();
        recipeImportService = new RecipeImportService(new ObjectMapper(), pageFetcher, new RecipeImportNetworkGuard());
    }

    @Test
    void parseHtml_extractsJsonLdRecipeFields() {
        String html = """
                <html>
                  <head>
                    <script type="application/ld+json">
                    {
                      "@context":"https://schema.org",
                      "@type":"Recipe",
                      "name":"Weeknight Tacos",
                      "image":"https://cdn.example.com/tacos.jpg",
                      "recipeIngredient":["1 lb beef","8 tortillas"],
                      "recipeInstructions":["Brown beef","Serve in tortillas"]
                    }
                    </script>
                  </head>
                </html>
                """;

        ImportedRecipe imported = recipeImportService.parseHtml("https://example.com/tacos", html);

        assertThat(imported.title()).isEqualTo("Weeknight Tacos");
        assertThat(imported.imageUrl()).isEqualTo("https://cdn.example.com/tacos.jpg");
        assertThat(imported.ingredients()).containsExactly("1 lb beef", "8 tortillas");
        assertThat(imported.instructions()).containsExactly("Brown beef", "Serve in tortillas");
        assertThat(imported.sourceUrl()).isEqualTo("https://example.com/tacos");
    }

    @Test
    void parseHtml_resolvesProtocolRelativeImageAgainstBase() {
        String html = """
                <script type="application/ld+json">
                {
                  "@type":"Recipe",
                  "name":"Tacos",
                  "image":"//cdn.example.com/tacos.jpg"
                }
                </script>
                """;

        ImportedRecipe imported = recipeImportService.parseHtml("https://example.com/recipes/tacos", html);

        assertThat(imported.imageUrl()).isEqualTo("https://cdn.example.com/tacos.jpg");
    }

    @Test
    void parseHtml_resolvesRootRelativeImageAgainstBase() {
        String html = """
                <script type="application/ld+json">
                {
                  "@type":"Recipe",
                  "name":"Tacos",
                  "image":"/img/tacos.jpg"
                }
                </script>
                """;

        ImportedRecipe imported = recipeImportService.parseHtml("https://example.com/recipes/tacos", html);

        assertThat(imported.imageUrl()).isEqualTo("https://example.com/img/tacos.jpg");
    }

    @Test
    void parseHtml_extractsHowToStepInstructionText() {
        String html = """
                <script type="application/ld+json">
                {
                  "@type":"Recipe",
                  "name":"Pancakes",
                  "recipeInstructions":[
                    {"@type":"HowToStep","text":"Mix batter"},
                    {"@type":"HowToStep","name":"Cook on griddle"}
                  ]
                }
                </script>
                """;

        ImportedRecipe imported = recipeImportService.parseHtml("https://example.com/pancakes", html);

        assertThat(imported.instructions()).containsExactly("Mix batter", "Cook on griddle");
    }

    @Test
    void parseHtml_skipsMalformedJsonLdScriptAndUsesLaterRecipeScript() {
        String html = """
                <script type="application/ld+json">
                { not valid json
                </script>
                <script type="application/ld+json">
                {
                  "@type":"Recipe",
                  "name":"Resilient Pasta",
                  "recipeIngredient":["1 lb pasta"],
                  "recipeInstructions":["Boil pasta"]
                }
                </script>
                """;

        ImportedRecipe imported = recipeImportService.parseHtml("https://example.com/pasta", html);

        assertThat(imported.title()).isEqualTo("Resilient Pasta");
        assertThat(imported.ingredients()).containsExactly("1 lb pasta");
        assertThat(imported.instructions()).containsExactly("Boil pasta");
    }

    @Test
    void importFromUrl_rejectsLoopbackTargetBeforeFetching() {
        assertThatThrownBy(() -> recipeImportService.importFromUrl("http://127.0.0.1:8080/private"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Could not import recipe");

        assertThat(pageFetcher.requests()).isEmpty();
    }

    @Test
    void importFromUrl_rejectsPrivateNetworkTargetBeforeFetching() {
        for (String url : List.of(
                "http://10.0.0.1/recipe",
                "http://172.16.0.1/recipe",
                "http://192.168.1.10/recipe",
                "http://169.254.1.10/recipe",
                "http://[::ffff:192.168.1.1]/recipe"
        )) {
            assertThatThrownBy(() -> recipeImportService.importFromUrl(url))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Could not import recipe");
        }

        assertThat(pageFetcher.requests()).isEmpty();
    }

    @Test
    void importFromUrl_rejectsUnsupportedSchemeBeforeFetching() {
        assertThatThrownBy(() -> recipeImportService.importFromUrl("file:///etc/passwd"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Could not import recipe");

        assertThat(pageFetcher.requests()).isEmpty();
    }

    @Test
    void importFromUrl_revalidatesRedirectTargetsBeforeFollowing() {
        pageFetcher.enqueue(FetchedRecipePage.redirect("http://127.0.0.1/private"));

        assertThatThrownBy(() -> recipeImportService.importFromUrl("https://example.com/recipe"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Could not import recipe");

        assertThat(pageFetcher.requests()).containsExactly(URI.create("https://example.com/recipe"));
    }

    @Test
    void importFromUrl_rejectsOversizedBodiesBeforeParsing() {
        pageFetcher.enqueue(FetchedRecipePage.ok("x".repeat(1_000_001)));

        assertThatThrownBy(() -> recipeImportService.importFromUrl("https://example.com/recipe"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Could not import recipe");
    }

    @Test
    void validatingDnsResolverRejectsPrivateAddressReturnedAtConnectionTime() throws Exception {
        RecipeImportNetworkGuard guard = new RecipeImportNetworkGuard();
        ValidatingDnsResolver resolver = new ValidatingDnsResolver(
                host -> new InetAddress[]{InetAddress.getByName("10.0.0.5")},
                guard
        );

        assertThatThrownBy(() -> resolver.resolve("attacker.example"))
                .isInstanceOf(UnknownHostException.class)
                .hasMessageContaining("non-public");
    }

    private static final class RecordingRecipePageFetcher implements RecipePageFetcher {
        private final List<FetchedRecipePage> responses = new java.util.ArrayList<>();
        private final List<URI> requests = new java.util.ArrayList<>();

        void enqueue(FetchedRecipePage response) {
            responses.add(response);
        }

        List<URI> requests() {
            return requests;
        }

        @Override
        public FetchedRecipePage fetch(URI uri) {
            requests.add(uri);
            if (responses.isEmpty()) {
                return FetchedRecipePage.ok("""
                        <script type="application/ld+json">
                        {
                          "@type":"Recipe",
                          "name":"Fetched Recipe"
                        }
                        </script>
                        """);
            }
            return responses.remove(0);
        }
    }
}
