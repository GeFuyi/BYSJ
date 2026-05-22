<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button default-href="/login" />
        </ion-buttons>
        <ion-title>注册</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content fullscreen class="ion-padding">
      <div class="register-wrap page-card">
        <ion-item>
          <ion-input v-model="form.username" label="用户名" label-placement="stacked" maxlength="20" placeholder="字母数字下划线" />
        </ion-item>
        <ion-item>
          <ion-input v-model="form.password" label="密码" label-placement="stacked" type="password" maxlength="32" />
        </ion-item>
        <ion-item>
          <ion-input v-model="form.nickname" label="昵称" label-placement="stacked" maxlength="30" />
        </ion-item>
        <ion-item>
          <ion-input v-model="form.phone" label="手机号" label-placement="stacked" maxlength="11" />
        </ion-item>
        <ion-button expand="block" :disabled="loading" @click="submit">注册</ion-button>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup>
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput,
  IonItem,
  IonPage,
  IonTitle,
  IonToolbar
} from "@ionic/vue";
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { register } from "../api/auth";
import { presentToast } from "../utils/toast";

const router = useRouter();
const loading = ref(false);

const form = reactive({
  username: "",
  password: "",
  phone: "",
  nickname: ""
});

async function submit() {
  if (!form.username.trim() || !form.password.trim()) {
    await presentToast("用户名和密码不能为空", "warning");
    return;
  }
  loading.value = true;
  try {
    await register(form);
    await presentToast("注册成功，请登录", "success");
    router.replace("/login");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.register-wrap {
  max-width: 560px;
  margin: 0 auto;
  padding: 12px;
}
</style>

