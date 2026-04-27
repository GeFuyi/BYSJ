<template>
  <div class="dashboard-page">
    <div class="hero">
      <div>
        <h2>欢迎回来，{{ userInfo.nickname || userInfo.username || "社区用户" }}</h2>
        <p>在这里可以快速进入邻里圈、聊天、便民服务与社区管理功能。</p>
      </div>
      <el-tag size="large">{{ userInfo.role || "-" }}</el-tag>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :xl="8">
        <el-card shadow="never" class="info-card">
          <template #header>
            <span class="card-title">个人信息</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户名">{{ userInfo.username || "-" }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ userInfo.nickname || "-" }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ userInfo.phone || "-" }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ userInfo.role || "-" }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="16">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">快捷入口</span>
          </template>
          <div class="shortcut-grid">
            <button
              v-for="item in shortcutItems"
              :key="item.path"
              type="button"
              class="shortcut-card"
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
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();
const userInfo = ref(readUserInfo());

const canManageUsers = computed(() => userInfo.value.role === "ADMIN" || userInfo.value.role === "EMPLOYEE");
const canProvideService = computed(() => userInfo.value.role === "ADMIN" || userInfo.value.role === "EMPLOYEE");
const isAdmin = computed(() => userInfo.value.role === "ADMIN");
const canPayElectricity = computed(() => userInfo.value.role === "USER" || userInfo.value.role === "ADMIN");

const shortcutItems = computed(() => {
  const items = [
    { title: "便民服务", desc: "查看社区服务与预约信息", path: "/home/services", visible: true },
    { title: "物业报修", desc: "提交报修并跟进工单进度", path: "/home/repair", visible: true },
    { title: "邻里圈", desc: "发布动态、评论与社区互动", path: "/home/moments", visible: true },
    { title: "聊天", desc: "查看会话、私聊好友与群聊", path: "/home/chats", visible: true },
    { title: "电费代缴", desc: "住户与管理员可在线缴费", path: "/home/electricity", visible: canPayElectricity.value },
    { title: "服务入驻", desc: "员工和管理员发布服务项目", path: "/home/service-provider", visible: canProvideService.value },
    { title: "入驻审核", desc: "管理员审核服务入驻申请", path: "/home/service-audit", visible: isAdmin.value },
    { title: "人员管理", desc: "管理员和员工维护用户资料", path: "/home/users", visible: canManageUsers.value },
    { title: "我的", desc: "查看资料、上传头像与退出登录", path: "/home/profile", visible: true }
  ];
  return items.filter((item) => item.visible);
});

function readUserInfo() {
  const local = sessionStorage.getItem("userInfo");
  if (!local) return {};
  try {
    return JSON.parse(local);
  } catch (e) {
    return {};
  }
}

function refreshUserInfo() {
  userInfo.value = readUserInfo();
}

function handleStorage(event) {
  if (event.key === "userInfo") {
    refreshUserInfo();
  }
}

onMounted(() => {
  window.addEventListener("user-info-updated", refreshUserInfo);
  window.addEventListener("storage", handleStorage);
});

onBeforeUnmount(() => {
  window.removeEventListener("user-info-updated", refreshUserInfo);
  window.removeEventListener("storage", handleStorage);
});
</script>

<style scoped>
.dashboard-page {
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

.hero h2 {
  margin: 0;
  font-size: 24px;
}

.hero p {
  margin: 8px 0 0;
  opacity: 0.94;
}

.card-title {
  font-weight: 600;
}

.info-card {
  height: 100%;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.shortcut-card {
  border: 1px solid #e5ebf5;
  border-radius: 14px;
  background: #f8fbff;
  padding: 16px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.shortcut-card:hover {
  border-color: #92c6ea;
  background: #eef7ff;
  transform: translateY(-1px);
}

.shortcut-card strong {
  display: block;
  color: #1d2b3a;
  font-size: 16px;
}

.shortcut-card span {
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
}
</style>
