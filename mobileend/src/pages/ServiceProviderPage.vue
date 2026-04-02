<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button default-href="/tabs/dashboard" />
        </ion-buttons>
        <ion-title>服务入驻</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content fullscreen class="ion-padding">
      <ion-segment v-model="tab">
        <ion-segment-button value="form">
          <ion-label>{{ isEdit ? "编辑入驻" : "提交入驻" }}</ion-label>
        </ion-segment-button>
        <ion-segment-button value="list">
          <ion-label>我的入驻记录</ion-label>
        </ion-segment-button>
      </ion-segment>

      <template v-if="tab === 'form'">
        <div class="page-card block">
          <ion-item>
            <ion-input v-model="form.name" label="服务名称" label-placement="stacked" maxlength="100" />
          </ion-item>
          <ion-item>
            <ion-select v-model="form.categoryCode" label="服务分类" label-placement="stacked" interface="popover">
              <ion-select-option v-for="item in categories" :key="item.code" :value="item.code">
                {{ item.name }}
              </ion-select-option>
            </ion-select>
          </ion-item>
          <ion-item>
            <ion-input v-model="form.summary" label="服务简介" label-placement="stacked" maxlength="255" />
          </ion-item>
          <ion-item>
            <ion-textarea v-model="form.description" label="详情介绍" label-placement="stacked" :rows="4" maxlength="5000" />
          </ion-item>
          <ion-item>
            <ion-input v-model="form.contactName" label="联系人" label-placement="stacked" />
          </ion-item>
          <ion-item>
            <ion-input v-model="form.contactPhone" label="联系电话" label-placement="stacked" />
          </ion-item>
          <ion-item>
            <ion-input v-model="form.address" label="服务地址" label-placement="stacked" maxlength="255" />
          </ion-item>
          <ion-item>
            <ion-input v-model.number="form.maxCapacity" type="number" label="可约名额" label-placement="stacked" />
          </ion-item>

          <div class="upload-row">
            <input ref="coverInputRef" class="hidden-input" type="file" accept="image/*" @change="onPickCover" />
            <ion-button size="small" fill="outline" @click="coverInputRef?.click()">上传封面图</ion-button>
            <app-image v-if="form.coverImagePath" :src="serviceImageUrl(form.coverImagePath)" class="cover-preview" alt="cover" />
          </div>

          <div class="upload-row">
            <input ref="galleryInputRef" class="hidden-input" type="file" accept="image/*" multiple @change="onPickGallery" />
            <ion-button size="small" fill="outline" @click="galleryInputRef?.click()">上传服务图片</ion-button>
          </div>
          <div class="gallery" v-if="form.imagePaths.length">
            <div v-for="path in form.imagePaths" :key="path" class="gallery-item">
              <app-image :src="serviceImageUrl(path)" alt="img" />
              <ion-button size="small" fill="clear" color="danger" @click="removeGallery(path)">移除</ion-button>
            </div>
          </div>

          <div class="btn-row">
            <ion-button @click="submit">{{ isEdit ? "保存并重新提交审核" : "提交审核" }}</ion-button>
            <ion-button fill="outline" @click="resetForm">重置</ion-button>
          </div>
        </div>
      </template>

      <template v-else>
        <div class="tool-row">
          <ion-select v-model="query.auditStatus" interface="popover" placeholder="审核状态">
            <ion-select-option value="">全部状态</ion-select-option>
            <ion-select-option value="PENDING">待审核</ion-select-option>
            <ion-select-option value="APPROVED">审核通过</ion-select-option>
            <ion-select-option value="REJECTED">审核拒绝</ion-select-option>
            <ion-select-option value="RETURNED">驳回修改</ion-select-option>
          </ion-select>
          <ion-button size="small" @click="fetchList">查询</ion-button>
        </div>

        <ion-card v-for="item in list" :key="item.id">
          <ion-card-header>
            <ion-card-title>{{ item.name }}</ion-card-title>
            <ion-card-subtitle>{{ item.categoryName }} · {{ item.updatedAt }}</ion-card-subtitle>
          </ion-card-header>
          <ion-card-content>
            <div class="provider-row">
              <user-avatar :src="item.providerAvatarPath" :name="item.providerName" :size="24" />
              <span>{{ item.providerName }}</span>
            </div>
            <p>审核：{{ item.auditStatusLabel }} | 服务：{{ item.serviceStatusLabel }}</p>
            <p>预约：{{ item.currentBooked }}/{{ item.maxCapacity }}</p>
            <div class="btn-row">
              <ion-button size="small" fill="outline" @click="editRow(item)">编辑</ion-button>
              <ion-button size="small" fill="outline" @click="viewDetail(item.id)">详情</ion-button>
              <ion-select
                v-if="item.auditStatus === 'APPROVED'"
                interface="popover"
                placeholder="调整状态"
                @ionChange="(e) => changeOperateStatus(item, e.detail.value)"
              >
                <ion-select-option value="RESERVABLE">可预约</ion-select-option>
                <ion-select-option value="FULL">约满</ion-select-option>
                <ion-select-option value="IN_SERVICE">进行中</ion-select-option>
                <ion-select-option value="PAUSED">暂停</ion-select-option>
              </ion-select>
            </div>
          </ion-card-content>
        </ion-card>
      </template>

      <ion-modal :is-open="detailVisible" @didDismiss="detailVisible = false">
        <ion-header>
          <ion-toolbar>
            <ion-title>服务详情</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="detailVisible = false">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>
        <ion-content class="ion-padding">
          <div v-if="detail">
            <h3>{{ detail.name }}</h3>
            <div class="provider-row">
              <user-avatar :src="detail.providerAvatarPath" :name="detail.providerName" :size="26" />
              <span>{{ detail.providerName }}</span>
            </div>
            <p>分类：{{ detail.categoryName }}</p>
            <p>审核状态：{{ detail.auditStatusLabel }}</p>
            <p>服务状态：{{ detail.serviceStatusLabel }}</p>
            <p>联系人：{{ detail.contactName }} {{ detail.contactPhone }}</p>
            <p>地址：{{ detail.address || '-' }}</p>
            <p>简介：{{ detail.summary }}</p>
            <p>详情：{{ detail.description }}</p>
            <p>审核意见：{{ detail.auditReason || '-' }}</p>
            <div class="gallery" v-if="detail.imagePaths && detail.imagePaths.length">
              <app-image v-for="path in detail.imagePaths" :key="path" :src="serviceImageUrl(path)" alt="img" />
            </div>
            <div class="timeline" v-if="detail.auditLogs && detail.auditLogs.length">
              <div class="timeline-item" v-for="log in detail.auditLogs" :key="log.id">
                <strong>{{ log.fromAuditStatusLabel }} -> {{ log.toAuditStatusLabel }}</strong>
                <div class="provider-row">
                  <user-avatar :src="log.reviewerAvatarPath" :name="log.reviewerName" :size="20" />
                  <span>{{ log.reviewerName }}</span>
                  <span>{{ log.createdAt }}</span>
                </div>
                <p>原因：{{ log.reason || '-' }}</p>
              </div>
            </div>
          </div>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup>
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardSubtitle,
  IonCardTitle,
  IonContent,
  IonHeader,
  IonInput,
  IonItem,
  IonLabel,
  IonModal,
  IonPage,
  IonSegment,
  IonSegmentButton,
  IonSelect,
  IonSelectOption,
  IonTextarea,
  IonTitle,
  IonToolbar
} from "@ionic/vue";
import { onMounted, reactive, ref } from "vue";
import AppImage from "../components/AppImage.vue";
import UserAvatar from "../components/UserAvatar.vue";
import {
  createServiceEntry,
  getServiceDetail,
  listProviderEntries,
  listServiceCategories,
  serviceImageUrl,
  updateServiceEntry,
  updateServiceOperateStatus,
  uploadServiceImage
} from "../api/service";
import { presentToast } from "../utils/toast";

const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
const tab = ref("form");
const categories = ref([]);
const list = ref([]);
const detailVisible = ref(false);
const detail = ref(null);
const isEdit = ref(false);
const editId = ref(null);

const coverInputRef = ref(null);
const galleryInputRef = ref(null);

const query = reactive({
  auditStatus: ""
});

const form = reactive({
  name: "",
  categoryCode: "",
  summary: "",
  description: "",
  contactName: "",
  contactPhone: "",
  address: "",
  coverImagePath: "",
  imagePaths: [],
  maxCapacity: 50
});

onMounted(async () => {
  await Promise.all([fetchCategories(), fetchList()]);
});

async function fetchCategories() {
  categories.value = await listServiceCategories();
}

async function fetchList() {
  list.value = await listProviderEntries({
    auditStatus: query.auditStatus || undefined
  });
}

async function validateImage(file) {
  if (!file?.type?.startsWith("image/")) {
    await presentToast("仅支持图片文件", "warning");
    return false;
  }
  if (file.size > MAX_IMAGE_BYTES) {
    await presentToast("图片大小不能超过10MB", "warning");
    return false;
  }
  return true;
}

async function onPickCover(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  if (!(await validateImage(file))) return;
  const data = await uploadServiceImage(file);
  form.coverImagePath = data.path;
  coverInputRef.value.value = "";
}

async function onPickGallery(event) {
  const files = Array.from(event.target.files || []);
  for (const file of files) {
    if (!(await validateImage(file))) continue;
    const data = await uploadServiceImage(file);
    if (data?.path && !form.imagePaths.includes(data.path)) {
      form.imagePaths.push(data.path);
    }
  }
  galleryInputRef.value.value = "";
}

function removeGallery(path) {
  form.imagePaths = form.imagePaths.filter((item) => item !== path);
}

async function submit() {
  if (!form.name.trim() || !form.categoryCode || !form.summary.trim() || !form.description.trim()) {
    await presentToast("请填写完整服务信息", "warning");
    return;
  }
  if (!form.contactName.trim() || !/^\d{11}$/.test(form.contactPhone || "")) {
    await presentToast("请填写正确联系人和联系电话", "warning");
    return;
  }
  const payload = {
    name: form.name,
    categoryCode: form.categoryCode,
    summary: form.summary,
    description: form.description,
    contactName: form.contactName,
    contactPhone: form.contactPhone,
    address: form.address || undefined,
    coverImagePath: form.coverImagePath || undefined,
    imagePaths: form.imagePaths,
    maxCapacity: Number(form.maxCapacity || 50)
  };
  if (isEdit.value) {
    await updateServiceEntry(editId.value, payload);
    await presentToast("更新成功，已重新进入待审核", "success");
  } else {
    await createServiceEntry(payload);
    await presentToast("提交成功，等待管理员审核", "success");
  }
  resetForm();
  tab.value = "list";
  await fetchList();
}

function resetForm() {
  isEdit.value = false;
  editId.value = null;
  form.name = "";
  form.categoryCode = "";
  form.summary = "";
  form.description = "";
  form.contactName = "";
  form.contactPhone = "";
  form.address = "";
  form.coverImagePath = "";
  form.imagePaths = [];
  form.maxCapacity = 50;
}

async function editRow(row) {
  const data = await getServiceDetail(row.id);
  isEdit.value = true;
  editId.value = row.id;
  form.name = data.name;
  form.categoryCode = data.categoryCode;
  form.summary = data.summary;
  form.description = data.description;
  form.contactName = data.contactName;
  form.contactPhone = data.contactPhone;
  form.address = data.address || "";
  form.coverImagePath = data.coverImagePath || "";
  form.imagePaths = (data.imagePaths || []).slice();
  form.maxCapacity = data.maxCapacity || 50;
  tab.value = "form";
}

async function changeOperateStatus(row, status) {
  if (!status) return;
  await updateServiceOperateStatus(row.id, { serviceStatus: status });
  await presentToast("服务状态更新成功", "success");
  await fetchList();
}

async function viewDetail(id) {
  detail.value = await getServiceDetail(id);
  detailVisible.value = true;
}
</script>

<style scoped>
.block {
  margin-top: 10px;
  padding: 10px;
}

.tool-row {
  margin-top: 10px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.hidden-input {
  display: none;
}

.upload-row {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.cover-preview {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
}

.gallery {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.gallery img {
  width: 84px;
  height: 84px;
  border-radius: 8px;
  object-fit: cover;
}

.gallery-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.btn-row {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.provider-row {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #364a62;
  font-size: 13px;
}

.timeline {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.timeline-item {
  border-left: 3px solid #9bc8ea;
  padding-left: 8px;
  background: #f7fbff;
  border-radius: 8px;
  padding-top: 6px;
  padding-bottom: 6px;
}
</style>
