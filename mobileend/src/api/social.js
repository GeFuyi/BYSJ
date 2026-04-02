import request, { toAbsoluteApiPath } from "./request";

export function uploadSocialImage(file) {
  const formData = new FormData();
  formData.append("file", file);
  return request.post("/social/upload-image", formData);
}

export function socialImageUrl(path) {
  return toAbsoluteApiPath(`/api/social/file?path=${encodeURIComponent(path)}`);
}

export function socialAssetUrl(path) {
  if (!path) return "";
  if (/^https?:\/\//i.test(path)) return path;
  if (path.startsWith("/api/social/file?")) return toAbsoluteApiPath(path);
  return socialImageUrl(path);
}
