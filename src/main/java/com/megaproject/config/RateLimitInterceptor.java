package com.megaproject.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // Buckets for IP addresses
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // We use X-Forwarded-For if behind a proxy, else remote address
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }

        // 100 requests per minute capacity
        TokenBucket bucket = buckets.computeIfAbsent(clientIp, k -> new TokenBucket(100, 100));

        if (bucket.tryConsume()) {
            return true;
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests. Please try again later.");
            return false;
        }
    }

    public static class TokenBucket {
        private final long capacity;
        private final long refillRatePerMinute;
        private long tokens;
        private long lastRefillTimestamp;

        public TokenBucket(long capacity, long refillRatePerMinute) {
            this.capacity = capacity;
            this.refillRatePerMinute = refillRatePerMinute;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long timeElapsed = now - lastRefillTimestamp;
            long tokensToAdd = (timeElapsed * refillRatePerMinute) / 60000;
            if (tokensToAdd > 0) {
                tokens = Math.min(capacity, tokens + tokensToAdd);
                // Adjust timestamp to account for the exact time the tokens were generated
                lastRefillTimestamp = now;
            }
        }
    }
}
