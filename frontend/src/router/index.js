import { createRouter, createWebHistory } from "vue-router";
import Login from "../views/Login.vue";
import Register from "../views/Register.vue";
import Home from "../views/Home.vue";
import Dashboard from "../views/Dashboard.vue";
import UserManagement from "../views/UserManagement.vue";

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
  next();
});

export default router;
