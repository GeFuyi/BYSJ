import { toastController } from "@ionic/vue";

export async function presentToast(message, color = "danger", duration = 1800) {
  const toast = await toastController.create({
    message: message || "操作失败",
    color,
    duration,
    position: "top"
  });
  await toast.present();
}

