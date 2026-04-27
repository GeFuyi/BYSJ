package com.community.service.impl;

import com.community.common.BusinessException;
import com.community.dto.WsChatMessageResponse;
import com.community.dto.WsFriendAddResult;
import com.community.dto.WsFriendRequestResponse;
import com.community.dto.WsFriendResponse;
import com.community.dto.WsGroupResponse;
import com.community.dto.WsSocialCommentResponse;
import com.community.dto.WsSocialPostResponse;
import com.community.dto.WsUserBriefResponse;
import com.community.entity.ChatGroup;
import com.community.entity.ChatGroupAnnouncementAck;
import com.community.entity.ChatGroupMember;
import com.community.entity.ChatMessage;
import com.community.entity.FriendRelation;
import com.community.entity.FriendRequest;
import com.community.entity.SocialComment;
import com.community.entity.SocialCommentImage;
import com.community.entity.SocialPost;
import com.community.entity.SocialPostImage;
import com.community.entity.SysUser;
import com.community.mapper.ChatGroupAnnouncementAckMapper;
import com.community.mapper.ChatGroupMapper;
import com.community.mapper.ChatGroupMemberMapper;
import com.community.mapper.ChatMessageMapper;
import com.community.mapper.FriendRelationMapper;
import com.community.mapper.FriendRequestMapper;
import com.community.mapper.SocialCommentImageMapper;
import com.community.mapper.SocialCommentMapper;
import com.community.mapper.SocialPostImageMapper;
import com.community.mapper.SocialPostMapper;
import com.community.service.SysUserService;
import com.community.ws.WsSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SocialChatDomainService {

    private static final String RECEIVER_TYPE_PRIVATE = "PRIVATE";
    private static final String RECEIVER_TYPE_GROUP = "GROUP";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String GROUP_ROLE_OWNER = "OWNER";
    private static final String GROUP_ROLE_MEMBER = "MEMBER";
    private static final String FRIEND_REQUEST_PENDING = "PENDING";
    private static final String FRIEND_REQUEST_ACCEPTED = "ACCEPTED";
    private static final String FRIEND_REQUEST_REJECTED = "REJECTED";
    private static final String FEED_VERSION_KEY = "social:feed:version";
    private static final String FEED_LIST_KEY_PREFIX = "social:feed:list:";
    private static final String POST_COMMENTS_KEY_PREFIX = "social:post:comments:";
    private static final String FRIEND_LIST_KEY_PREFIX = "social:friends:";
    private static final String FRIEND_REQUEST_LIST_KEY_PREFIX = "social:friend-requests:";
    private static final Duration FEED_TTL = Duration.ofSeconds(60);
    private static final Duration COMMENTS_TTL = Duration.ofSeconds(60);
    private static final Duration FRIENDS_TTL = Duration.ofMinutes(2);
    private static final Duration FRIEND_REQUESTS_TTL = Duration.ofMinutes(2);

    private final SocialPostMapper postMapper;
    private final SocialPostImageMapper postImageMapper;
    private final SocialCommentMapper commentMapper;
    private final SocialCommentImageMapper commentImageMapper;
    private final FriendRelationMapper friendRelationMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final ChatGroupMapper chatGroupMapper;
    private final ChatGroupMemberMapper chatGroupMemberMapper;
    private final ChatGroupAnnouncementAckMapper groupAnnouncementAckMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final SysUserService sysUserService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final WsSessionRegistry sessionRegistry;

    public SocialChatDomainService(SocialPostMapper postMapper,
                                   SocialPostImageMapper postImageMapper,
                                   SocialCommentMapper commentMapper,
                                   SocialCommentImageMapper commentImageMapper,
                                   FriendRelationMapper friendRelationMapper,
                                   FriendRequestMapper friendRequestMapper,
                                   ChatGroupMapper chatGroupMapper,
                                   ChatGroupMemberMapper chatGroupMemberMapper,
                                   ChatGroupAnnouncementAckMapper groupAnnouncementAckMapper,
                                   ChatMessageMapper chatMessageMapper,
                                   SysUserService sysUserService,
                                   RedisTemplate<String, Object> redisTemplate,
                                   ObjectMapper objectMapper,
                                   WsSessionRegistry sessionRegistry) {
        this.postMapper = postMapper;
        this.postImageMapper = postImageMapper;
        this.commentMapper = commentMapper;
        this.commentImageMapper = commentImageMapper;
        this.friendRelationMapper = friendRelationMapper;
        this.friendRequestMapper = friendRequestMapper;
        this.chatGroupMapper = chatGroupMapper;
        this.chatGroupMemberMapper = chatGroupMemberMapper;
        this.groupAnnouncementAckMapper = groupAnnouncementAckMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.sysUserService = sysUserService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.sessionRegistry = sessionRegistry;
    }

    @Transactional(rollbackFor = Exception.class)
    public WsSocialPostResponse createPost(Long userId, String content, List<String> imagePaths) {
        SysUser user = requireActiveUser(userId);
        List<String> sanitizedImages = sanitizeImagePaths(imagePaths);
        String safeContent = normalizeContent(content);
        ensureContentOrImages(safeContent, sanitizedImages);

        SocialPost post = new SocialPost();
        post.setUserId(userId);
        post.setContent(safeContent == null ? "" : safeContent);
        postMapper.insert(post);
        if (!sanitizedImages.isEmpty()) {
            postImageMapper.insertBatch(post.getId(), sanitizedImages);
        }
        incrementFeedVersion();
        SocialPost saved = postMapper.selectById(post.getId());
        return buildPostResponse(saved, user, sanitizedImages, new ArrayList<>());
    }

    public List<WsSocialPostResponse> listFeed(Long currentUserId, Long beforeId, Integer limit) {
        requireActiveUser(currentUserId);
        int safeLimit = limit == null ? 20 : Math.max(1, Math.min(50, limit));
        String cacheKey = feedKey(currentFeedVersion(), beforeId, safeLimit);
        Object cached = getCache(cacheKey);
        if (cached != null) {
            return convertList(cached, WsSocialPostResponse.class);
        }

        List<SocialPost> posts = postMapper.selectFeed(beforeId, safeLimit);
        if (posts.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> postIds = posts.stream().map(SocialPost::getId).collect(Collectors.toList());
        Map<Long, List<String>> postImages = groupPostImages(postImageMapper.selectByPostIds(postIds));
        Map<Long, List<WsSocialCommentResponse>> commentMap = groupCommentsByPost(postIds);
        Map<Long, SysUser> userMap = loadUsers(posts.stream().map(SocialPost::getUserId).collect(Collectors.toList()));

        List<WsSocialPostResponse> responses = new ArrayList<>();
        for (SocialPost post : posts) {
            SysUser owner = userMap.get(post.getUserId());
            responses.add(buildPostResponse(
                    post,
                    owner,
                    postImages.getOrDefault(post.getId(), new ArrayList<>()),
                    commentMap.getOrDefault(post.getId(), new ArrayList<>())
            ));
        }
        putCache(cacheKey, responses, FEED_TTL);
        return responses;
    }

    @Transactional(rollbackFor = Exception.class)
    public WsSocialCommentResponse createComment(Long userId, Long postId, Long parentId, String content, List<String> imagePaths) {
        SysUser user = requireActiveUser(userId);
        SocialPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "资源不存在");
        }

        SocialComment parent = null;
        Long replyToUserId = null;
        if (parentId != null) {
            parent = commentMapper.selectById(parentId);
            if (parent == null || !postId.equals(parent.getPostId())) {
                throw new BusinessException(400, "请求参数不合法");
            }
            replyToUserId = parent.getUserId();
        }

        List<String> sanitizedImages = sanitizeImagePaths(imagePaths);
        String safeContent = normalizeContent(content);
        ensureContentOrImages(safeContent, sanitizedImages);

        SocialComment comment = new SocialComment();
        comment.setPostId(postId);
        comment.setParentId(parentId);
        comment.setReplyToUserId(replyToUserId);
        comment.setUserId(userId);
        comment.setContent(safeContent == null ? "" : safeContent);
        commentMapper.insert(comment);
        if (!sanitizedImages.isEmpty()) {
            commentImageMapper.insertBatch(comment.getId(), sanitizedImages);
        }
        SysUser replyToUser = parent == null ? null : loadUser(parent.getUserId());
        evictCommentsCache(postId);
        incrementFeedVersion();
        SocialComment saved = commentMapper.selectById(comment.getId());
        return buildCommentResponse(saved, user, replyToUser, sanitizedImages);
    }

    public List<WsSocialCommentResponse> listComments(Long currentUserId, Long postId) {
        requireActiveUser(currentUserId);
        SocialPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "资源不存在");
        }
        Object cached = getCache(commentsKey(postId));
        if (cached != null) {
            return convertList(cached, WsSocialCommentResponse.class);
        }
        List<WsSocialCommentResponse> comments = commentsOfPost(postId);
        putCache(commentsKey(postId), comments, COMMENTS_TTL);
        return comments;
    }

    @Transactional(rollbackFor = Exception.class)
    public WsFriendAddResult addFriend(Long userId, Long friendUserId, String message) {
        SysUser currentUser = requireActiveUser(userId);
        if (friendUserId == null || friendUserId <= 0) {
            throw new BusinessException(400, "请求参数不合法");
        }
        if (userId.equals(friendUserId)) {
            throw new BusinessException(400, "请求参数不合法");
        }
        SysUser friend = requireActiveUser(friendUserId);

        if (isFriend(userId, friendUserId)) {
            WsFriendAddResult result = new WsFriendAddResult();
            result.setMode("DIRECT");
            result.setFriend(toFriendResponse(friendRelationMapper.selectByUserAndFriend(userId, friendUserId), friend));
            return result;
        }

        FriendRequest reversePending = friendRequestMapper.selectPending(friendUserId, userId);
        if (reversePending != null) {
            handleFriendRequestInternal(currentUser, reversePending, "ACCEPT");
            evictFriendRequestCache(userId);
            evictFriendListCache(userId);
            evictFriendListCache(friendUserId);
            WsFriendAddResult result = new WsFriendAddResult();
            result.setMode("DIRECT");
            result.setFriend(toFriendResponse(friendRelationMapper.selectByUserAndFriend(userId, friendUserId), friend));
            return result;
        }

        if (ROLE_ADMIN.equals(currentUser.getRole())) {
            ensureFriendRelation(userId, friendUserId);
            ensureFriendRelation(friendUserId, userId);
            evictFriendListCache(userId);
            evictFriendListCache(friendUserId);
            WsFriendAddResult result = new WsFriendAddResult();
            result.setMode("DIRECT");
            result.setFriend(toFriendResponse(friendRelationMapper.selectByUserAndFriend(userId, friendUserId), friend));
            return result;
        }

        FriendRequest existed = friendRequestMapper.selectPending(userId, friendUserId);
        if (existed != null) {
            WsFriendAddResult result = new WsFriendAddResult();
            result.setMode("PENDING");
            result.setRequest(toFriendRequestResponse(existed, currentUser, friend));
            return result;
        }

        FriendRequest request = new FriendRequest();
        request.setRequesterId(userId);
        request.setTargetUserId(friendUserId);
        request.setStatus(FRIEND_REQUEST_PENDING);
        request.setMessage(StringUtils.hasText(message) ? message.trim() : null);
        friendRequestMapper.insert(request);
        evictFriendRequestCache(friendUserId);

        WsFriendAddResult result = new WsFriendAddResult();
        result.setMode("PENDING");
        result.setRequest(toFriendRequestResponse(friendRequestMapper.selectById(request.getId()), currentUser, friend));
        return result;
    }

    public List<WsFriendRequestResponse> listFriendRequests(Long userId) {
        SysUser current = requireActiveUser(userId);
        Object cached = getCache(friendRequestListKey(current.getId()));
        if (cached != null) {
            return convertList(cached, WsFriendRequestResponse.class);
        }
        List<FriendRequest> requests = friendRequestMapper.selectPendingByTarget(current.getId());
        if (requests.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> userIds = new LinkedHashSet<>();
        for (FriendRequest request : requests) {
            userIds.add(request.getRequesterId());
            userIds.add(request.getTargetUserId());
        }
        Map<Long, SysUser> userMap = loadUsers(new ArrayList<>(userIds));
        List<WsFriendRequestResponse> responses = requests.stream()
                .map(item -> toFriendRequestResponse(item, userMap.get(item.getRequesterId()), userMap.get(item.getTargetUserId())))
                .collect(Collectors.toList());
        putCache(friendRequestListKey(current.getId()), responses, FRIEND_REQUESTS_TTL);
        return responses;
    }

    @Transactional(rollbackFor = Exception.class)
    public WsFriendRequestResponse handleFriendRequest(Long userId, Long requestId, String action) {
        SysUser current = requireActiveUser(userId);
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(404, "资源不存在");
        }
        if (!Objects.equals(request.getTargetUserId(), userId)) {
            throw new BusinessException(403, "无权限访问");
        }
        if (!FRIEND_REQUEST_PENDING.equals(request.getStatus())) {
            throw new BusinessException(400, "请求参数不合法");
        }

        String normalizedAction = StringUtils.hasText(action) ? action.trim().toUpperCase() : "";
        handleFriendRequestInternal(current, request, normalizedAction);

        FriendRequest refreshed = friendRequestMapper.selectById(requestId);
        evictFriendRequestCache(refreshed.getTargetUserId());
        if (FRIEND_REQUEST_ACCEPTED.equals(refreshed.getStatus())) {
            evictFriendListCache(refreshed.getRequesterId());
            evictFriendListCache(refreshed.getTargetUserId());
        }
        SysUser requester = loadUser(refreshed.getRequesterId());
        SysUser target = loadUser(refreshed.getTargetUserId());
        return toFriendRequestResponse(refreshed, requester, target);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeFriend(Long userId, Long friendUserId) {
        requireActiveUser(userId);
        requireActiveUser(friendUserId);
        friendRelationMapper.deleteByUserAndFriend(userId, friendUserId);
        friendRelationMapper.deleteByUserAndFriend(friendUserId, userId);
        evictFriendListCache(userId);
        evictFriendListCache(friendUserId);
    }

    public List<WsFriendResponse> listFriends(Long userId) {
        requireActiveUser(userId);
        Object cached = getCache(friendListKey(userId));
        if (cached != null) {
            return applyOnlineStatus(convertList(cached, WsFriendResponse.class));
        }
        List<FriendRelation> relations = friendRelationMapper.selectByUserId(userId);
        if (relations.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, SysUser> userMap = loadUsers(relations.stream().map(FriendRelation::getFriendId).collect(Collectors.toList()));
        List<WsFriendResponse> result = new ArrayList<>();
        for (FriendRelation relation : relations) {
            SysUser friend = userMap.get(relation.getFriendId());
            if (friend != null) {
                result.add(toFriendResponse(relation, friend));
            }
        }
        putCache(friendListKey(userId), result, FRIENDS_TTL);
        return applyOnlineStatus(result);
    }

    public List<WsUserBriefResponse> listUserDirectory(Long currentUserId) {
        requireActiveUser(currentUserId);
        List<SysUser> users = sysUserService.listUsers();
        List<WsUserBriefResponse> list = new ArrayList<>();
        for (SysUser user : users) {
            if (user == null || user.getStatus() == null || user.getStatus() != 1 || currentUserId.equals(user.getId())) {
                continue;
            }
            list.add(toUserBrief(user));
        }
        return list;
    }

    @Transactional(rollbackFor = Exception.class)
    public WsGroupResponse createGroup(Long ownerId, String name, List<Long> memberIds) {
        requireActiveUser(ownerId);
        String groupName = StringUtils.hasText(name) ? name.trim() : "";
        if (!StringUtils.hasText(groupName)) {
            throw new BusinessException(400, "请求参数不合法");
        }
        if (groupName.length() > 80) {
            throw new BusinessException(400, "请求参数不合法");
        }

        Set<Long> memberSet = new LinkedHashSet<>();
        memberSet.add(ownerId);
        if (memberIds != null) {
            for (Long memberId : memberIds) {
                if (memberId == null || memberId <= 0 || ownerId.equals(memberId)) {
                    continue;
                }
                requireActiveUser(memberId);
                if (!isFriend(ownerId, memberId)) {
                    throw new BusinessException(403, "无权限访问");
                }
                memberSet.add(memberId);
            }
        }
        if (memberSet.size() < 2) {
            throw new BusinessException(400, "请求参数不合法");
        }

        ChatGroup group = new ChatGroup();
        group.setName(groupName);
        group.setOwnerId(ownerId);
        group.setAnnouncement(null);
        group.setAnnouncementVersion(0L);
        group.setAnnouncementUpdatedAt(null);
        chatGroupMapper.insert(group);

        for (Long memberId : memberSet) {
            ChatGroupMember member = new ChatGroupMember();
            member.setGroupId(group.getId());
            member.setUserId(memberId);
            member.setRole(ownerId.equals(memberId) ? GROUP_ROLE_OWNER : GROUP_ROLE_MEMBER);
            member.setMuted(0);
            chatGroupMemberMapper.insert(member);
        }

        ChatGroup saved = chatGroupMapper.selectById(group.getId());
        ChatGroupMember ownerMember = chatGroupMemberMapper.selectByGroupAndUser(group.getId(), ownerId);
        return buildGroupResponse(saved, ownerId, ownerMember);
    }

    public List<WsGroupResponse> listGroups(Long userId) {
        requireActiveUser(userId);
        List<ChatGroupMember> memberships = chatGroupMemberMapper.selectByUserId(userId);
        if (memberships.isEmpty()) {
            return new ArrayList<>();
        }
        List<WsGroupResponse> list = new ArrayList<>();
        for (ChatGroupMember membership : memberships) {
            ChatGroup group = chatGroupMapper.selectById(membership.getGroupId());
            if (group != null) {
                list.add(buildGroupResponse(group, userId, membership));
            }
        }
        return list;
    }

    @Transactional(rollbackFor = Exception.class)
    public void quitGroup(Long userId, Long groupId) {
        requireActiveUser(userId);
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(404, "资源不存在");
        }
        ChatGroupMember membership = chatGroupMemberMapper.selectByGroupAndUser(groupId, userId);
        if (membership == null) {
            throw new BusinessException(403, "无权限访问");
        }

        boolean isOwner = Objects.equals(group.getOwnerId(), userId);
        chatGroupMemberMapper.deleteByGroupAndUser(groupId, userId);

        if (!isOwner) {
            return;
        }

        int remain = chatGroupMemberMapper.countByGroupId(groupId);
        if (remain <= 0) {
            chatGroupMapper.deleteById(groupId);
            return;
        }

        ChatGroupMember nextOwner = chatGroupMemberMapper.selectFirstByGroupId(groupId);
        if (nextOwner == null) {
            chatGroupMapper.deleteById(groupId);
            return;
        }

        chatGroupMemberMapper.updateRole(groupId, nextOwner.getUserId(), GROUP_ROLE_OWNER);
        chatGroupMapper.updateOwner(groupId, nextOwner.getUserId());
    }

    @Transactional(rollbackFor = Exception.class)
    public WsGroupResponse setGroupMuted(Long userId, Long groupId, boolean muted) {
        requireActiveUser(userId);
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(404, "资源不存在");
        }
        ChatGroupMember membership = chatGroupMemberMapper.selectByGroupAndUser(groupId, userId);
        if (membership == null) {
            throw new BusinessException(403, "无权限访问");
        }

        chatGroupMemberMapper.updateMuted(groupId, userId, muted ? 1 : 0);
        ChatGroupMember refreshed = chatGroupMemberMapper.selectByGroupAndUser(groupId, userId);
        return buildGroupResponse(group, userId, refreshed);
    }

    @Transactional(rollbackFor = Exception.class)
    public WsGroupResponse setGroupAnnouncement(Long userId, Long groupId, String announcement) {
        SysUser current = requireActiveUser(userId);
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(404, "资源不存在");
        }

        ChatGroupMember membership = chatGroupMemberMapper.selectByGroupAndUser(groupId, userId);
        if (membership == null) {
            throw new BusinessException(403, "无权限访问");
        }

        boolean canEdit = Objects.equals(group.getOwnerId(), userId) || ROLE_ADMIN.equals(current.getRole());
        if (!canEdit) {
            throw new BusinessException(403, "无权限访问");
        }

        String safeAnnouncement = StringUtils.hasText(announcement) ? announcement.trim() : null;
        if (safeAnnouncement != null && safeAnnouncement.length() > 500) {
            throw new BusinessException(400, "请求参数不合法");
        }

        long version = (group.getAnnouncementVersion() == null ? 0L : group.getAnnouncementVersion()) + 1L;
        chatGroupMapper.updateAnnouncement(groupId, safeAnnouncement, version);

        ChatGroup refreshed = chatGroupMapper.selectById(groupId);
        ChatGroupMember refreshedMember = chatGroupMemberMapper.selectByGroupAndUser(groupId, userId);
        return buildGroupResponse(refreshed, userId, refreshedMember);
    }

    @Transactional(rollbackFor = Exception.class)
    public WsGroupResponse ackGroupAnnouncement(Long userId, Long groupId) {
        requireActiveUser(userId);
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(404, "资源不存在");
        }

        ChatGroupMember membership = chatGroupMemberMapper.selectByGroupAndUser(groupId, userId);
        if (membership == null) {
            throw new BusinessException(403, "无权限访问");
        }

        if (StringUtils.hasText(group.getAnnouncement()) && group.getAnnouncementVersion() != null && group.getAnnouncementVersion() > 0) {
            ChatGroupAnnouncementAck ack = new ChatGroupAnnouncementAck();
            ack.setGroupId(groupId);
            ack.setUserId(userId);
            ack.setAnnouncementVersion(group.getAnnouncementVersion());
            groupAnnouncementAckMapper.insert(ack);
        }

        ChatGroupMember refreshedMember = chatGroupMemberMapper.selectByGroupAndUser(groupId, userId);
        return buildGroupResponse(group, userId, refreshedMember);
    }

    @Transactional(rollbackFor = Exception.class)
    public WsChatMessageResponse sendPrivate(Long senderId, Long targetUserId, String content, List<String> imagePaths) {
        SysUser sender = requireActiveUser(senderId);
        SysUser target = requireActiveUser(targetUserId);
        if (!isFriend(senderId, targetUserId)) {
            throw new BusinessException(403, "无权限访问");
        }
        return saveChatMessage(sender, RECEIVER_TYPE_PRIVATE, target.getId(), content, imagePaths);
    }

    @Transactional(rollbackFor = Exception.class)
    public WsChatMessageResponse sendGroup(Long senderId, Long groupId, String content, List<String> imagePaths) {
        SysUser sender = requireActiveUser(senderId);
        ensureGroupMember(groupId, senderId);
        return saveChatMessage(sender, RECEIVER_TYPE_GROUP, groupId, content, imagePaths);
    }

    public List<WsChatMessageResponse> listPrivateHistory(Long userId, Long peerId, Integer limit) {
        requireActiveUser(userId);
        requireActiveUser(peerId);
        if (!isFriend(userId, peerId)) {
            throw new BusinessException(403, "无权限访问");
        }
        int safeLimit = limit == null ? 50 : Math.max(1, Math.min(200, limit));
        List<ChatMessage> messages = chatMessageMapper.selectPrivateHistory(userId, peerId, safeLimit);
        Collections.reverse(messages);
        Map<Long, SysUser> userMap = loadUsers(messages.stream().map(ChatMessage::getSenderId).collect(Collectors.toList()));
        return messages.stream().map(msg -> toMessageResponse(msg, userMap.get(msg.getSenderId()))).collect(Collectors.toList());
    }

    public List<WsChatMessageResponse> listGroupHistory(Long userId, Long groupId, Integer limit) {
        requireActiveUser(userId);
        ensureGroupMember(groupId, userId);
        int safeLimit = limit == null ? 50 : Math.max(1, Math.min(200, limit));
        List<ChatMessage> messages = chatMessageMapper.selectGroupHistory(groupId, safeLimit);
        Collections.reverse(messages);
        Map<Long, SysUser> userMap = loadUsers(messages.stream().map(ChatMessage::getSenderId).collect(Collectors.toList()));
        return messages.stream().map(msg -> toMessageResponse(msg, userMap.get(msg.getSenderId()))).collect(Collectors.toList());
    }

    public List<Long> listGroupMemberIds(Long groupId) {
        return chatGroupMemberMapper.selectByGroupId(groupId).stream().map(ChatGroupMember::getUserId).collect(Collectors.toList());
    }

    public ChatGroup getGroup(Long groupId) {
        return chatGroupMapper.selectById(groupId);
    }

    public boolean isFriend(Long userId, Long friendId) {
        return friendRelationMapper.selectByUserAndFriend(userId, friendId) != null
                && friendRelationMapper.selectByUserAndFriend(friendId, userId) != null;
    }

    public boolean isGroupMuted(Long userId, Long groupId) {
        ChatGroupMember membership = chatGroupMemberMapper.selectByGroupAndUser(groupId, userId);
        return membership != null && membership.getMuted() != null && membership.getMuted() == 1;
    }

    private void handleFriendRequestInternal(SysUser operator, FriendRequest request, String action) {
        if ("ACCEPT".equals(action)) {
            ensureFriendRelation(request.getRequesterId(), request.getTargetUserId());
            ensureFriendRelation(request.getTargetUserId(), request.getRequesterId());
            friendRequestMapper.updateStatus(request.getId(), FRIEND_REQUEST_ACCEPTED, operator.getId());
            return;
        }
        if ("REJECT".equals(action)) {
            friendRequestMapper.updateStatus(request.getId(), FRIEND_REQUEST_REJECTED, operator.getId());
            return;
        }
        throw new BusinessException(400, "请求参数不合法");
    }

    private WsChatMessageResponse saveChatMessage(SysUser sender, String receiverType, Long receiverId, String content, List<String> imagePaths) {
        List<String> sanitizedImages = sanitizeImagePaths(imagePaths);
        String safeContent = normalizeContent(content);
        ensureContentOrImages(safeContent, sanitizedImages);

        ChatMessage message = new ChatMessage();
        message.setSenderId(sender.getId());
        message.setReceiverType(receiverType);
        message.setReceiverId(receiverId);
        message.setContent(safeContent == null ? "" : safeContent);
        message.setImagePaths(String.join(",", sanitizedImages));
        chatMessageMapper.insert(message);

        ChatMessage saved = new ChatMessage();
        saved.setId(message.getId());
        saved.setSenderId(message.getSenderId());
        saved.setReceiverType(message.getReceiverType());
        saved.setReceiverId(message.getReceiverId());
        saved.setContent(message.getContent());
        saved.setImagePaths(message.getImagePaths());
        saved.setCreatedAt(new Date());
        return toMessageResponse(saved, sender);
    }

    private void ensureGroupMember(Long groupId, Long userId) {
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(404, "资源不存在");
        }
        ChatGroupMember membership = chatGroupMemberMapper.selectByGroupAndUser(groupId, userId);
        if (membership == null) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    private void ensureFriendRelation(Long userId, Long friendId) {
        FriendRelation existing = friendRelationMapper.selectByUserAndFriend(userId, friendId);
        if (existing != null) {
            return;
        }
        FriendRelation relation = new FriendRelation();
        relation.setUserId(userId);
        relation.setFriendId(friendId);
        friendRelationMapper.insert(relation);
    }

    private WsGroupResponse buildGroupResponse(ChatGroup group, Long viewerUserId, ChatGroupMember viewerMembership) {
        WsGroupResponse response = new WsGroupResponse();
        response.setGroupId(group.getId());
        response.setName(group.getName());
        response.setOwnerId(group.getOwnerId());
        response.setAnnouncement(group.getAnnouncement());
        response.setAnnouncementVersion(group.getAnnouncementVersion());
        response.setAnnouncementUpdatedAt(group.getAnnouncementUpdatedAt());
        response.setCreatedAt(group.getCreatedAt());

        boolean muted = viewerMembership != null && viewerMembership.getMuted() != null && viewerMembership.getMuted() == 1;
        response.setMuted(muted);

        if (viewerUserId == null || !StringUtils.hasText(group.getAnnouncement())
                || group.getAnnouncementVersion() == null || group.getAnnouncementVersion() <= 0) {
            response.setAnnouncementAcked(true);
        } else {
            ChatGroupAnnouncementAck ack = groupAnnouncementAckMapper.selectByGroupUserVersion(
                    group.getId(),
                    viewerUserId,
                    group.getAnnouncementVersion()
            );
            response.setAnnouncementAcked(ack != null);
        }

        List<ChatGroupMember> members = chatGroupMemberMapper.selectByGroupId(group.getId());
        Map<Long, SysUser> memberUsers = loadUsers(members.stream().map(ChatGroupMember::getUserId).collect(Collectors.toList()));
        List<WsUserBriefResponse> memberBriefs = new ArrayList<>();
        for (ChatGroupMember member : members) {
            SysUser memberUser = memberUsers.get(member.getUserId());
            if (memberUser != null) {
                memberBriefs.add(toUserBrief(memberUser));
            }
        }
        response.setMembers(memberBriefs);
        return response;
    }

    private WsFriendResponse toFriendResponse(FriendRelation relation, SysUser friend) {
        WsFriendResponse response = new WsFriendResponse();
        response.setRelationId(relation == null ? null : relation.getId());
        response.setUserId(friend.getId());
        response.setUsername(friend.getUsername());
        response.setNickname(resolveNickname(friend));
        response.setAvatarPath(friend.getAvatarPath());
        response.setRole(friend.getRole());
        response.setOnline(sessionRegistry.isOnline(friend.getId()));
        response.setCreatedAt(relation == null ? null : relation.getCreatedAt());
        return response;
    }

    private WsFriendRequestResponse toFriendRequestResponse(FriendRequest request, SysUser requester, SysUser target) {
        WsFriendRequestResponse response = new WsFriendRequestResponse();
        response.setRequestId(request.getId());
        response.setRequesterId(request.getRequesterId());
        response.setRequesterUsername(requester == null ? null : requester.getUsername());
        response.setRequesterNickname(requester == null ? null : resolveNickname(requester));
        response.setRequesterAvatarPath(requester == null ? null : requester.getAvatarPath());
        response.setRequesterRole(requester == null ? null : requester.getRole());

        response.setTargetUserId(request.getTargetUserId());
        response.setTargetUsername(target == null ? null : target.getUsername());
        response.setTargetNickname(target == null ? null : resolveNickname(target));
        response.setTargetAvatarPath(target == null ? null : target.getAvatarPath());
        response.setTargetRole(target == null ? null : target.getRole());

        response.setStatus(request.getStatus());
        response.setMessage(request.getMessage());
        response.setHandledAt(request.getHandledAt());
        response.setCreatedAt(request.getCreatedAt());
        return response;
    }

    private WsUserBriefResponse toUserBrief(SysUser user) {
        WsUserBriefResponse brief = new WsUserBriefResponse();
        brief.setUserId(user.getId());
        brief.setUsername(user.getUsername());
        brief.setNickname(resolveNickname(user));
        brief.setAvatarPath(user.getAvatarPath());
        brief.setRole(user.getRole());
        brief.setOnline(sessionRegistry.isOnline(user.getId()));
        return brief;
    }

    private WsSocialPostResponse buildPostResponse(SocialPost post, SysUser owner, List<String> imagePaths, List<WsSocialCommentResponse> comments) {
        WsSocialPostResponse response = new WsSocialPostResponse();
        response.setId(post.getId());
        response.setUserId(post.getUserId());
        response.setUsername(owner == null ? "-" : owner.getUsername());
        response.setNickname(owner == null ? "-" : resolveNickname(owner));
        response.setAvatarPath(owner == null ? null : owner.getAvatarPath());
        response.setContent(post.getContent());
        response.setCreatedAt(post.getCreatedAt());
        response.setImagePaths(imagePaths);
        response.setComments(comments);
        return response;
    }

    private List<WsSocialCommentResponse> commentsOfPost(Long postId) {
        List<SocialComment> comments = commentMapper.selectByPostId(postId);
        if (comments.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> commentIds = comments.stream().map(SocialComment::getId).collect(Collectors.toList());
        Map<Long, List<String>> commentImages = groupCommentImages(commentImageMapper.selectByCommentIds(commentIds));
        Set<Long> userIds = new LinkedHashSet<>();
        for (SocialComment comment : comments) {
            userIds.add(comment.getUserId());
            if (comment.getReplyToUserId() != null) {
                userIds.add(comment.getReplyToUserId());
            }
        }
        Map<Long, SysUser> userMap = loadUsers(new ArrayList<>(userIds));

        List<WsSocialCommentResponse> responses = new ArrayList<>();
        for (SocialComment comment : comments) {
            SysUser user = userMap.get(comment.getUserId());
            SysUser replyTo = comment.getReplyToUserId() == null ? null : userMap.get(comment.getReplyToUserId());
            responses.add(buildCommentResponse(comment, user, replyTo, commentImages.getOrDefault(comment.getId(), new ArrayList<>())));
        }
        return responses;
    }

    private Map<Long, List<WsSocialCommentResponse>> groupCommentsByPost(List<Long> postIds) {
        Map<Long, List<WsSocialCommentResponse>> result = new HashMap<>();
        for (Long postId : postIds) {
            result.put(postId, commentsOfPost(postId));
        }
        return result;
    }

    private WsSocialCommentResponse buildCommentResponse(SocialComment comment, SysUser user, SysUser replyTo, List<String> imagePaths) {
        WsSocialCommentResponse response = new WsSocialCommentResponse();
        response.setId(comment.getId());
        response.setPostId(comment.getPostId());
        response.setParentId(comment.getParentId());
        response.setReplyToUserId(comment.getReplyToUserId());
        response.setUserId(comment.getUserId());
        response.setNickname(user == null ? "-" : resolveNickname(user));
        response.setAvatarPath(user == null ? null : user.getAvatarPath());
        response.setReplyToNickname(replyTo == null ? null : resolveNickname(replyTo));
        response.setReplyToAvatarPath(replyTo == null ? null : replyTo.getAvatarPath());
        response.setContent(comment.getContent());
        response.setImagePaths(imagePaths);
        response.setCreatedAt(comment.getCreatedAt());
        return response;
    }

    private WsChatMessageResponse toMessageResponse(ChatMessage message, SysUser sender) {
        WsChatMessageResponse response = new WsChatMessageResponse();
        response.setId(message.getId());
        response.setReceiverType(message.getReceiverType());
        response.setReceiverId(message.getReceiverId());
        response.setSenderId(message.getSenderId());
        response.setSenderNickname(sender == null ? "-" : resolveNickname(sender));
        response.setSenderAvatarPath(sender == null ? null : sender.getAvatarPath());
        response.setContent(message.getContent());
        response.setImagePaths(splitImagePaths(message.getImagePaths()));
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }

    private Map<Long, List<String>> groupPostImages(List<SocialPostImage> images) {
        Map<Long, List<String>> map = new HashMap<>();
        for (SocialPostImage image : images) {
            map.computeIfAbsent(image.getPostId(), key -> new ArrayList<>()).add(image.getImagePath());
        }
        return map;
    }

    private Map<Long, List<String>> groupCommentImages(List<SocialCommentImage> images) {
        Map<Long, List<String>> map = new HashMap<>();
        for (SocialCommentImage image : images) {
            map.computeIfAbsent(image.getCommentId(), key -> new ArrayList<>()).add(image.getImagePath());
        }
        return map;
    }

    private Map<Long, SysUser> loadUsers(List<Long> userIds) {
        Set<Long> uniqueIds = userIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(userIds);
        Map<Long, SysUser> map = new HashMap<>();
        for (Long userId : uniqueIds) {
            if (userId == null) {
                continue;
            }
            SysUser user = loadUser(userId);
            if (user != null) {
                map.put(userId, user);
            }
        }
        return map;
    }

    private SysUser loadUser(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            return sysUserService.getUserById(userId);
        } catch (BusinessException ex) {
            return null;
        }
    }

    private <T> List<T> convertList(Object cached, Class<T> itemClass) {
        List<?> rawList = objectMapper.convertValue(cached, List.class);
        List<T> result = new ArrayList<>();
        for (Object item : rawList) {
            result.add(objectMapper.convertValue(item, itemClass));
        }
        return result;
    }

    private Object getCache(String key) {
        if (key == null) {
            return null;
        }
        return redisTemplate.opsForValue().get(key);
    }

    private void putCache(String key, Object value, Duration ttl) {
        if (key == null || value == null || ttl == null) {
            return;
        }
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    private long currentFeedVersion() {
        Object cached = redisTemplate.opsForValue().get(FEED_VERSION_KEY);
        if (cached instanceof Number) {
            return ((Number) cached).longValue();
        }
        if (cached != null) {
            Long value = objectMapper.convertValue(cached, Long.class);
            if (value != null && value > 0) {
                return value;
            }
        }
        redisTemplate.opsForValue().set(FEED_VERSION_KEY, 1L);
        return 1L;
    }

    private void incrementFeedVersion() {
        if (Boolean.FALSE.equals(redisTemplate.hasKey(FEED_VERSION_KEY))) {
            redisTemplate.opsForValue().set(FEED_VERSION_KEY, 1L);
        }
        redisTemplate.opsForValue().increment(FEED_VERSION_KEY);
    }

    private String feedKey(long version, Long beforeId, int limit) {
        return FEED_LIST_KEY_PREFIX + "v" + version + ":" + (beforeId == null ? "latest" : beforeId) + ":" + limit;
    }

    private String commentsKey(Long postId) {
        return POST_COMMENTS_KEY_PREFIX + postId;
    }

    private void evictCommentsCache(Long postId) {
        if (postId != null) {
            redisTemplate.delete(commentsKey(postId));
        }
    }

    private String friendListKey(Long userId) {
        return FRIEND_LIST_KEY_PREFIX + userId;
    }

    private String friendRequestListKey(Long userId) {
        return FRIEND_REQUEST_LIST_KEY_PREFIX + userId;
    }

    private void evictFriendListCache(Long userId) {
        if (userId != null) {
            redisTemplate.delete(friendListKey(userId));
        }
    }

    private void evictFriendRequestCache(Long userId) {
        if (userId != null) {
            redisTemplate.delete(friendRequestListKey(userId));
        }
    }

    private List<WsFriendResponse> applyOnlineStatus(List<WsFriendResponse> friends) {
        if (friends == null || friends.isEmpty()) {
            return friends == null ? new ArrayList<>() : friends;
        }
        for (WsFriendResponse friend : friends) {
            if (friend != null && friend.getUserId() != null) {
                friend.setOnline(sessionRegistry.isOnline(friend.getUserId()));
            }
        }
        return friends;
    }

    private SysUser requireActiveUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(400, "请求参数不合法");
        }
        SysUser user = loadUser(userId);
        if (user == null) {
            throw new BusinessException(404, "资源不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "无权限访问");
        }
        return user;
    }

    private String resolveNickname(SysUser user) {
        if (user == null) {
            return "-";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String value = content.trim();
        if (value.length() > 2000) {
            throw new BusinessException(400, "请求参数不合法");
        }
        return value;
    }

    private void ensureContentOrImages(String content, List<String> imagePaths) {
        if (!StringUtils.hasText(content) && (imagePaths == null || imagePaths.isEmpty())) {
            throw new BusinessException(400, "请求参数不合法");
        }
    }

    private List<String> sanitizeImagePaths(List<String> imagePaths) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return new ArrayList<>();
        }
        return imagePaths.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(path -> !path.contains(".."))
                .distinct()
                .limit(9)
                .collect(Collectors.toList());
    }

    private List<String> splitImagePaths(String imagePaths) {
        if (!StringUtils.hasText(imagePaths)) {
            return new ArrayList<>();
        }
        return Arrays.stream(imagePaths.split(","))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toList());
    }
}
