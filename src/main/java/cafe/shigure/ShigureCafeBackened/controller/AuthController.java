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
}
