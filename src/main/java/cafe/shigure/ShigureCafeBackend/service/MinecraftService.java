package cafe.shigure.ShigureCafeBackend.service;

import cafe.shigure.ShigureCafeBackend.dto.ChatMessageRequest;
import cafe.shigure.ShigureCafeBackend.dto.ChatMessageResponse;
import cafe.shigure.ShigureCafeBackend.dto.PagedResponse;
import cafe.shigure.ShigureCafeBackend.event.ChatMessageEvent;
import cafe.shigure.ShigureCafeBackend.model.ChatMessage;
import cafe.shigure.ShigureCafeBackend.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MinecraftService {

    private final ChatMessageRepository chatMessageRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final org.springframework.data.redis.listener.ChannelTopic chatTopic;
    private static final String CHAT_CACHE_KEY = "minecraft:chat:latest";
    private static final int CACHE_SIZE = 100;

    @Transactional(readOnly = true)
    public PagedResponse<ChatMessageResponse> getChatMessages(Pageable pageable) {
        // Try to get from Redis for the first page (latest messages)
        if (pageable.getPageNumber() == 0 && pageable.getPageSize() <= CACHE_SIZE && 
            pageable.getSort().getOrderFor("timestamp") != null && 
            pageable.getSort().getOrderFor("timestamp").isDescending()) {
            
            List<Object> cachedObjects = redisTemplate.opsForList().range(CHAT_CACHE_KEY, 0, pageable.getPageSize() - 1);
            if (cachedObjects != null && !cachedObjects.isEmpty()) {
                List<ChatMessageResponse> messages = cachedObjects.stream()
                        .map(obj -> (ChatMessageResponse) obj)
                        .collect(Collectors.toList());
                long totalElements = chatMessageRepository.count();
                int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());
                return new PagedResponse<>(messages, 0, pageable.getPageSize(), 
                        totalElements, totalPages, true, System.currentTimeMillis());
            }
        }

        Page<ChatMessage> page = chatMessageRepository.findAll(pageable);
        return PagedResponse.fromPage(page.map(entity -> new ChatMessageResponse(
                entity.getId(), entity.getName(), entity.getMessage(), entity.getTimestamp()
        )));
    }

    @Transactional
    public void saveChatMessage(ChatMessageRequest request) {
        saveChatMessage(request, null);
    }

    @Transactional
    public void saveChatMessage(ChatMessageRequest request, String senderSessionId) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setName(request.name());
        chatMessage.setMessage(request.message());
        chatMessage.setTimestamp(request.timestamp());
        chatMessageRepository.save(chatMessage);
        
        ChatMessageResponse resp = new ChatMessageResponse(
            chatMessage.getId(), chatMessage.getName(), chatMessage.getMessage(), chatMessage.getTimestamp()
        );
        
        // 1. Update Redis List Cache
        redisTemplate.opsForList().leftPush(CHAT_CACHE_KEY, resp);
        redisTemplate.opsForList().trim(CHAT_CACHE_KEY, 0, CACHE_SIZE - 1);
        
        // 2. Publish to Redis Topic (for multi-instance sync)
        redisTemplate.convertAndSend(chatTopic.getTopic(), Map.of(
            "message", resp,
            "senderSessionId", senderSessionId != null ? senderSessionId : ""
        ));
        
        // 3. Still publish local event for this instance
        eventPublisher.publishEvent(new ChatMessageEvent(this, resp, senderSessionId));
    }
}
