package cafe.shigure.ShigureCafeBackened.service;

import cafe.shigure.ShigureCafeBackened.exception.BusinessException;
import cafe.shigure.ShigureCafeBackened.model.VerificationCode;
import cafe.shigure.ShigureCafeBackened.repository.TokenBlacklistRepository;
import cafe.shigure.ShigureCafeBackened.repository.UserAuditRepository;
import cafe.shigure.ShigureCafeBackened.repository.UserRepository;
import cafe.shigure.ShigureCafeBackened.repository.VerificationCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

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
    private VerificationCodeRepository verificationCodeRepository;
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

    @Test
    void sendVerificationCode_shouldThrowException_whenRateLimited() {
        String email = "test@example.com";
        VerificationCode existingCode = new VerificationCode();
        existingCode.setEmail(email);
        existingCode.setCode("123456");
        existingCode.setExpiryDate(LocalDateTime.now().plusMinutes(5));
        existingCode.setLastSentTime(LocalDateTime.now().minusSeconds(30)); // Sent 30s ago (less than 60s)

        when(verificationCodeRepository.findByEmail(email)).thenReturn(Optional.of(existingCode));
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty()); // Assume user doesn't exist for registration check or similar

        // Use a type that triggers the simple path or mock checks for specific types
        // The code checks:
        // if ("REGISTER".equalsIgnoreCase(type) || "UPDATE_EMAIL".equalsIgnoreCase(type)) ...
        // else if ("RESET_PASSWORD".equalsIgnoreCase(type)) ...
        
        // Let's use "REGISTER" and ensure email is not in use (mocked above)
        assertThrows(BusinessException.class, () -> userService.sendVerificationCode(email, "REGISTER"));
        
        verify(emailService, never()).sendSimpleMessage(any(), any(), any());
    }

    @Test
    void sendVerificationCode_shouldSend_whenCooldownPassed() {
        String email = "test@example.com";
        VerificationCode existingCode = new VerificationCode();
        existingCode.setEmail(email);
        existingCode.setCode("123456");
        existingCode.setExpiryDate(LocalDateTime.now().minusMinutes(10));
        existingCode.setLastSentTime(LocalDateTime.now().minusSeconds(61)); // Sent 61s ago

        when(verificationCodeRepository.findByEmail(email)).thenReturn(Optional.of(existingCode));
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        userService.sendVerificationCode(email, "REGISTER");

        verify(emailService).sendSimpleMessage(eq(email), any(), any());
        verify(verificationCodeRepository).save(any(VerificationCode.class));
    }
    
    @Test
    void sendVerificationCode_shouldSend_whenNoPriorCode() {
        String email = "new@example.com";
        
        when(verificationCodeRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        userService.sendVerificationCode(email, "REGISTER");

        verify(emailService).sendSimpleMessage(eq(email), any(), any());
        verify(verificationCodeRepository).save(any(VerificationCode.class));
    }
}
