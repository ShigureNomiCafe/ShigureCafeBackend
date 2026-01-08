package cafe.shigure.ShigureCafeBackened.service;

import cafe.shigure.ShigureCafeBackened.dto.AuthResponse;
import cafe.shigure.ShigureCafeBackened.dto.LoginRequest;
import cafe.shigure.ShigureCafeBackened.dto.RegisterRequest;
import cafe.shigure.ShigureCafeBackened.exception.BusinessException;
import cafe.shigure.ShigureCafeBackened.model.Role;
import cafe.shigure.ShigureCafeBackened.model.User;
import cafe.shigure.ShigureCafeBackened.model.UserAudit;
import cafe.shigure.ShigureCafeBackened.model.UserStatus;
import cafe.shigure.ShigureCafeBackened.model.VerificationCode;
import cafe.shigure.ShigureCafeBackened.repository.UserAuditRepository;
import cafe.shigure.ShigureCafeBackened.repository.UserRepository;
import cafe.shigure.ShigureCafeBackened.repository.VerificationCodeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final cafe.shigure.ShigureCafeBackened.repository.TokenBlacklistRepository tokenBlacklistRepository;

    @Transactional
    public void sendVerificationCode(String email, String type) {
        if ("REGISTER".equalsIgnoreCase(type) || "UPDATE_EMAIL".equalsIgnoreCase(type)) {
            if (userRepository.findByEmail(email).isPresent()) {
                throw new BusinessException("EMAIL_IN_USE");
            }
        } else if ("RESET_PASSWORD".equalsIgnoreCase(type)) {
            if (userRepository.findByEmail(email).isEmpty()) {
                throw new BusinessException("USER_NOT_FOUND");
            }
        }

        // 生成 6 位验证码
        String code = String.format("%06d", new Random().nextInt(999999));

        // 保存或更新验证码
        VerificationCode verificationCode = verificationCodeRepository.findByEmail(email)
                .orElse(new VerificationCode());

        if (verificationCode.getLastSentTime() != null &&
                verificationCode.getLastSentTime().plusSeconds(60).isAfter(LocalDateTime.now())) {
            throw new BusinessException("RATE_LIMIT_EXCEEDED", java.util.Map.of("retryAfter", 60));
        }

        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setExpiryDate(java.time.LocalDateTime.now().plusMinutes(5)); // 5分钟有效
        verificationCode.setLastSentTime(LocalDateTime.now());

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
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
        
        if (user.getStatus() == UserStatus.PENDING) {
            throw new BusinessException("ACCOUNT_PENDING");
        }
        if (user.getStatus() == UserStatus.BANNED) {
            throw new BusinessException("ACCOUNT_BANNED");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("ACCOUNT_INACTIVE");
        }

        var jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken);
    }

    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.substring(7);
            java.util.Date expirationDate = jwtService.extractExpiration(jwt);
            tokenBlacklistRepository.save(new cafe.shigure.ShigureCafeBackened.model.TokenBlacklist(jwt, expirationDate));
        }
    }

    @Transactional
    public String register(RegisterRequest request) {
        Optional<User> existingUser = userRepository.findByUsername(request.getUsername());
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.getStatus() == UserStatus.ACTIVE) {
                throw new BusinessException("USER_ALREADY_EXISTS");
            }
            if (user.getStatus() == UserStatus.BANNED) {
                throw new BusinessException("ACCOUNT_BANNED");
            }
            // User is PENDING, verify code and refresh audit code
            verifyCode(request.getEmail(), request.getVerificationCode());
            
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
        verifyCode(request.getEmail(), request.getVerificationCode());

        if (request.getNickname() != null && request.getNickname().length() > 50) {
            throw new BusinessException("NICKNAME_TOO_LONG", java.util.Map.of("maxLength", 50));
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname() != null && !request.getNickname().isBlank() 
                ? request.getNickname() : request.getUsername());
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

    @Transactional
    public void resetPasswordByEmail(cafe.shigure.ShigureCafeBackened.dto.ResetPasswordRequest request) {
        verifyCode(request.getEmail(), request.getVerificationCode());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
    
    private void verifyCode(String email, String code) {
        // 验证验证码
        VerificationCode verificationCode = verificationCodeRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("VERIFICATION_CODE_NOT_FOUND"));

        if (verificationCode.isExpired()) {
            throw new BusinessException("VERIFICATION_CODE_EXPIRED");
        }

        if (!verificationCode.getCode().equals(code)) {
            throw new BusinessException("VERIFICATION_CODE_INVALID");
        }

        // 验证通过，删除验证码（防止复用）
        verificationCodeRepository.delete(verificationCode);
    }
    
    public User getUserByAuditCode(String auditCode) {
        UserAudit audit = userAuditRepository.findByAuditCode(auditCode)
                .orElseThrow(() -> new BusinessException("INVALID_AUDIT_CODE"));
        return audit.getUser();
    }

    public List<String> getAllAuditCodes() {
        return userAuditRepository.findAll().stream()
                .map(UserAudit::getAuditCode)
                .collect(Collectors.toList());
    }

    public cafe.shigure.ShigureCafeBackened.dto.RegistrationDetailsResponse getRegistrationDetails(String auditCode) {
        UserAudit audit = userAuditRepository.findByAuditCode(auditCode)
                .orElseThrow(() -> new BusinessException("INVALID_AUDIT_CODE"));
        User user = audit.getUser();
        return new cafe.shigure.ShigureCafeBackened.dto.RegistrationDetailsResponse(
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getStatus(),
                audit.getAuditCode(),
                audit.isExpired()
        );
    }

    @Transactional
    public void approveUser(String auditCode) {
        UserAudit audit = userAuditRepository.findByAuditCode(auditCode)
                .orElseThrow(() -> new BusinessException("INVALID_AUDIT_CODE"));
        
        if (audit.isExpired()) {
             throw new BusinessException("AUDIT_CODE_EXPIRED");
        }

        User user = audit.getUser();
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException("USER_ALREADY_ACTIVE");
        }
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        
        // Remove audit record after successful approval
        userAuditRepository.delete(audit);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new BusinessException("USER_NOT_FOUND");
        }
        userRepository.deleteById(id);
    }

    public void changePassword(Long id, String oldPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("OLD_PASSWORD_MISMATCH");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void updateEmail(Long id, String newEmail, String verificationCode) {
        verifyCode(newEmail, verificationCode);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
        
        // 检查新邮箱是否已被使用 (排除自己)
        userRepository.findByEmail(newEmail).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(id)) {
                throw new BusinessException("EMAIL_IN_USE");
            }
        });

        user.setEmail(newEmail);
        userRepository.save(user);
    }

    @Transactional
    public void updateEmailDirectly(Long id, String newEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));

        // Check if new email is already in use (excluding self)
        userRepository.findByEmail(newEmail).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(id)) {
                throw new BusinessException("EMAIL_IN_USE");
            }
        });

        user.setEmail(newEmail);
        userRepository.save(user);
    }

    @Transactional
    public void updateRole(Long id, Role newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
        user.setRole(newRole);
        userRepository.save(user);
    }

    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
    }

    @Transactional
    public void updateNickname(Long id, String nickname) {
        if (nickname != null && nickname.length() > 50) {
            throw new BusinessException("NICKNAME_TOO_LONG", java.util.Map.of("maxLength", 50));
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
        user.setNickname(nickname);
        userRepository.save(user);
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupExpiredCodes() {
        verificationCodeRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }
}
