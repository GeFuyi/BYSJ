<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button default-href="/tabs/chats" />
        </ion-buttons>
        <ion-title>{{ titleText }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen>
      <div class="room-wrap ion-padding">
        <div class="message-box" ref="messageBoxRef">
          <div v-for="(msg, idx) in messages" :key="`${msg.id || 'm'}-${idx}`" class="msg-row" :class="{ mine: isMine(msg) }">
            <user-avatar
              class="avatar"
              :src="msg.senderAvatarPath"
              :name="msg.senderNickname || '用户'"
              :size="36"
            />
            <div class="content-col">
              <div class="sender" v-if="!isMine(msg)">{{ msg.senderNickname || "用户" }}</div>
              <div class="bubble" :class="{ imageOnly: !msg.content && msg.imagePaths && msg.imagePaths.length }">
                <div v-if="msg.content" class="txt">{{ msg.content }}</div>
                <div class="img-grid" v-if="msg.imagePaths && msg.imagePaths.length">
                  <app-image
                    v-for="path in msg.imagePaths"
                    :key="`${msg.id || idx}-${path}`"
                    :src="socialImageUrl(path)"
                    alt="chat-image"
                  />
                </div>
              </div>
              <div class="time">{{ formatTime(msg.createdAt) }}</div>
            </div>
          </div>
        </div>

        <div class="composer page-card">
          <div class="draft-images" v-if="draft.imagePaths.length">
            <div v-for="path in draft.imagePaths" :key="path" class="draft-item">
              <app-image :src="socialImageUrl(path)" alt="draft-image" />
              <button class="remove-btn" type="button" @click="removeImage(path)">×</button>
            </div>
          </div>
          <ion-textarea
            v-model="draft.content"
            class="composer-input"
            placeholder="请输入消息..."
            :rows="2"
            auto-grow
            maxlength="2000"
          />
          <div class="tool-row">
            <input ref="fileInputRef" class="hidden-input" type="file" accept="image/*" multiple @change="onPickImages" />
            <ion-button size="small" fill="clear" class="tool-btn" @click="openImagePicker">图片</ion-button>
            <ion-button size="small" class="send-btn" @click="sendChat">发送</ion-button>
          </div>
        </div>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup>
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonPage,
  IonTextarea,
  IonTitle,
  IonToolbar,
  onIonViewDidLeave,
  onIonViewWillEnter
} from "@ionic/vue";
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { socialImageUrl, uploadSocialImage } from "../api/social";
import AppImage from "../components/AppImage.vue";
import UserAvatar from "../components/UserAvatar.vue";
import { getUserInfo } from "../composables/useAuth";
import { useRealtime } from "../composables/useRealtime";
import { presentToast } from "../utils/toast";

const route = useRoute();
const currentUser = getUserInfo();
const MAX_IMAGE_BYTES = 10 * 1024 * 1024;

const chatType = computed(() => String(route.params.chatType || "").toUpperCase());
const targetId = computed(() => Number(route.params.targetId));
const titleText = computed(() => String(route.query.title || "聊天"));

const draft = reactive({
  content: "",
  imagePaths: []
});

const messageBoxRef = ref(null);
const fileInputRef = ref(null);
const pageVisible = ref(false);
const currentActiveKey = ref("");

const {
  connect,
  conversationMessages,
  conversationKey,
  loadConversationHistory,
  sendChatMessage,
  setActiveConversation,
  markConversationRead,
  clearActiveConversationByKey
} = useRealtime();

const chatKey = computed(() => conversationKey(chatType.value, targetId.value));
const messages = computed(() => conversationMessages[chatKey.value] || []);

onIonViewWillEnter(async () => {
  pageVisible.value = true;
  await enterRoom();
});

onIonViewDidLeave(() => {
  pageVisible.value = false;
  leaveRoom();
});

onBeforeUnmount(() => {
  leaveRoom();
});

watch(chatKey, async (next, prev) => {
  if (!pageVisible.value || !next || next === prev) return;
  await enterRoom();
});

watch(
  () => messages.value.length,
  async () => {
    await nextTick();
    scrollToBottom();
  }
);

function scrollToBottom() {
  if (messageBoxRef.value) {
    messageBoxRef.value.scrollTop = messageBoxRef.value.scrollHeight;
  }
}

function isMine(message) {
  return Number(message?.senderId) === Number(currentUser?.id);
}

function formatTime(value) {
  if (!value) return "";
  if (typeof value === "string") return value;
  try {
    return new Date(value).toLocaleString();
  } catch (e) {
    return String(value);
  }
}

async function enterRoom() {
  const key = chatKey.value;
  if (!key || !targetId.value) return;
  connect();
  if (currentActiveKey.value && currentActiveKey.value !== key) {
    clearActiveConversationByKey(currentActiveKey.value);
  }
  currentActiveKey.value = key;
  setActiveConversation(chatType.value, targetId.value);
  await loadConversationHistory(chatType.value, targetId.value, 120);
  markConversationRead(chatType.value, targetId.value);
  await nextTick();
  scrollToBottom();
}

function leaveRoom() {
  if (!currentActiveKey.value) return;
  clearActiveConversationByKey(currentActiveKey.value);
  currentActiveKey.value = "";
}

function openImagePicker() {
  fileInputRef.value?.click();
}

async function validateImageFile(file) {
  if (!file?.type?.startsWith("image/")) {
    await presentToast("仅支持图片文件", "warning");
    return false;
  }
  if (file.size > MAX_IMAGE_BYTES) {
    await presentToast("图片大小不能超过10MB", "warning");
    return false;
  }
  return true;
}

async function onPickImages(event) {
  const files = Array.from(event.target.files || []);
  for (const file of files) {
    if (!(await validateImageFile(file))) continue;
    const data = await uploadSocialImage(file);
    if (data?.path) {
      draft.imagePaths.push(data.path);
    }
  }
  if (fileInputRef.value) fileInputRef.value.value = "";
}

function removeImage(path) {
  draft.imagePaths = draft.imagePaths.filter((item) => item !== path);
}

async function sendChat() {
  if (!draft.content.trim() && draft.imagePaths.length === 0) {
    await presentToast("请输入消息内容", "warning");
    return;
  }
  await sendChatMessage(chatType.value, targetId.value, {
    content: draft.content,
    imagePaths: draft.imagePaths
  });
  draft.content = "";
  draft.imagePaths = [];
  await nextTick();
  scrollToBottom();
}
</script>

<style scoped>
.room-wrap {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message-box {
  flex: 1;
  min-height: 52vh;
  max-height: 63vh;
  overflow-y: auto;
  border-radius: 16px;
  padding: 12px 10px;
  background: linear-gradient(180deg, #f4f8ff 0%, #edf3ff 100%);
  border: 1px solid #e2eaf8;
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 14px;
}

.msg-row.mine {
  flex-direction: row-reverse;
}

.content-col {
  max-width: calc(100% - 56px);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.msg-row.mine .content-col {
  align-items: flex-end;
}

.sender {
  font-size: 11px;
  color: #8090a7;
  padding-left: 2px;
}

.bubble {
  border-radius: 12px;
  padding: 8px 10px;
  background: #ffffff;
  box-shadow: 0 4px 12px rgba(33, 54, 87, 0.08);
}

.msg-row.mine .bubble {
  background: #c8f4a2;
}

.bubble.imageOnly {
  padding: 6px;
}

.txt {
  white-space: pre-wrap;
  word-break: break-word;
  color: #1f2a36;
  font-size: 14px;
}

.img-grid {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.img-grid img {
  width: 96px;
  height: 96px;
  object-fit: cover;
  border-radius: 10px;
}

.time {
  font-size: 11px;
  color: #95a4b7;
}

.composer {
  padding: 10px;
}

.composer-input {
  --background: #f4f7fc;
  --padding-start: 12px;
  --padding-end: 12px;
  border-radius: 12px;
}

.hidden-input {
  display: none;
}

.tool-row {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tool-btn {
  --background: #ecf3ff;
  --color: #2d64de;
  --border-radius: 10px;
}

.send-btn {
  --border-radius: 10px;
  min-width: 72px;
}

.draft-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.draft-item {
  width: 66px;
  height: 66px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.draft-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  right: 2px;
  top: 2px;
  width: 18px;
  height: 18px;
  border: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 14px;
  line-height: 1;
}
</style>
