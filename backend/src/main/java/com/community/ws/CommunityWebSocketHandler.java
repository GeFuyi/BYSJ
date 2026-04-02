package com.community.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class CommunityWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CommunityWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final WsSessionRegistry sessionRegistry;
    private final WsPushService pushService;
    private final CommunityWsCommandService commandService;

    public CommunityWebSocketHandler(ObjectMapper objectMapper,
                                     WsSessionRegistry sessionRegistry,
                                     WsPushService pushService,
                                     CommunityWsCommandService commandService) {
        this.objectMapper = objectMapper;
        this.sessionRegistry = sessionRegistry;
        this.pushService = pushService;
        this.commandService = commandService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WsSessionContext context = resolveContext(session);
        if (context == null) {
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("认证失败"));
            } catch (Exception ignored) {
                // ignore
            }
            return;
        }
        sessionRegistry.register(session, context);
        commandService.onConnect(session, context);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        WsSessionContext context = resolveContext(session);
        if (context == null) {
            pushService.sendError(session, null, 401, "请先登录");
            return;
        }
        try {
            WsEnvelope envelope = objectMapper.readValue(message.getPayload(), WsEnvelope.class);
            if (!StringUtils.hasText(envelope.getType())) {
                pushService.sendError(session, envelope.getRequestId(), 400, "消息类型不能为空");
                return;
            }
            commandService.onMessage(session, context, envelope);
        } catch (Exception ex) {
            log.warn("handle ws message failed, err={}", ex.getMessage());
            pushService.sendError(session, null, 400, "消息格式不正确");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        WsSessionContext context = sessionRegistry.unregister(session);
        if (context != null) {
            commandService.onDisconnect(session, context);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("ws transport error sessionId={}, err={}", session.getId(), exception.getMessage());
    }

    private WsSessionContext resolveContext(WebSocketSession session) {
        Object userIdObj = session.getAttributes().get(WsAuthHandshakeInterceptor.ATTR_USER_ID);
        if (!(userIdObj instanceof Long)) {
            return null;
        }
        WsSessionContext context = new WsSessionContext();
        context.setUserId((Long) userIdObj);
        context.setUsername(String.valueOf(session.getAttributes().get(WsAuthHandshakeInterceptor.ATTR_USERNAME)));
        Object nicknameObj = session.getAttributes().get(WsAuthHandshakeInterceptor.ATTR_NICKNAME);
        context.setNickname(nicknameObj == null ? null : String.valueOf(nicknameObj));
        Object avatarObj = session.getAttributes().get(WsAuthHandshakeInterceptor.ATTR_AVATAR_PATH);
        context.setAvatarPath(avatarObj == null ? null : String.valueOf(avatarObj));
        context.setRole(String.valueOf(session.getAttributes().get(WsAuthHandshakeInterceptor.ATTR_ROLE)));
        context.setSessionId(session.getId());
        return context;
    }
}
