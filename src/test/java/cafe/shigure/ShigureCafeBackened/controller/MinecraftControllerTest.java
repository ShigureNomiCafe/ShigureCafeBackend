package cafe.shigure.ShigureCafeBackened.controller;

import cafe.shigure.ShigureCafeBackened.model.Role;
import cafe.shigure.ShigureCafeBackened.model.User;
import cafe.shigure.ShigureCafeBackened.model.UserStatus;
import cafe.shigure.ShigureCafeBackened.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class MinecraftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private cafe.shigure.ShigureCafeBackened.repository.UserAuditRepository userAuditRepository;

    @Autowired
    private cafe.shigure.ShigureCafeBackened.repository.NoticeRepository noticeRepository;

    @Autowired
    private cafe.shigure.ShigureCafeBackened.repository.ChatMessageRepository chatMessageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setup() {
        chatMessageRepository.deleteAll();
        noticeRepository.deleteAll();
        userAuditRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testGetWhitelist() throws Exception {
        // ... (existing test logic)
    }

    @Test
    public void testSyncChatMessages() throws Exception {
        long now = System.currentTimeMillis();
        String json = """
                {
                    "messages": [
                        {
                            "name": "TestPlayer",
                            "message": "Hello, world!",
                            "timestamp": %d
                        }
                    ],
                    "lastTimestamp": 0
                }
                """.formatted(now);

        // First sync: sends a message, should get nothing back (threshold = now)
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/minecraft/message-sync")
                        .header("X-API-KEY", "shigure-cafe-secret-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        org.junit.jupiter.api.Assertions.assertEquals(1, chatMessageRepository.count());

        // Second sync: empty list, lastTimestamp = now - 1
        // Should get the message we just sent
        String json2 = """
                {
                    "messages": [],
                    "lastTimestamp": %d
                }
                """.formatted(now - 1);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/minecraft/message-sync")
                        .header("X-API-KEY", "shigure-cafe-secret-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("TestPlayer"))
                .andExpect(jsonPath("$[0].message").value("Hello, world!"))
                .andExpect(jsonPath("$[0].timestamp").value(now));
    }

    @Test
    public void testSyncChatMessagesInvalidApiKey() throws Exception {
        String json = """
                {
                    "messages": [],
                    "lastTimestamp": 0
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/minecraft/message-sync")
                        .header("X-API-KEY", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetChatMessages() throws Exception {
        // Save some messages
        cafe.shigure.ShigureCafeBackened.model.ChatMessage msg1 = new cafe.shigure.ShigureCafeBackened.model.ChatMessage();
        msg1.setName("Player1");
        msg1.setMessage("Msg 1");
        msg1.setTimestamp(System.currentTimeMillis() - 1000);
        chatMessageRepository.save(msg1);

        cafe.shigure.ShigureCafeBackened.model.ChatMessage msg2 = new cafe.shigure.ShigureCafeBackened.model.ChatMessage();
        msg2.setName("Player2");
        msg2.setMessage("Msg 2");
        msg2.setTimestamp(System.currentTimeMillis());
        chatMessageRepository.save(msg2);

        mockMvc.perform(get("/api/v1/minecraft/chat")
                        .with(user("testuser").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Player2"))
                .andExpect(jsonPath("$.content[1].name").value("Player1"));
    }
}
