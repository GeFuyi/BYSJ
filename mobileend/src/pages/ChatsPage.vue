<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>聊天</ion-title>
        <ion-buttons slot="end">
          <ion-button fill="clear" @click="openQuickActions">
            <ion-icon :icon="addOutline" />
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen>
      <div class="chat-shell ion-padding">
        <ion-searchbar v-model="keyword" placeholder="搜索好友或群聊" :debounce="120" class="chat-search" />

        <div v-if="friendRequests.length" class="request-box page-card">
          <div class="request-title">好友申请</div>
          <div v-for="req in friendRequests" :key="req.requestId" class="request-item">
            <user-avatar :src="req.requesterAvatarPath" :name="req.requesterNickname || req.requesterUsername" :size="38" />
            <div class="request-main">
              <strong>{{ req.requesterNickname || req.requesterUsername }}</strong>
              <small>{{ req.message || "请求添加你为好友" }}</small>
            </div>
            <div class="request-actions">
              <ion-button size="small" fill="clear" color="medium" @click="onHandleFriendRequest(req, 'REJECT')">拒绝</ion-button>
              <ion-button size="small" @click="onHandleFriendRequest(req, 'ACCEPT')">同意</ion-button>
            </div>
          </div>
        </div>

        <div class="chat-list page-card">
          <div v-if="!filteredConversations.length" class="empty-row">暂无会话，点击右上角开始聊天</div>
          <button
            v-for="item in filteredConversations"
            :key="item.key"
            class="chat-item"
            type="button"
            @click="openConversation(item)"
          >
            <div class="avatar-wrap">
              <user-avatar :src="item.avatarPath" :name="item.title" :size="46" />
              <span v-if="item.online" class="online-dot" />
            </div>
            <div class="center">
              <div class="title-row">
                <h3>{{ item.title }}</h3>
                <span class="tag" :class="item.type === 'GROUP' ? 'group' : 'private'">
                  {{ item.type === "GROUP" ? "群聊" : "好友" }}
                </span>
                <span v-if="item.muted" class="muted">免打扰</span>
              </div>
              <p>{{ item.preview || "点击开始聊天" }}</p>
            </div>
            <ion-badge v-if="item.unread > 0" color="danger" class="badge">{{ formatBadge(item.unread) }}</ion-badge>
            <ion-button fill="clear" size="small" class="more-btn" @click.stop="openConversationActions(item)">
              <ion-icon :icon="ellipsisHorizontal" />
            </ion-button>
          </button>
        </div>
      </div>
    </ion-content>

    <ion-action-sheet
      :is-open="actionsOpen"
      header="快捷操作"
      :buttons="actionButtons"
      @didDismiss="actionsOpen = false"
    />

    <ion-action-sheet
      :is-open="conversationActionsOpen"
      :header="selectedConversation?.title || '会话操作'"
      :buttons="conversationActionButtons"
      @didDismiss="conversationActionsOpen = false"
    />

    <ion-modal :is-open="friendModalOpen" @didDismiss="closeFriendModal">
      <ion-header>
        <ion-toolbar>
          <ion-title>添加好友</ion-title>
          <ion-buttons slot="end">
            <ion-button @click="closeFriendModal">关闭</ion-button>
          </ion-buttons>
        </ion-toolbar>
      </ion-header>
      <ion-content class="ion-padding">
        <ion-item>
          <ion-select v-model="addFriendUserId" label="选择用户" label-placement="stacked" interface="popover" placeholder="请选择">
            <ion-select-option v-for="user in addableUsers" :key="user.userId" :value="user.userId">
              {{ user.nickname || user.username }} ({{ user.role }})
            </ion-select-option>
          </ion-select>
        </ion-item>
        <ion-item>
          <ion-input v-model="addFriendMessage" label="申请备注" label-placement="stacked" maxlength="50" placeholder="可选" />
        </ion-item>
        <ion-button expand="block" @click="submitAddFriend">确认添加</ion-button>
      </ion-content>
    </ion-modal>

    <ion-modal :is-open="groupModalOpen" @didDismiss="closeGroupModal">
      <ion-header>
        <ion-toolbar>
          <ion-title>发起群聊</ion-title>
          <ion-buttons slot="end">
            <ion-button @click="closeGroupModal">关闭</ion-button>
          </ion-buttons>
        </ion-toolbar>
      </ion-header>
      <ion-content class="ion-padding">
        <ion-item>
          <ion-input v-model="groupDraft.name" label="群名称" label-placement="stacked" maxlength="80" placeholder="请输入群名称" />
        </ion-item>
        <ion-item>
          <ion-select
            v-model="groupDraft.memberIds"
            multiple
            label="群成员"
            label-placement="stacked"
            interface="alert"
            placeholder="请选择好友"
          >
            <ion-select-option v-for="friend in friends" :key="friend.userId" :value="friend.userId">
              {{ friend.nickname || friend.username }}
            </ion-select-option>
          </ion-select>
        </ion-item>
        <ion-button expand="block" @click="submitCreateGroup">确认创建</ion-button>
      </ion-content>
    </ion-modal>

    <ion-modal :is-open="announcementModalOpen" @didDismiss="closeAnnouncementModal">
      <ion-header>
        <ion-toolbar>
          <ion-title>群公告</ion-title>
          <ion-buttons slot="end">
            <ion-button @click="closeAnnouncementModal">关闭</ion-button>
          </ion-buttons>
        </ion-toolbar>
      </ion-header>
      <ion-content class="ion-padding">
        <div class="announce-box page-card">
          <p class="announce-text">{{ selectedGroup?.announcement || "暂无群公告" }}</p>
          <small v-if="selectedGroup?.announcementUpdatedAt">更新时间：{{ selectedGroup.announcementUpdatedAt }}</small>
        </div>

        <ion-item>
          <ion-textarea
            v-model="announcementDraft"
            label="更新公告"
            label-placement="stacked"
            :rows="3"
            maxlength="500"
            placeholder="群主可编辑，成员也可尝试提交"
          />
        </ion-item>

        <ion-button expand="block" @click="submitAnnouncement">提交公告</ion-button>
        <ion-button expand="block" fill="outline" :disabled="selectedGroup?.announcementAcked" @click="confirmAnnouncementAck">
          {{ selectedGroup?.announcementAcked ? "已确认" : "确认收到" }}
        </ion-button>
      </ion-content>
    </ion-modal>
  </ion-page>
</template>

<script setup>
import {
  IonActionSheet,
  IonBadge,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonInput,
  IonItem,
  IonModal,
  IonPage,
  IonSearchbar,
  IonSelect,
  IonSelectOption,
  IonTextarea,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter
} from "@ionic/vue";
import { addOutline, ellipsisHorizontal } from "ionicons/icons";
import { computed, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import UserAvatar from "../components/UserAvatar.vue";
import { useRealtime } from "../composables/useRealtime";
import { presentToast } from "../utils/toast";

const router = useRouter();
const {
  connect,
  friends,
  friendRequests,
  groups,
  userDirectory,
  unreadMap,
  previewMap,
  conversationKey,
  refreshFriends,
  refreshFriendRequests,
  refreshGroups,
  refreshOnlineUsers,
  refreshUserDirectory,
  addFriend,
  handleFriendRequest,
  removeFriend,
  createGroup,
  quitGroup,
  setGroupMuted,
  setGroupAnnouncement,
  ackGroupAnnouncement,
  markConversationRead,
  getGroupById
} = useRealtime();

const keyword = ref("");
const actionsOpen = ref(false);
const conversationActionsOpen = ref(false);
const friendModalOpen = ref(false);
const groupModalOpen = ref(false);
const announcementModalOpen = ref(false);
const selectedConversation = ref(null);
const selectedGroup = ref(null);
const addFriendUserId = ref(null);
const addFriendMessage = ref("");
const announcementDraft = ref("");
const groupDraft = reactive({
  name: "",
  memberIds: []
});

const addableUsers = computed(() => {
  const friendIds = new Set(friends.value.map((item) => Number(item.userId)));
  return userDirectory.value.filter((item) => !friendIds.has(Number(item.userId)));
});

const conversationList = computed(() => {
  const items = [];
  friends.value.forEach((friend) => {
    const key = conversationKey("PRIVATE", friend.userId);
    items.push({
      key,
      type: "PRIVATE",
      targetId: Number(friend.userId),
      title: friend.nickname || friend.username || "好友",
      avatarPath: friend.avatarPath,
      preview: previewMap[key] || "点击开始聊天",
      unread: Number(unreadMap[key] || 0),
      online: !!friend.online,
      muted: false
    });
  });

  groups.value.forEach((group) => {
    const key = conversationKey("GROUP", group.groupId);
    items.push({
      key,
      type: "GROUP",
      targetId: Number(group.groupId),
      title: group.name || "群聊",
      avatarPath: group.avatarPath || "",
      preview: previewMap[key] || "点击开始聊天",
      unread: Number(unreadMap[key] || 0),
      online: false,
      muted: !!group.muted,
      announcement: group.announcement,
      announcementAcked: !!group.announcementAcked
    });
  });

  return items.sort((a, b) => b.unread - a.unread);
});

const filteredConversations = computed(() => {
  const text = String(keyword.value || "").trim().toLowerCase();
  if (!text) return conversationList.value;
  return conversationList.value.filter((item) => item.title.toLowerCase().includes(text));
});

const actionButtons = computed(() => [
  {
    text: "添加好友",
    handler: () => {
      friendModalOpen.value = true;
    }
  },
  {
    text: "发起群聊",
    handler: () => {
      groupModalOpen.value = true;
    }
  },
  {
    text: "取消",
    role: "cancel"
  }
]);

const conversationActionButtons = computed(() => {
  const item = selectedConversation.value;
  if (!item) {
    return [{ text: "取消", role: "cancel" }];
  }
  if (item.type === "PRIVATE") {
    return [
      {
        text: "删除好友",
        role: "destructive",
        handler: () => onRemoveFriend(item)
      },
      {
        text: "取消",
        role: "cancel"
      }
    ];
  }

  return [
    {
      text: item.muted ? "关闭免打扰" : "开启免打扰",
      handler: () => onToggleGroupMute(item)
    },
    {
      text: "群公告",
      handler: () => onOpenAnnouncement(item)
    },
    {
      text: "退出群聊",
      role: "destructive",
      handler: () => onQuitGroup(item)
    },
    {
      text: "取消",
      role: "cancel"
    }
  ];
});

onIonViewWillEnter(async () => {
  try {
    connect();
    await Promise.all([
      refreshFriends(),
      refreshFriendRequests(),
      refreshGroups(),
      refreshOnlineUsers(),
      refreshUserDirectory()
    ]);
  } catch (e) {
    await presentToast(e.message || "加载会话失败");
  }
});

function openQuickActions() {
  actionsOpen.value = true;
}

function openConversationActions(item) {
  selectedConversation.value = item;
  conversationActionsOpen.value = true;
}

function closeFriendModal() {
  friendModalOpen.value = false;
  addFriendUserId.value = null;
  addFriendMessage.value = "";
}

function closeGroupModal() {
  groupModalOpen.value = false;
  groupDraft.name = "";
  groupDraft.memberIds = [];
}

function closeAnnouncementModal() {
  announcementModalOpen.value = false;
  selectedGroup.value = null;
  announcementDraft.value = "";
}

function formatBadge(count) {
  const value = Number(count || 0);
  if (value > 99) return "99+";
  return String(value);
}

function openConversation(item) {
  markConversationRead(item.type, item.targetId);
  const typePath = item.type === "GROUP" ? "group" : "private";
  router.push({
    path: `/tabs/chat/${typePath}/${item.targetId}`,
    query: {
      title: item.title
    }
  });
}

async function submitAddFriend() {
  if (!addFriendUserId.value) {
    await presentToast("请选择用户", "warning");
    return;
  }
  const response = await addFriend(Number(addFriendUserId.value), addFriendMessage.value || null);
  closeFriendModal();
  if (response?.mode === "PENDING") {
    await presentToast("已发送好友申请，等待对方确认", "success");
  } else {
    await presentToast("好友添加成功", "success");
  }
}

async function submitCreateGroup() {
  if (!String(groupDraft.name || "").trim()) {
    await presentToast("请输入群名称", "warning");
    return;
  }
  const group = await createGroup(groupDraft.name.trim(), groupDraft.memberIds);
  closeGroupModal();
  await presentToast("群聊创建成功", "success");
  if (group?.groupId) {
    openConversation({
      type: "GROUP",
      targetId: group.groupId,
      title: group.name || groupDraft.name || "群聊"
    });
  }
}

async function onHandleFriendRequest(req, action) {
  await handleFriendRequest(req.requestId, action);
  await presentToast(action === "ACCEPT" ? "已同意好友申请" : "已拒绝好友申请", "success");
}

async function onRemoveFriend(item) {
  await removeFriend(item.targetId);
  await presentToast("好友已删除", "success");
}

async function onQuitGroup(item) {
  await quitGroup(item.targetId);
  await presentToast("已退出群聊", "success");
}

async function onToggleGroupMute(item) {
  await setGroupMuted(item.targetId, !item.muted);
  await presentToast(item.muted ? "已关闭免打扰" : "已开启免打扰", "success");
}

function onOpenAnnouncement(item) {
  const group = getGroupById(item.targetId);
  selectedGroup.value = group;
  announcementDraft.value = group?.announcement || "";
  announcementModalOpen.value = true;
}

async function submitAnnouncement() {
  if (!selectedGroup.value?.groupId) return;
  await setGroupAnnouncement(selectedGroup.value.groupId, announcementDraft.value || "");
  selectedGroup.value = getGroupById(selectedGroup.value.groupId);
  await presentToast("公告已提交", "success");
}

async function confirmAnnouncementAck() {
  if (!selectedGroup.value?.groupId) return;
  await ackGroupAnnouncement(selectedGroup.value.groupId);
  selectedGroup.value = getGroupById(selectedGroup.value.groupId);
  await presentToast("已确认收到", "success");
}
</script>

<style scoped>
.chat-shell {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chat-search {
  --background: #ffffff;
  --border-radius: 14px;
  --box-shadow: 0 8px 16px rgba(17, 24, 39, 0.06);
  padding: 0;
}

.request-box {
  padding: 10px;
}

.request-title {
  font-size: 13px;
  color: #55657a;
  margin-bottom: 8px;
}

.request-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #eff3f8;
}

.request-item:last-child {
  border-bottom: 0;
}

.request-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.request-main strong {
  font-size: 14px;
  color: #1f2733;
}

.request-main small {
  color: #8896a8;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.request-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.chat-list {
  overflow: hidden;
}

.empty-row {
  padding: 24px 16px;
  text-align: center;
  color: #8392a5;
}

.chat-item {
  width: 100%;
  border: 0;
  background: #fff;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 12px;
  text-align: left;
  border-bottom: 1px solid #eff3f8;
}

.chat-item:last-child {
  border-bottom: 0;
}

.avatar-wrap {
  position: relative;
}

.online-dot {
  position: absolute;
  right: 0;
  bottom: 2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #20c66b;
  border: 2px solid #fff;
}

.center {
  flex: 1;
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.title-row h3 {
  margin: 0;
  font-size: 15px;
  color: #1f2733;
  font-weight: 600;
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 999px;
}

.tag.private {
  background: #edf5ff;
  color: #367dff;
}

.tag.group {
  background: #fff5e8;
  color: #d17a08;
}

.muted {
  font-size: 10px;
  color: #8f6d00;
  background: #fff8d6;
  border-radius: 999px;
  padding: 1px 6px;
}

.center p {
  margin: 0;
  color: #8896a8;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.badge {
  min-width: 20px;
  height: 20px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
}

.more-btn {
  margin: 0;
}

.announce-box {
  padding: 10px;
  margin-bottom: 10px;
}

.announce-text {
  margin: 0 0 4px;
  white-space: pre-wrap;
  color: #223043;
  font-size: 14px;
}
</style>
