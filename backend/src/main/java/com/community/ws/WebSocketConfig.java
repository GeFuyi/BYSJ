package com.community.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final CommunityWebSocketHandler webSocketHandler;
    private final WsAuthHandshakeInterceptor authHandshakeInterceptor;

    public WebSocketConfig(CommunityWebSocketHandler webSocketHandler,
                           WsAuthHandshakeInterceptor authHandshakeInterceptor) {
        this.webSocketHandler = webSocketHandler;
        this.authHandshakeInterceptor = authHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/community")
                .addInterceptors(authHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}

