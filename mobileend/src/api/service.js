import request, { toAbsoluteApiPath } from "./request";

export function uploadServiceImage(file) {
  const formData = new FormData();
  formData.append("file", file);
  return request.post("/services/upload-image", formData);
}

export function listServiceCategories() {
  return request.get("/services/categories");
}

export function listPublishedServices(params) {
  return request.get("/services/list", { params });
}

export function createServiceEntry(data) {
  return request.post("/services/provider/entries", data);
}

export function updateServiceEntry(id, data) {
  return request.put(`/services/provider/entries/${id}`, data);
}

export function listProviderEntries(params) {
  return request.get("/services/provider/entries", { params });
}

export function updateServiceOperateStatus(id, data) {
  return request.put(`/services/provider/entries/${id}/operate-status`, data);
}

export function listAuditEntries(params) {
  return request.get("/services/audit/entries", { params });
}

export function auditServiceEntry(id, data) {
  return request.put(`/services/audit/entries/${id}`, data);
}

export function getServiceDetail(id) {
  return request.get(`/services/${id}`);
}

export function createServiceBooking(id, data) {
  return request.post(`/services/${id}/bookings`, data);
}

export function listMyServiceBookings() {
  return request.get("/services/my/bookings");
}

export function submitServiceReview(id, data) {
  return request.post(`/services/${id}/reviews`, data);
}

export function serviceImageUrl(path) {
  return toAbsoluteApiPath(`/api/services/file?path=${encodeURIComponent(path)}`);
}
