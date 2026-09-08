package com.familyhub.demo.service;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpRecipePageFetcherTest {

    @Test
    void fetch_consumesNonSuccessResponseBody() throws Exception {
        CloseAwareInputStream body = new CloseAwareInputStream("error".getBytes());
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(500);
        response.setEntity(new InputStreamEntity(body, 5, ContentType.TEXT_HTML));
        HttpRecipePageFetcher fetcher = new HttpRecipePageFetcher(new StubHttpClient(response));

        FetchedRecipePage fetched = fetcher.fetch(URI.create("https://example.com/recipe"));

        assertThat(fetched.statusCode()).isEqualTo(500);
        assertThat(body.closed()).isTrue();
    }

    @Test
    void fetch_rejectsResponseWithOversizedContentLength() {
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(200);
        response.setEntity(new InputStreamEntity(
                new ByteArrayInputStream(new byte[0]),
                HttpRecipePageFetcher.MAX_RESPONSE_BYTES + 1L,
                ContentType.TEXT_HTML
        ));
        HttpRecipePageFetcher fetcher = new HttpRecipePageFetcher(new StubHttpClient(response));

        assertThatThrownBy(() -> fetcher.fetch(URI.create("https://example.com/recipe")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("maximum size");
    }

    @Test
    void fetch_rejectsResponseThatExceedsCapWhileReading() {
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(200);
        response.setEntity(new InputStreamEntity(
                new ByteArrayInputStream(new byte[HttpRecipePageFetcher.MAX_RESPONSE_BYTES + 1]),
                -1,
                ContentType.TEXT_HTML
        ));
        HttpRecipePageFetcher fetcher = new HttpRecipePageFetcher(new StubHttpClient(response));

        assertThatThrownBy(() -> fetcher.fetch(URI.create("https://example.com/recipe")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("maximum size");
    }

    @Test
    void fetch_rejectsBodyThatExceedsTotalReadBudget() {
        // A trickle stream that never errors and never trips the size cap, but whose cumulative
        // read time (simulated) crosses the absolute read budget.
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(200);
        response.setEntity(new InputStreamEntity(new TricklingInputStream(10_000), -1, ContentType.TEXT_HTML));

        // First call sets the deadline at t=0 (+10s budget); each later check advances 3s, so the
        // budget is crossed mid-stream after a few small reads, none of which hit the byte cap.
        AtomicLong now = new AtomicLong(0);
        LongSupplier advancingClock = () -> now.getAndAdd(java.time.Duration.ofSeconds(3).toNanos());
        HttpRecipePageFetcher fetcher =
                new HttpRecipePageFetcher(new StubHttpClient(response), advancingClock);

        assertThatThrownBy(() -> fetcher.fetch(URI.create("https://example.com/recipe")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("maximum read time");
    }

    private static final class StubHttpClient extends CloseableHttpClient {
        private final BasicClassicHttpResponse response;

        private StubHttpClient(BasicClassicHttpResponse response) {
            this.response = response;
        }

        @Override
        protected CloseableHttpResponse doExecute(HttpHost target, ClassicHttpRequest request, HttpContext context) {
            return CloseableHttpResponse.adapt(response);
        }

        @Override
        public void close(CloseMode closeMode) {
        }

        @Override
        public void close() {
        }
    }

    private static final class TricklingInputStream extends InputStream {
        private int remaining;

        private TricklingInputStream(int totalBytes) {
            this.remaining = totalBytes;
        }

        @Override
        public int read() {
            if (remaining <= 0) {
                return -1;
            }
            remaining--;
            return 'x';
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (remaining <= 0) {
                return -1;
            }
            // One byte per call to model a slow drip, regardless of buffer size.
            b[off] = 'x';
            remaining--;
            return 1;
        }
    }

    private static final class CloseAwareInputStream extends ByteArrayInputStream {
        private boolean closed;

        private CloseAwareInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        boolean closed() {
            return closed;
        }
    }
}
