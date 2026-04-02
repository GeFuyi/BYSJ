<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>登录</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content fullscreen class="ion-padding">
      <div class="auth-wrap">
        <div class="auth-hero page-card">
          <h2>社区便民移动端</h2>
          <p>账号登录 / 短信登录</p>
        </div>

        <ion-segment v-model="mode">
          <ion-segment-button value="password">
            <ion-label>账号登录</ion-label>
          </ion-segment-button>
          <ion-segment-button value="sms">
            <ion-label>短信登录</ion-label>
          </ion-segment-button>
        </ion-segment>

        <div class="page-card form-card">
          <template v-if="mode === 'password'">
            <ion-item>
              <ion-input v-model="passwordForm.username" label="用户名" label-placement="stacked" placeholder="请输入用户名" />
            </ion-item>
            <ion-item>
              <ion-input
                v-model="passwordForm.password"
                type="password"
                label="密码"
                label-placement="stacked"
                placeholder="请输入密码"
              />
            </ion-item>
            <ion-button expand="block" :disabled="loading" @click="handlePasswordLogin">登录</ion-button>
          </template>

          <template v-else>
            <ion-item>
              <ion-input v-model="smsForm.countryCode" label="国家码" label-placement="stacked" placeholder="86" />
            </ion-item>
            <ion-item>
              <ion-input v-model="smsForm.phoneNumber" label="手机号" label-placement="stacked" placeholder="11位手机号" />
            </ion-item>
            <ion-item>
              <ion-input v-model="smsForm.schemeName" label="方案名" label-placement="stacked" placeholder="TestScheme" />
            </ion-item>
            <div class="sms-row">
              <ion-input v-model="smsForm.verifyCode" label="验证码" label-placement="stacked" placeholder="请输入验证码" />
              <ion-button size="small" fill="outline" :disabled="countdown > 0 || loading" @click="handleSendSms">
                {{ countdown > 0 ? `${countdown}s` : "发送验证码" }}
              </ion-button>
            </div>
            <ion-button expand="block" :disabled="loading" @click="handleSmsLogin">登录</ion-button>
          </template>
        </div>

        <ion-button fill="clear" expand="block" router-link="/register">没有账号？去注册</ion-button>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup>
import {
  IonButton,
  IonContent,
  IonHeader,
  IonInput,
  IonItem,
  IonLabel,
  IonPage,
  IonSegment,
  IonSegmentButton,
  IonTitle,
  IonToolbar
} from "@ionic/vue";
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { login, sendSmsCode, smsLogin } from "../api/auth";
import { setToken, setUserInfo } from "../composables/useAuth";
import { presentToast } from "../utils/toast";

const router = useRouter();
const mode = ref("password");
const loading = ref(false);
const countdown = ref(0);
let timer = null;

const passwordForm = reactive({
  username: "",
  password: ""
});

const smsForm = reactive({
  schemeName: "TestScheme",
  countryCode: "86",
  phoneNumber: "",
  verifyCode: ""
});

async function handlePasswordLogin() {
  if (!passwordForm.username.trim() || !passwordForm.password.trim()) {
    await presentToast("请输入用户名和密码", "warning");
    return;
  }
  loading.value = true;
  try {
    const data = await login(passwordForm);
    afterLogin(data);
  } finally {
    loading.value = false;
  }
}

async function handleSendSms() {
  if (!smsForm.phoneNumber.trim()) {
    await presentToast("请输入手机号", "warning");
    return;
  }
  loading.value = true;
  try {
    await sendSmsCode({
      schemeName: smsForm.schemeName,
      countryCode: smsForm.countryCode,
      phoneNumber: smsForm.phoneNumber
    });
    countdown.value = 60;
    timer = setInterval(() => {
      countdown.value -= 1;
      if (countdown.value <= 0 && timer) {
        clearInterval(timer);
        timer = null;
      }
    }, 1000);
    await presentToast("验证码已发送", "success");
  } finally {
    loading.value = false;
  }
}

async function handleSmsLogin() {
  if (!smsForm.phoneNumber.trim() || !smsForm.verifyCode.trim()) {
    await presentToast("请输入手机号和验证码", "warning");
    return;
  }
  loading.value = true;
  try {
    const data = await smsLogin({
      schemeName: smsForm.schemeName,
      countryCode: smsForm.countryCode,
      phoneNumber: smsForm.phoneNumber,
      verifyCode: smsForm.verifyCode
    });
    afterLogin(data);
  } finally {
    loading.value = false;
  }
}

function afterLogin(data) {
  setToken(data.token);
  setUserInfo(data.user);
  router.replace("/tabs/dashboard");
}
</script>

<style scoped>
.auth-wrap {
  max-width: 560px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.auth-hero {
  padding: 18px;
  background: linear-gradient(140deg, #0f7eb5 0%, #1a9acd 100%);
  color: #fff;
}

.auth-hero h2 {
  margin: 0;
  font-size: 22px;
}

.auth-hero p {
  margin: 8px 0 0;
  opacity: 0.94;
}

.form-card {
  padding: 12px;
}

.sms-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  align-items: end;
  padding: 8px 0;
}
</style>

