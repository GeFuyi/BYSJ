import request from "./request";

export function login(data) {
  return request.post("/auth/login", data);
}

export function register(data) {
  return request.post("/auth/register", data);
}

export function sendSmsCode(data) {
  return request.post("/auth/sms/send", data);
}

export function smsLogin(data) {
  return request.post("/auth/sms/login", data);
}

