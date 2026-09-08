package com.familyhub.demo.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final Set<String> SKIP_PATHS = Set.of("/api/health");
    private static final int MAX_CORRELATION_ID_LENGTH = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank() || correlationId.length() > MAX_CORRELATION_ID_LENGTH) {
            correlationId = UUID.randomUUID().toString();
        } else {
            correlationId = correlationId.replaceAll("[\\r\\n]", "");
        }

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        if (SKIP_PATHS.contains(request.getRequestURI())) {
            try {
                filterChain.doFilter(request, response);
            } finally {
                MDC.remove(CORRELATION_ID_MDC_KEY);
            }
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            log.info(">>> {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("<<< {} {} status={} duration={}ms",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), duration);
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
