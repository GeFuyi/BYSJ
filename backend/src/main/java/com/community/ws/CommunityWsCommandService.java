package com.community.ws;

import org.springframework.web.socket.WebSocketSession;

public interface CommunityWsCommandService {

    void onConnect(WebSocketSession session, WsSessionContext context);

    void onDisconnect(WebSocketSession session, WsSessionContext context);

    void onMessage(WebSocketSession session, WsSessionContext context, WsEnvelope envelope);
}

