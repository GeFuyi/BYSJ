<template>
  <el-row :gutter="16">
    <el-col :xs="24" :lg="10">
      <el-card shadow="never">
        <template #header>
          <span style="font-weight: 600">提交物业报修</span>
        </template>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
          <el-form-item label="报修标题" prop="title">
            <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="如：3号楼2单元电梯故障" />
          </el-form-item>
          <el-form-item label="问题描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              maxlength="2000"
              show-word-limit
              placeholder="请描述故障现象、发生时间、具体位置等"
            />
          </el-form-item>
          <el-form-item label="联系电话" prop="contactPhone">
            <el-input v-model="form.contactPhone" placeholder="默认取当前账号手机号" />
          </el-form-item>
          <el-form-item label="问题图片">
            <el-upload
              list-type="picture-card"
              :limit="6"
              :http-request="handleUpload"
              :on-remove="handleRemove"
              :file-list="uploadFileList"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交报修</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>

    <el-col :xs="24" :lg="14">
      <el-card shadow="never">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap">
            <span style="font-weight: 600">报修工单进度</span>
            <div style="display: flex; gap: 8px; align-items: center">
              <el-select v-model="query.status" clearable placeholder="按状态筛选" style="width: 180px">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <el-button @click="fetchOrders">查询</el-button>
            </div>
          </div>
        </template>

        <el-table :data="tableData" v-loading="loading" border>
          <el-table-column prop="id" label="工单ID" width="90" />
          <el-table-column prop="title" label="报修标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="statusLabel" label="状态" width="150">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ row.statusLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="userNickname" label="报修人" min-width="110">
            <template #default="{ row }">
              {{ row.userNickname || row.username }}
            </template>
          </el-table-column>
          <el-table-column prop="handlerName" label="处理人" min-width="110" />
          <el-table-column prop="updatedAt" label="更新时间" min-width="170" />
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>

              <el-button
                v-if="canTake(row)"
                link
                type="primary"
                @click="changeStatus(row, 'ACCEPTED')"
              >
                接单
              </el-button>
              <el-button
                v-if="canStartRepair(row)"
                link
                type="warning"
                @click="changeStatus(row, 'IN_PROGRESS')"
              >
                维修中
              </el-button>
              <el-button
                v-if="canFinishPending(row)"
                link
                type="success"
                @click="changeStatus(row, 'COMPLETED_PENDING_CONFIRM')"
              >
                待确认
              </el-button>
              <el-button
                v-if="canConfirmDone(row)"
                link
                type="success"
                @click="changeStatus(row, 'COMPLETED')"
              >
                确认完成
              </el-button>
              <el-button
                v-if="canRollback(row)"
                link
                type="danger"
                @click="changeStatus(row, 'ROLLBACK', true)"
              >
                异常回退
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
  </el-row>

  <el-dialog v-model="detailVisible" title="报修工单详情" width="780px" destroy-on-close>
    <div v-if="detail">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工单ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.statusLabel }}</el-descriptions-item>
        <el-descriptions-item label="报修人">{{ detail.userNickname || detail.username }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ detail.handlerName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.contactPhone || "-" }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updatedAt }}</el-descriptions-item>
        <el-descriptions-item :span="2" label="报修标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item :span="2" label="问题描述">{{ detail.description }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-top: 14px">
        <div style="font-weight: 600; margin-bottom: 8px">问题图片</div>
        <el-empty v-if="!detail.imagePaths || detail.imagePaths.length === 0" description="未上传图片" :image-size="60" />
        <div v-else style="display: flex; flex-wrap: wrap; gap: 10px">
          <el-image
            v-for="path in detail.imagePaths"
            :key="path"
            :src="fileUrl(path)"
            style="width: 100px; height: 100px; border-radius: 6px"
            fit="cover"
            :preview-src-list="detail.imagePaths.map(fileUrl)"
            preview-teleported
          />
        </div>
      </div>

      <div style="margin-top: 18px">
        <div style="font-weight: 600; margin-bottom: 8px">流转记录</div>
        <el-timeline>
          <el-timeline-item
            v-for="item in detail.flows || []"
            :key="item.id"
            :timestamp="item.createdAt"
            placement="top"
          >
            <div style="font-weight: 600">{{ item.fromStatusLabel }} -> {{ item.toStatusLabel }}</div>
            <div style="margin-top: 4px; color: #606266">
              操作人：{{ item.operatorName }}（{{ item.operatorRole }}）
            </div>
            <div style="margin-top: 4px; color: #909399">{{ item.remark || "-" }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import {
  createRepairOrder,
  getRepairOrderDetail,
  listRepairOrders,
  repairImageUrl,
  updateRepairOrderStatus,
  uploadRepairImage
} from "../api/repair";

const currentUser = (() => {
  const raw = sessionStorage.getItem("userInfo");
  if (!raw) return {};
  try {
    return JSON.parse(raw);
  } catch (e) {
    return {};
  }
})();

const isEmployee = currentUser.role === "ADMIN" || currentUser.role === "EMPLOYEE";

const loading = ref(false);
const submitLoading = ref(false);
const tableData = ref([]);
const query = reactive({
  status: ""
});

const formRef = ref(null);
const form = reactive({
  title: "",
  description: "",
  contactPhone: ""
});
const rules = {
  title: [{ required: true, message: "请输入报修标题", trigger: "blur" }],
  description: [{ required: true, message: "请输入问题描述", trigger: "blur" }],
  contactPhone: [{ pattern: /^\d{11}$/, message: "联系电话格式不正确", trigger: "blur" }]
};

const uploadedPaths = ref([]);
const uploadFileList = ref([]);

const detailVisible = ref(false);
const detail = ref(null);

const statusOptions = [
  { value: "SUBMITTED", label: "用户提交报修" },
  { value: "ACCEPTED", label: "物业接单" },
  { value: "IN_PROGRESS", label: "维修中" },
  { value: "COMPLETED_PENDING_CONFIRM", label: "维修完成待确认" },
  { value: "COMPLETED", label: "维修完成（用户确认）" },
  { value: "ROLLBACK", label: "异常回退" }
];

onMounted(() => {
  fetchOrders();
});

async function fetchOrders() {
  loading.value = true;
  try {
    tableData.value = await listRepairOrders({
      status: query.status || undefined
    });
  } finally {
    loading.value = false;
  }
}

async function handleUpload(option) {
  const data = new FormData();
  data.append("file", option.file);
  try {
    const res = await uploadRepairImage(data);
    uploadedPaths.value.push(res.path);
    uploadFileList.value.push({
      name: option.file.name,
      url: res.url,
      path: res.path
    });
    option.onSuccess(res);
  } catch (error) {
    option.onError(error);
  }
}

function handleRemove(file) {
  uploadedPaths.value = uploadedPaths.value.filter((item) => item !== file.path);
  uploadFileList.value = uploadFileList.value.filter((item) => item.path !== file.path);
}

async function handleSubmit() {
  await formRef.value.validate();
  submitLoading.value = true;
  try {
    await createRepairOrder({
      title: form.title,
      description: form.description,
      contactPhone: form.contactPhone || undefined,
      imagePaths: uploadedPaths.value
    });
    ElMessage.success("报修提交成功");
    resetForm();
    await fetchOrders();
  } finally {
    submitLoading.value = false;
  }
}

function resetForm() {
  form.title = "";
  form.description = "";
  form.contactPhone = "";
  uploadedPaths.value = [];
  uploadFileList.value = [];
  formRef.value?.clearValidate();
}

async function openDetail(id) {
  detail.value = await getRepairOrderDetail(id);
  detailVisible.value = true;
}

async function changeStatus(row, targetStatus, needRemark = false) {
  let remark = "";
  if (needRemark) {
    try {
      const result = await ElMessageBox.prompt("请输入异常说明", "异常回退", {
        confirmButtonText: "确认",
        cancelButtonText: "取消",
        inputPlaceholder: "例如：配件短缺，需重新预约",
        inputPattern: /^.{2,255}$/,
        inputErrorMessage: "备注至少2个字符"
      });
      remark = result.value;
    } catch (e) {
      return;
    }
  }

  if (!needRemark) {
    try {
      await ElMessageBox.confirm("确认执行该状态变更吗？", "提示", { type: "warning" });
    } catch (e) {
      return;
    }
  }

  await updateRepairOrderStatus(row.id, {
    targetStatus,
    remark: remark || undefined
  });
  ElMessage.success("状态更新成功");
  await fetchOrders();
  if (detailVisible.value && detail.value && detail.value.id === row.id) {
    detail.value = await getRepairOrderDetail(row.id);
  }
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
  return currentUser.role === "ADMIN" || row.userId === currentUser.id;
}

function canRollback(row) {
  if (!isEmployee) return false;
  return row.status === "ACCEPTED" || row.status === "IN_PROGRESS" || row.status === "COMPLETED_PENDING_CONFIRM";
}

function statusTagType(status) {
  if (status === "SUBMITTED") return "info";
  if (status === "ACCEPTED") return "warning";
  if (status === "IN_PROGRESS") return "primary";
  if (status === "COMPLETED_PENDING_CONFIRM") return "success";
  if (status === "COMPLETED") return "success";
  if (status === "ROLLBACK") return "danger";
  return "info";
}

function fileUrl(path) {
  return repairImageUrl(path);
}
</script>

