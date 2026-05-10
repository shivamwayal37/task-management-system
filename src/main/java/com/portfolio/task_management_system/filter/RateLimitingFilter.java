package com.portfolio.task_management_system.filter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-Rate-Limit-Remaining";

    private final Map<String, ClientBucket> buckets = new ConcurrentHashMap<>();
    private final long capacity;
    private final Duration refillDuration;
    private final Duration bucketTtl;

    public RateLimitingFilter(
            @Value("${rate-limit.capacity:100}") long capacity,
            @Value("${rate-limit.refill-duration:1m}") Duration refillDuration,
            @Value("${rate-limit.bucket-ttl:15m}") Duration bucketTtl) {
        this.capacity = capacity;
        this.refillDuration = refillDuration;
        this.bucketTtl = bucketTtl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        ClientBucket clientBucket = buckets.computeIfAbsent(clientIp, ignored -> new ClientBucket(createBucket()));
        clientBucket.touch();

        ConsumptionProbe probe = clientBucket.bucket().tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
        response.setHeader(RETRY_AFTER_HEADER, String.valueOf(retryAfterSeconds));
        writeTooManyRequestsResponse(request, response);
        log.warn("Rate limit exceeded clientIp={} path={}", clientIp, request.getRequestURI());
    }

    @Scheduled(fixedDelayString = "${rate-limit.cleanup-interval:5m}")
    public void cleanupIdleBuckets() {
        long expiresBefore = System.currentTimeMillis() - bucketTtl.toMillis();
        buckets.entrySet().removeIf(entry -> entry.getValue().lastAccessedAt() < expiresBefore);
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, refillDuration)
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private void writeTooManyRequestsResponse(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        String responseBody = """
                {
                  "timestamp": "%s",
                  "status": %d,
                  "error": "%s",
                  "message": "%s",
                  "path": "%s",
                  "validationErrors": null
                }
                """.formatted(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                "Rate limit exceeded. Try again later.",
                escapeJson(request.getRequestURI()));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(responseBody);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class ClientBucket {
        private final Bucket bucket;
        private volatile long lastAccessedAt;

        private ClientBucket(Bucket bucket) {
            this.bucket = bucket;
            touch();
        }

        private Bucket bucket() {
            return bucket;
        }

        private long lastAccessedAt() {
            return lastAccessedAt;
        }

        private void touch() {
            lastAccessedAt = System.currentTimeMillis();
        }
    }
}
