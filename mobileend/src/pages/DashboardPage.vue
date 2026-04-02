<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>首页</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen>
      <div class="dashboard ion-padding">
        <section class="hero-card page-card">
          <div class="hero-top">
            <user-avatar :src="user.avatarPath" :name="user.nickname || user.username" :size="52" />
            <div>
              <h2>{{ user.nickname || user.username || "用户" }}</h2>
              <p>{{ roleLabel }}</p>
            </div>
          </div>
          <div class="hero-sub">社区服务、报修工单、邻里互动与即时聊天都在这里</div>
        </section>

        <section class="module-wrap">
          <h3>常用功能</h3>
          <div class="module-grid">
            <button class="module-card page-card" type="button" @click="go('/tabs/services')">
              <strong>便民服务</strong>
              <span>服务浏览与预约</span>
            </button>
            <button class="module-card page-card" type="button" @click="go('/tabs/repair')">
              <strong>物业报修</strong>
              <span>进度追踪与确认</span>
            </button>
            <button class="module-card page-card" type="button" @click="go('/tabs/moments')">
              <strong>邻里圈</strong>
              <span>动态发布和互动</span>
            </button>
            <button class="module-card page-card" type="button" @click="go('/tabs/chats')">
              <strong>聊天</strong>
              <span>私聊与群聊消息</span>
            </button>
            <button v-if="canPayElectricity" class="module-card page-card" type="button" @click="go('/tabs/electricity')">
              <strong>电费代缴</strong>
              <span>订单创建和支付状态</span>
            </button>
          </div>
        </section>

        <section class="module-wrap" v-if="canProvideService || isAdmin || canManageUsers">
          <h3>管理功能</h3>
          <div class="module-grid admin">
            <button v-if="canProvideService" class="module-card page-card" type="button" @click="go('/tabs/service-provider')">
              <strong>服务入驻</strong>
              <span>发布与上下架</span>
            </button>
            <button v-if="isAdmin" class="module-card page-card" type="button" @click="go('/tabs/service-audit')">
              <strong>入驻审核</strong>
              <span>通过、驳回、复审</span>
            </button>
            <button v-if="canManageUsers" class="module-card page-card" type="button" @click="go('/tabs/users')">
              <strong>人员管理</strong>
              <span>账号与权限维护</span>
            </button>
          </div>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup>
import { computed } from "vue";
import { IonContent, IonHeader, IonPage, IonTitle, IonToolbar } from "@ionic/vue";
import { useRouter } from "vue-router";
import UserAvatar from "../components/UserAvatar.vue";
import { getUserInfo } from "../composables/useAuth";

const user = getUserInfo();
const router = useRouter();

const canManageUsers = computed(() => user.role === "ADMIN" || user.role === "EMPLOYEE");
const canProvideService = computed(() => user.role === "ADMIN" || user.role === "EMPLOYEE");
const isAdmin = computed(() => user.role === "ADMIN");
const canPayElectricity = computed(() => user.role === "USER" || user.role === "ADMIN");

const roleLabel = computed(() => {
  if (user.role === "ADMIN") return "管理员";
  if (user.role === "EMPLOYEE") return "员工";
  return "住户";
});

function go(path) {
  router.push(path);
}
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero-card {
  padding: 14px;
  background: linear-gradient(140deg, #0f6cb5 0%, #2d9fe6 100%);
  color: #fff;
}

.hero-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-top h2 {
  margin: 0;
  font-size: 20px;
}

.hero-top p {
  margin: 2px 0 0;
  font-size: 13px;
  opacity: 0.94;
}

.hero-sub {
  margin-top: 10px;
  font-size: 13px;
  opacity: 0.92;
}

.module-wrap h3 {
  margin: 0 0 8px;
  color: #243245;
  font-size: 16px;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.module-grid.admin {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.module-card {
  border: 0;
  text-align: left;
  padding: 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: #fff;
}

.module-card strong {
  color: #1d2a39;
  font-size: 15px;
}

.module-card span {
  color: #6f7f95;
  font-size: 12px;
  line-height: 1.4;
}
</style>
