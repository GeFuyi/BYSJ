<template>
  <div class="page-shell">
    <div class="auth-card">
      <h2 class="auth-title">社区便民服务系统登录</h2>

      <el-tabs v-model="loginType" style="margin-bottom: 12px">
        <el-tab-pane label="账号密码登录" name="password" />
        <el-tab-pane label="手机号验证码登录" name="sms" />
      </el-tabs>

      <el-form v-if="loginType === 'password'" ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="pwdForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="pwdForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" :loading="loading" style="width: 100%" @click="handlePwdLogin">登录</el-button>
      </el-form>

      <el-form v-else ref="smsFormRef" :model="smsForm" :rules="smsRules" label-position="top">
        <el-form-item label="国家编码" prop="countryCode">
          <el-input v-model="smsForm.countryCode" placeholder="默认 86" />
        </el-form-item>
        <el-form-item label="手机号" prop="phoneNumber">
          <el-input v-model="smsForm.phoneNumber" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="验证码" prop="verifyCode">
          <el-input v-model="smsForm.verifyCode" placeholder="请输入验证码">
            <template #append>
              <el-button :disabled="countdown > 0" @click="handleSendSmsCode">
                {{ countdown > 0 ? `${countdown}s` : "发送验证码" }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-button type="primary" :loading="loading" style="width: 100%" @click="handleSmsLogin">登录</el-button>
      </el-form>

      <div style="margin-top: 16px; text-align: right">
        <el-link type="primary" @click="$router.push('/register')">没有账号？去注册</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { login, sendSmsCode, smsLogin } from "../api/auth";
import { useRouter } from "vue-router";

const router = useRouter();
const loading = ref(false);
const loginType = ref("password");

const pwdFormRef = ref(null);
const pwdForm = reactive({
  username: "",
  password: ""
});
const pwdRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }]
};

const smsFormRef = ref(null);
const smsForm = reactive({
  schemeName: "测试方案",
  countryCode: "86",
  phoneNumber: "",
  outId: "",
  verifyCode: ""
});
const smsRules = {
  countryCode: [{ required: true, message: "请输入国家编码", trigger: "blur" }],
  phoneNumber: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    { pattern: /^\d{11}$/, message: "手机号格式不正确", trigger: "blur" }
  ],
  verifyCode: [{ required: true, message: "请输入验证码", trigger: "blur" }]
};

const countdown = ref(0);
let timer = null;

function saveLogin(res) {
  sessionStorage.setItem("token", res.token);
  sessionStorage.setItem("userInfo", JSON.stringify(res.user));
  ElMessage.success("登录成功");
  router.push("/home/dashboard");
}

async function handlePwdLogin() {
  await pwdFormRef.value.validate();
  loading.value = true;
  try {
    const res = await login(pwdForm);
    saveLogin(res);
  } finally {
    loading.value = false;
  }
}

async function handleSendSmsCode() {
  await smsFormRef.value.validateField(["countryCode", "phoneNumber"]);
  const data = await sendSmsCode({
    schemeName: smsForm.schemeName,
    countryCode: smsForm.countryCode,
    phoneNumber: smsForm.phoneNumber,
    outId: smsForm.outId
  });
  smsForm.outId = data.outId || smsForm.outId;

  if (data.verifyCode) {
    ElMessage.success(`验证码已发送：${data.verifyCode}`);
  } else {
    ElMessage.success("验证码发送成功");
  }

  countdown.value = 60;
  timer = setInterval(() => {
    countdown.value -= 1;
    if (countdown.value <= 0) {
      clearInterval(timer);
      timer = null;
    }
  }, 1000);
}

async function handleSmsLogin() {
  await smsFormRef.value.validate();
  loading.value = true;
  try {
    const res = await smsLogin({
      schemeName: smsForm.schemeName,
      countryCode: smsForm.countryCode,
      phoneNumber: smsForm.phoneNumber,
      outId: smsForm.outId,
      verifyCode: smsForm.verifyCode,
      caseAuthPolicy: "IGNORE_CASE"
    });
    saveLogin(res);
  } finally {
    loading.value = false;
  }
}

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
});
</script>
