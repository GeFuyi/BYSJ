import { createRouter, createWebHistory } from "@ionic/vue-router";
import { getUserInfo, isLoggedIn } from "../composables/useAuth";

const routes = [
  {
    path: "/",
    redirect: "/tabs/dashboard"
  },
  {
    path: "/login",
    component: () => import("../pages/LoginPage.vue")
  },
  {
    path: "/register",
    component: () => import("../pages/RegisterPage.vue")
  },
  {
    path: "/tabs",
    component: () => import("../pages/TabsPage.vue"),
    children: [
      {
        path: "",
        redirect: "/tabs/dashboard"
      },
      {
        path: "dashboard",
        component: () => import("../pages/DashboardPage.vue")
      },
      {
        path: "services",
        component: () => import("../pages/ServicesPage.vue")
      },
      {
        path: "repair",
        component: () => import("../pages/RepairPage.vue")
      },
      {
        path: "moments",
        component: () => import("../pages/MomentsPage.vue")
      },
      {
        path: "chats",
        component: () => import("../pages/ChatsPage.vue")
      },
      {
        path: "chat/:chatType/:targetId",
        component: () => import("../pages/ChatRoomPage.vue")
      },
      {
        path: "electricity",
        component: () => import("../pages/ElectricityPage.vue"),
        meta: {
          roles: ["USER", "ADMIN"]
        }
      },
      {
        path: "service-provider",
        component: () => import("../pages/ServiceProviderPage.vue"),
        meta: {
          roles: ["ADMIN", "EMPLOYEE"]
        }
      },
      {
        path: "service-audit",
        component: () => import("../pages/ServiceAuditPage.vue"),
        meta: {
          roles: ["ADMIN"]
        }
      },
      {
        path: "users",
        component: () => import("../pages/UserManagementPage.vue"),
        meta: {
          roles: ["ADMIN", "EMPLOYEE"]
        }
      },
      {
        path: "profile",
        component: () => import("../pages/ProfilePage.vue")
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
});

router.beforeEach((to, from, next) => {
  const logged = isLoggedIn();
  const user = getUserInfo();
  const role = user?.role || "";
  const authPages = ["/login", "/register"];
  if (!logged && !authPages.includes(to.path)) {
    next("/login");
    return;
  }
  if (logged && authPages.includes(to.path)) {
    next("/tabs/dashboard");
    return;
  }
  const allowedRoles = to.meta?.roles;
  if (Array.isArray(allowedRoles) && allowedRoles.length > 0 && !allowedRoles.includes(role)) {
    next("/tabs/dashboard");
    return;
  }
  next();
});

export default router;
