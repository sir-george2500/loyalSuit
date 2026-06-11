package com.loyalsuit.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds defence-in-depth response headers to every response. The API serves JSON, so it locks
 * the browser surface right down (no framing, no sniffing, a tight CSP). The Swagger UI is the
 * one HTML surface we serve, so it gets a CSP permissive enough to render its own bundle.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String API_CSP = "default-src 'none'; frame-ancestors 'none'";
    private static final String DOCS_CSP =
            "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        // HSTS is honoured by browsers only over HTTPS; harmless over plain HTTP in dev.
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setHeader("Content-Security-Policy", isDocs(request) ? DOCS_CSP : API_CSP);

        filterChain.doFilter(request, response);
    }

    private static boolean isDocs(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui") || path.startsWith("/api-docs") || path.equals("/swagger-ui.html");
    }
}
