package cafe.shigure.ShigureCafeBackened.controller;

import cafe.shigure.ShigureCafeBackened.dto.LoginRequest;
import cafe.shigure.ShigureCafeBackened.dto.RegisterRequest;
import cafe.shigure.ShigureCafeBackened.model.Role;
import cafe.shigure.ShigureCafeBackened.model.User;
import cafe.shigure.ShigureCafeBackened.model.UserStatus;
import cafe.shigure.ShigureCafeBackened.model.VerificationCode;
import cafe.shigure.ShigureCafeBackened.repository.UserRepository;
import cafe.shigure.ShigureCafeBackened.repository.VerificationCodeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;
    
    @Autowired
    private cafe.shigure.ShigureCafeBackened.repository.UserAuditRepository userAuditRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setup() {
        userAuditRepository.deleteAll();
        userRepository.deleteAll();
        verificationCodeRepository.deleteAll();
    }

    @Test
    public void testRegister() throws Exception {
        String email = "test@example.com";
        String code = "123456";

        // Pre-seed verification code
        VerificationCode vc = new VerificationCode();
        vc.setEmail(email);
        vc.setCode(code);
        vc.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        verificationCodeRepository.save(vc);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setEmail(email);
        request.setVerificationCode(code);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    public void testLogin() throws Exception {
        // Create active user
        User user = new User();
        user.setUsername("loginuser");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setEmail("login@example.com");
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("loginuser");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    public void testLoginWithPendingUser() throws Exception {
        // Create pending user
        User user = new User();
        user.setUsername("pendinguser");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setEmail("pending@example.com");
        user.setRole(Role.USER);
        user.setStatus(UserStatus.PENDING);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("pendinguser");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()); // Expecting 401
    }

    @Test
    public void testLoginFailure() throws Exception {
        // Create active user
        User user = new User();
        user.setUsername("activeuser");
        user.setPassword(passwordEncoder.encode("correctpassword"));
        user.setEmail("active@example.com");
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // Try with wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("activeuser");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$").value("Authentication failed: Bad credentials"));
    }
}
