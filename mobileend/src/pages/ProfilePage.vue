<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>我的</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="ion-padding">
      <div class="profile-card page-card">
        <div class="hero">
          <user-avatar :src="user.avatarPath" :name="user.nickname || user.username" :size="72" />
          <div>
            <h2>{{ user.nickname || user.username || "-" }}</h2>
            <p>{{ roleLabel }}</p>
          </div>
        </div>

        <div class="info-grid">
          <div><span>用户名</span><strong>{{ user.username || "-" }}</strong></div>
          <div><span>手机号</span><strong>{{ user.phone || "-" }}</strong></div>
        </div>

        <input ref="avatarInputRef" class="hidden-input" type="file" accept="image/*" @change="onPickAvatar" />
        <div class="actions">
          <ion-button fill="outline" size="small" @click="openAvatarPicker">上传头像</ion-button>
          <ion-button fill="clear" size="small" @click="refreshMe">刷新资料</ion-button>
        </div>
      </div>

      <ion-list inset>
        <ion-item button @click="go('/tabs/moments')">
          <ion-label>邻里圈</ion-label>
        </ion-item>
        <ion-item button @click="go('/tabs/chats')">
          <ion-label>聊天会话</ion-label>
        </ion-item>
        <ion-item v-if="canPayElectricity()" button @click="go('/tabs/electricity')">
          <ion-label>电费代缴</ion-label>
        </ion-item>
        <ion-item v-if="canProvideService()" button @click="go('/tabs/service-provider')">
          <ion-label>服务入驻</ion-label>
        </ion-item>
        <ion-item v-if="isAdmin()" button @click="go('/tabs/service-audit')">
          <ion-label>入驻审核</ion-label>
        </ion-item>
        <ion-item v-if="canManageUsers()" button @click="go('/tabs/users')">
          <ion-label>人员管理</ion-label>
        </ion-item>
      </ion-list>

      <ion-button expand="block" color="danger" @click="logout">退出登录</ion-button>
    </ion-content>
  </ion-page>
</template>

<script setup>
import {
  IonButton,
  IonContent,
  IonHeader,
  IonItem,
  IonLabel,
  IonList,
  IonPage,
  IonTitle,
  IonToolbar
} from "@ionic/vue";
import { computed, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { me, uploadMyAvatar } from "../api/user";
import UserAvatar from "../components/UserAvatar.vue";
import { clearAuth, getUserInfo, setUserInfo } from "../composables/useAuth";
import { presentToast } from "../utils/toast";

const router = useRouter();
const user = reactive(getUserInfo());
const avatarInputRef = ref(null);

const roleLabel = computed(() => {
  if (user.role === "ADMIN") return "管理员";
  if (user.role === "EMPLOYEE") return "员工";
  return "住户";
});

function patchUser(next) {
  Object.keys(user).forEach((key) => {
    delete user[key];
  });
  Object.assign(user, next || {});
}

async function refreshMe() {
  const data = await me();
  patchUser(data);
  setUserInfo(data);
  await presentToast("已刷新", "success");
}

function openAvatarPicker() {
  avatarInputRef.value?.click();
}

async function onPickAvatar(event) {
  const file = event?.target?.files?.[0];
  if (!file) return;
  if (!file.type.startsWith("image/")) {
    await presentToast("仅支持图片文件", "warning");
    return;
  }
  if (file.size > 10 * 1024 * 1024) {
    await presentToast("头像不能超过10MB", "warning");
    return;
  }
  const data = await uploadMyAvatar(file);
  patchUser(data);
  setUserInfo(data);
  if (avatarInputRef.value) avatarInputRef.value.value = "";
  await presentToast("头像已更新", "success");
}

function canManageUsers() {
  return user.role === "ADMIN" || user.role === "EMPLOYEE";
}

function canProvideService() {
  return user.role === "ADMIN" || user.role === "EMPLOYEE";
}

function isAdmin() {
  return user.role === "ADMIN";
}

function canPayElectricity() {
  return user.role === "USER" || user.role === "ADMIN";
}

function go(path) {
  router.push(path);
}

function logout() {
  clearAuth();
  router.replace("/login");
}
</script>

<style scoped>
.profile-card {
  padding: 16px;
  margin-bottom: 12px;
}

.hero {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero h2 {
  margin: 0;
}

.hero p {
  margin: 4px 0 0;
  color: #6c7c92;
}

.info-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.info-grid > div {
  background: #f5f8fd;
  border-radius: 10px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-grid span {
  color: #8c9bb0;
  font-size: 12px;
}

.info-grid strong {
  color: #203043;
  font-size: 13px;
}

.actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}

.hidden-input {
  display: none;
}
</style>
