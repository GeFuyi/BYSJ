package com.community.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WsSessionRegistry {

    private final Map<String, WebSocketSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, WsSessionContext> contextBySessionId = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> userSessionIds = new ConcurrentHashMap<>();

    public void register(WebSocketSession session, WsSessionContext context) {
        sessionsById.put(session.getId(), session);
        contextBySessionId.put(session.getId(), context);
        userSessionIds.computeIfAbsent(context.getUserId(), key -> ConcurrentHashMap.newKeySet()).add(session.getId());
    }

    public WsSessionContext unregister(WebSocketSession session) {
        String sessionId = session.getId();
        sessionsById.remove(sessionId);
        WsSessionContext context = contextBySessionId.remove(sessionId);
        if (context != null) {
            Set<String> sessionIds = userSessionIds.get(context.getUserId());
            if (sessionIds != null) {
                sessionIds.remove(sessionId);
                if (sessionIds.isEmpty()) {
                    userSessionIds.remove(context.getUserId());
                }
            }
        }
        return context;
    }

    public List<WebSocketSession> getUserSessions(Long userId) {
        Set<String> sessionIds = userSessionIds.get(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<WebSocketSession> sessions = new ArrayList<>();
        for (String sessionId : sessionIds) {
            WebSocketSession session = sessionsById.get(sessionId);
            if (session != null && session.isOpen()) {
                sessions.add(session);
            }
        }
        return sessions;
    }

    public boolean isOnline(Long userId) {
        return !getUserSessions(userId).isEmpty();
    }

    public List<WsSessionContext> listOnlineUsers() {
        Map<Long, WsSessionContext> dedup = new ConcurrentHashMap<>();
        for (WsSessionContext context : contextBySessionId.values()) {
            if (context == null) {
                continue;
            }
            dedup.putIfAbsent(context.getUserId(), context);
        }
        return new ArrayList<>(dedup.values());
    }

    public List<WebSocketSession> allSessions() {
        List<WebSocketSession> sessions = new ArrayList<>();
        for (WebSocketSession session : sessionsById.values()) {
            if (session != null && session.isOpen()) {
                sessions.add(session);
            }
        }
        return sessions;
    }
}

