package com.ecommerce.platform.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!("POST".equals(request.getMethod()) && (uri.endsWith("/auth/login") || uri.endsWith("/auth/register")))) {
            chain.doFilter(request, response);
            return;
        }
        Bucket bucket = buckets.computeIfAbsent(request.getRemoteAddr(), ignored -> Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)))).build());
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Too many authentication attempts. Please retry later.\"}");
    }
}
