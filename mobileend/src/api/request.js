import axios from "axios";
import { clearAuth, getToken } from "../composables/useAuth";
import { presentToast } from "../utils/toast";

//api作为基础url
const configuredBase = (import.meta.env.VITE_API_BASE_URL || "/api").trim();
//判断是否在浏览器window
const browserOrigin = typeof window !== "undefined" ? window.location.origin : "http://localhost";
//ngrok免费域名正则，匹配 ngrok-free.dev 和 ngrok.io 的子域名
const NGROK_HOST_RE = /(?:^|\.)ngrok-free\.dev$|(?:^|\.)ngrok\.io$/i;

//封装axios
export const request = axios.create({
  baseURL: configuredBase,
  timeout: 12000
});
//如果 value 是相对 URL，则会使用 fallback 作为基准，绝对路径直接解析
function toUrlObject(value, fallback = browserOrigin) {
  if (!value) return null;
  try {
    return new URL(value, fallback);
  } catch (e) {
    return null;
  }
}
//判断是否需要使用 ngrok 绕过警告，主要针对免费 ngrok 域名
function shouldUseNgrokBypass(url, baseURL = configuredBase) {
  const baseObj = toUrlObject(baseURL, browserOrigin);
  const target = toUrlObject(url, baseObj?.href || browserOrigin) || baseObj;
  return !!target && NGROK_HOST_RE.test(target.hostname);
}
//请求拦截器，添加 token 和 ngrok 绕过头
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
//响应拦截器，统一处理响应格式和错误，并针对 ngrok 返回的 HTML 提示用户检查配置
request.interceptors.response.use(
  (response) => {
    const contentType = String(response.headers?.["content-type"] || "").toLowerCase();
    //如果返回的是 HTML（通常是网关错误页或重定向页），直接 reject 并提示开发者检查 Ngrok 配置
    //防止后端返回 HTML 页面而前端当 JSON 处理导致错误
    if (contentType.includes("text/html")) {
      return Promise.reject(new Error("网关返回了页面内容，请检查 ngrok 配置"));
    }
    //将响应体赋给 res。
    // 如果响应不是对象、为 null 或没有 code 字段，直接返回原数据。
    // ✅ 适配一些非标准 API 返回，或者第三方接口。
    const res = response.data;
    if (typeof res !== "object" || res === null || !Object.prototype.hasOwnProperty.call(res, "code")) {
      return res;
    }
    //如果 code 不为 0，认为请求失败
    //如果 code === 0，返回 res.data，也就是实际业务数据
    if (res.code !== 0) {
      presentToast(res.message || "请求失败");
      return Promise.reject(new Error(res.message || "请求失败"));
    }
    return res.data;
  },
  //获取 HTTP 状态码和 Content-Type
  async (error) => {
    const status = error.response?.status;
    const contentType = String(error.response?.headers?.["content-type"] || "").toLowerCase();
    let message = error.response?.data?.message || error.message || "网络异常";
    if (contentType.includes("text/html")) {
      message = "请求被网关拦截，请确认 ngrok 地址和请求头配置";
    }
    //自动登出并跳转到登录页,清除token，防止 token 过期导致的无限请求循环。
    //调用 clearAuth() 清除 token。
// 如果当前页面不在 /login，跳转到登录页
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

//将相对路径转换为绝对 URL，适配不同的 baseURL 配置和 ngrok 绕过场景
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
