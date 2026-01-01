package cafe.shigure.UserService.service;

import cafe.shigure.UserService.dto.AuthResponse;
import cafe.shigure.UserService.dto.LoginRequest;
import cafe.shigure.UserService.dto.RegisterRequest;
import cafe.shigure.UserService.exception.BusinessException;
import cafe.shigure.UserService.model.Role;
import cafe.shigure.UserService.model.User;
import cafe.shigure.UserService.model.UserAudit;
import cafe.shigure.UserService.model.UserStatus;
import cafe.shigure.UserService.model.VerificationCode;
import cafe.shigure.UserService.repository.UserAuditRepository;
import cafe.shigure.UserService.repository.UserRepository;
import cafe.shigure.UserService.repository.VerificationCodeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserAuditRepository userAuditRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public void sendVerificationCode(String email) {
        if (userRepository.findByUsername(email).isPresent()) {
            // 这里根据需求处理重名逻辑
        }
        // 生成 6 位验证码
        String code = String.format("%06d", new Random().nextInt(999999));

        // 保存或更新验证码
        VerificationCode verificationCode = verificationCodeRepository.findByEmail(email)
                .orElse(new VerificationCode());
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setExpiryDate(java.time.LocalDateTime.now().plusMinutes(5)); // 5分钟有效

        verificationCodeRepository.save(verificationCode);

        // 发送邮件
        emailService.sendSimpleMessage(email, "猫咖验证码", "您的验证码是：" + code + "，请在5分钟内使用。");
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("账号状态异常，请联系管理员");
        }

        var jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken);
    }

    @Transactional
    public String register(RegisterRequest request) {
        Optional<User> existingUser = userRepository.findByUsername(request.getUsername());
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.getStatus() == UserStatus.ACTIVE) {
                throw new BusinessException("用户已存在");
            }
            if (user.getStatus() == UserStatus.BANNED) {
                throw new BusinessException("用户被封禁");
            }
            // User is PENDING, verify code and refresh audit code
            verifyCode(request);
            
            // Check existing audit
            UserAudit audit = userAuditRepository.findByUserId(user.getId())
                    .orElse(new UserAudit(user, UUID.randomUUID().toString(), 7));
            
            // Always refresh code and expiry if re-registering
            audit.setAuditCode(UUID.randomUUID().toString());
            audit.setExpiryDate(LocalDateTime.now().plusDays(7));
            userAuditRepository.save(audit);
            
            return audit.getAuditCode();
        }

        // New User
        verifyCode(request);

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.PENDING);
        
        userRepository.save(user);

        // 生成审核码 (7天有效)
        String auditCode = UUID.randomUUID().toString();
        UserAudit userAudit = new UserAudit(user, auditCode, 7);
        userAuditRepository.save(userAudit);

        return auditCode;
    }
    
    private void verifyCode(RegisterRequest request) {
        // 验证验证码
        VerificationCode verificationCode = verificationCodeRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("验证码不存在或已过期"));

        if (verificationCode.isExpired()) {
            throw new BusinessException("验证码已过期");
        }

        if (!verificationCode.getCode().equals(request.getVerificationCode())) {
            throw new BusinessException("验证码错误");
        }

        // 验证通过，删除验证码（防止复用）
        verificationCodeRepository.delete(verificationCode);
    }
    
    public User getUserByAuditCode(String auditCode) {
        UserAudit audit = userAuditRepository.findByAuditCode(auditCode)
                .orElseThrow(() -> new BusinessException("Invalid audit code"));
        return audit.getUser();
    }

    @Transactional
    public void approveUser(String auditCode) {
        UserAudit audit = userAuditRepository.findByAuditCode(auditCode)
                .orElseThrow(() -> new BusinessException("Invalid audit code"));
        
        if (audit.isExpired()) {
             throw new BusinessException("Audit code expired. Please request a new one.");
        }

        User user = audit.getUser();
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException("User already active");
        }
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        
        // Remove audit record after successful approval
        userAuditRepository.delete(audit);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new BusinessException("User not found");
        }
        userRepository.deleteById(id);
    }

    public void changePassword(Long id, String oldPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("Old password does not match");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupExpiredCodes() {
        verificationCodeRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }
}
