package cafe.shigure.ShigureCafeBackend.service;

import cafe.shigure.ShigureCafeBackend.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
    }

    @Test
    void checkRateLimit_shouldAllow_whenWithinLimit() {
        String key = "test-key";
        // Allow 2 requests per 1000ms
        assertDoesNotThrow(() -> rateLimitService.checkRateLimit(key, 2, 1000, 1));
        assertDoesNotThrow(() -> rateLimitService.checkRateLimit(key, 2, 1000, 1));
    }

    @Test
    void checkRateLimit_shouldThrow_whenExceedLimit() {
        String key = "test-key-exceed";
        // Allow 1 request per 1000ms
        rateLimitService.checkRateLimit(key, 1, 1000, 1);
        
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> rateLimitService.checkRateLimit(key, 1, 1000, 1));
        
        assertEquals("RATE_LIMIT_EXCEEDED", exception.getMessage());
        assertTrue((Long) exception.getMetadata().get("retryAfter") > 0);
    }

    @Test
    void getClientIp_shouldReturnForwardedFor() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");

        String ip = rateLimitService.getClientIp(request);
        assertEquals("1.2.3.4", ip);
    }

    @Test
    void getClientIp_shouldReturnFirstIp_whenMultipleForwardedFor() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 10.0.0.1");

        String ip = rateLimitService.getClientIp(request);
        assertEquals("1.2.3.4", ip);
    }

    @Test
    void getClientIp_shouldReturnRemoteAddr_whenNoHeaders() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String ip = rateLimitService.getClientIp(request);
        assertEquals("127.0.0.1", ip);
    }
}