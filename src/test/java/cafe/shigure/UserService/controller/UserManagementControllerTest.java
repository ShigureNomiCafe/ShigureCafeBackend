package cafe.shigure.UserService.controller;

import cafe.shigure.UserService.model.Role;
import cafe.shigure.UserService.model.User;
import cafe.shigure.UserService.model.UserStatus;
import cafe.shigure.UserService.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UserManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private cafe.shigure.UserService.repository.UserAuditRepository userAuditRepository;

    @Autowired
    private cafe.shigure.UserService.repository.VerificationCodeRepository verificationCodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User user1;
    private Long user1Id;
    private Long user2Id;

    @BeforeEach
    public void setup() {
        userAuditRepository.deleteAll();
        userRepository.deleteAll();
        verificationCodeRepository.deleteAll();

        user1 = new User();
        user1.setUsername("user1");
        user1.setPassword(passwordEncoder.encode("password"));
        user1.setEmail("user1@example.com");
        user1.setRole(Role.USER);
        user1.setStatus(UserStatus.ACTIVE);
        user1 = userRepository.save(user1);
        user1Id = user1.getId();

        User user2 = new User();
        user2.setUsername("user2");
        user2.setPassword(passwordEncoder.encode("password"));
        user2.setEmail("user2@example.com");
        user2.setRole(Role.USER);
        user2.setStatus(UserStatus.ACTIVE);
        user2 = userRepository.save(user2);
        user2Id = user2.getId();
    }

    @Test
    public void testChangeOwnPassword() throws Exception {
        UserManagementController.ChangePasswordRequest request = 
            new UserManagementController.ChangePasswordRequest("password", "newPassword");

        mockMvc.perform(put("/api/users/{id}/password", user1Id)
                        .with(user(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password changed successfully"));
    }

    @Test
    public void testChangeOtherPassword_Forbidden() throws Exception {
        UserManagementController.ChangePasswordRequest request = 
            new UserManagementController.ChangePasswordRequest("password", "newPassword");

        mockMvc.perform(put("/api/users/{id}/password", user2Id)
                        .with(user(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("You can only change your own password"));
    }
}
