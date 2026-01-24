package cafe.shigure.ShigureCafeBackend.service;

import cafe.shigure.ShigureCafeBackend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public void checkRateLimit(String key, long milliseconds) {
        String fullKey = "ratelimit:" + key;
        String value = redisTemplate.opsForValue().get(fullKey);
        if (value != null) {
            Long expire = redisTemplate.getExpire(fullKey, TimeUnit.MILLISECONDS);
            throw new BusinessException("RATE_LIMIT_EXCEEDED",
                    Map.of("retryAfter", expire != null && expire > 0 ? expire : milliseconds));
        }
        redisTemplate.opsForValue().set(fullKey, "1", milliseconds, TimeUnit.MILLISECONDS);
    }

    public String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
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
        return ip;
    }
}
