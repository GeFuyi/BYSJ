<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span style="font-weight: 600">人员管理</span>
        <el-button type="primary" @click="openCreate">新增人员</el-button>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column type="index" width="60" label="#" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="phone" label="手机号" min-width="140" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="role" label="角色" width="120">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'EMPLOYEE' ? 'warning' : 'info'">
            {{ row.role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? "启用" : "禁用" }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="180" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog :title="isEdit ? '编辑人员' : '新增人员'" v-model="dialogVisible" width="520px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" :disabled="isEdit" />
      </el-form-item>
      <el-form-item :label="isEdit ? '重置密码' : '密码'" prop="password">
        <el-input v-model="form.password" type="password" show-password :placeholder="isEdit ? '留空则不修改' : ''" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="默认 15138114047" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" />
      </el-form-item>
      <el-form-item label="角色" prop="role">
        <el-select v-model="form.role" style="width: 100%">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="员工" value="EMPLOYEE" />
          <el-option label="用户" value="USER" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="form.status" style="width: 100%">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { createUser, deleteUser, listUsers, updateUser } from "../api/user";

const loading = ref(false);
const tableData = ref([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const currentId = ref(null);
const formRef = ref(null);
const form = reactive({
  username: "",
  password: "",
  phone: "",
  nickname: "",
  role: "USER",
  status: 1
});

const rules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 4, max: 20, message: "用户名长度需在4-20位", trigger: "blur" }
  ],
  password: [
    {
      validator: (_, value, callback) => {
        if (!isEdit.value && !value) {
          callback(new Error("请输入密码"));
          return;
        }
        if (value && (value.length < 6 || value.length > 32)) {
          callback(new Error("密码长度需在6-32位"));
          return;
        }
        callback();
      },
      trigger: "blur"
    }
  ],
  phone: [{ pattern: /^\d{11}$/, message: "手机号格式不正确", trigger: "blur" }],
  role: [{ required: true, message: "请选择角色", trigger: "change" }]
};

onMounted(() => {
  fetchUsers();
});

async function fetchUsers() {
  loading.value = true;
  try {
    tableData.value = await listUsers();
  } finally {
    loading.value = false;
  }
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
  currentId.value = null;
  dialogVisible.value = true;
}

function openEdit(row) {
  isEdit.value = true;
  currentId.value = row.id;
  form.username = row.username;
  form.password = "";
  form.phone = row.phone || "";
  form.nickname = row.nickname;
  form.role = row.role;
  form.status = row.status;
  dialogVisible.value = true;
}

async function handleSubmit() {
  await formRef.value.validate();
  const payload = {
    username: form.username,
    password: form.password || undefined,
    phone: form.phone || undefined,
    nickname: form.nickname,
    role: form.role,
    status: form.status
  };
  if (isEdit.value) {
    await updateUser(currentId.value, payload);
    ElMessage.success("更新成功");
  } else {
    await createUser(payload);
    ElMessage.success("新增成功");
  }
  dialogVisible.value = false;
  await fetchUsers();
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除用户 ${row.username} 吗？`, "提示", { type: "warning" });
  await deleteUser(row.id);
  ElMessage.success("删除成功");
  await fetchUsers();
}
</script>
