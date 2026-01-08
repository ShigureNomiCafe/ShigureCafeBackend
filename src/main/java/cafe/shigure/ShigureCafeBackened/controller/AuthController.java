package cafe.shigure.ShigureCafeBackened.controller;

import cafe.shigure.ShigureCafeBackened.dto.AuthResponse;
import cafe.shigure.ShigureCafeBackened.dto.LoginRequest;
import cafe.shigure.ShigureCafeBackened.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/token")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<AuthResponse> verify2FA(@RequestBody TwoFactorRequest request) {
        return ResponseEntity.ok(userService.verify2FA(request.username(), request.code()));
    }

    @PostMapping("/password-reset")
    public ResponseEntity<Void> resetPassword(@RequestBody cafe.shigure.ShigureCafeBackened.dto.ResetPasswordRequest request) {
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
        userService.sendVerificationCode(request.email(), request.type());
        return ResponseEntity.ok().build();
    }

    public record EmailRequest(String email, String type) {}
    public record TwoFactorRequest(String username, String code) {}
}
