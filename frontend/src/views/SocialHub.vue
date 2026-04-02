<template>
  <div class="social-hub-page">
    <div class="hub-hero">
      <div>
        <h2>邻里圈与聊天室</h2>
        <p>支持动态发布、评论回复、好友私聊、群聊、在线状态和离线补拉。</p>
      </div>
      <el-tag :type="wsStateTagType" effect="dark">{{ wsStateText }}</el-tag>
    </div>

    <el-tabs v-model="activeTab" class="hub-tabs">
      <el-tab-pane label="邻里圈" name="feed">
        <el-card shadow="never">
          <template #header>
            <div class="title-row">
              <span>发布动态</span>
              <el-button size="small" @click="reloadFeed">刷新动态</el-button>
            </div>
          </template>
          <el-input
            v-model="postDraft.content"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            placeholder="分享今天的社区见闻..."
          />
          <div class="draft-image-list" v-if="postDraft.imagePaths.length">
            <div v-for="path in postDraft.imagePaths" :key="path" class="draft-image-item">
              <el-image :src="imageUrl(path)" fit="cover" />
              <el-button link type="danger" @click="removePostImage(path)">移除</el-button>
            </div>
          </div>
          <div class="publish-row">
            <el-upload :show-file-list="false" :before-upload="beforeUploadImage" :http-request="uploadPostImage">
              <el-button>上传图片</el-button>
            </el-upload>
            <el-button type="primary" :loading="publishing" @click="publishPost">发布动态</el-button>
          </div>
        </el-card>

        <div class="feed-list">
          <el-card v-for="post in feed" :key="post.id" shadow="never">
            <div class="post-head">
              <div class="avatar">{{ shortName(post.nickname || post.username) }}</div>
              <div>
                <div class="post-name">{{ post.nickname || post.username }}</div>
                <div class="post-time">{{ formatTime(post.createdAt) }}</div>
              </div>
            </div>
            <div v-if="post.content" class="post-content">{{ post.content }}</div>
            <div v-if="post.imagePaths && post.imagePaths.length" class="post-images">
              <el-image
                v-for="path in post.imagePaths"
                :key="path"
                :src="imageUrl(path)"
                :preview-src-list="post.imagePaths.map(imageUrl)"
                fit="cover"
                preview-teleported
              />
            </div>

            <div class="comment-area">
              <div class="comment-title">评论区</div>
              <div class="comment-list" v-if="post.comments && post.comments.length">
                <div v-for="comment in post.comments" :key="comment.id" class="comment-item">
                  <div class="comment-main">
                    <span class="comment-user">{{ comment.nickname }}</span>
                    <span v-if="comment.replyToNickname" class="comment-reply"> 回复 {{ comment.replyToNickname }}</span>
                    ：{{ comment.content }}
                  </div>
                  <div v-if="comment.imagePaths && comment.imagePaths.length" class="comment-images">
                    <el-image
                      v-for="path in comment.imagePaths"
                      :key="path"
                      :src="imageUrl(path)"
                      :preview-src-list="comment.imagePaths.map(imageUrl)"
                      fit="cover"
                      preview-teleported
                    />
                  </div>
                  <div class="comment-meta">
                    <span>{{ formatTime(comment.createdAt) }}</span>
                    <el-button link type="primary" @click="replyTo(post.id, comment)">回复</el-button>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无评论" :image-size="56" />

              <div class="comment-editor">
                <el-tag
                  v-if="getCommentDraft(post.id).replyNickname"
                  type="warning"
                  closable
                  @close="cancelReply(post.id)"
                >
                  回复 {{ getCommentDraft(post.id).replyNickname }}
                </el-tag>
                <el-input
                  v-model="getCommentDraft(post.id).content"
                  type="textarea"
                  :rows="2"
                  maxlength="1000"
                  placeholder="写下你的评论..."
                />
                <div class="draft-image-list" v-if="getCommentDraft(post.id).imagePaths.length">
                  <div v-for="path in getCommentDraft(post.id).imagePaths" :key="path" class="draft-image-item">
                    <el-image :src="imageUrl(path)" fit="cover" />
                    <el-button link type="danger" @click="removeCommentImage(post.id, path)">移除</el-button>
                  </div>
                </div>
                <div class="publish-row">
                  <el-upload
                    :show-file-list="false"
                    :before-upload="beforeUploadImage"
                    :http-request="(option) => uploadCommentImage(post.id, option)"
                  >
                    <el-button size="small">上传图片</el-button>
                  </el-upload>
                  <el-button size="small" type="primary" @click="publishComment(post.id)">发送评论</el-button>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="聊天室" name="chat">
        <div class="chat-layout">
          <el-card class="chat-sidebar" shadow="never">
            <template #header>
              <div class="title-row">
                <span>会话中心</span>
                <el-button size="small" @click="reloadLists">刷新</el-button>
              </div>
            </template>

            <div class="section-title">在线用户</div>
            <div class="online-list">
              <span v-for="item in onlineUsers" :key="item.userId" class="online-chip">
                {{ item.nickname || item.username }}
              </span>
            </div>

            <div class="section-title">添加好友</div>
            <div class="inline-row">
              <el-select v-model="addFriendUserId" filterable placeholder="选择用户" style="flex: 1">
                <el-option
                  v-for="user in addableUsers"
                  :key="user.userId"
                  :label="`${user.nickname || user.username} (${user.role})`"
                  :value="user.userId"
                />
              </el-select>
              <el-button type="primary" @click="addFriend">添加</el-button>
            </div>

            <div class="section-title">创建群聊</div>
            <el-input v-model="groupDraft.name" maxlength="80" placeholder="群名称" />
            <el-select
              v-model="groupDraft.memberIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="选择好友"
              style="width: 100%; margin-top: 8px"
            >
              <el-option
                v-for="friend in friends"
                :key="friend.userId"
                :label="friend.nickname || friend.username"
                :value="friend.userId"
              />
            </el-select>
            <el-button style="margin-top: 8px; width: 100%" @click="createGroup">创建群聊</el-button>

            <div class="section-title">好友私聊</div>
            <div class="session-list">
              <div
                v-for="friend in friends"
                :key="`f-${friend.userId}`"
                class="session-item"
                :class="{ active: isActiveChat('PRIVATE', friend.userId) }"
                @click="openPrivateChat(friend)"
              >
                <div>
                  <div>{{ friend.nickname || friend.username }}</div>
                  <small>{{ friend.online ? "在线" : "离线" }}</small>
                </div>
                <el-badge v-if="unreadCount('PRIVATE', friend.userId) > 0" :value="unreadCount('PRIVATE', friend.userId)" />
              </div>
            </div>

            <div class="section-title">群聊</div>
            <div class="session-list">
              <div
                v-for="group in groups"
                :key="`g-${group.groupId}`"
                class="session-item"
                :class="{ active: isActiveChat('GROUP', group.groupId) }"
                @click="openGroupChat(group)"
              >
                <div>
                  <div>{{ group.name }}</div>
                  <small>{{ group.members?.length || 0 }} 人</small>
                </div>
                <el-badge v-if="unreadCount('GROUP', group.groupId) > 0" :value="unreadCount('GROUP', group.groupId)" />
              </div>
            </div>
          </el-card>

          <el-card class="chat-main" shadow="never">
            <template #header>
              <div class="title-row">
                <span>{{ chatTarget ? chatTarget.title : "请选择会话" }}</span>
                <el-button size="small" :disabled="!chatTarget" @click="reloadChatHistory">刷新消息</el-button>
              </div>
            </template>

            <el-empty v-if="!chatTarget" description="左侧选择好友或群聊开始聊天" />
            <template v-else>
              <div class="message-list" ref="messageListRef">
                <div
                  v-for="msg in chatMessages"
                  :key="`${msg.id}-${msg.createdAt}`"
                  class="msg-row"
                  :class="{ mine: msg.senderId === currentUser.id }"
                >
                  <div class="msg-bubble">
                    <div class="msg-sender">{{ msg.senderNickname }}</div>
                    <div v-if="msg.content" class="msg-content">{{ msg.content }}</div>
                    <div v-if="msg.imagePaths && msg.imagePaths.length" class="msg-images">
                      <el-image
                        v-for="path in msg.imagePaths"
                        :key="path"
                        :src="imageUrl(path)"
                        :preview-src-list="msg.imagePaths.map(imageUrl)"
                        fit="cover"
                        preview-teleported
                      />
                    </div>
                    <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
                  </div>
                </div>
              </div>

              <div class="chat-editor">
                <el-input
                  v-model="chatDraft.content"
                  type="textarea"
                  :rows="3"
                  maxlength="2000"
                  placeholder="输入消息..."
                />
                <div class="draft-image-list" v-if="chatDraft.imagePaths.length">
                  <div v-for="path in chatDraft.imagePaths" :key="path" class="draft-image-item">
                    <el-image :src="imageUrl(path)" fit="cover" />
                    <el-button link type="danger" @click="removeChatImage(path)">移除</el-button>
                  </div>
                </div>
                <div class="publish-row">
                  <el-upload :show-file-list="false" :before-upload="beforeUploadImage" :http-request="uploadChatImage">
                    <el-button>上传图片</el-button>
                  </el-upload>
                  <el-button type="primary" @click="sendChatMessage">发送</el-button>
                </div>
              </div>
            </template>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { socialImageUrl, uploadSocialImage } from "../api/social";

const activeTab = ref("feed");
const publishing = ref(false);
const feed = ref([]);
const friends = ref([]);
const groups = ref([]);
const onlineUsers = ref([]);
const userDirectory = ref([]);
const chatMessages = ref([]);
const addFriendUserId = ref(null);
const messageListRef = ref(null);
const unreadMap = reactive({});

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

const wsState = ref("connecting");
let ws = null;
let requestSeed = 1;
let reconnectTimer = null;
let manualClose = false;
const pendingMap = new Map();

const chatTarget = ref(null);

const currentUser = (() => {
  const raw = sessionStorage.getItem("userInfo");
  if (!raw) return {};
  try {
    return JSON.parse(raw);
  } catch (e) {
    return {};
  }
})();

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
  const friendIds = new Set(friends.value.map((item) => item.userId));
  return userDirectory.value.filter((user) => !friendIds.has(user.userId));
});

onMounted(() => {
  connectWs();
});

onBeforeUnmount(() => {
  manualClose = true;
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  if (ws) {
    ws.close();
    ws = null;
  }
  clearPending("页面已关闭");
});

watch(
  () => chatMessages.value.length,
  async () => {
    await nextTick();
    const el = messageListRef.value;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }
);

function connectWs() {
  const token = sessionStorage.getItem("token");
  if (!token) {
    wsState.value = "offline";
    return;
  }
  wsState.value = "connecting";
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  const url = `${protocol}://${window.location.host}/ws/community?token=${encodeURIComponent(token)}`;
  ws = new WebSocket(url);

  ws.onopen = async () => {
    wsState.value = "online";
    try {
      await initData();
    } catch (e) {
      ElMessage.warning(e?.message || "初始化失败");
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

  ws.onclose = () => {
    wsState.value = "offline";
    clearPending("连接已断开");
    if (!manualClose) {
      reconnectTimer = setTimeout(() => {
        connectWs();
      }, 3000);
    }
  };

  ws.onerror = () => {
    wsState.value = "offline";
  };
}

function clearPending(message) {
  for (const [, task] of pendingMap.entries()) {
    clearTimeout(task.timer);
    task.reject(new Error(message));
  }
  pendingMap.clear();
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

function sendWs(type, payload = {}) {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    return Promise.reject(new Error("WebSocket 未连接"));
  }
  const requestId = `${Date.now()}-${requestSeed++}`;
  const message = { type, requestId, payload };
  ws.send(JSON.stringify(message));
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

async function reloadFeed() {
  const list = await sendWs("POST_LIST", { limit: 20 });
  feed.value = list || [];
}

async function reloadLists() {
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
  if (group) {
    openGroupChat(group);
  }
  ElMessage.success("群聊创建成功");
}

function isActiveChat(type, id) {
  return chatTarget.value && chatTarget.value.type === type && chatTarget.value.id === id;
}

function unreadKey(type, id) {
  return `${type}-${id}`;
}

function unreadCount(type, id) {
  return unreadMap[unreadKey(type, id)] || 0;
}

async function openPrivateChat(friend) {
  chatTarget.value = {
    type: "PRIVATE",
    id: friend.userId,
    title: `与 ${friend.nickname || friend.username} 的私聊`
  };
  unreadMap[unreadKey("PRIVATE", friend.userId)] = 0;
  chatMessages.value = await sendWs("PRIVATE_HISTORY", { peerId: friend.userId, limit: 100 });
}

async function openGroupChat(group) {
  chatTarget.value = {
    type: "GROUP",
    id: group.groupId,
    title: `群聊：${group.name}`
  };
  unreadMap[unreadKey("GROUP", group.groupId)] = 0;
  chatMessages.value = await sendWs("GROUP_HISTORY", { groupId: group.groupId, limit: 100 });
}

async function reloadChatHistory() {
  if (!chatTarget.value) return;
  if (chatTarget.value.type === "PRIVATE") {
    chatMessages.value = await sendWs("PRIVATE_HISTORY", { peerId: chatTarget.value.id, limit: 100 });
  } else {
    chatMessages.value = await sendWs("GROUP_HISTORY", { groupId: chatTarget.value.id, limit: 100 });
  }
}

async function sendChatMessage() {
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
  }
  chatDraft.content = "";
  chatDraft.imagePaths = [];
}

function processIncomingChat(message, expectedType) {
  if (!message) return;
  const type = message.receiverType || expectedType;
  if (type === "PRIVATE") {
    const peerId = message.senderId === currentUser.id ? message.receiverId : message.senderId;
    if (isActiveChat("PRIVATE", peerId)) {
      chatMessages.value.push(message);
      return;
    }
    unreadMap[unreadKey("PRIVATE", peerId)] = (unreadMap[unreadKey("PRIVATE", peerId)] || 0) + 1;
    return;
  }
  if (type === "GROUP") {
    if (isActiveChat("GROUP", message.receiverId)) {
      chatMessages.value.push(message);
      return;
    }
    unreadMap[unreadKey("GROUP", message.receiverId)] = (unreadMap[unreadKey("GROUP", message.receiverId)] || 0) + 1;
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
  const onlineSet = new Set((onlineUsers.value || []).map((item) => item.userId));
  friends.value.forEach((friend) => {
    friend.online = onlineSet.has(friend.userId);
  });
  userDirectory.value.forEach((user) => {
    user.online = onlineSet.has(user.userId);
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
  return socialImageUrl(path);
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
</script>

<style scoped>
.social-hub-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hub-hero {
  border-radius: 14px;
  padding: 16px 18px;
  background: linear-gradient(125deg, #0c5a89 0%, #1f7ab3 45%, #49a8d5 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.hub-hero h2 {
  margin: 0;
  font-size: 22px;
}

.hub-hero p {
  margin: 8px 0 0;
  opacity: 0.95;
}

.hub-tabs :deep(.el-tabs__content) {
  padding-top: 8px;
}

.title-row {
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.publish-row {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
}

.feed-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #0f7eb5;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.post-name {
  font-weight: 600;
}

.post-time {
  font-size: 12px;
  color: #909399;
}

.post-content {
  margin-top: 8px;
  white-space: pre-wrap;
  color: #303133;
}

.post-images {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 8px;
}

.post-images .el-image {
  width: 100%;
  height: 110px;
  border-radius: 8px;
}

.comment-area {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
}

.comment-title {
  font-weight: 600;
  color: #606266;
}

.comment-list {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.comment-item {
  border-radius: 8px;
  background: #f8fafc;
  padding: 8px 10px;
}

.comment-main {
  color: #303133;
  white-space: pre-wrap;
}

.comment-user {
  color: #0b72ad;
  font-weight: 600;
}

.comment-reply {
  color: #909399;
}

.comment-meta {
  margin-top: 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #909399;
  font-size: 12px;
}

.comment-images {
  margin-top: 6px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.comment-images .el-image {
  width: 72px;
  height: 72px;
  border-radius: 6px;
}

.comment-editor {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.draft-image-list {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.draft-image-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.draft-image-item .el-image {
  width: 70px;
  height: 70px;
  border-radius: 6px;
}

.chat-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 12px;
}

.chat-sidebar {
  height: 76vh;
  overflow: auto;
}

.section-title {
  margin-top: 12px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.online-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.online-chip {
  border-radius: 999px;
  background: #e9f7ef;
  color: #1f8f4f;
  padding: 3px 10px;
  font-size: 12px;
}

.inline-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.session-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 8px 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: all 0.2s ease;
}

.session-item:hover {
  border-color: #89c4ea;
  background: #f5fbff;
}

.session-item.active {
  border-color: #2490d1;
  background: #edf7ff;
}

.session-item small {
  color: #909399;
}

.chat-main {
  height: 76vh;
  display: flex;
  flex-direction: column;
}

.chat-main :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.msg-row {
  display: flex;
  justify-content: flex-start;
}

.msg-row.mine {
  justify-content: flex-end;
}

.msg-bubble {
  max-width: 70%;
  border-radius: 10px;
  background: #f3f6fb;
  padding: 8px 10px;
}

.msg-row.mine .msg-bubble {
  background: #dff3ff;
}

.msg-sender {
  font-size: 12px;
  color: #607080;
}

.msg-content {
  margin-top: 3px;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-time {
  margin-top: 4px;
  font-size: 11px;
  color: #909399;
  text-align: right;
}

.msg-images {
  margin-top: 6px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.msg-images .el-image {
  width: 86px;
  height: 86px;
  border-radius: 8px;
}

.chat-editor {
  margin-top: 10px;
  border-top: 1px solid #ebeef5;
  padding-top: 10px;
}

@media (max-width: 1100px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }

  .chat-sidebar,
  .chat-main {
    height: auto;
  }

  .chat-main :deep(.el-card__body) {
    min-height: 420px;
  }
}
</style>

