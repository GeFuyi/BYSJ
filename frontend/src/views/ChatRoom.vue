<template>
  <div class="chat-room-page">
    <div class="hero">
      <div>
        <h2>{{ chatTitle }}</h2>
        <p>支持文字和图片消息，已对齐独立聊天室路由。</p>
      </div>
      <div class="hero-side">
        <el-tag :type="wsStateTagType" effect="dark">{{ wsStateText }}</el-tag>
        <el-button size="small" @click="router.push('/home/chats')">返回会话列表</el-button>
      </div>
    </div>

    <el-card shadow="never" class="chat-main">
      <template #header>
        <div class="title-row">
          <span>{{ chatTarget?.title || chatTitle }}</span>
          <el-button size="small" @click="reloadChatHistory">刷新消息</el-button>
        </div>
      </template>

      <div class="message-list" ref="messageListRef">
        <div
          v-for="msg in chatMessages"
          :key="`${msg.id}-${msg.createdAt}`"
          class="msg-row"
          :class="{ mine: Number(msg.senderId) === Number(currentUser.id) }"
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
        <el-empty v-if="!chatMessages.length" description="暂无消息，发一条开始聊天吧" :image-size="56" />
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
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useDesktopSocial } from "../composables/useDesktopSocial";

const route = useRoute();
const router = useRouter();
const messageListRef = ref(null);

const {
  chatMessages,
  wsState,
  chatTarget,
  chatDraft,
  currentUser,
  wsStateTagType,
  wsStateText,
  connect,
  openChat,
  reloadChatHistory,
  sendChatMessage,
  beforeUploadImage,
  uploadChatImage,
  removeChatImage,
  resolveChatTitle,
  imageUrl,
  formatTime
} = useDesktopSocial();

const chatType = computed(() => String(route.params.chatType || "").toUpperCase());
const targetId = computed(() => Number(route.params.targetId));
const chatTitle = computed(() => resolveChatTitle(chatType.value, targetId.value));

async function loadCurrentChat() {
  if (!chatType.value || !targetId.value) return;
  await connect();
  await openChat(chatType.value, targetId.value, chatTitle.value);
}

watch(
  () => [route.params.chatType, route.params.targetId],
  async () => {
    await loadCurrentChat();
  },
  { immediate: true }
);

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

onMounted(async () => {
  await loadCurrentChat();
});
</script>

<style scoped>
.chat-room-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero {
  border-radius: 14px;
  padding: 16px 18px;
  background: linear-gradient(125deg, #0c5a89 0%, #1f7ab3 45%, #49a8d5 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.hero h2 {
  margin: 0;
  font-size: 22px;
}

.hero p {
  margin: 8px 0 0;
  opacity: 0.95;
}

.hero-side {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.chat-main {
  height: calc(100vh - 220px);
  display: flex;
  flex-direction: column;
}

.chat-main :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.title-row {
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: space-between;
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

.publish-row {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
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

@media (max-width: 900px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-side {
    justify-content: flex-start;
  }

  .chat-main {
    height: auto;
  }

  .chat-main :deep(.el-card__body) {
    min-height: 420px;
  }
}
</style>
