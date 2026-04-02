<template>
  <ion-app>
    <ion-router-outlet />
  </ion-app>
</template>

<script setup>
import { IonApp, IonRouterOutlet } from "@ionic/vue";
import { watch } from "vue";
import { useAuthState } from "./composables/useAuth";
import { useRealtime } from "./composables/useRealtime";

const { tokenRef } = useAuthState();
const { connect, disconnect } = useRealtime();

watch(
  tokenRef,
  (token) => {
    if (token) {
      connect();
    } else {
      disconnect({ clearState: true });
    }
  },
  { immediate: true }
);
</script>
