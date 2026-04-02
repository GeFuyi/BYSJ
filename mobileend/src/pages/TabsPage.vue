<template>
  <ion-page>
    <ion-tabs>
      <ion-router-outlet />
      <ion-tab-bar slot="bottom" class="bottom-tabs">
        <ion-tab-button tab="dashboard" href="/tabs/dashboard">
          <ion-icon :icon="homeOutline" />
          <ion-label>首页</ion-label>
        </ion-tab-button>
        <ion-tab-button tab="services" href="/tabs/services">
          <ion-icon :icon="gridOutline" />
          <ion-label>便民</ion-label>
        </ion-tab-button>
        <ion-tab-button tab="repair" href="/tabs/repair">
          <ion-icon :icon="buildOutline" />
          <ion-label>报修</ion-label>
        </ion-tab-button>
        <ion-tab-button tab="moments" href="/tabs/moments" class="tab-with-badge">
          <ion-icon :icon="imagesOutline" />
          <ion-label>邻里圈</ion-label>
          <ion-badge v-if="momentsBadge > 0" color="danger" class="tab-badge">{{ formatBadge(momentsBadge) }}</ion-badge>
        </ion-tab-button>
        <ion-tab-button tab="chats" href="/tabs/chats" class="tab-with-badge">
          <ion-icon :icon="chatbubblesOutline" />
          <ion-label>聊天</ion-label>
          <ion-badge v-if="chatBadge > 0" color="danger" class="tab-badge">{{ formatBadge(chatBadge) }}</ion-badge>
        </ion-tab-button>
        <ion-tab-button tab="profile" href="/tabs/profile">
          <ion-icon :icon="personOutline" />
          <ion-label>我的</ion-label>
        </ion-tab-button>
      </ion-tab-bar>
    </ion-tabs>
  </ion-page>
</template>

<script setup>
import {
  IonBadge,
  IonIcon,
  IonLabel,
  IonPage,
  IonRouterOutlet,
  IonTabBar,
  IonTabButton,
  IonTabs
} from "@ionic/vue";
import { computed } from "vue";
import { buildOutline, chatbubblesOutline, gridOutline, homeOutline, imagesOutline, personOutline } from "ionicons/icons";
import { useRealtime } from "../composables/useRealtime";

const { chatUnreadTotal, momentsUnread } = useRealtime();

const chatBadge = computed(() => Number(chatUnreadTotal.value || 0));
const momentsBadge = computed(() => Number(momentsUnread.value || 0));

function formatBadge(count) {
  const num = Number(count || 0);
  if (num > 99) return "99+";
  return String(num);
}
</script>

<style scoped>
.bottom-tabs {
  border-top-left-radius: 16px;
  border-top-right-radius: 16px;
  box-shadow: 0 -6px 20px rgba(16, 35, 60, 0.1);
  overflow: hidden;
}

.tab-with-badge {
  position: relative;
}

.tab-badge {
  position: absolute;
  top: 4px;
  right: 12px;
  min-width: 18px;
  height: 18px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  line-height: 1;
  padding: 0 5px;
  pointer-events: none;
}
</style>
