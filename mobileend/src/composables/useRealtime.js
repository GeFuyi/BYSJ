import { App as CapacitorApp } from "@capacitor/app";
import { Capacitor } from "@capacitor/core";
import { LocalNotifications } from "@capacitor/local-notifications";
import { computed, reactive, ref } from "vue";
import { presentToast } from "../utils/toast";
import { getToken, getUserInfo } from "./useAuth";

const WS_OPEN = 1;
const WS_CONNECTING = 0;
const REQUEST_TIMEOUT = 15000;
const READY_TIMEOUT = 12000;
const RECONNECT_DELAY = 3000;
const PING_INTERVAL = 25000;
const CHAT_PRIVATE = "PRIVATE";
const CHAT_GROUP = "GROUP";
const NOTIFY_CHANNEL_ID = "community-message";

const friends = ref([]);
const friendRequests = ref([]);
const groups = ref([]);
const onlineUsers = ref([]);
const userDirectory = ref([]);
const feed = ref([]);
const unreadMap = reactive({});
const previewMap = reactive({});
const conversationMessages = reactive({});
const activeConversationKey = ref("");
const momentsActive = ref(false);
const momentsUnread = ref(0);
const connected = ref(false);
const appIsActive = ref(true);

let ws = null;
let manualClose = false;
let reconnectTimer = null;
let pingTimer = null;
let requestSeq = 1;
let notifySeq = 1;
let appStateListener = null;
let channelReady = false;
let notifyPermissionGranted = false;
const pending = new Map();

const chatUnreadTotal = computed(() => {
  let total = 0;
  Object.keys(unreadMap).forEach((key) => {
    if (key.startsWith(`${CHAT_PRIVATE}-`) || key.startsWith(`${CHAT_GROUP}-`)) {
      total += Number(unreadMap[key] || 0);
    }
  });
  return total;
});

function isNativePlatform() {
  const platform = Capacitor.getPlatform();
  return platform === "android" || platform === "ios";
}

function normalizeArray(value) {
  return Array.isArray(value) ? value : [];
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function clearObject(target) {
  Object.keys(target).forEach((key) => {
    delete target[key];
  });
}

function toNumber(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

function resolveWsUrl() {
  const token = getToken();
  const encoded = encodeURIComponent(token);
  const configured = import.meta.env.VITE_WS_BASE_URL;
  if (configured) {
    const clean = configured.replace(/\/$/, "");
    const url = clean.includes("/ws/community") ? clean : `${clean}/ws/community`;
    return `${url}${url.includes("?") ? "&" : "?"}token=${encoded}`;
  }
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  return `${protocol}://${window.location.host}/ws/community?token=${encoded}`;
}

function buildMessagePreview(message) {
  if (!message) return "新消息";
  if (message.content && String(message.content).trim()) return String(message.content).trim();
  if (Array.isArray(message.imagePaths) && message.imagePaths.length) return "[图片]";
  return "新消息";
}

function conversationKey(chatType, targetId) {
  const type = String(chatType || "").toUpperCase();
  const id = toNumber(targetId);
  if (!id) return "";
  if (type !== CHAT_PRIVATE && type !== CHAT_GROUP) return "";
  return `${type}-${id}`;
}

function ensureConversationList(key) {
  if (!key) return [];
  if (!Array.isArray(conversationMessages[key])) {
    conversationMessages[key] = [];
  }
  return conversationMessages[key];
}

function appendConversationMessage(key, message) {
  if (!key || !message) return;
  const list = ensureConversationList(key);
  const id = message.id;
  if (id && list.some((item) => item?.id === id)) return;
  list.push(message);
}

function setConversationMessages(key, messages) {
  if (!key) return;
  const seen = new Set();
  const deduped = [];
  normalizeArray(messages).forEach((item) => {
    if (!item) return;
    const hash = item.id ? `id:${item.id}` : `f:${item.senderId}-${item.receiverId}-${item.createdAt}-${item.content}`;
    if (seen.has(hash)) return;
    seen.add(hash);
    deduped.push(item);
  });
  conversationMessages[key] = deduped;
}

function setPreviewForMessage(key, message) {
  if (!key) return;
  previewMap[key] = buildMessagePreview(message);
}

function markConversationRead(chatType, targetId) {
  const key = conversationKey(chatType, targetId);
  if (!key) return;
  unreadMap[key] = 0;
}

function setActiveConversation(chatType, targetId) {
  const key = conversationKey(chatType, targetId);
  activeConversationKey.value = key;
  if (key) {
    unreadMap[key] = 0;
  }
}

function clearActiveConversation(chatType, targetId) {
  const key = conversationKey(chatType, targetId);
  if (!key || activeConversationKey.value === key) {
    activeConversationKey.value = "";
  }
}

function clearActiveConversationByKey(key) {
  if (!key || activeConversationKey.value === key) {
    activeConversationKey.value = "";
  }
}

function incrementUnread(key) {
  if (!key) return;
  unreadMap[key] = Number(unreadMap[key] || 0) + 1;
}

function getCurrentUserId() {
  return toNumber(getUserInfo()?.id);
}

function resolvePrivateConversationKey(message) {
  const senderId = toNumber(message?.senderId);
  const receiverId = toNumber(message?.receiverId);
  const selfId = getCurrentUserId();
  const peerId = senderId === selfId ? receiverId : senderId;
  return conversationKey(CHAT_PRIVATE, peerId || senderId || receiverId);
}

function resolveGroupConversationKey(message) {
  return conversationKey(CHAT_GROUP, toNumber(message?.receiverId));
}

function getGroupById(groupId) {
  const id = toNumber(groupId);
  if (!id) return null;
  return normalizeArray(groups.value).find((item) => toNumber(item?.groupId) === id) || null;
}

function isGroupMuted(groupId) {
  return !!getGroupById(groupId)?.muted;
}

function applyOnlineStatus() {
  const onlineSet = new Set(normalizeArray(onlineUsers.value).map((item) => toNumber(item?.userId)));
  friends.value = normalizeArray(friends.value).map((item) => ({
    ...item,
    online: onlineSet.has(toNumber(item?.userId))
  }));
  userDirectory.value = normalizeArray(userDirectory.value).map((item) => ({
    ...item,
    online: onlineSet.has(toNumber(item?.userId))
  }));
}

function mergePost(post) {
  if (!post?.id) return;
  const list = normalizeArray(feed.value).slice();
  const idx = list.findIndex((item) => item?.id === post.id);
  if (idx >= 0) {
    list.splice(idx, 1);
  }
  list.unshift(post);
  feed.value = list;
}

function mergeComment(comment) {
  if (!comment?.postId) return;
  const post = normalizeArray(feed.value).find((item) => item?.id === comment.postId);
  if (!post) return;
  if (!Array.isArray(post.comments)) {
    post.comments = [];
  }
  if (post.comments.some((item) => item?.id === comment.id)) return;
  post.comments.push(comment);
}

async function ensureNotifyPermission() {
  if (!isNativePlatform()) return;
  if (notifyPermissionGranted && channelReady) return;
  try {
    const status = await LocalNotifications.checkPermissions();
    let display = status?.display;
    if (display !== "granted") {
      const req = await LocalNotifications.requestPermissions();
      display = req?.display;
    }
    notifyPermissionGranted = display === "granted";
    if (!notifyPermissionGranted || channelReady) return;
    await LocalNotifications.createChannel({
      id: NOTIFY_CHANNEL_ID,
      name: "社区消息通知",
      description: "聊天和邻里圈消息提醒",
      importance: 5,
      visibility: 1
    });
    channelReady = true;
  } catch (e) {
    notifyPermissionGranted = false;
  }
}

async function pushLocalNotification(title, body, extra) {
  if (!isNativePlatform()) return;
  if (appIsActive.value) return;
  await ensureNotifyPermission();
  if (!notifyPermissionGranted) return;
  notifySeq += 1;
  const id = Number(`${Date.now()}`.slice(-8)) + (notifySeq % 97);
  try {
    await LocalNotifications.schedule({
      notifications: [
        {
          id,
          title: title || "新消息",
          body: body || "",
          channelId: NOTIFY_CHANNEL_ID,
          schedule: { at: new Date(Date.now() + 50) },
          extra: extra || {}
        }
      ]
    });
  } catch (e) {
    // ignore
  }
}

function setupAppStateListener() {
  if (appStateListener || !isNativePlatform()) return;
  appStateListener = CapacitorApp.addListener("appStateChange", (state) => {
    appIsActive.value = !!state?.isActive;
  });
}

function clearPending(reason) {
  pending.forEach((entry) => {
    clearTimeout(entry.timer);
    entry.reject(new Error(reason));
  });
  pending.clear();
}

function stopPing() {
  if (pingTimer) {
    clearInterval(pingTimer);
    pingTimer = null;
  }
}

function startPing() {
  stopPing();
  pingTimer = setInterval(() => {
    if (!ws || ws.readyState !== WS_OPEN) return;
    ws.send(JSON.stringify({ type: "PING", payload: {} }));
  }, PING_INTERVAL);
}

function scheduleReconnect() {
  if (manualClose || reconnectTimer || !getToken()) return;
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    connect();
  }, RECONNECT_DELAY);
}

function handleEvent(event) {
  const type = String(event?.type || "").toUpperCase();
  if (!type) return;

  if (type === "ONLINE_LIST") {
    onlineUsers.value = normalizeArray(event.payload);
    applyOnlineStatus();
    return;
  }

  if (type === "FRIEND_LIST") {
    friends.value = normalizeArray(event.payload);
    applyOnlineStatus();
    return;
  }

  if (type === "FRIEND_REQUEST_LIST") {
    friendRequests.value = normalizeArray(event.payload);
    return;
  }

  if (type === "GROUP_LIST") {
    groups.value = normalizeArray(event.payload);
    normalizeArray(groups.value).forEach((group) => {
      if (group?.muted) {
        const key = conversationKey(CHAT_GROUP, group.groupId);
        if (key) unreadMap[key] = 0;
      }
    });
    return;
  }

  if (type === "NEW_PRIVATE_MESSAGE") {
    const message = event.payload || {};
    const key = resolvePrivateConversationKey(message);
    appendConversationMessage(key, message);
    setPreviewForMessage(key, message);

    const senderId = toNumber(message.senderId);
    const selfId = getCurrentUserId();
    if (activeConversationKey.value !== key && senderId !== selfId) {
      incrementUnread(key);
    }
    if (senderId !== selfId) {
      pushLocalNotification(
        message.senderNickname ? `${message.senderNickname} 发来消息` : "收到新的私聊消息",
        buildMessagePreview(message),
        { scope: "private", key }
      );
    }
    return;
  }

  if (type === "NEW_GROUP_MESSAGE") {
    const message = event.payload || {};
    const key = resolveGroupConversationKey(message);
    appendConversationMessage(key, message);
    setPreviewForMessage(key, message);

    const senderId = toNumber(message.senderId);
    const selfId = getCurrentUserId();
    const muted = isGroupMuted(message.receiverId);

    if (!muted && activeConversationKey.value !== key && senderId !== selfId) {
      incrementUnread(key);
    }
    if (!muted && senderId !== selfId) {
      pushLocalNotification(
        message.senderNickname ? `${message.senderNickname} 的群消息` : "收到新的群消息",
        buildMessagePreview(message),
        { scope: "group", key }
      );
    }
    return;
  }

  if (type === "NEW_POST") {
    const post = event.payload || {};
    mergePost(post);
    if (!momentsActive.value && toNumber(post.userId) !== getCurrentUserId()) {
      momentsUnread.value += 1;
      pushLocalNotification("邻里圈有新动态", post.content || "点击查看详情", { scope: "moments", type: "post" });
    }
    return;
  }

  if (type === "NEW_COMMENT") {
    const comment = event.payload || {};
    mergeComment(comment);
    if (!momentsActive.value && toNumber(comment.userId) !== getCurrentUserId()) {
      momentsUnread.value += 1;
      pushLocalNotification("邻里圈有新评论", comment.content || "点击查看详情", {
        scope: "moments",
        type: "comment",
        postId: comment.postId
      });
    }
  }
}

function handleWsMessage(message) {
  if (!message || typeof message !== "object") return;

  if (message.requestId && pending.has(message.requestId)) {
    const item = pending.get(message.requestId);
    clearTimeout(item.timer);
    pending.delete(message.requestId);
    if (message.code === 0 || message.code === undefined || message.code === null) {
      item.resolve(message.payload);
    } else {
      item.reject(new Error(message.message || "请求失败"));
    }
    return;
  }

  if (String(message.type || "").toUpperCase() === "OFFLINE_BATCH") {
    normalizeArray(message.payload).forEach((entry) => handleEvent(entry));
    return;
  }

  handleEvent(message);
}

function connect() {
  const token = getToken();
  if (!token) return;
  if (ws && (ws.readyState === WS_OPEN || ws.readyState === WS_CONNECTING)) return;

  setupAppStateListener();
  ensureNotifyPermission();
  manualClose = false;
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }

  ws = new WebSocket(resolveWsUrl());

  ws.onopen = async () => {
    connected.value = true;
    startPing();
    try {
      const init = await sendWs("INIT");
      feed.value = normalizeArray(init?.feed);
      friends.value = normalizeArray(init?.friends);
      friendRequests.value = normalizeArray(init?.friendRequests);
      groups.value = normalizeArray(init?.groups);
      normalizeArray(groups.value).forEach((group) => {
        if (group?.muted) {
          const key = conversationKey(CHAT_GROUP, group.groupId);
          if (key) unreadMap[key] = 0;
        }
      });
      onlineUsers.value = normalizeArray(init?.onlineUsers);
      userDirectory.value = normalizeArray(init?.userDirectory);
      applyOnlineStatus();
    } catch (e) {
      await presentToast(e.message || "实时连接初始化失败");
    }
  };

  ws.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data || "{}");
      handleWsMessage(message);
    } catch (e) {
      // ignore
    }
  };

  ws.onclose = () => {
    connected.value = false;
    stopPing();
    clearPending("连接已断开");
    ws = null;
    scheduleReconnect();
  };
}

async function ensureSocketReady() {
  if (ws && ws.readyState === WS_OPEN) return;
  connect();
  const begin = Date.now();
  while (!ws || ws.readyState !== WS_OPEN) {
    if (!getToken()) {
      throw new Error("未登录");
    }
    if (Date.now() - begin > READY_TIMEOUT) {
      throw new Error("WebSocket 连接超时");
    }
    await sleep(120);
  }
}

async function sendWs(type, payload = {}) {
  await ensureSocketReady();
  const requestId = `${Date.now()}-${requestSeq++}`;
  ws.send(JSON.stringify({ type, requestId, payload }));
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      pending.delete(requestId);
      reject(new Error("请求超时"));
    }, REQUEST_TIMEOUT);
    pending.set(requestId, { resolve, reject, timer });
  });
}

function resetRealtimeState() {
  friends.value = [];
  friendRequests.value = [];
  groups.value = [];
  onlineUsers.value = [];
  userDirectory.value = [];
  feed.value = [];
  momentsUnread.value = 0;
  activeConversationKey.value = "";
  momentsActive.value = false;
  clearObject(unreadMap);
  clearObject(previewMap);
  clearObject(conversationMessages);
}

function disconnect(options = {}) {
  const shouldClear = !!options.clearState;
  manualClose = true;
  stopPing();
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  clearPending("连接已关闭");
  connected.value = false;
  if (ws) {
    ws.close();
    ws = null;
  }
  if (shouldClear) {
    resetRealtimeState();
  }
}

async function refreshFriends() {
  friends.value = normalizeArray(await sendWs("FRIEND_LIST"));
  applyOnlineStatus();
  return friends.value;
}

async function refreshFriendRequests() {
  friendRequests.value = normalizeArray(await sendWs("FRIEND_REQUEST_LIST"));
  return friendRequests.value;
}

async function refreshGroups() {
  groups.value = normalizeArray(await sendWs("GROUP_LIST"));
  normalizeArray(groups.value).forEach((group) => {
    if (group?.muted) {
      const key = conversationKey(CHAT_GROUP, group.groupId);
      if (key) unreadMap[key] = 0;
    }
  });
  return groups.value;
}

async function refreshUserDirectory() {
  userDirectory.value = normalizeArray(await sendWs("USER_DIRECTORY"));
  applyOnlineStatus();
  return userDirectory.value;
}

async function refreshOnlineUsers() {
  onlineUsers.value = normalizeArray(await sendWs("ONLINE_LIST"));
  applyOnlineStatus();
  return onlineUsers.value;
}

async function refreshFeed(limit = 20) {
  feed.value = normalizeArray(await sendWs("POST_LIST", { limit }));
  return feed.value;
}

async function addFriend(friendUserId, message = null) {
  const response = await sendWs("FRIEND_ADD", { friendUserId, message });
  await Promise.all([refreshFriends(), refreshFriendRequests(), refreshUserDirectory()]);
  return response;
}

async function handleFriendRequest(requestId, action) {
  const response = await sendWs("FRIEND_REQUEST_HANDLE", { requestId, action });
  await Promise.all([refreshFriends(), refreshFriendRequests(), refreshUserDirectory()]);
  return response;
}

async function removeFriend(friendUserId) {
  await sendWs("FRIEND_REMOVE", { friendUserId });
  await Promise.all([refreshFriends(), refreshUserDirectory()]);
}

async function createGroup(name, memberIds) {
  const response = await sendWs("GROUP_CREATE", { name, memberIds });
  await refreshGroups();
  return response;
}

async function quitGroup(groupId) {
  await sendWs("GROUP_QUIT", { groupId });
  await refreshGroups();
}

async function setGroupMuted(groupId, muted) {
  const response = await sendWs("GROUP_MUTE", { groupId, muted: !!muted });
  await refreshGroups();
  return response;
}

async function setGroupAnnouncement(groupId, announcement) {
  const response = await sendWs("GROUP_ANNOUNCEMENT_SET", { groupId, announcement });
  await refreshGroups();
  return response;
}

async function ackGroupAnnouncement(groupId) {
  const response = await sendWs("GROUP_ANNOUNCEMENT_ACK", { groupId });
  await refreshGroups();
  return response;
}

async function loadConversationHistory(chatType, targetId, limit = 100) {
  const key = conversationKey(chatType, targetId);
  if (!key) return [];
  const type = String(chatType || "").toUpperCase();
  let list = [];
  if (type === CHAT_PRIVATE) {
    list = normalizeArray(await sendWs("PRIVATE_HISTORY", { peerId: toNumber(targetId), limit }));
  } else if (type === CHAT_GROUP) {
    list = normalizeArray(await sendWs("GROUP_HISTORY", { groupId: toNumber(targetId), limit }));
  }
  setConversationMessages(key, list);
  const last = list[list.length - 1];
  if (last) {
    setPreviewForMessage(key, last);
  }
  unreadMap[key] = 0;
  return conversationMessages[key];
}

async function sendChatMessage(chatType, targetId, payload) {
  const type = String(chatType || "").toUpperCase();
  const key = conversationKey(type, targetId);
  if (!key) {
    throw new Error("会话信息无效");
  }
  const content = payload?.content;
  const imagePaths = normalizeArray(payload?.imagePaths);
  let message = null;
  if (type === CHAT_PRIVATE) {
    message = await sendWs("PRIVATE_SEND", {
      toUserId: toNumber(targetId),
      content,
      imagePaths
    });
  } else {
    message = await sendWs("GROUP_SEND", {
      groupId: toNumber(targetId),
      content,
      imagePaths
    });
  }
  appendConversationMessage(key, message);
  setPreviewForMessage(key, message);
  unreadMap[key] = 0;
  return message;
}

async function createPost(payload) {
  const response = await sendWs("POST_CREATE", {
    content: payload?.content,
    imagePaths: normalizeArray(payload?.imagePaths)
  });
  mergePost(response);
  return response;
}

async function createComment(payload) {
  const response = await sendWs("COMMENT_CREATE", {
    postId: payload?.postId,
    parentId: payload?.parentId,
    content: payload?.content,
    imagePaths: normalizeArray(payload?.imagePaths)
  });
  mergeComment(response);
  return response;
}

function setMomentsActive(active) {
  momentsActive.value = !!active;
}

function markMomentsRead() {
  momentsUnread.value = 0;
}

export function useRealtime() {
  return {
    connected,
    friends,
    friendRequests,
    groups,
    onlineUsers,
    userDirectory,
    feed,
    unreadMap,
    previewMap,
    conversationMessages,
    activeConversationKey,
    chatUnreadTotal,
    momentsUnread,
    momentsActive,
    conversationKey,
    connect,
    disconnect,
    sendWs,
    refreshFriends,
    refreshFriendRequests,
    refreshGroups,
    refreshUserDirectory,
    refreshOnlineUsers,
    refreshFeed,
    addFriend,
    handleFriendRequest,
    removeFriend,
    createGroup,
    quitGroup,
    setGroupMuted,
    setGroupAnnouncement,
    ackGroupAnnouncement,
    loadConversationHistory,
    sendChatMessage,
    createPost,
    createComment,
    markConversationRead,
    setActiveConversation,
    clearActiveConversation,
    clearActiveConversationByKey,
    setMomentsActive,
    markMomentsRead,
    isGroupMuted,
    getGroupById
  };
}
