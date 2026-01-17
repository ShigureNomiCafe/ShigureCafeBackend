package cafe.shigure.ShigureCafeBackened.service;

import cafe.shigure.ShigureCafeBackened.dto.ChatMessageRequest;
import cafe.shigure.ShigureCafeBackened.dto.ChatMessageResponse;
import cafe.shigure.ShigureCafeBackened.dto.ChatSyncRequest;
import cafe.shigure.ShigureCafeBackened.dto.PagedResponse;
import cafe.shigure.ShigureCafeBackened.model.ChatMessage;
import cafe.shigure.ShigureCafeBackened.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MinecraftService {

    private final ChatMessageRepository chatMessageRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CHAT_CACHE_KEY = "minecraft:chat:latest";
    private static final int CACHE_SIZE = 100;

    @Transactional(readOnly = true)
    public PagedResponse<ChatMessageResponse> getChatMessages(Pageable pageable) {
        Page<ChatMessage> page = chatMessageRepository.findAll(pageable);
        return PagedResponse.fromPage(page.map(entity -> new ChatMessageResponse(
                entity.getId(), entity.getName(), entity.getMessage(), entity.getTimestamp()
        )));
    }

    @Transactional
    public List<ChatMessageResponse> syncChatMessages(ChatSyncRequest request) {
        long lastTimestamp = request.lastTimestamp() != null ? request.lastTimestamp() : 0;
        long maxIncomingTimestamp = lastTimestamp;

        // 1. Save new messages
        if (request.messages() != null && !request.messages().isEmpty()) {
            List<ChatMessage> entities = new ArrayList<>();
            for (ChatMessageRequest msg : request.messages()) {
                ChatMessage entity = new ChatMessage();
                entity.setName(msg.name());
                entity.setMessage(msg.message());
                entity.setTimestamp(msg.timestamp());
                entities.add(entity);
                if (msg.timestamp() > maxIncomingTimestamp) {
                    maxIncomingTimestamp = msg.timestamp();
                }
            }
            chatMessageRepository.saveAll(entities);
            
            // Update cache
            entities.forEach(entity -> {
                ChatMessageResponse resp = new ChatMessageResponse(
                    entity.getId(), entity.getName(), entity.getMessage(), entity.getTimestamp()
                );
                redisTemplate.opsForList().leftPush(CHAT_CACHE_KEY, resp);
            });
            redisTemplate.opsForList().trim(CHAT_CACHE_KEY, 0, CACHE_SIZE - 1);
            redisTemplate.expire(CHAT_CACHE_KEY, 1, TimeUnit.DAYS);
        }

        // 2. Get newer messages (newer than everything the requester has/sent)
        final long queryThreshold = maxIncomingTimestamp;
        
        // Try to get from cache first
        List<Object> cachedObjects = redisTemplate.opsForList().range(CHAT_CACHE_KEY, 0, -1);
        List<ChatMessageResponse> newerMessages;
        
        if (cachedObjects != null && !cachedObjects.isEmpty()) {
            newerMessages = cachedObjects.stream()
                .map(obj -> (ChatMessageResponse) obj)
                .filter(msg -> msg.timestamp() > queryThreshold)
                .sorted(Comparator.comparingLong(ChatMessageResponse::timestamp))
                .collect(Collectors.toList());
            
            // Check if cache is sufficient
            if (cachedObjects.size() == CACHE_SIZE) {
                ChatMessageResponse oldestInCache = (ChatMessageResponse) cachedObjects.get(cachedObjects.size() - 1);
                if (oldestInCache.timestamp() > queryThreshold) {
                    newerMessages = fetchFromDb(queryThreshold);
                }
            }
        } else {
            newerMessages = fetchFromDb(queryThreshold);
        }

        return newerMessages;
    }

    private List<ChatMessageResponse> fetchFromDb(long lastTimestamp) {
        return chatMessageRepository.findAllByTimestampGreaterThan(lastTimestamp).stream()
            .map(entity -> new ChatMessageResponse(
                entity.getId(), entity.getName(), entity.getMessage(), entity.getTimestamp()
            ))
            .sorted(Comparator.comparingLong(ChatMessageResponse::timestamp))
            .collect(Collectors.toList());
    }

    @Transactional
    public void saveChatMessage(ChatMessageRequest request) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setName(request.name());
        chatMessage.setMessage(request.message());
        chatMessage.setTimestamp(request.timestamp());
        chatMessageRepository.save(chatMessage);
        
        ChatMessageResponse resp = new ChatMessageResponse(
            chatMessage.getId(), chatMessage.getName(), chatMessage.getMessage(), chatMessage.getTimestamp()
        );
        redisTemplate.opsForList().leftPush(CHAT_CACHE_KEY, resp);
        redisTemplate.opsForList().trim(CHAT_CACHE_KEY, 0, CACHE_SIZE - 1);
    }
}
