<template>
  <div class="service-page">
    <div class="hero">
      <div>
        <h2>社区便民服务广场</h2>
        <p>一站式查看家政、维修、照护等服务信息，直接联系服务方，无交易环节。</p>
      </div>
      <el-tag type="success" effect="dark">信息平台 · 非交易</el-tag>
    </div>

    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-input
          v-model="query.keyword"
          placeholder="搜索服务名称/关键字"
          clearable
          style="max-width: 300px"
          @keyup.enter="fetchServices"
        />
        <el-select v-model="query.categoryCode" clearable placeholder="分类" style="width: 180px">
          <el-option v-for="item in categories" :key="item.code" :label="item.name" :value="item.code" />
        </el-select>
        <el-select v-model="query.serviceStatus" clearable placeholder="服务状态" style="width: 160px">
          <el-option label="可预约" value="RESERVABLE" />
          <el-option label="约满" value="FULL" />
          <el-option label="进行中" value="IN_SERVICE" />
          <el-option label="暂停" value="PAUSED" />
        </el-select>
        <el-button type="primary" @click="fetchServices">查询</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" style="margin-top: 4px">
      <el-col v-for="item in services" :key="item.id" :xs="24" :sm="12" :lg="8">
        <el-card class="service-card" shadow="hover">
          <div class="cover-wrap">
            <img :src="item.coverImagePath ? serviceImageUrl(item.coverImagePath) : fallbackCover" alt="cover" class="cover-img" />
            <el-tag class="status-tag" :type="statusType(item.serviceStatus)">{{ item.serviceStatusLabel }}</el-tag>
          </div>
          <div class="service-title">{{ item.name }}</div>
          <div class="meta-row">
            <el-tag size="small" type="info">{{ item.categoryName }}</el-tag>
            <span class="score">★ {{ Number(item.avgScore || 0).toFixed(1) }} ({{ item.scoreCount || 0 }})</span>
          </div>
          <div class="summary">{{ item.summary }}</div>
          <div class="contact">联系人：{{ item.contactName }} / {{ item.contactPhone }}</div>
          <div class="action-row">
            <el-button link type="primary" @click="openDetail(item.id)">查看详情</el-button>
            <el-button
              v-if="canBook(item)"
              type="primary"
              size="small"
              @click="openBooking(item)"
            >
              立即预约
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="services.length === 0 && !loading" description="暂无可展示的服务" style="margin-top: 20px" />

    <el-card v-if="isResident" shadow="never" style="margin-top: 18px">
      <template #header>
        <div class="section-title">我的预约</div>
      </template>
      <el-table :data="myBookings" border size="small">
        <el-table-column prop="serviceName" label="服务名称" min-width="180" />
        <el-table-column prop="contactName" label="联系人" min-width="120" />
        <el-table-column prop="contactPhone" label="联系电话" min-width="130" />
        <el-table-column prop="statusLabel" label="状态" width="100" />
        <el-table-column prop="createdAt" label="预约时间" min-width="160" />
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" title="服务详情" width="860px" destroy-on-close>
      <div v-if="detail">
        <div class="detail-header">
          <div>
            <h3>{{ detail.name }}</h3>
            <p>{{ detail.summary }}</p>
          </div>
          <el-tag :type="statusType(detail.serviceStatus)">{{ detail.serviceStatusLabel }}</el-tag>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="分类">{{ detail.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="评分">★ {{ Number(detail.avgScore || 0).toFixed(1) }}（{{ detail.scoreCount || 0 }}条）</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detail.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="服务地址">{{ detail.address || "-" }}</el-descriptions-item>
          <el-descriptions-item label="预约情况">{{ detail.currentBooked }}/{{ detail.maxCapacity }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="详情介绍">{{ detail.description }}</el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: 14px">
          <div class="section-title">服务图片</div>
          <el-empty v-if="!detail.imagePaths || detail.imagePaths.length === 0" description="暂无图片" :image-size="60" />
          <div v-else class="gallery">
            <el-image
              v-for="path in detail.imagePaths"
              :key="path"
              :src="serviceImageUrl(path)"
              :preview-src-list="detail.imagePaths.map(serviceImageUrl)"
              preview-teleported
              fit="cover"
              class="gallery-item"
            />
          </div>
        </div>

        <div style="margin-top: 16px">
          <div class="section-title">居民评价</div>
          <el-empty v-if="!detail.reviews || detail.reviews.length === 0" description="暂无评价" :image-size="60" />
          <el-timeline v-else>
            <el-timeline-item
              v-for="review in detail.reviews"
              :key="review.id"
              :timestamp="review.createdAt"
              placement="top"
            >
              <div style="font-weight: 600">{{ review.reviewerName }} · {{ review.rating }}分</div>
              <div style="color: #606266; margin-top: 4px">{{ review.content || "未填写文字评价" }}</div>
            </el-timeline-item>
          </el-timeline>
        </div>

        <div v-if="isResident" class="review-box">
          <div class="section-title">发表评价</div>
          <el-form inline>
            <el-form-item label="评分">
              <el-rate v-model="reviewForm.rating" />
            </el-form-item>
          </el-form>
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="写下你的体验（可选）"
          />
          <div style="margin-top: 8px">
            <el-button type="primary" @click="submitReview">提交评价</el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="bookingVisible" title="预约服务" width="500px" destroy-on-close>
      <el-form ref="bookingFormRef" :model="bookingForm" :rules="bookingRules" label-width="90px">
        <el-form-item label="服务名称">
          <span>{{ bookingTarget?.name || "-" }}</span>
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="bookingForm.contactName" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="bookingForm.contactPhone" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="bookingForm.remark" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bookingVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBooking">确认预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  createServiceBooking,
  getServiceDetail,
  listMyServiceBookings,
  listPublishedServices,
  listServiceCategories,
  serviceImageUrl,
  submitServiceReview
} from "../api/service";

const fallbackCover =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='640' height='420'%3E%3Crect width='100%25' height='100%25' fill='%23e7effa'/%3E%3Ctext x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' fill='%23607d9c' font-size='26'%3E%E7%A4%BE%E5%8C%BA%E4%BE%BF%E6%B0%91%E6%9C%8D%E5%8A%A1%3C/text%3E%3C/svg%3E";

const userInfo = (() => {
  const raw = sessionStorage.getItem("userInfo");
  if (!raw) return {};
  try {
    return JSON.parse(raw);
  } catch (e) {
    return {};
  }
})();

const isResident = userInfo.role === "USER" || userInfo.role === "ADMIN";

const loading = ref(false);
const categories = ref([]);
const services = ref([]);
const myBookings = ref([]);
const query = reactive({
  keyword: "",
  categoryCode: "",
  serviceStatus: ""
});

const detailVisible = ref(false);
const detail = ref(null);
const reviewForm = reactive({
  rating: 5,
  content: ""
});

const bookingVisible = ref(false);
const bookingTarget = ref(null);
const bookingFormRef = ref(null);
const bookingForm = reactive({
  contactName: userInfo.nickname || userInfo.username || "",
  contactPhone: userInfo.phone || "",
  remark: ""
});
const bookingRules = {
  contactName: [{ required: true, message: "请输入联系人", trigger: "blur" }],
  contactPhone: [
    { required: true, message: "请输入联系电话", trigger: "blur" },
    { pattern: /^\d{11}$/, message: "联系电话格式不正确", trigger: "blur" }
  ]
};

onMounted(async () => {
  await Promise.all([fetchCategories(), fetchServices()]);
  if (isResident) {
    await fetchMyBookings();
  }
});

async function fetchCategories() {
  categories.value = await listServiceCategories();
}

async function fetchServices() {
  loading.value = true;
  try {
    services.value = await listPublishedServices({
      keyword: query.keyword || undefined,
      categoryCode: query.categoryCode || undefined,
      serviceStatus: query.serviceStatus || undefined
    });
  } finally {
    loading.value = false;
  }
}

async function fetchMyBookings() {
  myBookings.value = await listMyServiceBookings();
}

function canBook(item) {
  return isResident && item.serviceStatus === "RESERVABLE";
}

function statusType(status) {
  if (status === "RESERVABLE") return "success";
  if (status === "FULL") return "danger";
  if (status === "IN_SERVICE") return "warning";
  return "info";
}

async function openDetail(id) {
  detail.value = await getServiceDetail(id);
  reviewForm.rating = 5;
  reviewForm.content = "";
  detailVisible.value = true;
}

function openBooking(item) {
  bookingTarget.value = item;
  bookingForm.contactName = userInfo.nickname || userInfo.username || "";
  bookingForm.contactPhone = userInfo.phone || "";
  bookingForm.remark = "";
  bookingVisible.value = true;
}

async function submitBooking() {
  await bookingFormRef.value.validate();
  if (!bookingTarget.value) return;
  await createServiceBooking(bookingTarget.value.id, {
    contactName: bookingForm.contactName,
    contactPhone: bookingForm.contactPhone,
    remark: bookingForm.remark || undefined
  });
  ElMessage.success("预约成功");
  bookingVisible.value = false;
  await Promise.all([fetchServices(), fetchMyBookings()]);
}

async function submitReview() {
  if (!detail.value) return;
  if (!reviewForm.rating) {
    ElMessage.warning("请选择评分");
    return;
  }
  await submitServiceReview(detail.value.id, {
    rating: reviewForm.rating,
    content: reviewForm.content || undefined
  });
  ElMessage.success("评价提交成功");
  detail.value = await getServiceDetail(detail.value.id);
  reviewForm.content = "";
}
</script>

<style scoped>
.service-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero {
  border-radius: 14px;
  padding: 16px 18px;
  background: linear-gradient(125deg, #0f4da1 0%, #0e7ac7 48%, #34a5d6 100%);
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

.filter-card {
  border-radius: 12px;
}

.filter-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.service-card {
  margin-bottom: 14px;
  border-radius: 12px;
}

.cover-wrap {
  position: relative;
}

.cover-img {
  width: 100%;
  height: 170px;
  border-radius: 10px;
  object-fit: cover;
}

.status-tag {
  position: absolute;
  right: 10px;
  top: 10px;
}

.service-title {
  margin-top: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2d3d;
}

.meta-row {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.score {
  color: #e6a23c;
  font-weight: 600;
}

.summary {
  margin-top: 8px;
  color: #606266;
  min-height: 42px;
}

.contact {
  margin-top: 10px;
  color: #303133;
}

.action-row {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2d3d;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  margin-bottom: 10px;
}

.detail-header h3 {
  margin: 0;
  font-size: 22px;
}

.detail-header p {
  margin: 6px 0 0;
  color: #606266;
}

.gallery {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.gallery-item {
  width: 112px;
  height: 112px;
  border-radius: 8px;
}

.review-box {
  margin-top: 16px;
  padding: 12px;
  border-radius: 10px;
  background: #f8fbff;
  border: 1px solid #e5eef8;
}
</style>

