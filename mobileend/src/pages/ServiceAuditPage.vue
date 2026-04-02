<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button default-href="/tabs/dashboard" />
        </ion-buttons>
        <ion-title>入驻审核</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content fullscreen class="ion-padding">
      <div class="tool-row page-card">
        <ion-item>
          <ion-input v-model="query.keyword" label="关键词" label-placement="stacked" placeholder="服务名称/联系人" />
        </ion-item>
        <ion-item>
          <ion-select v-model="query.auditStatus" label="审核状态" label-placement="stacked" interface="popover">
            <ion-select-option value="">全部状态</ion-select-option>
            <ion-select-option value="PENDING">待审核</ion-select-option>
            <ion-select-option value="APPROVED">审核通过</ion-select-option>
            <ion-select-option value="REJECTED">审核拒绝</ion-select-option>
            <ion-select-option value="RETURNED">驳回修改</ion-select-option>
          </ion-select>
        </ion-item>
        <div class="btn-row">
          <ion-button size="small" @click="fetchList">查询</ion-button>
        </div>
      </div>

      <ion-card v-for="item in list" :key="item.id">
        <ion-card-header>
          <ion-card-title>{{ item.name }}</ion-card-title>
          <ion-card-subtitle>
            <span>{{ item.updatedAt }}</span>
          </ion-card-subtitle>
        </ion-card-header>
        <ion-card-content>
          <div class="provider-row">
            <user-avatar :src="item.providerAvatarPath" :name="item.providerName" :size="26" />
            <span>{{ item.providerName }}</span>
          </div>
          <p>分类：{{ item.categoryName }}</p>
          <p>联系电话：{{ item.contactPhone }}</p>
          <p>审核状态：{{ item.auditStatusLabel }}</p>
          <div class="btn-row">
            <ion-button size="small" fill="outline" @click="viewDetail(item.id)">详情</ion-button>
            <ion-button v-if="item.auditStatus === 'PENDING'" size="small" color="success" @click="audit(item, 'APPROVE')">通过</ion-button>
            <ion-button v-if="item.auditStatus === 'PENDING'" size="small" color="warning" @click="audit(item, 'RETURN')">驳回</ion-button>
            <ion-button v-if="item.auditStatus === 'PENDING'" size="small" color="danger" @click="audit(item, 'REJECT')">拒绝</ion-button>
          </div>
        </ion-card-content>
      </ion-card>

      <ion-modal :is-open="detailVisible" @didDismiss="detailVisible = false">
        <ion-header>
          <ion-toolbar>
            <ion-title>审核详情</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="detailVisible = false">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>
        <ion-content class="ion-padding">
          <div v-if="detail">
            <h3>{{ detail.name }}</h3>
            <div class="provider-row">
              <user-avatar :src="detail.providerAvatarPath" :name="detail.providerName" :size="30" />
              <span>{{ detail.providerName }}</span>
            </div>
            <p>分类：{{ detail.categoryName }}</p>
            <p>审核状态：{{ detail.auditStatusLabel }}</p>
            <p>联系人：{{ detail.contactName }} {{ detail.contactPhone }}</p>
            <p>地址：{{ detail.address || '-' }}</p>
            <p>简介：{{ detail.summary }}</p>
            <p>详情：{{ detail.description }}</p>
            <p>审核意见：{{ detail.auditReason || '-' }}</p>
            <div class="timeline" v-if="detail.auditLogs && detail.auditLogs.length">
              <div class="timeline-item" v-for="log in detail.auditLogs" :key="log.id">
                <strong>{{ log.fromAuditStatusLabel }} -> {{ log.toAuditStatusLabel }}</strong>
                <div class="reviewer-row">
                  <user-avatar :src="log.reviewerAvatarPath" :name="log.reviewerName" :size="22" />
                  <span>{{ log.reviewerName }}</span>
                  <span>{{ log.createdAt }}</span>
                </div>
                <p>动作：{{ log.action }}</p>
                <p>原因：{{ log.reason || '-' }}</p>
              </div>
            </div>
          </div>
        </ion-content>
      </ion-modal>

      <ion-alert
        :is-open="reasonPromptOpen"
        header="审核说明"
        :inputs="[{ name: 'reason', type: 'text', placeholder: reasonPlaceholder }]"
        :buttons="reasonPromptButtons"
        @didDismiss="reasonPromptOpen = false"
      />
    </ion-content>
  </ion-page>
</template>

<script setup>
import {
  IonAlert,
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
  IonModal,
  IonPage,
  IonSelect,
  IonSelectOption,
  IonTitle,
  IonToolbar
} from "@ionic/vue";
import { computed, reactive, ref } from "vue";
import { auditServiceEntry, getServiceDetail, listAuditEntries } from "../api/service";
import UserAvatar from "../components/UserAvatar.vue";
import { presentToast } from "../utils/toast";

const list = ref([]);
const detailVisible = ref(false);
const detail = ref(null);
const reasonPromptOpen = ref(false);
const auditTarget = ref(null);
const auditAction = ref("");

const query = reactive({
  keyword: "",
  auditStatus: "PENDING"
});

const reasonPlaceholder = computed(() => (auditAction.value === "RETURN" ? "请输入驳回原因" : "请输入拒绝原因"));

const reasonPromptButtons = [
  {
    text: "取消",
    role: "cancel"
  },
  {
    text: "确认",
    handler: async (data) => {
      const reason = String(data.reason || "").trim();
      if (reason.length < 2) {
        await presentToast("原因至少2个字符", "warning");
        return false;
      }
      await submitAudit(reason);
      return true;
    }
  }
];

fetchList();

async function fetchList() {
  list.value = await listAuditEntries({
    keyword: query.keyword || undefined,
    auditStatus: query.auditStatus || undefined
  });
}

async function viewDetail(id) {
  detail.value = await getServiceDetail(id);
  detailVisible.value = true;
}

async function audit(row, action) {
  auditTarget.value = row;
  auditAction.value = action;
  if (action === "APPROVE") {
    await submitAudit();
    return;
  }
  reasonPromptOpen.value = true;
}

async function submitAudit(reason) {
  if (!auditTarget.value || !auditAction.value) return;
  await auditServiceEntry(auditTarget.value.id, {
    action: auditAction.value,
    reason: reason || undefined
  });
  await presentToast("审核处理成功", "success");
  await fetchList();
  if (detailVisible.value && detail.value?.id === auditTarget.value.id) {
    detail.value = await getServiceDetail(auditTarget.value.id);
  }
  auditTarget.value = null;
  auditAction.value = "";
}
</script>

<style scoped>
.tool-row {
  padding: 8px;
  margin-bottom: 10px;
}

.btn-row {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.provider-row {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #32445c;
  font-size: 13px;
}

.reviewer-row {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #51637a;
  font-size: 12px;
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

.timeline-item p {
  margin: 4px 0;
}
</style>
