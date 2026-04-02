<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>电费代缴</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content fullscreen class="ion-padding">
      <ion-card class="hero-card">
        <ion-card-header>
          <ion-card-title>在线电费代缴</ion-card-title>
          <ion-card-subtitle>{{ defaults.alipayEnabled ? "支付宝已启用" : "支付宝未启用" }}</ion-card-subtitle>
        </ion-card-header>
        <ion-card-content>
          创建缴费订单后，可在本页面完成扫码支付与状态刷新。
          <div class="btn-row">
            <ion-button size="small" fill="outline" @click="openAlipayLifePayment">跳转支付宝生活缴费</ion-button>
          </div>
        </ion-card-content>
      </ion-card>

      <div class="page-card form-wrap">
        <ion-item>
          <ion-input v-model="form.chargeInst" label="缴费机构" label-placement="stacked" />
        </ion-item>
        <ion-item>
          <ion-input v-model="form.billKey" label="电费户号" label-placement="stacked" />
        </ion-item>
        <ion-item>
          <ion-input v-model="form.ownerName" label="户主姓名" label-placement="stacked" />
        </ion-item>
        <ion-item>
          <ion-input v-model.number="form.payAmount" type="number" label="缴费金额(元)" label-placement="stacked" />
        </ion-item>
        <div class="btn-row">
          <ion-button @click="createOrder">生成支付二维码</ion-button>
          <ion-button fill="outline" @click="resetForm">重置</ion-button>
        </div>
      </div>

      <ion-card v-if="currentOrder">
        <ion-card-header>
          <ion-card-title>当前订单</ion-card-title>
          <ion-card-subtitle>{{ currentOrder.statusLabel }}</ion-card-subtitle>
        </ion-card-header>
        <ion-card-content>
          <img v-if="currentOrder.qrCodeImage" :src="currentOrder.qrCodeImage" class="qr-image" alt="qrcode" />
          <p>订单号：<span class="mono">{{ currentOrder.outTradeNo }}</span></p>
          <p>机构：{{ currentOrder.chargeInst }}</p>
          <p>户号：{{ currentOrder.billKey }}</p>
          <p>金额：{{ currentOrder.payAmount }} 元</p>
          <div class="btn-row">
            <ion-button size="small" fill="outline" @click="refreshStatus(currentOrder.id)">刷新状态</ion-button>
            <ion-button size="small" fill="clear" @click="openQrLink(currentOrder.qrCode)">打开支付链接</ion-button>
          </div>
        </ion-card-content>
      </ion-card>

      <div class="list-head">
        <strong>我的订单</strong>
        <ion-button size="small" fill="outline" @click="loadOrders">刷新</ion-button>
      </div>
      <ion-card v-for="item in orders" :key="item.id">
        <ion-card-header>
          <ion-card-title>{{ item.chargeInst }}</ion-card-title>
          <ion-card-subtitle>{{ item.statusLabel }}</ion-card-subtitle>
        </ion-card-header>
        <ion-card-content>
          <p>户号：{{ item.billKey }}</p>
          <p>金额：{{ item.payAmount }} 元</p>
          <p>创建时间：{{ item.createdAt }}</p>
          <ion-button size="small" fill="clear" @click="selectOrder(item)">查看</ion-button>
        </ion-card-content>
      </ion-card>
    </ion-content>
  </ion-page>
</template>

<script setup>
import {
  IonButton,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardSubtitle,
  IonCardTitle,
  IonContent,
  IonHeader,
  IonInput,
  IonItem,
  IonPage,
  IonTitle,
  IonToolbar
} from "@ionic/vue";
import { onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { createElectricityOrder, getElectricityDefaults, listMyElectricityOrders, refreshElectricityOrder } from "../api/electricity";
import { getUserInfo } from "../composables/useAuth";
import { presentToast } from "../utils/toast";

const user = getUserInfo();
const defaults = reactive({
  defaultChargeInst: "",
  defaultBillKey: "",
  defaultOwnerName: "",
  alipayEnabled: false
});
const form = reactive({
  chargeInst: "",
  billKey: "",
  ownerName: "",
  payAmount: 50
});
const orders = ref([]);
const currentOrder = ref(null);
let pollTimer = null;
let pollCount = 0;

onMounted(async () => {
  await loadDefaults();
  await loadOrders();
});

onBeforeUnmount(() => {
  stopPoll();
});

async function loadDefaults() {
  const data = await getElectricityDefaults();
  defaults.defaultChargeInst = data.defaultChargeInst || "";
  defaults.defaultBillKey = data.defaultBillKey || "";
  defaults.defaultOwnerName = data.defaultOwnerName || "";
  defaults.alipayEnabled = !!data.alipayEnabled;
  resetForm();
}

async function loadOrders() {
  orders.value = await listMyElectricityOrders();
  const waitPay = orders.value.find((item) => item.status === "WAIT_PAY");
  if (waitPay) {
    currentOrder.value = waitPay;
    startPoll(waitPay.id);
  }
}

function resetForm() {
  form.chargeInst = defaults.defaultChargeInst;
  form.billKey = defaults.defaultBillKey;
  form.ownerName = user.nickname || user.username || defaults.defaultOwnerName;
  form.payAmount = 50;
}

async function createOrder() {
  if (!form.chargeInst.trim() || !form.billKey.trim() || !form.ownerName.trim()) {
    await presentToast("请完整填写缴费信息", "warning");
    return;
  }
  const data = await createElectricityOrder(form);
  currentOrder.value = data;
  await presentToast("订单创建成功，请扫码支付", "success");
  await loadOrders();
  startPoll(data.id);
}

function selectOrder(order) {
  currentOrder.value = order;
  if (order.status === "WAIT_PAY") {
    startPoll(order.id);
  } else {
    stopPoll();
  }
}

async function refreshStatus(id) {
  const data = await refreshElectricityOrder(id);
  if (currentOrder.value && currentOrder.value.id === id) {
    currentOrder.value = data;
  }
  const index = orders.value.findIndex((item) => item.id === id);
  if (index >= 0) orders.value[index] = data;
  if (data.status === "PAID") {
    await presentToast("支付成功", "success");
    stopPoll();
  }
  if (data.status === "CLOSED") {
    await presentToast("订单已关闭", "warning");
    stopPoll();
  }
}

function openQrLink(url) {
  if (!url) return;
  window.open(url, "_blank");
}

function openAlipayLifePayment() {
  const scheme = "alipays://platformapi/startapp?appId=20000193";
  const jumpUrl = `https://render.alipay.com/p/s/i?scheme=${encodeURIComponent(scheme)}`;
  window.open(jumpUrl, "_blank");
}

function startPoll(orderId) {
  stopPoll();
  pollCount = 0;
  pollTimer = setInterval(async () => {
    pollCount += 1;
    await refreshStatus(orderId);
    if (!currentOrder.value || currentOrder.value.status !== "WAIT_PAY" || pollCount >= 24) {
      stopPoll();
    }
  }, 5000);
}

function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
}
</script>

<style scoped>
.hero-card {
  margin-bottom: 10px;
}

.form-wrap {
  padding: 10px;
}

.btn-row {
  display: flex;
  gap: 8px;
  padding: 10px 0 0;
}

.qr-image {
  width: 220px;
  max-width: 100%;
  border-radius: 10px;
  border: 1px solid #eaeef5;
}

.list-head {
  margin-top: 12px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
