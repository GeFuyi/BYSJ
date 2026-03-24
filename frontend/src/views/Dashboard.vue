<template>
  <el-card shadow="never">
    <template #header>
      <span style="font-weight: 600">首页概览</span>
    </template>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="用户名">{{ userInfo.username || "-" }}</el-descriptions-item>
      <el-descriptions-item label="昵称">{{ userInfo.nickname || "-" }}</el-descriptions-item>
      <el-descriptions-item label="角色">{{ userInfo.role || "-" }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup>
const local = sessionStorage.getItem("userInfo");
const userInfo = local ? JSON.parse(local) : {};
</script>
