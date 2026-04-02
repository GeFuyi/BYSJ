<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>物业报修</ion-title>
        <ion-buttons slot="end">
          <ion-button fill="clear" @click="submitModalOpen = true">
            <ion-icon :icon="addOutline" />
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen>
      <div class="repair-page ion-padding">
        <section class="list-section">
          <div class="list-head">
            <div>
              <h3>报修工单</h3>
              <small>当前角色：{{ roleName }}</small>
            </div>
            <div class="head-actions">
              <ion-select v-model="query.status" interface="popover" placeholder="状态筛选">
                <ion-select-option value="">全部</ion-select-option>
                <ion-select-option v-for="item in statusOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </ion-select-option>
              </ion-select>
              <ion-button size="small" fill="outline" @click="fetchOrders">筛选</ion-button>
            </div>
          </div>

          <article class="order-card page-card" v-for="order in orders" :key="order.id">
            <div class="order-top">
              <h4>{{ order.title }}</h4>
              <span class="status-chip">{{ order.statusLabel }}</span>
            </div>
            <p class="desc">{{ order.description }}</p>
            <div class="identity-row">
              <div class="identity-item">
                <user-avatar :src="order.userAvatarPath" :name="order.userNickname || order.username" :size="24" />
                <span>报修人：{{ order.userNickname || order.username }}</span>
              </div>
              <div class="identity-item">
                <user-avatar :src="order.handlerAvatarPath" :name="order.handlerName || '未分配'" :size="24" />
                <span>处理人：{{ order.handlerName || "-" }}</span>
              </div>
            </div>
            <div class="meta-grid">
              <span>联系电话：{{ order.contactPhone || "-" }}</span>
              <span>更新时间：{{ order.updatedAt || order.createdAt }}</span>
            </div>
            <div class="image-preview" v-if="order.imagePaths && order.imagePaths.length">
              <app-image v-for="path in order.imagePaths" :key="path" :src="repairImageUrl(path)" alt="img" />
            </div>
            <div class="btn-row">
              <ion-button size="small" fill="outline" @click="openDetail(order.id)">详情</ion-button>
              <ion-button v-if="canTake(order)" size="small" @click="openStatusModal(order, 'ACCEPTED')">接单</ion-button>
              <ion-button v-if="canStartRepair(order)" size="small" color="warning" @click="openStatusModal(order, 'IN_PROGRESS')">维修中</ion-button>
              <ion-button
                v-if="canFinishPending(order)"
                size="small"
                color="success"
                @click="openStatusModal(order, 'COMPLETED_PENDING_CONFIRM')"
              >
                待确认
              </ion-button>
              <ion-button v-if="canConfirmDone(order)" size="small" color="success" @click="openStatusModal(order, 'COMPLETED')">确认完成</ion-button>
              <ion-button v-if="canRollback(order)" size="small" color="danger" @click="openStatusModal(order, 'ROLLBACK')">异常回退</ion-button>
            </div>
          </article>
        </section>
      </div>

      <ion-modal :is-open="submitModalOpen" @didDismiss="closeSubmitModal">
        <ion-header>
          <ion-toolbar>
            <ion-title>提交报修</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="closeSubmitModal">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>
        <ion-content class="ion-padding">
          <ion-item>
            <ion-input v-model="form.title" label="问题标题" label-placement="stacked" maxlength="100" />
          </ion-item>
          <ion-item>
            <ion-textarea v-model="form.description" label="问题描述" label-placement="stacked" :rows="4" maxlength="2000" />
          </ion-item>
          <ion-item>
            <ion-input v-model="form.contactPhone" label="联系电话" label-placement="stacked" maxlength="20" />
          </ion-item>

          <div class="upload-row">
            <input ref="fileInputRef" class="hidden-input" type="file" accept="image/*" multiple @change="onPickImages" />
            <ion-button size="small" fill="clear" class="outline-btn" @click="openImagePicker">上传图片</ion-button>
            <ion-text color="medium" class="upload-tip">仅支持图片，单张不超过 10MB</ion-text>
          </div>

          <div class="image-preview" v-if="form.imagePaths.length">
            <div v-for="path in form.imagePaths" :key="path" class="image-chip">
              <app-image :src="repairImageUrl(path)" alt="img" />
              <button class="remove-btn" type="button" @click="removeImage(path)">×</button>
            </div>
          </div>

          <ion-button expand="block" @click="submitOrder">提交报修</ion-button>
        </ion-content>
      </ion-modal>

      <ion-modal :is-open="statusModalOpen" @didDismiss="closeStatusModal">
        <ion-header>
          <ion-toolbar>
            <ion-title>变更状态</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="closeStatusModal">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>
        <ion-content class="ion-padding">
          <div class="status-tip">目标状态：{{ statusLabelOf(statusForm.targetStatus) }}</div>
          <ion-item>
            <ion-textarea v-model="statusForm.remark" label="备注" label-placement="stacked" :rows="3" maxlength="255" placeholder="请填写处理说明" />
          </ion-item>
          <div class="upload-row">
            <input ref="flowFileInputRef" class="hidden-input" type="file" accept="image/*" multiple @change="onPickFlowImages" />
            <ion-button size="small" fill="clear" class="outline-btn" @click="openFlowImagePicker">上传进度图片</ion-button>
          </div>
          <div class="image-preview" v-if="statusForm.imagePaths.length">
            <div v-for="path in statusForm.imagePaths" :key="path" class="image-chip">
              <app-image :src="repairImageUrl(path)" alt="img" />
              <button class="remove-btn" type="button" @click="removeFlowImage(path)">×</button>
            </div>
          </div>
          <ion-button expand="block" @click="submitStatusChange">确认变更</ion-button>
        </ion-content>
      </ion-modal>

      <ion-modal :is-open="detailVisible" @didDismiss="detailVisible = false">
        <ion-header>
          <ion-toolbar>
            <ion-title>工单详情</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="detailVisible = false">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>
        <ion-content class="ion-padding">
          <div v-if="detail" class="detail-card">
            <h3>{{ detail.title }}</h3>
            <p>状态：{{ detail.statusLabel }}</p>
            <div class="identity-row">
              <div class="identity-item">
                <user-avatar :src="detail.userAvatarPath" :name="detail.userNickname || detail.username" :size="24" />
                <span>{{ detail.userNickname || detail.username }}</span>
              </div>
              <div class="identity-item">
                <user-avatar :src="detail.handlerAvatarPath" :name="detail.handlerName || '-'" :size="24" />
                <span>{{ detail.handlerName || "-" }}</span>
              </div>
            </div>
            <p>联系电话：{{ detail.contactPhone || "-" }}</p>
            <p class="desc">描述：{{ detail.description }}</p>
            <div class="image-preview" v-if="detail.imagePaths && detail.imagePaths.length">
              <app-image v-for="path in detail.imagePaths" :key="path" :src="repairImageUrl(path)" alt="img" />
            </div>

            <h4>流程记录</h4>
            <div class="timeline" v-if="detail.flows && detail.flows.length">
              <div v-for="flow in detail.flows" :key="flow.id" class="timeline-item">
                <div class="flow-head">
                  <strong>{{ flow.fromStatusLabel }} → {{ flow.toStatusLabel }}</strong>
                  <span>{{ flow.createdAt }}</span>
                </div>
                <div class="identity-item">
                  <user-avatar :src="flow.operatorAvatarPath" :name="flow.operatorName" :size="22" />
                  <span>{{ flow.operatorName }}（{{ flow.operatorRole }}）</span>
                </div>
                <p>备注：{{ flow.remark || "-" }}</p>
                <div class="image-preview" v-if="flow.imagePaths && flow.imagePaths.length">
                  <app-image v-for="path in flow.imagePaths" :key="`${flow.id}-${path}`" :src="repairImageUrl(path)" alt="img" />
                </div>
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
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonInput,
  IonItem,
  IonModal,
  IonPage,
  IonSelect,
  IonSelectOption,
  IonText,
  IonTextarea,
  IonTitle,
  IonToolbar
} from "@ionic/vue";
import { addOutline } from "ionicons/icons";
import { computed, onMounted, reactive, ref } from "vue";
import {
  createRepairOrder,
  getRepairOrderDetail,
  listRepairOrders,
  repairImageUrl,
  updateRepairOrderStatus,
  uploadRepairImage
} from "../api/repair";
import AppImage from "../components/AppImage.vue";
import UserAvatar from "../components/UserAvatar.vue";
import { getUserInfo } from "../composables/useAuth";
import { presentToast } from "../utils/toast";

const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
const currentUser = getUserInfo();
const currentRole = String(currentUser.role || "").toUpperCase();
const isEmployee = currentRole === "ADMIN" || currentRole === "EMPLOYEE";

const fileInputRef = ref(null);
const flowFileInputRef = ref(null);
const orders = ref([]);
const detailVisible = ref(false);
const detail = ref(null);
const submitModalOpen = ref(false);
const statusModalOpen = ref(false);
const statusTargetOrder = ref(null);

const query = reactive({
  status: ""
});

const form = reactive({
  title: "",
  description: "",
  contactPhone: currentUser.phone || "",
  imagePaths: []
});

const statusForm = reactive({
  targetStatus: "",
  remark: "",
  imagePaths: []
});

const statusOptions = [
  { value: "SUBMITTED", label: "用户提交" },
  { value: "ACCEPTED", label: "物业接单" },
  { value: "IN_PROGRESS", label: "维修中" },
  { value: "COMPLETED_PENDING_CONFIRM", label: "完成待确认" },
  { value: "COMPLETED", label: "已完成" },
  { value: "ROLLBACK", label: "异常回退" }
];

const roleName = computed(() => {
  if (currentRole === "ADMIN") return "管理员";
  if (currentRole === "EMPLOYEE") return "员工";
  return "住户";
});

onMounted(() => {
  fetchOrders();
});

async function fetchOrders() {
  const mineOnly = !isEmployee;
  orders.value = await listRepairOrders({
    status: query.status || undefined,
    mineOnly
  });
}

function openImagePicker() {
  fileInputRef.value?.click();
}

function openFlowImagePicker() {
  flowFileInputRef.value?.click();
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

async function onPickImages(event) {
  const files = Array.from(event.target.files || []);
  for (const file of files) {
    if (!(await validateImage(file))) continue;
    const data = await uploadRepairImage(file);
    if (data?.path) {
      form.imagePaths.push(data.path);
    }
  }
  if (fileInputRef.value) fileInputRef.value.value = "";
}

async function onPickFlowImages(event) {
  const files = Array.from(event.target.files || []);
  for (const file of files) {
    if (!(await validateImage(file))) continue;
    const data = await uploadRepairImage(file);
    if (data?.path) {
      statusForm.imagePaths.push(data.path);
    }
  }
  if (flowFileInputRef.value) flowFileInputRef.value.value = "";
}

function removeImage(path) {
  form.imagePaths = form.imagePaths.filter((item) => item !== path);
}

function removeFlowImage(path) {
  statusForm.imagePaths = statusForm.imagePaths.filter((item) => item !== path);
}

function closeSubmitModal() {
  submitModalOpen.value = false;
  form.title = "";
  form.description = "";
  form.imagePaths = [];
}

async function submitOrder() {
  if (!form.title.trim() || !form.description.trim()) {
    await presentToast("标题和描述不能为空", "warning");
    return;
  }
  await createRepairOrder({
    title: form.title,
    description: form.description,
    contactPhone: form.contactPhone || undefined,
    imagePaths: form.imagePaths
  });
  await presentToast("提交成功", "success");
  closeSubmitModal();
  await fetchOrders();
}

function openStatusModal(order, targetStatus) {
  statusTargetOrder.value = order;
  statusForm.targetStatus = targetStatus;
  statusForm.remark = "";
  statusForm.imagePaths = [];
  statusModalOpen.value = true;
}

function closeStatusModal() {
  statusModalOpen.value = false;
  statusTargetOrder.value = null;
  statusForm.targetStatus = "";
  statusForm.remark = "";
  statusForm.imagePaths = [];
}

async function submitStatusChange() {
  if (!statusTargetOrder.value?.id || !statusForm.targetStatus) return;
  await updateRepairOrderStatus(statusTargetOrder.value.id, {
    targetStatus: statusForm.targetStatus,
    remark: statusForm.remark || undefined,
    imagePaths: statusForm.imagePaths
  });
  await presentToast("状态更新成功", "success");
  closeStatusModal();
  await refreshAfterStatusChange();
}

async function openDetail(id) {
  detail.value = await getRepairOrderDetail(id);
  detailVisible.value = true;
}

async function refreshAfterStatusChange() {
  await fetchOrders();
  if (detailVisible.value && detail.value) {
    detail.value = await getRepairOrderDetail(detail.value.id);
  }
}

function statusLabelOf(code) {
  const item = statusOptions.find((it) => it.value === code);
  return item?.label || code || "-";
}

function canTake(row) {
  return isEmployee && (row.status === "SUBMITTED" || row.status === "ROLLBACK");
}

function canStartRepair(row) {
  return isEmployee && row.status === "ACCEPTED";
}

function canFinishPending(row) {
  return isEmployee && row.status === "IN_PROGRESS";
}

function canConfirmDone(row) {
  if (row.status !== "COMPLETED_PENDING_CONFIRM") return false;
  return currentRole === "ADMIN" || row.userId === currentUser.id;
}

function canRollback(row) {
  if (!isEmployee) return false;
  return row.status === "ACCEPTED" || row.status === "IN_PROGRESS" || row.status === "COMPLETED_PENDING_CONFIRM";
}
</script>

<style scoped>
.repair-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.list-head {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 8px;
  flex-wrap: wrap;
}

.list-head h3 {
  margin: 0 0 6px;
}

.list-head small {
  color: #7d8ca1;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-card {
  padding: 12px;
}

.order-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}

.order-top h4 {
  margin: 0;
  color: #1d2b3a;
}

.status-chip {
  background: #eef4ff;
  color: #2e67e4;
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 11px;
}

.desc {
  margin: 8px 0 0;
  white-space: pre-wrap;
  color: #44556d;
}

.identity-row {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.identity-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #4b5c73;
  font-size: 12px;
}

.meta-grid {
  margin-top: 8px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  color: #6f7f95;
  font-size: 12px;
}

.image-preview {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.image-preview img {
  width: 84px;
  height: 84px;
  border-radius: 10px;
  object-fit: cover;
}

.image-chip {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  overflow: hidden;
  position: relative;
}

.image-chip img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  right: 2px;
  top: 2px;
  width: 18px;
  height: 18px;
  border: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.58);
  color: #fff;
  font-size: 14px;
  line-height: 1;
}

.btn-row {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.upload-row {
  padding: 8px 0 4px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.hidden-input {
  display: none;
}

.outline-btn {
  --background: #edf4ff;
  --color: #2d67e4;
  --border-radius: 10px;
}

.upload-tip {
  font-size: 12px;
}

.status-tip {
  margin-bottom: 8px;
  color: #3f5270;
  font-size: 13px;
}

.timeline {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.timeline-item {
  border-left: 3px solid #97b8ec;
  background: #f7fbff;
  border-radius: 8px;
  padding: 8px;
}

.flow-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  color: #2a3a4f;
  font-size: 12px;
}

.timeline-item p {
  margin: 4px 0;
}
</style>
