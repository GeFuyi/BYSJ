import request from "./request";

export function uploadServiceImage(formData) {
  return request.post("/services/upload-image", formData, {
    headers: {
      "Content-Type": "multipart/form-data"
    }
  });
}

export function listServiceCategories() {
  return request.get("/services/categories");
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

export function listPublishedServices(params) {
  return request.get("/services/list", { params });
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

export function listServiceReviews(id) {
  return request.get(`/services/${id}/reviews`);
}

export function serviceImageUrl(path) {
  return `/api/services/file?path=${encodeURIComponent(path)}`;
}

