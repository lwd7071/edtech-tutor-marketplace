package com.edtech.platform.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (!isSafeRequestId(requestId)) {
            requestId = UUID.randomUUID().toString();
        }

        long startedAt = System.nanoTime();
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("requestId", requestId)) {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            int status = response.getStatus();
            if (status >= 500) {
                log.error("requestId={} HTTP {} {} -> {} ({} ms)", requestId, request.getMethod(), request.getRequestURI(), status, durationMs);
            } else if (status >= 400) {
                log.warn("requestId={} HTTP {} {} -> {} ({} ms)", requestId, request.getMethod(), request.getRequestURI(), status, durationMs);
            } else {
                log.info("requestId={} HTTP {} {} -> {} ({} ms)", requestId, request.getMethod(), request.getRequestURI(), status, durationMs);
            }
        }
    }

    static boolean isSafeRequestId(String requestId) {
        return requestId != null && requestId.length() <= 100 && requestId.matches("[A-Za-z0-9._:-]+");
    }
}
