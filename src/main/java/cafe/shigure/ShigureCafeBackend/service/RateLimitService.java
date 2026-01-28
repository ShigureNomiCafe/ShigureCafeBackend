package cafe.shigure.ShigureCafeBackend.service;

import cafe.shigure.ShigureCafeBackend.exception.BusinessException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Checks if the rate limit has been exceeded for the given key.
     *
     * @param key        The rate limit key.
     * @param capacity   The capacity of the bucket.
     * @param periodMs   The period in milliseconds for refilling the bucket.
     * @param tokens     The tokens to consume.
     */
    public void checkRateLimit(String key, long capacity, long periodMs, long tokens) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(limit -> limit.capacity(capacity).refillGreedy(capacity, Duration.ofMillis(periodMs)))
                .build());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(tokens);

        if (!probe.isConsumed()) {
            long waitTimeMs = TimeUnit.NANOSECONDS.toMillis(probe.getNanosToWaitForRefill());
            throw new BusinessException("RATE_LIMIT_EXCEEDED",
                    Map.of("retryAfter", waitTimeMs > 0 ? waitTimeMs : periodMs));
        }
    }

    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple IPs in X-Forwarded-For
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
