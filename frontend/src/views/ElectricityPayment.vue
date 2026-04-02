<template>
  <div class="electricity-page">
    <div class="hero">
      <div>
        <h2>电费代缴</h2>
        <p>生活缴费账单创建 + 支付宝扫码支付，缴费记录可在本页面查询。</p>
      </div>
      <div class="hero-actions">
        <el-tag :type="defaults.alipayEnabled ? 'success' : 'danger'" effect="dark">
          {{ defaults.alipayEnabled ? "支付宝已启用" : "支付宝未启用" }}
        </el-tag>
        <el-button type="warning" plain @click="openAlipayLifePayment">跳转支付宝生活缴费</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="10">
        <el-card shadow="never">
          <template #header>
            <div class="title-row">创建电费订单</div>
          </template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="98px">
            <el-form-item label="缴费机构" prop="chargeInst">
              <el-input v-model="form.chargeInst" maxlength="64" />
            </el-form-item>
            <el-form-item label="电费户号" prop="billKey">
              <el-input v-model="form.billKey" maxlength="64" />
            </el-form-item>
            <el-form-item label="户主姓名" prop="ownerName">
              <el-input v-model="form.ownerName" maxlength="50" />
            </el-form-item>
            <el-form-item label="缴费金额" prop="payAmount">
              <el-input-number v-model="form.payAmount" :min="0.01" :precision="2" :step="10" style="width: 220px" />
              <span style="margin-left: 8px">元</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="creating" @click="submitOrder">生成支付二维码</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="14">
        <el-card shadow="never">
          <template #header>
            <div class="title-row">当前待支付订单</div>
          </template>

          <el-empty v-if="!currentOrder" description="暂未生成订单" :image-size="70" />

          <div v-else class="qr-wrap">
            <img v-if="currentOrder.qrCodeImage" :src="currentOrder.qrCodeImage" class="qr-image" alt="支付宝收款码" />
            <div class="qr-info">
              <div class="info-item"><span>订单号：</span>{{ currentOrder.outTradeNo }}</div>
              <div class="info-item"><span>机构：</span>{{ currentOrder.chargeInst }}</div>
              <div class="info-item"><span>户号：</span>{{ currentOrder.billKey }}</div>
              <div class="info-item"><span>金额：</span>{{ currentOrder.payAmount }} 元</div>
              <div class="info-item">
                <span>状态：</span>
                <el-tag :type="statusType(currentOrder.status)">{{ currentOrder.statusLabel }}</el-tag>
              </div>
              <div class="button-row">
                <el-button type="primary" plain @click="refreshStatus(currentOrder.id)">刷新支付状态</el-button>
                <el-button link type="primary" :href="currentOrder.qrCode" target="_blank">打开支付链接</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 14px">
      <template #header>
        <div class="title-row">
          <span>我的电费订单</span>
          <el-button size="small" @click="loadOrders">刷新列表</el-button>
        </div>
      </template>
      <el-table :data="orders" border v-loading="loadingOrders">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="chargeInst" label="缴费机构" min-width="190" />
        <el-table-column prop="billKey" label="户号" min-width="150" />
        <el-table-column prop="payAmount" label="金额(元)" width="110" />
        <el-table-column label="支付状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="170" />
        <el-table-column prop="paidAt" label="支付时间" min-width="170" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewOrder(row)">查看</el-button>
            <el-button
              v-if="row.status === 'WAIT_PAY'"
              link
              type="success"
              @click="refreshStatus(row.id)"
            >
              刷新状态
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  createElectricityOrder,
  getElectricityDefaults,
  listMyElectricityOrders,
  refreshElectricityOrder
} from "../api/electricity";

const formRef = ref(null);
const creating = ref(false);
const loadingOrders = ref(false);
const orders = ref([]);
const currentOrder = ref(null);
let pollTimer = null;
let pollCount = 0;

const defaults = reactive({
  defaultChargeInst: "",
  defaultBillKey: "",
  defaultOwnerName: "",
  defaultOrderType: "JF",
  defaultSubOrderType: "ELEC",
  alipayEnabled: false
});

const userInfo = (() => {
  const raw = sessionStorage.getItem("userInfo");
  if (!raw) return {};
  try {
    return JSON.parse(raw);
  } catch (e) {
    return {};
  }
})();

const form = reactive({
  chargeInst: "",
  billKey: "",
  ownerName: "",
  payAmount: 50
});

const rules = {
  chargeInst: [{ required: true, message: "请输入缴费机构", trigger: "blur" }],
  billKey: [{ required: true, message: "请输入电费户号", trigger: "blur" }],
  ownerName: [{ required: true, message: "请输入户主姓名", trigger: "blur" }],
  payAmount: [{ required: true, message: "请输入缴费金额", trigger: "blur" }]
};

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
  defaults.defaultOrderType = data.defaultOrderType || "JF";
  defaults.defaultSubOrderType = data.defaultSubOrderType || "ELEC";
  defaults.alipayEnabled = !!data.alipayEnabled;
  form.chargeInst = defaults.defaultChargeInst;
  form.billKey = defaults.defaultBillKey;
  form.ownerName = userInfo.nickname || userInfo.username || defaults.defaultOwnerName;
}

async function loadOrders() {
  loadingOrders.value = true;
  try {
    orders.value = await listMyElectricityOrders();
    const waitPay = orders.value.find((item) => item.status === "WAIT_PAY");
    if (waitPay) {
      currentOrder.value = waitPay;
    }
  } finally {
    loadingOrders.value = false;
  }
}

async function submitOrder() {
  await formRef.value.validate();
  creating.value = true;
  try {
    const data = await createElectricityOrder({
      chargeInst: form.chargeInst,
      billKey: form.billKey,
      ownerName: form.ownerName,
      payAmount: form.payAmount
    });
    currentOrder.value = data;
    ElMessage.success("订单创建成功，请使用支付宝扫码支付");
    await loadOrders();
    startPoll(data.id);
  } finally {
    creating.value = false;
  }
}

function resetForm() {
  form.chargeInst = defaults.defaultChargeInst;
  form.billKey = defaults.defaultBillKey;
  form.ownerName = userInfo.nickname || userInfo.username || defaults.defaultOwnerName;
  form.payAmount = 50;
}

function viewOrder(order) {
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
  const idx = orders.value.findIndex((item) => item.id === id);
  if (idx >= 0) {
    orders.value[idx] = data;
  }
  if (data.status === "PAID") {
    ElMessage.success("支付成功");
    stopPoll();
  }
  if (data.status === "CLOSED") {
    ElMessage.warning("订单已关闭");
    stopPoll();
  }
}

function startPoll(orderId) {
  stopPoll();
  pollCount = 0;
  pollTimer = setInterval(async () => {
    pollCount += 1;
    await refreshStatus(orderId);
    if (!currentOrder.value || currentOrder.value.status !== "WAIT_PAY") {
      stopPoll();
      return;
    }
    if (pollCount >= 24) {
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

function statusType(status) {
  if (status === "PAID") return "success";
  if (status === "WAIT_PAY") return "warning";
  if (status === "FAILED") return "danger";
  if (status === "CLOSED") return "info";
  return "";
}

function openAlipayLifePayment() {
  const scheme = "alipays://platformapi/startapp?appId=20000193";
  const jumpUrl = `https://render.alipay.com/p/s/i?scheme=${encodeURIComponent(scheme)}`;
  window.open(jumpUrl, "_blank", "noopener,noreferrer");
}
</script>


<style scoped>
.electricity-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero {
  border-radius: 14px;
  padding: 16px 18px;
  background: linear-gradient(130deg, #0d6cb7 0%, #0f8bc1 48%, #1ca4bc 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.hero h2 {
  margin: 0;
  font-size: 22px;
}

.hero p {
  margin: 6px 0 0;
  opacity: 0.95;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.title-row {
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.qr-wrap {
  display: flex;
  gap: 18px;
  align-items: center;
}

.qr-image {
  width: 260px;
  height: 260px;
  border-radius: 10px;
  border: 1px solid #e4e7ed;
  background: #fff;
}

.qr-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-item {
  color: #303133;
}

.info-item span {
  color: #909399;
}

.button-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

@media (max-width: 900px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>






