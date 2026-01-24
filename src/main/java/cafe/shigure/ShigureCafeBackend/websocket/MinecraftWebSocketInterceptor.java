package cafe.shigure.ShigureCafeBackend.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

import cafe.shigure.ShigureCafeBackend.service.JwtService;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinecraftWebSocketInterceptor implements HandshakeInterceptor {

    @Value("${application.security.api-key:}")
    private String apiKey;

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final cafe.shigure.ShigureCafeBackend.repository.TokenBlacklistRepository tokenBlacklistRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            log.warn("Unauthorized WebSocket handshake attempt: not a servlet request");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // 1. Try API Key Authentication (primarily for MCDR plugins)
        String requestApiKey = servletRequest.getServletRequest().getParameter("cafe_api_key");

        if (apiKey != null && !apiKey.isEmpty() && apiKey.equals(requestApiKey)) {
            return true;
        }

        // 2. Try JWT Authentication (primarily for Web users)
        String token = servletRequest.getServletRequest().getParameter("token");
        if (token != null && !token.isEmpty()) {
            try {
                // Check blacklist
                if (tokenBlacklistRepository.existsByToken(token)) {
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                }

                String username = jwtService.extractUsername(token);
                if (username != null) {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                    if (jwtService.isTokenValid(token, userDetails)) {
                        // Optionally store user details in attributes if needed
                        attributes.put("user", userDetails);
                        return true;
                    }
                }
            } catch (Exception e) {
                log.warn("JWT validation failed for WebSocket handshake: {}", e.getMessage());
            }
        }

        log.warn("Unauthorized WebSocket handshake attempt from {}", request.getRemoteAddress());
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}