package com.community.ws;

import com.community.common.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;

@Service
public class WsPushService {

    private static final Logger log = LoggerFactory.getLogger(WsPushService.class);

    private final ObjectMapper objectMapper;
    private final WsSessionRegistry sessionRegistry;
    private final WsOfflineQueueService offlineQueueService;

    public WsPushService(ObjectMapper objectMapper,
                         WsSessionRegistry sessionRegistry,
                         WsOfflineQueueService offlineQueueService) {
        this.objectMapper = objectMapper;
        this.sessionRegistry = sessionRegistry;
        this.offlineQueueService = offlineQueueService;
    }

    public void sendToSession(WebSocketSession session, WsEnvelope envelope) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String text = objectMapper.writeValueAsString(envelope);
            session.sendMessage(new TextMessage(text));
        } catch (JsonProcessingException ex) {
            log.warn("serialize ws envelope failed: {}", ex.getMessage());
        } catch (IOException ex) {
            log.warn("send ws message failed, sessionId={}, err={}", session.getId(), ex.getMessage());
        }
    }

    public void sendToUser(Long userId, WsEnvelope envelope) {
        List<WebSocketSession> sessions = sessionRegistry.getUserSessions(userId);
        if (sessions.isEmpty()) {
            offlineQueueService.enqueue(userId, envelope);
            return;
        }
        for (WebSocketSession session : sessions) {
            sendToSession(session, envelope);
        }
    }

    public void sendToUserOnlineOnly(Long userId, WsEnvelope envelope) {
        List<WebSocketSession> sessions = sessionRegistry.getUserSessions(userId);
        for (WebSocketSession session : sessions) {
            sendToSession(session, envelope);
        }
    }

    public void broadcast(WsEnvelope envelope) {
        for (WebSocketSession session : sessionRegistry.allSessions()) {
            sendToSession(session, envelope);
        }
    }

    public void sendError(WebSocketSession session, String requestId, int code, String message) {
        sendToSession(session, WsEnvelope.error(requestId, code, message));
    }

    public void sendOk(WebSocketSession session, String type, String requestId, Object payload) {
        sendToSession(session, WsEnvelope.response(type, requestId, payload));
    }

    public void assertPayloadType(Object payload, Class<?> expectedType, String message) {
        if (payload != null && !expectedType.isAssignableFrom(payload.getClass())) {
            throw new BusinessException(400, "请求参数不合法");
        }
    }
}

