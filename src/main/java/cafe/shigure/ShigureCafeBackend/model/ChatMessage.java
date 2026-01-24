package cafe.shigure.ShigureCafeBackend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_messages_timestamp", columnList = "timestamp")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private long timestamp;
}