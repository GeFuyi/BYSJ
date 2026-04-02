import request from "./request";

export function uploadSocialImage(file) {
  const formData = new FormData();
  formData.append("file", file);
  return request.post("/social/upload-image", formData, {
    headers: {
      "Content-Type": "multipart/form-data"
    }
  });
}

export function socialImageUrl(path) {
  return `/api/social/file?path=${encodeURIComponent(path)}`;
}

