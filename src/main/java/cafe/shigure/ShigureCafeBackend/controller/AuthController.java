package cafe.shigure.ShigureCafeBackend.controller;

import cafe.shigure.ShigureCafeBackend.annotation.RateLimit;
import cafe.shigure.ShigureCafeBackend.dto.AuthResponse;
import cafe.shigure.ShigureCafeBackend.dto.LoginRequest;
import cafe.shigure.ShigureCafeBackend.dto.ResetPasswordRequest;
import cafe.shigure.ShigureCafeBackend.exception.BusinessException;
import cafe.shigure.ShigureCafeBackend.service.TurnstileService;
import cafe.shigure.ShigureCafeBackend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final TurnstileService turnstileService;

    @PostMapping("/token")
    @RateLimit(key = "login", useIp = true, milliseconds = 1000)
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        if (!turnstileService.verifyToken(request.getTurnstileToken())) {
            throw new BusinessException("INVALID_CAPTCHA");
        }
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<AuthResponse> verify2FA(@RequestBody TwoFactorRequest request) {
        return ResponseEntity.ok(userService.verify2FA(request.username(), request.code()));
    }

    @PostMapping("/password-reset")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (!turnstileService.verifyToken(request.getTurnstileToken())) {
            throw new BusinessException("INVALID_CAPTCHA");
        }
        userService.resetPasswordByEmail(request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/token")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        userService.logout(request.getHeader("Authorization"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verification-codes")
    public ResponseEntity<Void> sendCode(@RequestBody EmailRequest request) {
        if (!turnstileService.verifyToken(request.turnstileToken())) {
            throw new BusinessException("INVALID_CAPTCHA");
        }
        userService.sendVerificationCode(request.email(), request.type());
        return ResponseEntity.ok().build();
    }

    public record EmailRequest(String email, String type, String turnstileToken) {
    }

    public record TwoFactorRequest(String username, String code) {
    }
}
