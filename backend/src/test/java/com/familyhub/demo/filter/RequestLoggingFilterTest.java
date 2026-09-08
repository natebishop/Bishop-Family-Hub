package com.familyhub.demo.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @AfterEach
    void cleanMdc() {
        MDC.clear();
    }

    @Test
    void setsCorrelationIdInMdcDuringRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] capturedId = {null};
        FilterChain chain = (req, res) -> capturedId[0] = MDC.get("correlationId");

        filter.doFilter(request, response, chain);

        assertThat(capturedId[0]).isNotNull().hasSize(36);
    }

    @Test
    void clearsCorrelationIdAfterRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void usesProvidedCorrelationIdFromHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events");
        request.addHeader("X-Correlation-ID", "my-custom-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] capturedId = {null};
        FilterChain chain = (req, res) -> capturedId[0] = MDC.get("correlationId");

        filter.doFilter(request, response, chain);

        assertThat(capturedId[0]).isEqualTo("my-custom-id");
    }

    @Test
    void addsCorrelationIdToResponseHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader("X-Correlation-ID")).isNotNull();
    }

    @Test
    void rejectsOversizedCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events");
        request.addHeader("X-Correlation-ID", "a".repeat(65));
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] capturedId = {null};
        FilterChain chain = (req, res) -> capturedId[0] = MDC.get("correlationId");

        filter.doFilter(request, response, chain);

        assertThat(capturedId[0]).hasSize(36); // falls back to UUID
    }

    @Test
    void stripsNewlinesFromCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events");
        request.addHeader("X-Correlation-ID", "valid-id\r\ninjected-header");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] capturedId = {null};
        FilterChain chain = (req, res) -> capturedId[0] = MDC.get("correlationId");

        filter.doFilter(request, response, chain);

        assertThat(capturedId[0]).doesNotContain("\r", "\n");
    }

    @Test
    void skipsHealthEndpoint() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean[] chainCalled = {false};
        filter.doFilter(request, response, (req, res) -> chainCalled[0] = true);

        assertThat(chainCalled[0]).isTrue();
    }
}
