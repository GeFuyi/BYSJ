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

const tokenRef = ref(localStorage.getItem(TOKEN_KEY) || "");
const userInfoRef = ref(parseUser(localStorage.getItem(USER_KEY)));

function syncFromStorage() {
  tokenRef.value = localStorage.getItem(TOKEN_KEY) || "";
  userInfoRef.value = parseUser(localStorage.getItem(USER_KEY));
}

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
