<template>
  <img
    v-if="displaySrc"
    :src="displaySrc"
    :alt="alt"
    v-bind="$attrs"
    @error="fallbackToDirectUrl"
    @click="openPreview"
  />
  <teleport to="body">
    <div v-if="previewOpen" class="image-preview-overlay" @click="closePreview">
      <img class="image-preview-main" :src="displaySrc" :alt="alt" @click.stop="closePreview" />
    </div>
  </teleport>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from "vue";
import { fetchImageObjectUrl, shouldLoadImageAsBlob, toAbsoluteApiPath } from "../api/request";

defineOptions({
  inheritAttrs: false
});

const props = defineProps({
  src: {
    type: String,
    default: ""
  },
  alt: {
    type: String,
    default: "image"
  },
  preview: {
    type: Boolean,
    default: true
  }
});

const displaySrc = ref("");
const previewOpen = ref(false);
let objectUrl = "";
let currentRequestId = 0;
let previousOverflow = "";

function cleanupObjectUrl() {
  if (objectUrl) {
    URL.revokeObjectURL(objectUrl);
    objectUrl = "";
  }
}

function fallbackToDirectUrl() {
  if (!props.src) {
    displaySrc.value = "";
    return;
  }
  displaySrc.value = toAbsoluteApiPath(props.src);
}

function lockBodyScroll() {
  if (typeof document === "undefined") return;
  previousOverflow = document.body.style.overflow || "";
  document.body.style.overflow = "hidden";
}

function unlockBodyScroll() {
  if (typeof document === "undefined") return;
  document.body.style.overflow = previousOverflow;
  previousOverflow = "";
}

function openPreview() {
  if (!props.preview || !displaySrc.value) return;
  previewOpen.value = true;
  lockBodyScroll();
}

function closePreview() {
  previewOpen.value = false;
  unlockBodyScroll();
}

async function resolveImage() {
  currentRequestId += 1;
  const requestId = currentRequestId;
  cleanupObjectUrl();
  if (!props.src) {
    displaySrc.value = "";
    return;
  }
  const absoluteUrl = toAbsoluteApiPath(props.src);
  if (!absoluteUrl) {
    displaySrc.value = "";
    return;
  }
  if (!shouldLoadImageAsBlob(absoluteUrl)) {
    displaySrc.value = absoluteUrl;
    return;
  }
  try {
    const blobUrl = await fetchImageObjectUrl(absoluteUrl);
    if (requestId !== currentRequestId) {
      if (blobUrl) URL.revokeObjectURL(blobUrl);
      return;
    }
    objectUrl = blobUrl;
    displaySrc.value = blobUrl || absoluteUrl;
  } catch (e) {
    if (requestId !== currentRequestId) return;
    displaySrc.value = absoluteUrl;
  }
}

watch(
  () => props.src,
  () => {
    resolveImage();
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  currentRequestId += 1;
  previewOpen.value = false;
  unlockBodyScroll();
  cleanupObjectUrl();
});
</script>

<style scoped>
.image-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
}

.image-preview-main {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 8px;
}
</style>
