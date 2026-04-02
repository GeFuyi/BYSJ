package com.community.ws;

import com.community.dto.WsChatMessageResponse;
import com.community.dto.WsFriendAddResult;
import com.community.dto.WsFriendRequestResponse;
import com.community.dto.WsGroupResponse;
import com.community.dto.WsInitPayload;
import com.community.dto.WsOnlineUserResponse;
import com.community.dto.WsSocialCommentResponse;
import com.community.dto.WsSocialPostResponse;
import com.community.dto.WsUserBriefResponse;
import com.community.entity.ChatGroup;
import com.community.service.impl.SocialChatDomainService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommunityWsCommandServiceImpl implements CommunityWsCommandService {

    private static final String TYPE_PRIVATE = "PRIVATE";
    private static final String TYPE_GROUP = "GROUP";

    private final WsPushService pushService;
    private final WsSessionRegistry sessionRegistry;
    private final WsOfflineQueueService offlineQueueService;
    private final SocialChatDomainService domainService;
    private final ObjectMapper objectMapper;

    public CommunityWsCommandServiceImpl(WsPushService pushService,
                                         WsSessionRegistry sessionRegistry,
                                         WsOfflineQueueService offlineQueueService,
                                         SocialChatDomainService domainService,
                                         ObjectMapper objectMapper) {
        this.pushService = pushService;
        this.sessionRegistry = sessionRegistry;
        this.offlineQueueService = offlineQueueService;
        this.domainService = domainService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onConnect(WebSocketSession session, WsSessionContext context) {
        List<WsEnvelope> offline = offlineQueueService.pullAll(context.getUserId());
        if (!offline.isEmpty()) {
            pushService.sendToSession(session, WsEnvelope.event("OFFLINE_BATCH", offline));
        }
        broadcastOnlineUsers();
    }

    @Override
    public void onDisconnect(WebSocketSession session, WsSessionContext context) {
        broadcastOnlineUsers();
    }

    @Override
    public void onMessage(WebSocketSession session, WsSessionContext context, WsEnvelope envelope) {
        try {
            String type = envelope.getType().trim().toUpperCase();
            if ("PING".equals(type)) {
                pushService.sendOk(session, "PONG", envelope.getRequestId(), null);
                return;
            }
            if ("ONLINE_LIST".equals(type)) {
                pushService.sendOk(session, "ONLINE_LIST", envelope.getRequestId(), toOnlineUsers());
                return;
            }
            if ("INIT".equals(type)) {
                pushService.sendOk(session, "INIT", envelope.getRequestId(), buildInitPayload(context.getUserId()));
                return;
            }
            if ("USER_DIRECTORY".equals(type)) {
                pushService.sendOk(session, "USER_DIRECTORY", envelope.getRequestId(), domainService.listUserDirectory(context.getUserId()));
                return;
            }
            if ("FRIEND_LIST".equals(type)) {
                pushService.sendOk(session, "FRIEND_LIST", envelope.getRequestId(), domainService.listFriends(context.getUserId()));
                return;
            }
            if ("FRIEND_ADD".equals(type)) {
                handleFriendAdd(session, context, envelope);
                return;
            }
            if ("FRIEND_REQUEST_LIST".equals(type)) {
                pushService.sendOk(session, "FRIEND_REQUEST_LIST", envelope.getRequestId(),
                        domainService.listFriendRequests(context.getUserId()));
                return;
            }
            if ("FRIEND_REQUEST_HANDLE".equals(type)) {
                handleFriendRequestHandle(session, context, envelope);
                return;
            }
            if ("FRIEND_REMOVE".equals(type)) {
                handleFriendRemove(session, context, envelope);
                return;
            }
            if ("GROUP_LIST".equals(type)) {
                pushService.sendOk(session, "GROUP_LIST", envelope.getRequestId(), domainService.listGroups(context.getUserId()));
                return;
            }
            if ("GROUP_CREATE".equals(type)) {
                handleGroupCreate(session, context, envelope);
                return;
            }
            if ("GROUP_QUIT".equals(type)) {
                handleGroupQuit(session, context, envelope);
                return;
            }
            if ("GROUP_MUTE".equals(type)) {
                handleGroupMute(session, context, envelope);
                return;
            }
            if ("GROUP_ANNOUNCEMENT_SET".equals(type)) {
                handleGroupAnnouncementSet(session, context, envelope);
                return;
            }
            if ("GROUP_ANNOUNCEMENT_ACK".equals(type)) {
                handleGroupAnnouncementAck(session, context, envelope);
                return;
            }
            if ("POST_LIST".equals(type)) {
                handlePostList(session, context, envelope);
                return;
            }
            if ("POST_CREATE".equals(type)) {
                handlePostCreate(session, context, envelope);
                return;
            }
            if ("COMMENT_LIST".equals(type)) {
                handleCommentList(session, context, envelope);
                return;
            }
            if ("COMMENT_CREATE".equals(type)) {
                handleCommentCreate(session, context, envelope);
                return;
            }
            if ("PRIVATE_SEND".equals(type)) {
                handlePrivateSend(session, context, envelope);
                return;
            }
            if ("GROUP_SEND".equals(type)) {
                handleGroupSend(session, context, envelope);
                return;
            }
            if ("PRIVATE_HISTORY".equals(type)) {
                handlePrivateHistory(session, context, envelope);
                return;
            }
            if ("GROUP_HISTORY".equals(type)) {
                handleGroupHistory(session, context, envelope);
                return;
            }
            pushService.sendError(session, envelope.getRequestId(), 400, "不支持的消息类型: " + envelope.getType());
        } catch (Exception ex) {
            pushService.sendError(session, envelope.getRequestId(), 400, ex.getMessage());
        }
    }

    private void broadcastOnlineUsers() {
        pushService.broadcast(WsEnvelope.event("ONLINE_LIST", toOnlineUsers()));
    }

    private List<WsOnlineUserResponse> toOnlineUsers() {
        List<WsOnlineUserResponse> list = new ArrayList<>();
        for (WsSessionContext online : sessionRegistry.listOnlineUsers()) {
            WsOnlineUserResponse item = new WsOnlineUserResponse();
            item.setUserId(online.getUserId());
            item.setUsername(online.getUsername());
            item.setNickname(online.getNickname());
            item.setAvatarPath(online.getAvatarPath());
            item.setRole(online.getRole());
            list.add(item);
        }
        return list;
    }

    private WsInitPayload buildInitPayload(Long userId) {
        WsInitPayload payload = new WsInitPayload();
        payload.setFeed(domainService.listFeed(userId, null, 20));
        payload.setFriends(domainService.listFriends(userId));
        payload.setFriendRequests(domainService.listFriendRequests(userId));
        payload.setGroups(domainService.listGroups(userId));
        payload.setOnlineUsers(toOnlineUsers());
        payload.setUserDirectory(domainService.listUserDirectory(userId));
        return payload;
    }

    private void handleFriendAdd(WebSocketSession session, WsSessionContext context, WsEnvelope envelope) {
        Map<String, Object> payload = payloadMap(envelope.getPayload());
        Long friendUserId = asLong(payload.get("friendUserId"));
        String message = asString(payload.get("message"));
        WsFriendAddResult response = domainService.addFriend(context.getUserId(), friendUserId, message);
        pushService.sendOk(session, "FRIEND_ADD", envelope.getRequestId(), response);

        refreshFriendStateForUser(context.getUserId());
        if (friendUserId != null) {
            refreshFriendStateForUser(friendUserId);
        }
    }

    private void handleFriendRequestHandle(WebSocketSession session, WsSessionContext context, WsEnvelope envelope) {
        Map<String, Object> payload = payloadMap(envelope.getPayload());
        Long requestId = asLong(payload.get("requestId"));
        String action = asString(payload.get("action"));
        WsFriendRequestResponse response = domainService.handleFriendRequest(context.getUserId(), requestId, action);
        pushService.sendOk(session, "FRIEND_REQUEST_HANDLE", envelope.getRequestId(), response);

        refreshFriendStateForUser(context.getUserId());
        if (response.getRequesterId() != null) {
            refreshFriendStateForUser(response.getRequesterId());
        }
    }

    private void handleFriendRemove(WebSocketSession session, WsSessionContext context, WsEnvelope envelope) {
        Map<String, Object> payload = payloadMap(envelope.getPayload());
        Long friendUserId = asLong(payload.get("friendUserId"));
        domainService.removeFriend(context.getUserId(), friendUserId);
        pushService.sendOk(session, "FRIEND_REMOVE", envelope.getRequestId(), true);

        refreshFriendStateForUser(context.getUserId());
        if (friendUserId != null) {
            refreshFriendStateForUser(friendUserId);
        }
    }

    private void refreshFriendStateForUser(Long userId) {
        if (userId == null) {
            return;
        }
        pushService.sendToUserOnlineOnly(userId, WsEnvelope.event("FRIEND_LIST", domainService.listFriends(userId)));
        pushService.sendToUserOnlineOnly(userId, WsEnvelope.event("FRIEND_REQUEST_LIST", domainService.listFriendRequests(userId)));
    }

    private void refreshGroupStateForUser(Long userId) {
        if (userId == null) {
            return;
        }
        pushService.sendToUserOnlineOnly(userId, WsEnvelope.event("GROUP_LIST", domainService.listGroups(userId)));
    }

    private void handleGroupCreate(WebSocketSession session, WsSessionContext context, WsEnvelope envelope) {
        Map<String, Object> payload = payloadMap(envelope.getPayload());
        String name = asString(payload.get("name"));
        List<Long> memberIds = asLongList(payload.get("memberIds"));
        WsGroupResponse response = domainService.createGroup(context.getUserId(), name, memberIds);
        pushService.sendOk(session, "GROUP_CREATE", envelope.getRequestId(), response);
        for (WsUserBriefResponse member : response.getMembers()) {
            pushService.sendToUserOnlineOnly(member.getUserId(),
                    WsEnvelope.event("GROUP_LIST", domainService.listGroups(member.getUserId())));
        }
    }

    private void handleGroupQuit(WebSocketSession session, WsSessionContext context, WsEnvelope envelope) {
        Map<String, Object> payload = payloadMap(envelope.getPayload());
        Long groupId = asLong(payload.get("groupId"));
        List<Long> beforeMembers = domainService.listGroupMemberIds(groupId);
        domainService.quitGroup(context.getUserId(), groupId);
        pushService.sendOk(session, "GROUP_QUIT", envelope.getRequestId(), true);

        for (Long memberId : beforeMembers) {
            refreshGroupStateForUser(memberId);
        }
    }

    private void handleGroupMute(WebSocketSession session, WsSessionContext context, WsEnvelope envelope) {
        Map<String, Object> payload = payloadMap(envelope.getPayload());
        Long groupId = asLong(payload.get("groupId"));
        Boolean muted = asBoolean(payload.get("muted"));
        WsGroupResponse response = domainService.setGroupMuted(context.getUserId(), groupId, Boolean.TRUE.equals(muted));
        pushService.sendOk(session, "GROUP_MUTE", envelope.getRequestId(), response);
        refreshGroupStateForUser(context.getUserId());
    }

    private void handleGroupAnnouncementSet(WebSocketSession session, WsSessionContext context, WsEnvelope envelope) {
        Map<String, Object> payload = payloadMap(envelope.getPayload());
        Long groupId = asLong(payload.get("groupId"));
        String announcement = asString(payload.get("announcement"));
        WsGroupResponse response = domainService.setGroupAnnouncement(context.getUserId(), groupId, announcement);
        pushService.sendOk(session, "GROUP_ANNOUNCEMENT_SET", envelope.getRequestId(), response);

        List<Long> members = domainService.listGroupMemberIds(groupId);
        for (Long memberId : members) {
            refreshGroupStateForUser(memberId);
        }
    }

    private void handleGroupAnnouncementAck(WebSocketSession session, WsSessionContext context, WsEnvelope envelope) {
        Map<String, Object> payload = payloadMap(envelope.getPayload());
        Long groupId = asLong(payload.get("groupId"));
        WsGroupResponse response = domainService.ackGroupAnnouncement(context.getUserId(), groupId);
        pushService.sendOk(session, "GROUP_ANNOUNCEMENT_ACK", envelope.getRequestId(), response);
        refreshGroupStateForUser(context.getUserId());
    }

    private void handlePostList(WebSocketSession session, WsSessionContext context, WsEnvelope envelope) {
        Map<String, Object> payload = payloadMap(envelope.getPayload());
        Long beforeId = asLong(payload.get("beforeId"));
        Integer limit = asInteger(payload.get("limit"));
        List<WsSocialPostResponse> list = domainService.listFeed(context.getUserId(), beforeId, limit);
        pushService.sendOk(session, "POST_LIST", envelope.getRequestId(), list);
    }

    private void handlePostCreate(WebSocketSession session, WsSessionContext context, WsEnvelope envelope) {
        Map<String, Object> payload = payloadMap(envelope.getPayload());
        String content = asString(payload.get("content"));
        List<String> imagePaths = asStringList(payload.get("imagePaths"));
        WsSocialPostResponse response = domainService.createPost(context.getUserId(), content, imagePaths);
        pushService.sendOk(session, "POST_CREATE", envelope.getRequestId(), response);
        pushService.broadcast(WsEnvelope.event("NEW_POST", response));
    }

    private void handleCommentList(WebSocketSession session, WsSessionContext context, WsEnvelope envelope) {
        Map<String, Object> payload = payloadMap(envelope.getPayload());
        Long postId = asLong(payload.get("postId"));
        List<WsSocialCommentResponse> list = domainService.listComments(context.getUserId(), postId);
        pushService.sendOk(session, "COMMENT_LIST", envelope.getRequestId(), list);
    }

    private void handleCommentCreate(WebSocketSession session, WsSessionContext context, WsEnvelope envelope) {
        Map<String, Object> payload = payloadMap(envelope.getPayload());
        Long postId = asLong(payload.get("postId"));
        Long parentId = asLong(payload.get("parentId"));
        String content = asString(payload.get("content"));
        List<String> imagePaths = asStringList(payload.get("imagePaths"));
        WsSocialCommentResponse response = domainService.createComment(context.getUserId(), postId, parentId, content, imagePaths);
        pushService.sendOk(session, "COMMENT_CREATE", envelope.getRequestId(), response);
        pushService.broadcast(WsEnvelope.event("NEW_COMMENT", response));
    }

    private void handlePrivateSend(WebSocketSession session, WsSessionContext context, WsEnvelope envelope) {
        Map<String, Object> payload = payloadMap(envelope.getPayload());
        Long toUserId = asLong(payload.get("toUserId"));
        String content = asString(payload.get("content"));
        List<String> imagePaths = asStringList(payload.get("imagePaths"));
        WsChatMessageResponse response = domainService.sendPrivate(context.getUserId(), toUserId, content, imagePaths);
        pushService.sendOk(session, "PRIVATE_SEND", envelope.getRequestId(), response);
        pushService.sendToUser(toUserId, WsEnvelope.event("NEW_PRIVATE_MESSAGE", response));
    }

    private void handleGroupSend(WebSocketSession session, WsSessionContext context, WsEnvelope envelope) {
        Map<String, Object> payload = payloadMap(envelope.getPayload());
        Long groupId = asLong(payload.get("groupId"));
        String content = asString(payload.get("content"));
        List<String> imagePaths = asStringList(payload.get("imagePaths"));
        WsChatMessageResponse response = domainService.sendGroup(context.getUserId(), groupId, content, imagePaths);
        pushService.sendOk(session, "GROUP_SEND", envelope.getRequestId(), response);

        List<Long> members = domainService.listGroupMemberIds(groupId);
        for (Long memberId : members) {
            if (context.getUserId().equals(memberId)) {
                continue;
            }
            pushService.sendToUser(memberId, WsEnvelope.event("NEW_GROUP_MESSAGE", response));
        }
    }

    private void handlePrivateHistory(WebSocketSession session, WsSessionContext context, WsEnvelope envelope) {
        Map<String, Object> payload = payloadMap(envelope.getPayload());
        Long peerId = asLong(payload.get("peerId"));
        Integer limit = asInteger(payload.get("limit"));
        List<WsChatMessageResponse> list = domainService.listPrivateHistory(context.getUserId(), peerId, limit);
        pushService.sendOk(session, "PRIVATE_HISTORY", envelope.getRequestId(), list);
    }

    private void handleGroupHistory(WebSocketSession session, WsSessionContext context, WsEnvelope envelope) {
        Map<String, Object> payload = payloadMap(envelope.getPayload());
        Long groupId = asLong(payload.get("groupId"));
        Integer limit = asInteger(payload.get("limit"));
        List<WsChatMessageResponse> list = domainService.listGroupHistory(context.getUserId(), groupId, limit);
        pushService.sendOk(session, "GROUP_HISTORY", envelope.getRequestId(), list);
    }

    private Map<String, Object> payloadMap(Object payload) {
        if (payload == null) {
            return new HashMap<>();
        }
        if (payload instanceof Map) {
            return (Map<String, Object>) payload;
        }
        return objectMapper.convertValue(payload, new TypeReference<Map<String, Object>>() {
        });
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : null;
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Integer.parseInt(text);
    }

    private Boolean asBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        if ("1".equals(text)) {
            return true;
        }
        if ("0".equals(text)) {
            return false;
        }
        return Boolean.parseBoolean(text);
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Long.parseLong(text);
    }

    private List<String> asStringList(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }
        List<String> list = new ArrayList<>();
        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                String text = asString(item);
                if (text != null) {
                    list.add(text);
                }
            }
        }
        return list;
    }

    private List<Long> asLongList(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }
        List<Long> list = new ArrayList<>();
        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                Long number = asLong(item);
                if (number != null) {
                    list.add(number);
                }
            }
        }
        return list;
    }
}



