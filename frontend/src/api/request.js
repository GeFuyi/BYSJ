import axios from "axios";
import { ElMessage } from "element-plus";

const request = axios.create({
  baseURL: "/api",
  timeout: 10000
});

const DEFAULT_MESSAGES = {
  400: "请求参数不合法",
  401: "请先登录",
  403: "无权限访问",
  404: "资源不存在",
  409: "当前状态不允许操作",
  415: "请求格式不支持",
  500: "服务器内部错误"
};

function errorCategory(status) {
  if (status === 400) return "validation";
  if (status === 401) return "auth";
  if (status === 403) return "permission";
  if (status === 404) return "not_found";
  if (status === 409) return "conflict";
  if (status === 415) return "unsupported_media_type";
  if (status >= 500) return "server";
  return "unknown";
}

function attachApiError(error, { status, code, message }) {
  const apiStatus = code && code !== 0 ? code : status;
  const apiCode = code || status;
  const apiMessage = message || DEFAULT_MESSAGES[apiStatus] || "网络异常";
  const target = error instanceof Error ? error : new Error(apiMessage);
  target.status = apiStatus;
  target.apiCode = apiCode;
  target.apiMessage = apiMessage;
  target.category = errorCategory(apiStatus);
  target.message = apiMessage;
  return target;
}

request.interceptors.request.use((config) => {
  const token = sessionStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code !== 0) {
      const error = attachApiError(new Error(), {
        status: response.status,
        code: res.code,
        message: res.message
      });
      ElMessage.error(error.apiMessage);
      return Promise.reject(error);
    }
    return res.data;
  },
  (error) => {
    const status = error.response?.status;
    const code = error.response?.data?.code ?? status;
    const apiError = attachApiError(error, {
      status,
      code,
      message: error.response?.data?.message || error.message
    });
    if (apiError.status === 401) {
      sessionStorage.removeItem("token");
      sessionStorage.removeItem("userInfo");
      if (window.location.pathname !== "/login") {
        window.location.href = "/login";
      }
    }
    ElMessage.error(apiError.apiMessage);
    return Promise.reject(apiError);
  }
);

export default request;
