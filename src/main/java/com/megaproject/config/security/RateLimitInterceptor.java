package com.megaproject.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong(System.currentTimeMillis());

    private static final long CAPACITY               = 100L;
    private static final long REFILL_PER_MINUTE      = 100L;
    private static final long CLEANUP_INTERVAL_MS    = 5 * 60_000L;
    private static final long STALE_THRESHOLD_MS     = 10 * 60_000L;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest req,
                             @NotNull HttpServletResponse res,
                             @NotNull Object handler) throws Exception {

        String ip = resolveIp(req);
        maybeCleanup();

        TokenBucket bucket = buckets.computeIfAbsent(ip, k -> new TokenBucket(CAPACITY, REFILL_PER_MINUTE));

        if (bucket.tryConsume()) return true;

        res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        res.setHeader("Retry-After", "60");
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"Too many requests\"}");
        return false;
    }

    private String resolveIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) return req.getRemoteAddr();
        int comma = ip.indexOf(',');
        return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
    }

    private void maybeCleanup() {
        long now  = System.currentTimeMillis();
        long last = lastCleanup.get();
        if (now - last < CLEANUP_INTERVAL_MS) return;
        if (!lastCleanup.compareAndSet(last, now)) return;
        buckets.entrySet().removeIf(e -> e.getValue().isStale(now));
    }


    static final class TokenBucket {

        private record State(long tokens, long refillTs, long accessTs) {}

        private final AtomicReference<State> state;
        private final long capacity;
        private final long refillPerMinute;

        TokenBucket(long capacity, long refillPerMinute) {
            this.capacity      = capacity;
            this.refillPerMinute = refillPerMinute;
            long now = System.currentTimeMillis();
            this.state = new AtomicReference<>(new State(capacity, now, now));
        }

        boolean tryConsume() {
            long now = System.currentTimeMillis();
            while (true) {
                State cur = state.get();
                long elapsed = now - cur.refillTs();
                long toAdd   = (elapsed * refillPerMinute) / 60_000L;
                long newTokens = Math.min(capacity, cur.tokens() + toAdd);
                long newRefillTs = toAdd > 0 ? now : cur.refillTs();

                if (newTokens <= 0) {
                    state.compareAndSet(cur, new State(0, newRefillTs, now));
                    return false;
                }
                State next = new State(newTokens - 1, newRefillTs, now);
                if (state.compareAndSet(cur, next)) return true;
            }
        }

        boolean isStale(long now) {
            return now - state.get().accessTs() > RateLimitInterceptor.STALE_THRESHOLD_MS;
        }
    }
}