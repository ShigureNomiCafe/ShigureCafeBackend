package cafe.shigure.UserService.controller;

import cafe.shigure.UserService.dto.AuthResponse;
import cafe.shigure.UserService.dto.LoginRequest;
import cafe.shigure.UserService.dto.RegisterRequest;
import cafe.shigure.UserService.dto.UserResponse;
import cafe.shigure.UserService.model.User;
import cafe.shigure.UserService.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/send-code")
    public ResponseEntity<String> sendCode(@RequestParam String email) {
        userService.sendVerificationCode(email);
        return ResponseEntity.ok("验证码已发送");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        String auditCode = userService.register(request);
        return ResponseEntity.ok("注册申请已提交，请等待审核。您的审核码是：" + auditCode);
    }

    @GetMapping("/check-audit/{auditCode}")
    public ResponseEntity<UserResponse> checkAudit(@PathVariable String auditCode) {
        User user = userService.getUserByAuditCode(auditCode);
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole()));
    }

    @PostMapping("/approve-audit/{auditCode}")
    public ResponseEntity<String> approveAudit(@PathVariable String auditCode) {
        userService.approveUser(auditCode);
        return ResponseEntity.ok("用户审核通过");
    }
}