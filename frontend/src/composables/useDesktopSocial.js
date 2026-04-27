import { computed, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { socialAssetUrl, uploadSocialImage } from "../api/social";

const feed = ref([]);
const friends = ref([]);
const groups = ref([]);
const onlineUsers = ref([]);
const userDirectory = ref([]);
const chatMessages = ref([]);
const addFriendUserId = ref(null);
const unreadMap = reactive({});
const previewMap = reactive({});
const publishing = ref(false);
const wsState = ref("connecting");
const chatTarget = ref(null);

const postDraft = reactive({
  content: "",
  imagePaths: []
});

const groupDraft = reactive({
  name: "",
  memberIds: []
});

const chatDraft = reactive({
  content: "",
  imagePaths: []
});

const commentDraftMap = reactive({});
const currentUser = ref(readCurrentUser());

let ws = null;
let requestSeed = 1;
let reconnectTimer = null;
let manualClose = false;
let connectPromise = null;
const pendingMap = new Map();

const wsStateTagType = computed(() => {
  if (wsState.value === "online") return "success";
  if (wsState.value === "connecting") return "warning";
  return "danger";
});

const wsStateText = computed(() => {
  if (wsState.value === "online") return "WebSocket 在线";
  if (wsState.value === "connecting") return "WebSocket 连接中";
  return "WebSocket 离线";
});

const addableUsers = computed(() => {
  const friendIds = new Set(friends.value.map((item) => Number(item.userId)));
  return userDirectory.value.filter((user) => !friendIds.has(Number(user.userId)));
});

function readCurrentUser() {
  const raw = sessionStorage.getItem("userInfo");
  if (!raw) return {};
  try {
    return JSON.parse(raw);
  } catch (e) {
    return {};
  }
}

function refreshCurrentUser() {
  currentUser.value = readCurrentUser();
}

if (typeof window !== "undefined") {
  window.addEventListener("user-info-updated", refreshCurrentUser);
  window.addEventListener("storage", (event) => {
    if (event.key === "userInfo") {
      refreshCurrentUser();
    }
  });
}

function connect() {
  refreshCurrentUser();
  const token = sessionStorage.getItem("token");
  if (!token) {
    wsState.value = "offline";
    return Promise.reject(new Error("请先登录"));
  }
  if (ws && ws.readyState === WebSocket.OPEN) {
    return Promise.resolve();
  }
  if (connectPromise) {
    return connectPromise;
  }
  manualClose = false;
  wsState.value = "connecting";
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  const url = `${protocol}://${window.location.host}/ws/community?token=${encodeURIComponent(token)}`;
  ws = new WebSocket(url);

  connectPromise = new Promise((resolve, reject) => {
    ws.onopen = async () => {
      wsState.value = "online";
      try {
        await initData();
        if (chatTarget.value) {
          await reloadChatHistory();
        }
        resolve();
      } catch (e) {
        reject(e);
        ElMessage.warning(e?.message || "初始化失败");
      } finally {
        connectPromise = null;
      }
    };

    ws.onerror = () => {
      wsState.value = "offline";
      reject(new Error("WebSocket 连接失败"));
      connectPromise = null;
    };

    ws.onclose = () => {
      wsState.value = "offline";
      clearPending("连接已断开");
      connectPromise = null;
      if (!manualClose) {
        reconnectTimer = setTimeout(() => {
          connect().catch(() => {});
        }, 3000);
      }
    };

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data || "{}");
        handleWsMessage(msg);
      } catch (e) {
        // ignore parse error
      }
    };
  });

  return connectPromise;
}

function disconnect() {
  manualClose = true;
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  clearPending("连接已关闭");
  if (ws) {
    ws.close();
    ws = null;
  }
}

function clearPending(message) {
  for (const [, task] of pendingMap.entries()) {
    clearTimeout(task.timer);
    task.reject(new Error(message));
  }
  pendingMap.clear();
}

function sendWs(type, payload = {}) {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    return Promise.reject(new Error("WebSocket 未连接"));
  }
  const requestId = `${Date.now()}-${requestSeed++}`;
  ws.send(JSON.stringify({ type, requestId, payload }));
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      pendingMap.delete(requestId);
      reject(new Error("请求超时"));
    }, 15000);
    pendingMap.set(requestId, { resolve, reject, timer });
  });
}

async function initData() {
  const init = await sendWs("INIT");
  feed.value = init.feed || [];
  friends.value = init.friends || [];
  groups.value = init.groups || [];
  onlineUsers.value = init.onlineUsers || [];
  userDirectory.value = init.userDirectory || [];
  applyOnlineStatus();
}

function handleWsMessage(msg) {
  if (msg.requestId && pendingMap.has(msg.requestId)) {
    const task = pendingMap.get(msg.requestId);
    clearTimeout(task.timer);
    pendingMap.delete(msg.requestId);
    if (msg.code === 0 || msg.code === null || msg.code === undefined) {
      task.resolve(msg.payload);
    } else {
      task.reject(new Error(msg.message || "请求失败"));
    }
    return;
  }

  if (msg.type === "OFFLINE_BATCH" && Array.isArray(msg.payload)) {
    msg.payload.forEach((item) => handleWsMessage(item));
    return;
  }

  if (msg.type === "ONLINE_LIST") {
    onlineUsers.value = msg.payload || [];
    applyOnlineStatus();
    return;
  }
  if (msg.type === "FRIEND_LIST") {
    friends.value = msg.payload || [];
    applyOnlineStatus();
    return;
  }
  if (msg.type === "GROUP_LIST") {
    groups.value = msg.payload || [];
    return;
  }
  if (msg.type === "NEW_POST") {
    prependPost(msg.payload);
    return;
  }
  if (msg.type === "NEW_COMMENT") {
    appendComment(msg.payload);
    return;
  }
  if (msg.type === "NEW_PRIVATE_MESSAGE") {
    processIncomingChat(msg.payload, "PRIVATE");
    return;
  }
  if (msg.type === "NEW_GROUP_MESSAGE") {
    processIncomingChat(msg.payload, "GROUP");
  }
}

async function reloadFeed() {
  await connect();
  const list = await sendWs("POST_LIST", { limit: 20 });
  feed.value = list || [];
}

async function reloadLists() {
  await connect();
  const [friendList, groupList, onlineList, users] = await Promise.all([
    sendWs("FRIEND_LIST"),
    sendWs("GROUP_LIST"),
    sendWs("ONLINE_LIST"),
    sendWs("USER_DIRECTORY")
  ]);
  friends.value = friendList || [];
  groups.value = groupList || [];
  onlineUsers.value = onlineList || [];
  userDirectory.value = users || [];
  applyOnlineStatus();
}

async function publishPost() {
  await connect();
  if (!postDraft.content.trim() && postDraft.imagePaths.length === 0) {
    ElMessage.warning("请输入内容或上传图片");
    return;
  }
  publishing.value = true;
  try {
    await sendWs("POST_CREATE", {
      content: postDraft.content,
      imagePaths: postDraft.imagePaths
    });
    postDraft.content = "";
    postDraft.imagePaths = [];
    ElMessage.success("发布成功");
  } finally {
    publishing.value = false;
  }
}

function getCommentDraft(postId) {
  if (!commentDraftMap[postId]) {
    commentDraftMap[postId] = {
      content: "",
      imagePaths: [],
      replyParentId: null,
      replyNickname: null
    };
  }
  return commentDraftMap[postId];
}

function replyTo(postId, comment) {
  const draft = getCommentDraft(postId);
  draft.replyParentId = comment.id;
  draft.replyNickname = comment.nickname;
}

function cancelReply(postId) {
  const draft = getCommentDraft(postId);
  draft.replyParentId = null;
  draft.replyNickname = null;
}

async function publishComment(postId) {
  await connect();
  const draft = getCommentDraft(postId);
  if (!draft.content.trim() && draft.imagePaths.length === 0) {
    ElMessage.warning("评论内容不能为空");
    return;
  }
  await sendWs("COMMENT_CREATE", {
    postId,
    parentId: draft.replyParentId,
    content: draft.content,
    imagePaths: draft.imagePaths
  });
  draft.content = "";
  draft.imagePaths = [];
  draft.replyParentId = null;
  draft.replyNickname = null;
}

async function addFriend() {
  await connect();
  if (!addFriendUserId.value) {
    ElMessage.warning("请先选择用户");
    return;
  }
  await sendWs("FRIEND_ADD", { friendUserId: addFriendUserId.value });
  addFriendUserId.value = null;
  userDirectory.value = await sendWs("USER_DIRECTORY");
  ElMessage.success("添加好友成功");
}

async function createGroup() {
  await connect();
  if (!groupDraft.name.trim()) {
    ElMessage.warning("请输入群名称");
    return;
  }
  const group = await sendWs("GROUP_CREATE", {
    name: groupDraft.name,
    memberIds: groupDraft.memberIds
  });
  groupDraft.name = "";
  groupDraft.memberIds = [];
  ElMessage.success("群聊创建成功");
  return group;
}

function unreadKey(type, id) {
  return `${type}-${Number(id)}`;
}

function unreadCount(type, id) {
  return unreadMap[unreadKey(type, id)] || 0;
}

function updatePreview(type, id, message) {
  const preview = message?.content?.trim() || (message?.imagePaths?.length ? "[图片]" : "点击开始聊天");
  previewMap[unreadKey(type, id)] = preview;
}

function getConversationPreview(type, id) {
  return previewMap[unreadKey(type, id)] || "点击开始聊天";
}

function resolveChatTitle(type, id) {
  if (type === "PRIVATE") {
    const friend = friends.value.find((item) => Number(item.userId) === Number(id));
    if (friend) {
      return `与 ${friend.nickname || friend.username} 的私聊`;
    }
    return "私聊";
  }
  const group = groups.value.find((item) => Number(item.groupId) === Number(id));
  if (group) {
    return `群聊：${group.name}`;
  }
  return "群聊";
}

function isActiveChat(type, id) {
  return chatTarget.value && chatTarget.value.type === type && Number(chatTarget.value.id) === Number(id);
}

function syncPreviewFromMessages(type, id) {
  const last = chatMessages.value[chatMessages.value.length - 1];
  if (last) {
    updatePreview(type, id, last);
  }
}

async function openChat(type, id, title = "") {
  await connect();
  const normalizedType = String(type || "").toUpperCase();
  const normalizedId = Number(id);
  if (!normalizedType || !normalizedId) return;
  chatTarget.value = {
    type: normalizedType,
    id: normalizedId,
    title: title || resolveChatTitle(normalizedType, normalizedId)
  };
  unreadMap[unreadKey(normalizedType, normalizedId)] = 0;
  if (normalizedType === "PRIVATE") {
    chatMessages.value = await sendWs("PRIVATE_HISTORY", { peerId: normalizedId, limit: 100 });
  } else {
    chatMessages.value = await sendWs("GROUP_HISTORY", { groupId: normalizedId, limit: 100 });
  }
  syncPreviewFromMessages(normalizedType, normalizedId);
}

async function openPrivateChat(friend) {
  return openChat("PRIVATE", friend.userId, `与 ${friend.nickname || friend.username} 的私聊`);
}

async function openGroupChat(group) {
  return openChat("GROUP", group.groupId, `群聊：${group.name}`);
}

async function reloadChatHistory() {
  if (!chatTarget.value) return;
  return openChat(chatTarget.value.type, chatTarget.value.id, chatTarget.value.title);
}

async function sendChatMessage() {
  await connect();
  if (!chatTarget.value) {
    ElMessage.warning("请先选择会话");
    return;
  }
  if (!chatDraft.content.trim() && chatDraft.imagePaths.length === 0) {
    ElMessage.warning("消息内容不能为空");
    return;
  }
  let sent;
  if (chatTarget.value.type === "PRIVATE") {
    sent = await sendWs("PRIVATE_SEND", {
      toUserId: chatTarget.value.id,
      content: chatDraft.content,
      imagePaths: chatDraft.imagePaths
    });
  } else {
    sent = await sendWs("GROUP_SEND", {
      groupId: chatTarget.value.id,
      content: chatDraft.content,
      imagePaths: chatDraft.imagePaths
    });
  }
  if (sent) {
    chatMessages.value.push(sent);
    updatePreview(chatTarget.value.type, chatTarget.value.id, sent);
  }
  chatDraft.content = "";
  chatDraft.imagePaths = [];
}

function processIncomingChat(message, expectedType) {
  if (!message) return;
  const type = message.receiverType || expectedType;
  if (type === "PRIVATE") {
    const peerId = Number(message.senderId) === Number(currentUser.value.id) ? Number(message.receiverId) : Number(message.senderId);
    updatePreview("PRIVATE", peerId, message);
    if (isActiveChat("PRIVATE", peerId)) {
      chatMessages.value.push(message);
      return;
    }
    unreadMap[unreadKey("PRIVATE", peerId)] = (unreadMap[unreadKey("PRIVATE", peerId)] || 0) + 1;
    return;
  }
  if (type === "GROUP") {
    const groupId = Number(message.receiverId);
    updatePreview("GROUP", groupId, message);
    if (isActiveChat("GROUP", groupId)) {
      chatMessages.value.push(message);
      return;
    }
    unreadMap[unreadKey("GROUP", groupId)] = (unreadMap[unreadKey("GROUP", groupId)] || 0) + 1;
  }
}

function prependPost(post) {
  if (!post || !post.id) return;
  const idx = feed.value.findIndex((item) => item.id === post.id);
  if (idx >= 0) {
    feed.value.splice(idx, 1);
  }
  feed.value.unshift(post);
}

function appendComment(comment) {
  if (!comment || !comment.postId || !comment.id) return;
  const post = feed.value.find((item) => item.id === comment.postId);
  if (!post) return;
  if (!post.comments) {
    post.comments = [];
  }
  const existed = post.comments.some((item) => item.id === comment.id);
  if (!existed) {
    post.comments.push(comment);
  }
}

function applyOnlineStatus() {
  const onlineSet = new Set((onlineUsers.value || []).map((item) => Number(item.userId)));
  friends.value.forEach((friend) => {
    friend.online = onlineSet.has(Number(friend.userId));
  });
  userDirectory.value.forEach((user) => {
    user.online = onlineSet.has(Number(user.userId));
  });
}

function beforeUploadImage(file) {
  const isImage = file.type && file.type.startsWith("image/");
  if (!isImage) {
    ElMessage.error("仅支持图片文件");
    return false;
  }
  const maxMb = 10;
  if (file.size > maxMb * 1024 * 1024) {
    ElMessage.error(`图片不能超过 ${maxMb}MB`);
    return false;
  }
  return true;
}

async function uploadPostImage(option) {
  try {
    const data = await uploadSocialImage(option.file);
    postDraft.imagePaths.push(data.path);
    option.onSuccess?.(data);
  } catch (e) {
    option.onError?.(e);
  }
}

async function uploadCommentImage(postId, option) {
  try {
    const data = await uploadSocialImage(option.file);
    getCommentDraft(postId).imagePaths.push(data.path);
    option.onSuccess?.(data);
  } catch (e) {
    option.onError?.(e);
  }
}

async function uploadChatImage(option) {
  try {
    const data = await uploadSocialImage(option.file);
    chatDraft.imagePaths.push(data.path);
    option.onSuccess?.(data);
  } catch (e) {
    option.onError?.(e);
  }
}

function removePostImage(path) {
  postDraft.imagePaths = postDraft.imagePaths.filter((item) => item !== path);
}

function removeCommentImage(postId, path) {
  const draft = getCommentDraft(postId);
  draft.imagePaths = draft.imagePaths.filter((item) => item !== path);
}

function removeChatImage(path) {
  chatDraft.imagePaths = chatDraft.imagePaths.filter((item) => item !== path);
}

function imageUrl(path) {
  return socialAssetUrl(path);
}

function shortName(name) {
  if (!name) return "?";
  const clean = String(name).trim();
  return clean.slice(0, 1).toUpperCase();
}

function formatTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  const hh = String(date.getHours()).padStart(2, "0");
  const mm = String(date.getMinutes()).padStart(2, "0");
  return `${y}-${m}-${d} ${hh}:${mm}`;
}

export function useDesktopSocial() {
  return {
    feed,
    friends,
    groups,
    onlineUsers,
    userDirectory,
    chatMessages,
    addFriendUserId,
    unreadMap,
    previewMap,
    publishing,
    wsState,
    chatTarget,
    postDraft,
    groupDraft,
    chatDraft,
    commentDraftMap,
    currentUser,
    wsStateTagType,
    wsStateText,
    addableUsers,
    connect,
    disconnect,
    refreshCurrentUser,
    reloadFeed,
    reloadLists,
    publishPost,
    getCommentDraft,
    replyTo,
    cancelReply,
    publishComment,
    addFriend,
    createGroup,
    unreadCount,
    getConversationPreview,
    resolveChatTitle,
    isActiveChat,
    openChat,
    openPrivateChat,
    openGroupChat,
    reloadChatHistory,
    sendChatMessage,
    beforeUploadImage,
    uploadPostImage,
    uploadCommentImage,
    uploadChatImage,
    removePostImage,
    removeCommentImage,
    removeChatImage,
    imageUrl,
    shortName,
    formatTime
  };
}
