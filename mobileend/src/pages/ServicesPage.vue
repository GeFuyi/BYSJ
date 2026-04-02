<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>便民服务</ion-title>
        <ion-buttons slot="end">
          <ion-button fill="clear" @click="openBookings">我的预约</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen>
      <div class="services-page ion-padding">
        <section class="top-box page-card">
          <div class="filter-grid">
            <ion-input v-model="keyword" placeholder="搜索服务关键词" />
            <ion-select v-model="categoryCode" interface="popover" placeholder="服务分类">
              <ion-select-option value="">全部分类</ion-select-option>
              <ion-select-option v-for="item in categories" :key="item.code" :value="item.code">
                {{ item.name }}
              </ion-select-option>
            </ion-select>
            <ion-select v-model="serviceStatus" interface="popover" placeholder="服务状态">
              <ion-select-option value="">全部状态</ion-select-option>
              <ion-select-option value="RESERVABLE">可预约</ion-select-option>
              <ion-select-option value="FULL">约满</ion-select-option>
              <ion-select-option value="IN_SERVICE">进行中</ion-select-option>
              <ion-select-option value="PAUSED">暂停</ion-select-option>
            </ion-select>
            <ion-button size="small" @click="loadServices">筛选</ion-button>
          </div>
        </section>

        <section class="market-list">
          <article class="service-card page-card" v-for="item in services" :key="item.id" @click="openDetail(item.id)">
            <app-image v-if="item.coverImagePath" :src="serviceImageUrl(item.coverImagePath)" class="cover" alt="cover" />
            <div class="service-main">
              <div class="top-line">
                <h3>{{ item.name }}</h3>
                <span class="status-chip">{{ item.serviceStatusLabel }}</span>
              </div>
              <p>{{ item.summary }}</p>
              <div class="provider-row">
                <user-avatar :src="item.providerAvatarPath" :name="item.providerName" :size="28" />
                <span>{{ item.providerName }}</span>
                <span>{{ item.categoryName }}</span>
              </div>
            </div>
          </article>
          <ion-text v-if="!services.length && !loadingServices" color="medium" class="empty-text">暂无可预约服务</ion-text>
        </section>

        <ion-modal :is-open="showDetail" @didDismiss="closeDetail">
          <ion-header>
            <ion-toolbar>
              <ion-title>{{ detail?.name || "服务详情" }}</ion-title>
              <ion-buttons slot="end">
                <ion-button @click="closeDetail">关闭</ion-button>
              </ion-buttons>
            </ion-toolbar>
          </ion-header>
          <ion-content class="ion-padding">
            <div v-if="detail" class="detail-wrap">
              <app-image v-if="detail.coverImagePath" :src="serviceImageUrl(detail.coverImagePath)" class="cover" alt="cover" />
              <h3>{{ detail.name }}</h3>
              <div class="provider-row detail-provider">
                <user-avatar :src="detail.providerAvatarPath" :name="detail.providerName" :size="34" />
                <span>{{ detail.providerName }}</span>
                <span>{{ detail.categoryName }}</span>
              </div>
              <p>{{ detail.summary }}</p>
              <p class="desc">{{ detail.description }}</p>
              <div>联系人：{{ detail.contactName }} {{ detail.contactPhone }}</div>

              <div class="block-title">预约服务</div>
              <ion-item>
                <ion-input v-model="bookingForm.contactName" label="联系人" label-placement="stacked" />
              </ion-item>
              <ion-item>
                <ion-input v-model="bookingForm.contactPhone" label="联系电话" label-placement="stacked" />
              </ion-item>
              <ion-item>
                <ion-textarea v-model="bookingForm.remark" label="备注" label-placement="stacked" :rows="3" />
              </ion-item>
              <ion-button expand="block" :disabled="!canBook" @click="submitBooking">
                {{ canBook ? "提交预约" : "当前角色不可预约" }}
              </ion-button>

              <div class="block-title">用户评价</div>
              <div v-if="detail.reviews && detail.reviews.length" class="review-list">
                <div v-for="review in detail.reviews" :key="review.id" class="review-item">
                  <div class="review-head">
                    <user-avatar :src="review.reviewerAvatarPath" :name="review.reviewerName" :size="28" />
                    <div>
                      <strong>{{ review.reviewerName }}</strong>
                      <small>{{ review.rating }} 分</small>
                    </div>
                  </div>
                  <p>{{ review.content || "用户未填写文字评价" }}</p>
                  <small>{{ review.createdAt }}</small>
                </div>
              </div>
              <ion-text v-else color="medium">暂无评价</ion-text>

              <div class="block-title">发布评价</div>
              <ion-item>
                <ion-select v-model.number="reviewForm.rating" label="评分" label-placement="stacked" interface="popover">
                  <ion-select-option :value="5">5分</ion-select-option>
                  <ion-select-option :value="4">4分</ion-select-option>
                  <ion-select-option :value="3">3分</ion-select-option>
                  <ion-select-option :value="2">2分</ion-select-option>
                  <ion-select-option :value="1">1分</ion-select-option>
                </ion-select>
              </ion-item>
              <ion-item>
                <ion-textarea v-model="reviewForm.content" label="评价内容" label-placement="stacked" :rows="3" maxlength="500" />
              </ion-item>
              <ion-button expand="block" :disabled="!canBook" @click="submitReview">
                {{ canBook ? "提交评价" : "当前角色不可评价" }}
              </ion-button>
            </div>
          </ion-content>
        </ion-modal>

        <ion-modal :is-open="bookingsOpen" @didDismiss="bookingsOpen = false">
          <ion-header>
            <ion-toolbar>
              <ion-title>我的预约</ion-title>
              <ion-buttons slot="end">
                <ion-button @click="bookingsOpen = false">关闭</ion-button>
              </ion-buttons>
            </ion-toolbar>
          </ion-header>
          <ion-content class="ion-padding">
            <section class="bookings-list">
              <ion-text v-if="!canBook" color="warning" class="warn-text">当前角色仅可浏览服务，无法预约</ion-text>
              <article class="booking-card page-card" v-for="item in bookings" :key="item.id">
                <div class="line">
                  <h4>{{ item.serviceName }}</h4>
                  <span class="status-chip">{{ item.statusLabel }}</span>
                </div>
                <div class="booking-user">
                  <user-avatar :src="item.userAvatarPath" :name="item.userNickname" :size="26" />
                  <span>{{ item.userNickname }}</span>
                </div>
                <p>联系人：{{ item.contactName }} {{ item.contactPhone }}</p>
                <p v-if="item.remark">备注：{{ item.remark }}</p>
                <small>预约时间：{{ item.createdAt }}</small>
              </article>
              <ion-text v-if="canBook && !bookings.length && !loadingBookings" color="medium" class="empty-text">暂无预约记录</ion-text>
            </section>
          </ion-content>
        </ion-modal>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup>
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
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
import { onMounted, reactive, ref } from "vue";
import {
  createServiceBooking,
  getServiceDetail,
  listMyServiceBookings,
  listPublishedServices,
  listServiceCategories,
  serviceImageUrl,
  submitServiceReview
} from "../api/service";
import AppImage from "../components/AppImage.vue";
import UserAvatar from "../components/UserAvatar.vue";
import { getUserInfo } from "../composables/useAuth";
import { presentToast } from "../utils/toast";

const user = getUserInfo();
const canBook = user.role === "USER" || user.role === "ADMIN";
const keyword = ref("");
const categoryCode = ref("");
const serviceStatus = ref("");
const categories = ref([]);
const services = ref([]);
const bookings = ref([]);
const loadingServices = ref(false);
const loadingBookings = ref(false);
const showDetail = ref(false);
const detail = ref(null);
const bookingsOpen = ref(false);

const bookingForm = reactive({
  contactName: user.nickname || user.username || "",
  contactPhone: user.phone || "",
  remark: ""
});
const reviewForm = reactive({
  rating: 5,
  content: ""
});

onMounted(async () => {
  await Promise.all([loadCategories(), loadServices()]);
});

async function loadCategories() {
  categories.value = await listServiceCategories();
}

async function loadServices() {
  loadingServices.value = true;
  try {
    services.value = await listPublishedServices({
      keyword: keyword.value || undefined,
      categoryCode: categoryCode.value || undefined,
      serviceStatus: serviceStatus.value || undefined
    });
  } finally {
    loadingServices.value = false;
  }
}

async function loadBookings() {
  if (!canBook) {
    bookings.value = [];
    return;
  }
  loadingBookings.value = true;
  try {
    bookings.value = await listMyServiceBookings();
  } finally {
    loadingBookings.value = false;
  }
}

async function openBookings() {
  bookingsOpen.value = true;
  await loadBookings();
}

async function openDetail(id) {
  detail.value = await getServiceDetail(id);
  reviewForm.rating = 5;
  reviewForm.content = "";
  showDetail.value = true;
}

function closeDetail() {
  showDetail.value = false;
}

async function submitBooking() {
  if (!canBook) {
    await presentToast("当前角色不可预约", "warning");
    return;
  }
  if (!detail.value) return;
  if (!bookingForm.contactName.trim() || !bookingForm.contactPhone.trim()) {
    await presentToast("请填写联系人和电话", "warning");
    return;
  }
  await createServiceBooking(detail.value.id, bookingForm);
  await presentToast("预约成功", "success");
  await loadBookings();
  closeDetail();
}

async function submitReview() {
  if (!canBook) {
    await presentToast("当前角色不可评价", "warning");
    return;
  }
  if (!detail.value) return;
  if (!reviewForm.rating) {
    await presentToast("请选择评分", "warning");
    return;
  }
  await submitServiceReview(detail.value.id, {
    rating: reviewForm.rating,
    content: reviewForm.content || undefined
  });
  await presentToast("评价提交成功", "success");
  detail.value = await getServiceDetail(detail.value.id);
  reviewForm.content = "";
}
</script>

<style scoped>
.services-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.top-box {
  padding: 10px;
}

.filter-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.filter-grid ion-input {
  grid-column: 1 / span 2;
}

.market-list,
.bookings-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.service-card {
  overflow: hidden;
}

.cover {
  width: 100%;
  height: 154px;
  object-fit: cover;
  display: block;
}

.service-main {
  padding: 10px;
}

.top-line {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}

.top-line h3 {
  margin: 0;
  font-size: 16px;
  color: #1f2a38;
}

.status-chip {
  background: #edf3ff;
  color: #2e67e3;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
}

.service-main p {
  margin: 8px 0 0;
  color: #526279;
  font-size: 13px;
}

.provider-row {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #687992;
  font-size: 12px;
}

.detail-provider {
  margin-bottom: 8px;
}

.booking-card {
  padding: 12px;
}

.booking-card .line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.booking-card h4 {
  margin: 0;
  color: #1f2a38;
}

.booking-user {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #415168;
  font-size: 13px;
}

.booking-card p {
  margin: 8px 0 0;
  color: #49596f;
  font-size: 13px;
}

.booking-card small {
  display: block;
  margin-top: 8px;
  color: #8796aa;
}

.warn-text,
.empty-text {
  display: inline-block;
  text-align: center;
}

.detail-wrap h3 {
  margin: 10px 0 4px;
}

.detail-wrap p {
  margin: 6px 0;
}

.desc {
  white-space: pre-wrap;
}

.block-title {
  margin-top: 12px;
  margin-bottom: 6px;
  font-weight: 600;
  color: #273548;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.review-item {
  padding: 8px;
  border-radius: 10px;
  background: #f7fbff;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-head strong {
  font-size: 13px;
  color: #243247;
}

.review-head small {
  color: #7e8c9f;
}

.review-item p {
  margin: 6px 0;
}

.review-item small {
  color: #7e8c9f;
}
</style>
