<template>
  <div class="profile-page">
    <div class="hero">
      <div class="hero-main">
        <div class="avatar-box">
          <img v-if="avatarUrl" :src="avatarUrl" alt="avatar" />
          <span v-else>{{ shortName(userInfo.nickname || userInfo.username) }}</span>
        </div>
        <div>
          <h2>{{ userInfo.nickname || userInfo.username || "社区用户" }}</h2>
          <p>{{ roleLabel }}</p>
        </div>
      </div>
      <div class="hero-actions">
        <el-upload :show-file-list="false" :before-upload="beforeAvatarUpload" :http-request="handleAvatarUpload">
          <el-button>上传头像</el-button>
        </el-upload>
        <el-button type="primary" @click="refreshProfile">刷新资料</el-button>
        <el-button type="danger" plain @click="logout">退出登录</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="10">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">个人资料</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户名">{{ userInfo.username || "-" }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ userInfo.nickname || "-" }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ userInfo.phone || "-" }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ roleLabel }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="14">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">功能入口</span>
          </template>
          <div class="action-grid">
            <button
              v-for="item in quickLinks"
              :key="item.path"
              type="button"
              class="action-card"
              @click="router.push(item.path)"
            >
              <strong>{{ item.title }}</strong>
              <span>{{ item.desc }}</span>
            </button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { socialAssetUrl } from "../api/social";
import { getMe, uploadMyAvatar } from "../api/user";

const router = useRouter();
const userInfo = ref(readUserInfo());

const roleLabel = computed(() => {
  if (userInfo.value.role === "ADMIN") return "管理员";
  if (userInfo.value.role === "EMPLOYEE") return "员工";
  if (userInfo.value.role === "USER") return "住户";
  return userInfo.value.role || "-";
});

const avatarUrl = computed(() => socialAssetUrl(userInfo.value.avatarPath));
const canManageUsers = computed(() => userInfo.value.role === "ADMIN" || userInfo.value.role === "EMPLOYEE");
const canProvideService = computed(() => userInfo.value.role === "ADMIN" || userInfo.value.role === "EMPLOYEE");
const isAdmin = computed(() => userInfo.value.role === "ADMIN");
const canPayElectricity = computed(() => userInfo.value.role === "USER" || userInfo.value.role === "ADMIN");

const quickLinks = computed(() => {
  const items = [
    { title: "邻里圈", desc: "查看社区动态与评论", path: "/home/moments", visible: true },
    { title: "聊天", desc: "进入好友私聊和群聊", path: "/home/chats", visible: true },
    { title: "便民服务", desc: "浏览服务、预约与评价", path: "/home/services", visible: true },
    { title: "物业报修", desc: "查看工单和流转记录", path: "/home/repair", visible: true },
    { title: "电费代缴", desc: "在线缴费和记录查询", path: "/home/electricity", visible: canPayElectricity.value },
    { title: "服务入驻", desc: "提交服务入驻申请", path: "/home/service-provider", visible: canProvideService.value },
    { title: "入驻审核", desc: "审核入驻服务项目", path: "/home/service-audit", visible: isAdmin.value },
    { title: "人员管理", desc: "管理社区用户资料", path: "/home/users", visible: canManageUsers.value }
  ];
  return items.filter((item) => item.visible);
});

function readUserInfo() {
  const raw = sessionStorage.getItem("userInfo");
  if (!raw) return {};
  try {
    return JSON.parse(raw);
  } catch (e) {
    return {};
  }
}

function saveUserInfo(user) {
  userInfo.value = user || {};
  sessionStorage.setItem("userInfo", JSON.stringify(userInfo.value));
  window.dispatchEvent(new Event("user-info-updated"));
}

function shortName(name) {
  if (!name) return "?";
  return String(name).trim().slice(0, 1).toUpperCase();
}

function beforeAvatarUpload(file) {
  const isImage = file.type && file.type.startsWith("image/");
  if (!isImage) {
    ElMessage.error("仅支持图片文件");
    return false;
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error("头像图片不能超过 10MB");
    return false;
  }
  return true;
}

async function handleAvatarUpload(option) {
  try {
    const data = await uploadMyAvatar(option.file);
    saveUserInfo(data);
    option.onSuccess?.(data);
    ElMessage.success("头像上传成功");
  } catch (error) {
    option.onError?.(error);
  }
}

async function refreshProfile() {
  const data = await getMe();
  saveUserInfo(data);
  ElMessage.success("资料已刷新");
}

function logout() {
  sessionStorage.removeItem("token");
  sessionStorage.removeItem("userInfo");
  window.dispatchEvent(new Event("user-info-updated"));
  router.push("/login");
}
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero {
  border-radius: 16px;
  padding: 20px 22px;
  background: linear-gradient(125deg, #0d5f93 0%, #1976b8 45%, #52a8d8 100%);
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.hero-main {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar-box {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
  border: 2px solid rgba(255, 255, 255, 0.3);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 600;
}

.avatar-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero h2 {
  margin: 0;
  font-size: 24px;
}

.hero p {
  margin: 8px 0 0;
  opacity: 0.94;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.card-title {
  font-weight: 600;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.action-card {
  border: 1px solid #e5ebf5;
  border-radius: 14px;
  background: #f8fbff;
  padding: 16px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-card:hover {
  border-color: #92c6ea;
  background: #eef7ff;
  transform: translateY(-1px);
}

.action-card strong {
  display: block;
  color: #1d2b3a;
  font-size: 16px;
}

.action-card span {
  display: block;
  margin-top: 8px;
  color: #65778f;
  line-height: 1.5;
  font-size: 13px;
}

@media (max-width: 900px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}
</style>
