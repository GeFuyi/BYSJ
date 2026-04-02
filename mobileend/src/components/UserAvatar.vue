<template>
  <div class="user-avatar" :style="wrapperStyle">
    <app-image v-if="resolvedSrc" class="avatar-image" :src="resolvedSrc" :alt="name || 'avatar'" :preview="false" />
    <span v-else class="avatar-fallback">{{ initials }}</span>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { socialAssetUrl } from "../api/social";
import AppImage from "./AppImage.vue";

const props = defineProps({
  src: {
    type: String,
    default: ""
  },
  name: {
    type: String,
    default: ""
  },
  size: {
    type: Number,
    default: 40
  }
});

const resolvedSrc = computed(() => socialAssetUrl(props.src || ""));

const initials = computed(() => {
  const value = String(props.name || "").trim();
  if (!value) return "?";
  return value.slice(0, 1).toUpperCase();
});

const wrapperStyle = computed(() => {
  const px = `${Math.max(24, Number(props.size) || 40)}px`;
  return {
    width: px,
    height: px,
    minWidth: px,
    minHeight: px
  };
});
</script>

<style scoped>
.user-avatar {
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #7fb7ff, #4b8bff);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
  user-select: none;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-fallback {
  font-size: 14px;
}
</style>
