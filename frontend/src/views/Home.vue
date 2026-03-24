<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background: #10306b; color: #fff">
      <div style="height: 60px; line-height: 60px; text-align: center; font-size: 18px; font-weight: 600">
        社区便民管理端
      </div>
      <el-menu
        router
        :default-active="$route.path"
        background-color="#10306b"
        text-color="#c8d8ff"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/home/dashboard">首页</el-menu-item>
        <el-menu-item v-if="canManageUsers" index="/home/users">人员管理</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header
        style="display: flex; justify-content: space-between; align-items: center; background: #fff; border-bottom: 1px solid #edf1f6"
      >
        <div style="font-weight: 600">首页</div>
        <div style="display: flex; align-items: center; gap: 12px">
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
import { computed, reactive } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();
const local = sessionStorage.getItem("userInfo");
const userInfo = reactive(local ? JSON.parse(local) : {});

const canManageUsers = computed(() => userInfo.role === "ADMIN" || userInfo.role === "EMPLOYEE");

function logout() {
  sessionStorage.removeItem("token");
  sessionStorage.removeItem("userInfo");
  router.push("/login");
}
</script>
