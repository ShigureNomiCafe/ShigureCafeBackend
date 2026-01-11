package cafe.shigure.ShigureCafeBackened.service;

import cafe.shigure.ShigureCafeBackened.exception.BusinessException;
import cafe.shigure.ShigureCafeBackened.repository.TokenBlacklistRepository;
import cafe.shigure.ShigureCafeBackened.repository.UserAuditRepository;
import cafe.shigure.ShigureCafeBackened.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserAuditRepository userAuditRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void sendVerificationCode_shouldThrowException_whenRateLimited() {
        String email = "test@example.com";
        String limitKey = "verify:limit:" + email;

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(redisTemplate.hasKey(limitKey)).thenReturn(true);
        when(redisTemplate.getExpire(limitKey, TimeUnit.SECONDS)).thenReturn(30L);

        assertThrows(BusinessException.class, () -> userService.sendVerificationCode(email, "REGISTER"));
        
        verify(emailService, never()).sendSimpleMessage(any(), any(), any());
    }

    @Test
    void sendVerificationCode_shouldSend_whenCooldownPassed() {
        String email = "test@example.com";
        String limitKey = "verify:limit:" + email;
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(redisTemplate.hasKey(limitKey)).thenReturn(false);

        userService.sendVerificationCode(email, "REGISTER");

        verify(emailService).sendSimpleMessage(eq(email), any(), any());
        verify(valueOperations).set(eq("verify:code:" + email), anyString(), eq(5L), eq(TimeUnit.MINUTES));
        verify(valueOperations).set(eq("verify:limit:" + email), eq("1"), eq(60L), eq(TimeUnit.SECONDS));
    }
    
    @Test
    void sendVerificationCode_shouldSend_whenNoPriorCode() {
        String email = "new@example.com";
        String limitKey = "verify:limit:" + email;
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(redisTemplate.hasKey(limitKey)).thenReturn(false);

        userService.sendVerificationCode(email, "REGISTER");

        verify(emailService).sendSimpleMessage(eq(email), any(), any());
        verify(valueOperations).set(eq("verify:code:" + email), anyString(), eq(5L), eq(TimeUnit.MINUTES));
    }
}