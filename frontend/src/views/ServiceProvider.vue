<template>
  <div class="provider-page">
    <el-row :gutter="16">
      <el-col :xs="24" :xl="11">
        <el-card shadow="never">
          <template #header>
            <div class="title-row">
              <span>{{ isEdit ? "编辑服务信息" : "服务入驻申请" }}</span>
              <el-tag type="warning" effect="plain">提交后需管理员审核</el-tag>
            </div>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="95px">
            <el-form-item label="服务名称" prop="name">
              <el-input v-model="form.name" maxlength="100" show-word-limit />
            </el-form-item>
            <el-form-item label="服务分类" prop="categoryCode">
              <el-select v-model="form.categoryCode" style="width: 100%">
                <el-option v-for="item in categories" :key="item.code" :label="item.name" :value="item.code" />
              </el-select>
            </el-form-item>
            <el-form-item label="服务简介" prop="summary">
              <el-input v-model="form.summary" maxlength="255" show-word-limit />
            </el-form-item>
            <el-form-item label="详情介绍" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="4" maxlength="5000" show-word-limit />
            </el-form-item>
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" />
            </el-form-item>
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" />
            </el-form-item>
            <el-form-item label="服务地址" prop="address">
              <el-input v-model="form.address" maxlength="255" show-word-limit />
            </el-form-item>
            <el-form-item label="可约名额" prop="maxCapacity">
              <el-input-number v-model="form.maxCapacity" :min="1" :max="9999" />
            </el-form-item>
            <el-form-item label="封面图">
              <el-upload :http-request="uploadCover" :show-file-list="false">
                <el-button>上传封面图</el-button>
              </el-upload>
              <el-image
                v-if="form.coverImagePath"
                :src="serviceImageUrl(form.coverImagePath)"
                style="width: 90px; height: 90px; margin-left: 10px; border-radius: 8px"
                fit="cover"
              />
            </el-form-item>
            <el-form-item label="服务图片">
              <el-upload
                list-type="picture-card"
                :limit="8"
                :http-request="uploadGallery"
                :file-list="galleryFiles"
                :on-remove="removeGallery"
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="submit">{{ isEdit ? "保存并重新提交审核" : "提交审核" }}</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="13">
        <el-card shadow="never">
          <template #header>
            <div class="title-row">
              <span>我的服务入驻记录</span>
              <div style="display: flex; gap: 8px">
                <el-select v-model="query.auditStatus" clearable placeholder="审核状态" style="width: 160px">
                  <el-option label="待审核" value="PENDING" />
                  <el-option label="审核通过" value="APPROVED" />
                  <el-option label="审核拒绝" value="REJECTED" />
                  <el-option label="驳回修改" value="RETURNED" />
                </el-select>
                <el-button @click="fetchList">查询</el-button>
              </div>
            </div>
          </template>

          <el-table :data="list" border v-loading="loading">
            <el-table-column prop="name" label="服务名称" min-width="180" />
            <el-table-column prop="categoryName" label="分类" width="120" />
            <el-table-column label="审核状态" width="130">
              <template #default="{ row }">
                <el-tag :type="auditType(row.auditStatus)">{{ row.auditStatusLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="服务状态" width="120">
              <template #default="{ row }">
                <el-tag :type="serviceType(row.serviceStatus)">{{ row.serviceStatusLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="currentBooked" label="预约" width="90">
              <template #default="{ row }">{{ row.currentBooked }}/{{ row.maxCapacity }}</template>
            </el-table-column>
            <el-table-column prop="updatedAt" label="更新时间" min-width="160" />
            <el-table-column label="操作" width="300" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="editRow(row)">编辑</el-button>
                <el-dropdown
                  v-if="row.auditStatus === 'APPROVED'"
                  @command="(cmd) => changeOperateStatus(row, cmd)"
                >
                  <el-button link type="success">
                    调整状态
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="RESERVABLE">可预约</el-dropdown-item>
                      <el-dropdown-item command="FULL">约满</el-dropdown-item>
                      <el-dropdown-item command="IN_SERVICE">进行中</el-dropdown-item>
                      <el-dropdown-item command="PAUSED">暂停</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-button link @click="viewDetail(row.id)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="detailVisible" title="服务详情" width="760px">
      <div v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="服务名称">{{ detail.name }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ detail.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="审核状态">{{ detail.auditStatusLabel }}</el-descriptions-item>
          <el-descriptions-item label="服务状态">{{ detail.serviceStatusLabel }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detail.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="服务地址">{{ detail.address || "-" }}</el-descriptions-item>
          <el-descriptions-item label="预约情况">{{ detail.currentBooked }}/{{ detail.maxCapacity }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="简介">{{ detail.summary }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="详情">{{ detail.description }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="审核意见">{{ detail.auditReason || "-" }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
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

const list = ref([]);
const categories = ref([]);
const loading = ref(false);
const saving = ref(false);
const isEdit = ref(false);
const editId = ref(null);
const formRef = ref(null);
const detailVisible = ref(false);
const detail = ref(null);

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

const galleryFiles = ref([]);

const rules = {
  name: [{ required: true, message: "请输入服务名称", trigger: "blur" }],
  categoryCode: [{ required: true, message: "请选择服务分类", trigger: "change" }],
  summary: [{ required: true, message: "请输入服务简介", trigger: "blur" }],
  description: [{ required: true, message: "请输入详情介绍", trigger: "blur" }],
  contactName: [{ required: true, message: "请输入联系人", trigger: "blur" }],
  contactPhone: [
    { required: true, message: "请输入联系电话", trigger: "blur" },
    { pattern: /^\d{11}$/, message: "联系电话格式不正确", trigger: "blur" }
  ]
};

onMounted(async () => {
  await Promise.all([fetchCategories(), fetchList()]);
});

async function fetchCategories() {
  categories.value = await listServiceCategories();
}

async function fetchList() {
  loading.value = true;
  try {
    list.value = await listProviderEntries({
      auditStatus: query.auditStatus || undefined
    });
  } finally {
    loading.value = false;
  }
}

async function uploadCover(option) {
  const fd = new FormData();
  fd.append("file", option.file);
  try {
    const data = await uploadServiceImage(fd);
    form.coverImagePath = data.path;
    ElMessage.success("封面上传成功");
    option.onSuccess(data);
  } catch (e) {
    option.onError(e);
  }
}

async function uploadGallery(option) {
  const fd = new FormData();
  fd.append("file", option.file);
  try {
    const data = await uploadServiceImage(fd);
    form.imagePaths.push(data.path);
    galleryFiles.value.push({
      name: option.file.name,
      path: data.path,
      url: data.url
    });
    option.onSuccess(data);
  } catch (e) {
    option.onError(e);
  }
}

function removeGallery(file) {
  form.imagePaths = form.imagePaths.filter((item) => item !== file.path);
  galleryFiles.value = galleryFiles.value.filter((item) => item.path !== file.path);
}

async function submit() {
  await formRef.value.validate();
  saving.value = true;
  try {
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
      maxCapacity: form.maxCapacity
    };
    if (isEdit.value) {
      await updateServiceEntry(editId.value, payload);
      ElMessage.success("更新成功，已重新进入待审核");
    } else {
      await createServiceEntry(payload);
      ElMessage.success("提交成功，等待管理员审核");
    }
    resetForm();
    await fetchList();
  } finally {
    saving.value = false;
  }
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
  galleryFiles.value = [];
  formRef.value?.clearValidate();
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
  galleryFiles.value = (data.imagePaths || []).map((path, idx) => ({
    name: `img_${idx + 1}.jpg`,
    path,
    url: serviceImageUrl(path)
  }));
}

async function changeOperateStatus(row, serviceStatus) {
  await updateServiceOperateStatus(row.id, { serviceStatus });
  ElMessage.success("服务状态更新成功");
  await fetchList();
}

async function viewDetail(id) {
  detail.value = await getServiceDetail(id);
  detailVisible.value = true;
}

function auditType(status) {
  if (status === "APPROVED") return "success";
  if (status === "PENDING") return "warning";
  if (status === "REJECTED") return "danger";
  return "info";
}

function serviceType(status) {
  if (status === "RESERVABLE") return "success";
  if (status === "FULL") return "danger";
  if (status === "IN_SERVICE") return "warning";
  return "info";
}
</script>

<style scoped>
.provider-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-weight: 600;
}
</style>

