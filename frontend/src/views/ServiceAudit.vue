<template>
  <div class="audit-page">
    <el-card shadow="never">
      <template #header>
        <div class="head-row">
          <span style="font-weight: 600">入驻审核管理</span>
          <div class="tools">
            <el-input
              v-model="query.keyword"
              placeholder="搜索服务名称/联系人"
              clearable
              style="width: 260px"
              @keyup.enter="fetchList"
            />
            <el-select v-model="query.auditStatus" clearable placeholder="审核状态" style="width: 160px">
              <el-option label="待审核" value="PENDING" />
              <el-option label="审核通过" value="APPROVED" />
              <el-option label="审核拒绝" value="REJECTED" />
              <el-option label="驳回修改" value="RETURNED" />
            </el-select>
            <el-button type="primary" @click="fetchList">查询</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" border v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="服务名称" min-width="180" />
        <el-table-column prop="providerName" label="发布方" min-width="120" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column label="审核状态" width="120">
          <template #default="{ row }">
            <el-tag :type="auditType(row.auditStatus)">{{ row.auditStatusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="160" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row.id)">详情</el-button>
            <el-button v-if="row.auditStatus === 'PENDING'" link type="success" @click="audit(row, 'APPROVE')">通过</el-button>
            <el-button v-if="row.auditStatus === 'PENDING'" link type="warning" @click="audit(row, 'RETURN')">驳回</el-button>
            <el-button v-if="row.auditStatus === 'PENDING'" link type="danger" @click="audit(row, 'REJECT')">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" title="服务审核详情" width="820px" destroy-on-close>
      <div v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="服务名称">{{ detail.name }}</el-descriptions-item>
          <el-descriptions-item label="发布方">{{ detail.providerName }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ detail.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="审核状态">{{ detail.auditStatusLabel }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detail.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="服务地址">{{ detail.address || "-" }}</el-descriptions-item>
          <el-descriptions-item label="预约名额">{{ detail.currentBooked }}/{{ detail.maxCapacity }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="服务简介">{{ detail.summary }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="详情介绍">{{ detail.description }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="审核意见">{{ detail.auditReason || "-" }}</el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: 14px">
          <div style="font-weight: 600; margin-bottom: 8px">审核记录</div>
          <el-timeline>
            <el-timeline-item
              v-for="item in detail.auditLogs || []"
              :key="item.id"
              :timestamp="item.createdAt"
              placement="top"
            >
              <div style="font-weight: 600">{{ item.fromAuditStatusLabel }} -> {{ item.toAuditStatusLabel }}</div>
              <div style="color: #606266; margin-top: 4px">动作：{{ item.action }}，审核人：{{ item.reviewerName }}</div>
              <div style="color: #909399; margin-top: 4px">{{ item.reason || "无" }}</div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { auditServiceEntry, getServiceDetail, listAuditEntries } from "../api/service";

const loading = ref(false);
const list = ref([]);
const detailVisible = ref(false);
const detail = ref(null);
const query = reactive({
  keyword: "",
  auditStatus: "PENDING"
});

onMounted(() => {
  fetchList();
});

async function fetchList() {
  loading.value = true;
  try {
    list.value = await listAuditEntries({
      keyword: query.keyword || undefined,
      auditStatus: query.auditStatus || undefined
    });
  } finally {
    loading.value = false;
  }
}

async function viewDetail(id) {
  detail.value = await getServiceDetail(id);
  detailVisible.value = true;
}

async function audit(row, action) {
  let reason = "";
  if (action !== "APPROVE") {
    try {
      const res = await ElMessageBox.prompt(action === "RETURN" ? "请输入驳回原因" : "请输入拒绝原因", "审核说明", {
        confirmButtonText: "确认",
        cancelButtonText: "取消",
        inputPattern: /^.{2,255}$/,
        inputErrorMessage: "原因至少2个字符"
      });
      reason = res.value;
    } catch (e) {
      return;
    }
  } else {
    try {
      await ElMessageBox.confirm(`确认通过服务“${row.name}”的入驻审核？`, "审核确认", { type: "warning" });
    } catch (e) {
      return;
    }
  }

  await auditServiceEntry(row.id, {
    action,
    reason: reason || undefined
  });
  ElMessage.success("审核处理成功");
  await fetchList();
  if (detailVisible.value && detail.value && detail.value.id === row.id) {
    detail.value = await getServiceDetail(row.id);
  }
}

function auditType(status) {
  if (status === "APPROVED") return "success";
  if (status === "PENDING") return "warning";
  if (status === "REJECTED") return "danger";
  return "info";
}
</script>

<style scoped>
.audit-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.tools {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>

