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
        component: Dashboard
      },
      {
        path: "services",
        component: ServiceMarket
      },
      {
        path: "service-provider",
        component: ServiceProvider
      },
      {
        path: "service-audit",
        component: ServiceAudit
      },
      {
        path: "repair",
        component: RepairOrder
      },
      {
        path: "users",
        component: UserManagement
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
  next();
});

export default router;
