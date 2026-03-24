<template>
  <div class="page-shell">
    <div class="auth-card">
      <h2 class="auth-title">注册账号</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="4-20位" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-32位" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号，默认 15138114047" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="可选" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="员工" value="EMPLOYEE" />
            <el-option label="用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-button type="primary" :loading="loading" style="width: 100%" @click="handleRegister">注册</el-button>
      </el-form>
      <div style="margin-top: 16px; text-align: right">
        <el-link type="primary" @click="$router.push('/login')">已有账号？去登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { register } from "../api/auth";
import { useRouter } from "vue-router";

const router = useRouter();
const loading = ref(false);
const formRef = ref(null);
const form = reactive({
  username: "",
  password: "",
  phone: "",
  nickname: "",
  role: "USER"
});

const rules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 4, max: 20, message: "用户名长度需在4-20位", trigger: "blur" }
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 32, message: "密码长度需在6-32位", trigger: "blur" }
  ],
  phone: [{ pattern: /^\d{11}$/, message: "手机号格式不正确", trigger: "blur" }],
  role: [{ required: true, message: "请选择角色", trigger: "change" }]
};

async function handleRegister() {
  await formRef.value.validate();
  loading.value = true;
  try {
    await register(form);
    ElMessage.success("注册成功，请登录");
    router.push("/login");
  } finally {
    loading.value = false;
  }
}
</script>
