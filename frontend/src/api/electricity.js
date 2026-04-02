import request from "./request";

export function getElectricityDefaults() {
  return request.get("/electricity/defaults");
}

export function createElectricityOrder(data) {
  return request.post("/electricity/orders", data);
}

export function listMyElectricityOrders() {
  return request.get("/electricity/orders/my");
}

export function getElectricityOrder(id) {
  return request.get(`/electricity/orders/${id}`);
}

export function refreshElectricityOrder(id) {
  return request.post(`/electricity/orders/${id}/refresh`);
}

