import request from "./request";

export function uploadRepairImage(formData) {
  return request.post("/repair/upload-image", formData, {
    headers: {
      "Content-Type": "multipart/form-data"
    }
  });
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

