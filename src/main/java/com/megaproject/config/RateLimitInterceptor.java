package com.megaproject.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong(System.currentTimeMillis());
    private static final long CLEANUP_INTERVAL_MS = 5 * 60 * 1000L;

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) clientIp = request.getRemoteAddr();
        if (clientIp != null && clientIp.contains(",")) clientIp = clientIp.split(",")[0].trim();

        periodicCleanup();

        TokenBucket bucket = buckets.computeIfAbsent(clientIp, k -> new TokenBucket(100, 100));

        if (bucket.tryConsume()) return true;

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", "60");
        response.getWriter().write("{\"error\":\"Too many requests\"}");
        return false;
    }

    private void periodicCleanup() {
        long now = System.currentTimeMillis();
        long last = lastCleanup.get();
        if (now - last < CLEANUP_INTERVAL_MS) return;
        if (!lastCleanup.compareAndSet(last, now)) return; // only one thread cleans
        // Remove buckets not accessed in last 10 minutes
        buckets.entrySet().removeIf(e -> e.getValue().isStale(now, 10 * 60 * 1000L));
    }

    public static class TokenBucket {
        private final long capacity;
        private final long refillRatePerMinute;
        private long tokens;
        private long lastRefillTimestamp;
        private volatile long lastAccessTime;

        public TokenBucket(long capacity, long refillRatePerMinute) {
            this.capacity = capacity;
            this.refillRatePerMinute = refillRatePerMinute;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
            this.lastAccessTime = lastRefillTimestamp;
        }

        public synchronized boolean tryConsume() {
            refill();
            lastAccessTime = System.currentTimeMillis();
            if (tokens > 0) { tokens--; return true; }
            return false;
        }

        public boolean isStale(long now, long maxIdleMs) {
            return now - lastAccessTime > maxIdleMs;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTimestamp;
            long toAdd = (elapsed * refillRatePerMinute) / 60_000L;
            if (toAdd > 0) {
                tokens = Math.min(capacity, tokens + toAdd);
                lastRefillTimestamp = now;
            }
        }
    }
}