import request, { toAbsoluteApiPath } from "./request";

export function uploadRepairImage(file) {
  const formData = new FormData();
  formData.append("file", file);
  return request.post("/repair/upload-image", formData);
}

export function createRepairOrder(data) {
  return request.post("/repair/orders", data);
}

export function listRepairOrders(params) {
  return request.get("/repair/orders", { params });
}

export function getRepairOrderDetail(id) {
  return request.get(`/repair/orders/${id}`);
}

export function updateRepairOrderStatus(id, data) {
  return request.put(`/repair/orders/${id}/status`, data);
}

export function repairImageUrl(path) {
  return toAbsoluteApiPath(`/api/repair/file?path=${encodeURIComponent(path)}`);
}
