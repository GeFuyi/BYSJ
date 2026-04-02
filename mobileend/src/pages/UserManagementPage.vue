<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button default-href="/tabs/dashboard" />
        </ion-buttons>
        <ion-title>人员管理</ion-title>
        <ion-buttons slot="end">
          <ion-button fill="clear" size="small" @click="openCreate">新增</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>
    <ion-content fullscreen class="ion-padding">
      <ion-card v-for="item in list" :key="item.id">
        <ion-card-header>
          <ion-card-title>{{ item.nickname || item.username }}</ion-card-title>
          <ion-card-subtitle>{{ item.username }} · {{ item.phone || "-" }}</ion-card-subtitle>
        </ion-card-header>
        <ion-card-content>
          <p>角色：{{ item.role }}</p>
          <p>状态：{{ item.status === 1 ? "启用" : "禁用" }}</p>
          <p>创建时间：{{ item.createdAt }}</p>
          <div class="btn-row">
            <ion-button size="small" fill="outline" @click="openEdit(item)">编辑</ion-button>
            <ion-button size="small" color="danger" @click="confirmDelete(item)">删除</ion-button>
          </div>
        </ion-card-content>
      </ion-card>

      <ion-modal :is-open="formModalOpen" @didDismiss="formModalOpen = false">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ isEdit ? "编辑人员" : "新增人员" }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="formModalOpen = false">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>
        <ion-content class="ion-padding">
          <ion-item>
            <ion-input v-model="form.username" label="用户名" label-placement="stacked" :disabled="isEdit" />
          </ion-item>
          <ion-item>
            <ion-input
              v-model="form.password"
              type="password"
              label="密码"
              label-placement="stacked"
              :placeholder="isEdit ? '留空则不修改' : '请输入密码'"
            />
          </ion-item>
          <ion-item>
            <ion-input v-model="form.phone" label="手机号" label-placement="stacked" />
          </ion-item>
          <ion-item>
            <ion-input v-model="form.nickname" label="昵称" label-placement="stacked" />
          </ion-item>
          <ion-item>
            <ion-select v-model="form.role" label="角色" label-placement="stacked" interface="popover">
              <ion-select-option value="ADMIN">管理员</ion-select-option>
              <ion-select-option value="EMPLOYEE">员工</ion-select-option>
              <ion-select-option value="USER">用户</ion-select-option>
            </ion-select>
          </ion-item>
          <ion-item>
            <ion-select v-model="form.status" label="状态" label-placement="stacked" interface="popover">
              <ion-select-option :value="1">启用</ion-select-option>
              <ion-select-option :value="0">禁用</ion-select-option>
            </ion-select>
          </ion-item>
          <ion-button expand="block" @click="submitForm">保存</ion-button>
        </ion-content>
      </ion-modal>

      <ion-alert
        :is-open="deleteAlertOpen"
        header="删除确认"
        :message="`确认删除用户 ${deleteTarget?.username || ''} 吗？`"
        :buttons="deleteButtons"
        @didDismiss="deleteAlertOpen = false"
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
import { reactive, ref } from "vue";
import { createUser, deleteUser, listUsers, updateUser } from "../api/user";
import { presentToast } from "../utils/toast";

const list = ref([]);
const formModalOpen = ref(false);
const isEdit = ref(false);
const editId = ref(null);

const deleteAlertOpen = ref(false);
const deleteTarget = ref(null);

const form = reactive({
  username: "",
  password: "",
  phone: "",
  nickname: "",
  role: "USER",
  status: 1
});

const deleteButtons = [
  {
    text: "取消",
    role: "cancel"
  },
  {
    text: "删除",
    role: "destructive",
    handler: async () => {
      if (!deleteTarget.value) return;
      await deleteUser(deleteTarget.value.id);
      await presentToast("删除成功", "success");
      deleteTarget.value = null;
      await fetchUsers();
    }
  }
];

fetchUsers();

async function fetchUsers() {
  list.value = await listUsers();
}

function resetForm() {
  form.username = "";
  form.password = "";
  form.phone = "";
  form.nickname = "";
  form.role = "USER";
  form.status = 1;
}

function openCreate() {
  resetForm();
  isEdit.value = false;
  editId.value = null;
  formModalOpen.value = true;
}

function openEdit(user) {
  isEdit.value = true;
  editId.value = user.id;
  form.username = user.username;
  form.password = "";
  form.phone = user.phone || "";
  form.nickname = user.nickname || "";
  form.role = user.role;
  form.status = user.status;
  formModalOpen.value = true;
}

function confirmDelete(user) {
  deleteTarget.value = user;
  deleteAlertOpen.value = true;
}

async function submitForm() {
  if (!form.username.trim() || (!isEdit.value && !form.password)) {
    await presentToast("请填写用户名和密码", "warning");
    return;
  }
  if (form.password && (form.password.length < 6 || form.password.length > 32)) {
    await presentToast("密码长度需在6-32位", "warning");
    return;
  }
  if (form.phone && !/^\d{11}$/.test(form.phone)) {
    await presentToast("手机号格式不正确", "warning");
    return;
  }
  const payload = {
    username: form.username,
    password: form.password || undefined,
    phone: form.phone || undefined,
    nickname: form.nickname || undefined,
    role: form.role,
    status: form.status
  };
  if (isEdit.value) {
    await updateUser(editId.value, payload);
    await presentToast("更新成功", "success");
  } else {
    await createUser(payload);
    await presentToast("新增成功", "success");
  }
  formModalOpen.value = false;
  await fetchUsers();
}
</script>

<style scoped>
.btn-row {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
