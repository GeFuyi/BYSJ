import request from "./request";

export function me() {
  return request.get("/users/me");
}

export function listUsers() {
  return request.get("/users");
}

export function getUser(id) {
  return request.get(`/users/${id}`);
}

export function createUser(data) {
  return request.post("/users", data);
}

export function updateUser(id, data) {
  return request.put(`/users/${id}`, data);
}

export function deleteUser(id) {
  return request.delete(`/users/${id}`);
}

export function uploadMyAvatar(file) {
  const formData = new FormData();
  formData.append("file", file);
  return request.post("/users/me/avatar", formData);
}
