import axios from "axios";
import { clearAuth, getToken } from "../composables/useAuth";
import { presentToast } from "../utils/toast";

const configuredBase = (import.meta.env.VITE_API_BASE_URL || "/api").trim();
const browserOrigin = typeof window !== "undefined" ? window.location.origin : "http://localhost";
const NGROK_HOST_RE = /(?:^|\.)ngrok-free\.dev$|(?:^|\.)ngrok\.io$/i;

export const request = axios.create({
  baseURL: configuredBase,
  timeout: 12000
});

function toUrlObject(value, fallback = browserOrigin) {
  if (!value) return null;
  try {
    return new URL(value, fallback);
  } catch (e) {
    return null;
  }
}

function shouldUseNgrokBypass(url, baseURL = configuredBase) {
  const baseObj = toUrlObject(baseURL, browserOrigin);
  const target = toUrlObject(url, baseObj?.href || browserOrigin) || baseObj;
  return !!target && NGROK_HOST_RE.test(target.hostname);
}

request.interceptors.request.use((config) => {
  if (!config.headers) {
    config.headers = {};
  }
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  if (shouldUseNgrokBypass(config.url, config.baseURL || configuredBase)) {
    config.headers["ngrok-skip-browser-warning"] = "true";
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    const contentType = String(response.headers?.["content-type"] || "").toLowerCase();
    if (contentType.includes("text/html")) {
      return Promise.reject(new Error("网关返回了页面内容，请检查 ngrok 配置"));
    }
    const res = response.data;
    if (typeof res !== "object" || res === null || !Object.prototype.hasOwnProperty.call(res, "code")) {
      return res;
    }
    if (res.code !== 0) {
      presentToast(res.message || "请求失败");
      return Promise.reject(new Error(res.message || "请求失败"));
    }
    return res.data;
  },
  async (error) => {
    const status = error.response?.status;
    const contentType = String(error.response?.headers?.["content-type"] || "").toLowerCase();
    let message = error.response?.data?.message || error.message || "网络异常";
    if (contentType.includes("text/html")) {
      message = "请求被网关拦截，请确认 ngrok 地址和请求头配置";
    }
    if (status === 401) {
      clearAuth();
      if (window.location.hash.indexOf("/login") === -1 && window.location.pathname.indexOf("/login") === -1) {
        window.location.href = "/login";
      }
    }
    await presentToast(message);
    return Promise.reject(error);
  }
);

export function toAbsoluteApiPath(path) {
  if (!path) return "";
  if (/^https?:\/\//i.test(path)) return path;
  if (/^\/\//.test(path)) {
    const protocol = typeof window !== "undefined" ? window.location.protocol : "https:";
    return `${protocol}${path}`;
  }
  const baseObj = toUrlObject(configuredBase, browserOrigin);
  if (/^\/api\//.test(path)) {
    if (baseObj && /^https?:\/\//i.test(configuredBase)) {
      return `${baseObj.origin}${path}`;
    }
    return path;
  }
  if (baseObj && /^https?:\/\//i.test(configuredBase) && path.startsWith("/")) {
    return `${baseObj.origin}${path}`;
  }
  return path.startsWith("/") ? path : `/${path}`;
}

export function shouldLoadImageAsBlob(pathOrUrl) {
  return shouldUseNgrokBypass(pathOrUrl, configuredBase);
}

const rawHttp = axios.create({
  timeout: 20000
});
const IMAGE_BLOB_CACHE_MAX = 260;
const imageBlobCache = new Map();
const imageBlobPending = new Map();

function imageCacheKey(url, token) {
  return `${token || "anon"}::${url}`;
}

function touchImageCacheEntry(key, entry) {
  imageBlobCache.delete(key);
  imageBlobCache.set(key, {
    blob: entry.blob,
    updatedAt: Date.now()
  });
}

function trimImageCache() {
  while (imageBlobCache.size > IMAGE_BLOB_CACHE_MAX) {
    const oldestKey = imageBlobCache.keys().next().value;
    if (!oldestKey) break;
    imageBlobCache.delete(oldestKey);
  }
}

export async function fetchImageObjectUrl(pathOrUrl) {
  const absoluteUrl = toAbsoluteApiPath(pathOrUrl);
  if (!absoluteUrl) return "";
  const token = getToken();
  const key = imageCacheKey(absoluteUrl, token);
  const cached = imageBlobCache.get(key);
  if (cached?.blob) {
    touchImageCacheEntry(key, cached);
    return URL.createObjectURL(cached.blob);
  }

  if (imageBlobPending.has(key)) {
    const blob = await imageBlobPending.get(key);
    return URL.createObjectURL(blob);
  }

  const headers = {};
  if (shouldUseNgrokBypass(absoluteUrl, configuredBase)) {
    headers["ngrok-skip-browser-warning"] = "true";
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  const task = rawHttp
    .get(absoluteUrl, {
      responseType: "blob",
      headers
    })
    .then((resp) => resp.data)
    .finally(() => {
      imageBlobPending.delete(key);
    });

  imageBlobPending.set(key, task);
  const blob = await task;
  imageBlobCache.set(key, {
    blob,
    updatedAt: Date.now()
  });
  trimImageCache();
  return URL.createObjectURL(blob);
}

export default request;
