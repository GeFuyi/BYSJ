import { createRouter, createWebHistory } from "vue-router";
import Login from "../views/Login.vue";
import Register from "../views/Register.vue";
import Home from "../views/Home.vue";
import Dashboard from "../views/Dashboard.vue";
import UserManagement from "../views/UserManagement.vue";
import RepairOrder from "../views/RepairOrder.vue";
import ServiceMarket from "../views/ServiceMarket.vue";
import ServiceProvider from "../views/ServiceProvider.vue";
import ServiceAudit from "../views/ServiceAudit.vue";
import ElectricityPayment from "../views/ElectricityPayment.vue";
import Moments from "../views/Moments.vue";
import Chats from "../views/Chats.vue";
import ChatRoom from "../views/ChatRoom.vue";
import Profile from "../views/Profile.vue";

const routes = [
  {
    path: "/",
    redirect: "/home/dashboard"
  },
  {
    path: "/login",
    component: Login
  },
  {
    path: "/register",
    component: Register
  },
  {
    path: "/home",
    component: Home,
    redirect: "/home/dashboard",
    children: [
      {
        path: "dashboard",
        component: Dashboard,
        meta: { title: "首页" }
      },
      {
        path: "services",
        component: ServiceMarket,
        meta: { title: "便民服务" }
      },
      {
        path: "service-provider",
        component: ServiceProvider,
        meta: { title: "服务入驻" }
      },
      {
        path: "service-audit",
        component: ServiceAudit,
        meta: { title: "入驻审核" }
      },
      {
        path: "repair",
        component: RepairOrder,
        meta: { title: "物业报修" }
      },
      {
        path: "electricity",
        component: ElectricityPayment,
        meta: { title: "电费代缴" }
      },
      {
        path: "social",
        redirect: "/home/moments"
      },
      {
        path: "moments",
        component: Moments,
        meta: { title: "邻里圈" }
      },
      {
        path: "chats",
        component: Chats,
        meta: { title: "聊天" }
      },
      {
        path: "chat/:chatType/:targetId",
        component: ChatRoom,
        meta: { title: "聊天室" }
      },
      {
        path: "profile",
        component: Profile,
        meta: { title: "我的" }
      },
      {
        path: "users",
        component: UserManagement,
        meta: { title: "人员管理" }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to, from, next) => {
  const token = sessionStorage.getItem("token");
  const userInfoStr = sessionStorage.getItem("userInfo");
  let role = "";
  if (userInfoStr) {
    try {
      role = JSON.parse(userInfoStr).role;
    } catch (e) {
      sessionStorage.removeItem("userInfo");
    }
  }

  if ((to.path === "/login" || to.path === "/register") && token) {
    next("/home/dashboard");
    return;
  }
  if (to.path.startsWith("/home") && !token) {
    next("/login");
    return;
  }
  if (to.path === "/home/users" && !(role === "ADMIN" || role === "EMPLOYEE")) {
    next("/home/dashboard");
    return;
  }
  if (to.path === "/home/service-provider" && !(role === "ADMIN" || role === "EMPLOYEE")) {
    next("/home/services");
    return;
  }
  if (to.path === "/home/service-audit" && role !== "ADMIN") {
    next("/home/services");
    return;
  }
  if (to.path === "/home/electricity" && !(role === "USER" || role === "ADMIN")) {
    next("/home/dashboard");
    return;
  }
  next();
});

export default router;
