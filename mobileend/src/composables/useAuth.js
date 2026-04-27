import { ref } from "vue";

const TOKEN_KEY = "token";
const USER_KEY = "userInfo";

function parseUser(raw) {
  if (!raw) return {};
  try {
    return JSON.parse(raw);
  } catch (e) {
    return {};
  }
}

// 创建两个 响应式引用：
// tokenRef：保存 token，如果 localStorage 没有，默认空字符串。/
// userInfoRef：保存用户信息对象，从 localStorage 解析 JSON。
// 响应式的好处是：组件中使用 tokenRef.value 或 userInfoRef.value 时，会自动更新视图。
const tokenRef = ref(localStorage.getItem(TOKEN_KEY) || "");
const userInfoRef = ref(parseUser(localStorage.getItem(USER_KEY)));

function syncFromStorage() {
  tokenRef.value = localStorage.getItem(TOKEN_KEY) || "";
  userInfoRef.value = parseUser(localStorage.getItem(USER_KEY));
}

// 监听 storage 事件：
// 当其他标签页修改了 token 或 userInfo 时，触发该事件
// 调用 syncFromStorage() 更新当前页面的响应式状态
if (typeof window !== "undefined") {
  window.addEventListener("storage", (event) => {
    if (!event || (event.key !== TOKEN_KEY && event.key !== USER_KEY && event.key !== null)) {
      return;
    }
    syncFromStorage();
  });
}

export function getToken() {
  return tokenRef.value || "";
}
//为空则清除token，否则保存到 localStorage 和响应式引用
export function setToken(token) {
  if (!token) {
    localStorage.removeItem(TOKEN_KEY);
    tokenRef.value = "";
    return;
  }
  localStorage.setItem(TOKEN_KEY, token);
  tokenRef.value = token;
}

export function getUserInfo() {
  return userInfoRef.value || {};
}

export function setUserInfo(user) {
  if (!user) {
    localStorage.removeItem(USER_KEY);
    userInfoRef.value = {};
    return;
  }
  localStorage.setItem(USER_KEY, JSON.stringify(user));
  userInfoRef.value = user;
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  tokenRef.value = "";
  userInfoRef.value = {};
}

export function isLoggedIn() {
  return !!tokenRef.value;
}

export function useAuthState() {
  return {
    tokenRef,
    userInfoRef
  };
}
