package cafe.shigure.ShigureCafeBackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final StringRedisTemplate redisTemplate;

    public static final String USER_LIST_KEY = "user_list:last_updated";
    public static final String AUDIT_LIST_KEY = "audit_list:last_updated";
    public static final String NOTICE_LIST_KEY = "notice_list:last_updated";

    public void updateTimestamp(String key) {
        redisTemplate.opsForValue().set(key, String.valueOf(Instant.now().toEpochMilli()));
    }

    public Long getTimestamp(String key) {
        String ts = redisTemplate.opsForValue().get(key);
        if (ts == null) {
            long now = Instant.now().toEpochMilli();
            redisTemplate.opsForValue().set(key, String.valueOf(now));
            return now;
        }
        return Long.parseLong(ts);
    }
}
