package com.familyhub.demo.service;

import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.LongSupplier;

@Component
public class HttpRecipePageFetcher implements RecipePageFetcher {
    static final int MAX_RESPONSE_BYTES = 1_000_000;
    private static final Timeout CONNECT_TIMEOUT = Timeout.ofSeconds(3);
    private static final Timeout READ_TIMEOUT = Timeout.ofSeconds(5);
    // Absolute wall-clock cap on the whole body read. READ_TIMEOUT only bounds inactivity between
    // reads, so a server trickling bytes just under both that gap and the size cap could otherwise
    // hold a pooled connection indefinitely.
    static final Duration MAX_TOTAL_READ = Duration.ofSeconds(10);
    private static final String USER_AGENT = "FamilyHubRecipeImporter/1.0";

    private final CloseableHttpClient httpClient;
    private final LongSupplier nanoTimeSource;

    @Autowired
    public HttpRecipePageFetcher(RecipeImportNetworkGuard networkGuard) {
        this(createHttpClient(networkGuard), System::nanoTime);
    }

    HttpRecipePageFetcher(CloseableHttpClient httpClient) {
        this(httpClient, System::nanoTime);
    }

    HttpRecipePageFetcher(CloseableHttpClient httpClient, LongSupplier nanoTimeSource) {
        this.httpClient = httpClient;
        this.nanoTimeSource = nanoTimeSource;
    }

    @Override
    public FetchedRecipePage fetch(URI uri) throws IOException {
        HttpGet request = new HttpGet(uri);
        request.setHeader("User-Agent", USER_AGENT);

        return httpClient.execute(request, this::toFetchedPage);
    }

    private FetchedRecipePage toFetchedPage(ClassicHttpResponse response) throws IOException {
        int statusCode = response.getCode();
        String redirectLocation = firstHeaderValue(response, "location");

        if (isRedirect(statusCode) || statusCode < 200 || statusCode >= 300) {
            EntityUtils.consumeQuietly(response.getEntity());
            return new FetchedRecipePage(statusCode, "", redirectLocation);
        }

        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return FetchedRecipePage.ok("");
        }
        if (entity.getContentLength() > MAX_RESPONSE_BYTES) {
            EntityUtils.consumeQuietly(entity);
            throw new IOException("Recipe import response exceeded maximum size.");
        }

        try (InputStream body = entity.getContent()) {
            return FetchedRecipePage.ok(readCappedBody(body));
        }
    }

    private static CloseableHttpClient createHttpClient(RecipeImportNetworkGuard networkGuard) {
        ValidatingDnsResolver dnsResolver = new ValidatingDnsResolver(
                SystemDefaultDnsResolver.INSTANCE::resolve,
                networkGuard
        );
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(dnsResolver)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(CONNECT_TIMEOUT)
                        .setSocketTimeout(READ_TIMEOUT)
                        .setValidateAfterInactivity(TimeValue.ofSeconds(1))
                        .build())
                .setMaxConnTotal(20)
                .setMaxConnPerRoute(5)
                .build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setRedirectsEnabled(false)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT)
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setResponseTimeout(READ_TIMEOUT)
                .setHardCancellationEnabled(true)
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .disableAutomaticRetries()
                .disableRedirectHandling()
                .disableContentCompression()
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();
    }

    private String readCappedBody(InputStream body) throws IOException {
        long deadline = nanoTimeSource.getAsLong() + MAX_TOTAL_READ.toNanos();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = body.read(buffer)) != -1) {
                if (nanoTimeSource.getAsLong() > deadline) {
                    throw new IOException("Recipe import exceeded maximum read time.");
                }
                total += read;
                if (total > MAX_RESPONSE_BYTES) {
                    throw new IOException("Recipe import response exceeded maximum size.");
                }
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8);
        }
    }

    private String firstHeaderValue(ClassicHttpResponse response, String name) {
        Header header = response.getFirstHeader(name);
        return header == null ? null : header.getValue();
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == 301
                || statusCode == 302
                || statusCode == 303
                || statusCode == 307
                || statusCode == 308;
    }
}
