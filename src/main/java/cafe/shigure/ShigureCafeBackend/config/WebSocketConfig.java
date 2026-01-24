package cafe.shigure.ShigureCafeBackend.config;

import cafe.shigure.ShigureCafeBackend.websocket.MinecraftWebSocketHandler;
import cafe.shigure.ShigureCafeBackend.websocket.MinecraftWebSocketInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final MinecraftWebSocketHandler minecraftWebSocketHandler;
    private final MinecraftWebSocketInterceptor minecraftWebSocketInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(minecraftWebSocketHandler, "/ws/minecraft/chat")
                .addInterceptors(minecraftWebSocketInterceptor)
                .setAllowedOrigins("*");
    }
}
