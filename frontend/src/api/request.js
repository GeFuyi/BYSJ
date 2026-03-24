import axios from "axios";
import { ElMessage } from "element-plus";

const request = axios.create({
  baseURL: "/api",
  timeout: 10000
});

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
      ElMessage.error(res.message || "请求失败");
      return Promise.reject(new Error(res.message || "请求失败"));
    }
    return res.data;
  },
  (error) => {
    const status = error.response?.status;
    const message = error.response?.data?.message || error.message || "网络异常";
    if (status === 401) {
      sessionStorage.removeItem("token");
      sessionStorage.removeItem("userInfo");
      if (window.location.pathname !== "/login") {
        window.location.href = "/login";
      }
    }
    ElMessage.error(message);
    return Promise.reject(error);
  }
);

export default request;
