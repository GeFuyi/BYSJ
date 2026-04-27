<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background: #10306b; color: #fff">
      <div class="brand">社区便民管理端</div>
      <el-menu
        router
        :default-active="activeMenu"
        background-color="#10306b"
        text-color="#c8d8ff"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/home/dashboard">首页</el-menu-item>
        <el-menu-item index="/home/services">便民服务</el-menu-item>
        <el-menu-item index="/home/repair">物业报修</el-menu-item>
        <el-menu-item v-if="canPayElectricity" index="/home/electricity">电费代缴</el-menu-item>
        <el-menu-item index="/home/moments">邻里圈</el-menu-item>
        <el-menu-item index="/home/chats">聊天</el-menu-item>
        <el-menu-item index="/home/profile">我的</el-menu-item>
        <el-menu-item v-if="canProvideService" index="/home/service-provider">服务入驻</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/home/service-audit">入驻审核</el-menu-item>
        <el-menu-item v-if="canManageUsers" index="/home/users">人员管理</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="page-header">
        <div style="font-weight: 600">{{ pageTitle }}</div>
        <div class="header-user">
          <el-tag>{{ userInfo.role || "-" }}</el-tag>
          <span>{{ userInfo.nickname || userInfo.username || "-" }}</span>
          <el-button link type="danger" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main style="background: #f5f7fb">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();
const userInfo = ref(readUserInfo());

const canManageUsers = computed(() => userInfo.value.role === "ADMIN" || userInfo.value.role === "EMPLOYEE");
const canProvideService = computed(() => userInfo.value.role === "ADMIN" || userInfo.value.role === "EMPLOYEE");
const isAdmin = computed(() => userInfo.value.role === "ADMIN");
const canPayElectricity = computed(() => userInfo.value.role === "USER" || userInfo.value.role === "ADMIN");

const pageTitle = computed(() => route.meta.title || "首页");
const activeMenu = computed(() => {
  if (route.path.startsWith("/home/chat/")) return "/home/chats";
  return route.path;
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

function logout() {
  sessionStorage.removeItem("token");
  sessionStorage.removeItem("userInfo");
  window.dispatchEvent(new Event("user-info-updated"));
  router.push("/login");
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
.brand {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 18px;
  font-weight: 600;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #edf1f6;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
