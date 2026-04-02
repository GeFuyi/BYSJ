<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>邻里圈</ion-title>
        <ion-buttons slot="end">
          <ion-button fill="clear" size="small" @click="reloadFeed">刷新</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen>
      <div class="moments-shell ion-padding">
        <section class="publish-box page-card">
          <div class="publish-head">
            <user-avatar :src="user.avatarPath" :name="displayName" :size="40" />
            <div class="meta">
              <strong>{{ displayName }}</strong>
              <small>记录今天的社区见闻</small>
            </div>
          </div>

          <ion-textarea
            v-model="postDraft.content"
            class="publish-input"
            placeholder="分享点什么..."
            :rows="3"
            maxlength="1000"
            auto-grow
          />

          <div class="preview-grid" v-if="postDraft.imagePaths.length">
            <div v-for="path in postDraft.imagePaths" :key="path" class="preview-item">
              <app-image :src="socialImageUrl(path)" alt="post-image" />
              <button class="remove-btn" type="button" @click="removePostImage(path)">×</button>
            </div>
          </div>

          <div class="publish-tools">
            <input ref="postFileRef" class="hidden-input" type="file" accept="image/*" multiple @change="onPickPostImages" />
            <ion-button fill="clear" size="small" class="outline-btn" @click="openPostImagePicker">图片</ion-button>
            <ion-button size="small" class="submit-btn" @click="publishPost">发布</ion-button>
          </div>
        </section>

        <section class="feed-list">
          <article v-for="post in feedList" :key="post.id" class="moment-card page-card">
            <div class="moment-header">
              <user-avatar :src="post.avatarPath" :name="post.nickname || post.username" :size="44" />
              <div class="meta">
                <strong>{{ post.nickname || post.username }}</strong>
                <small>{{ post.createdAt }}</small>
              </div>
            </div>

            <p v-if="post.content" class="moment-content">{{ post.content }}</p>
            <div class="moment-images" v-if="post.imagePaths && post.imagePaths.length">
              <app-image v-for="path in post.imagePaths" :key="path" :src="socialImageUrl(path)" alt="post-image" />
            </div>

            <div class="comment-zone">
              <div class="comment-title">评论区</div>

              <div v-if="post.comments && post.comments.length" class="comment-list">
                <div v-for="comment in post.comments" :key="comment.id" class="comment-item">
                  <user-avatar :src="comment.avatarPath" :name="comment.nickname" :size="32" />
                  <div class="comment-body">
                    <div class="comment-line">
                      <strong>{{ comment.nickname }}</strong>
                      <span class="reply-label" v-if="comment.replyToNickname">回复 {{ comment.replyToNickname }}</span>
                    </div>
                    <p>{{ comment.content || "" }}</p>
                    <div class="moment-images comment-images" v-if="comment.imagePaths && comment.imagePaths.length">
                      <app-image
                        v-for="path in comment.imagePaths"
                        :key="`${comment.id}-${path}`"
                        :src="socialImageUrl(path)"
                        alt="comment-image"
                      />
                    </div>
                    <div class="comment-bottom">
                      <span>{{ comment.createdAt }}</span>
                      <button type="button" @click="replyTo(post.id, comment)">回复</button>
                    </div>
                  </div>
                </div>
              </div>
              <ion-text v-else color="medium" class="empty-comment">还没有评论，快来抢沙发</ion-text>

              <div class="reply-tip" v-if="getCommentDraft(post.id).replyNickname">
                正在回复 {{ getCommentDraft(post.id).replyNickname }}
                <button type="button" @click="cancelReply(post.id)">取消</button>
              </div>

              <ion-textarea
                v-model="getCommentDraft(post.id).content"
                class="comment-input"
                placeholder="留下你的看法..."
                :rows="2"
                maxlength="1000"
                auto-grow
              />

              <div class="preview-grid small" v-if="getCommentDraft(post.id).imagePaths.length">
                <div v-for="path in getCommentDraft(post.id).imagePaths" :key="path" class="preview-item">
                  <app-image :src="socialImageUrl(path)" alt="comment-preview-image" />
                  <button class="remove-btn" type="button" @click="removeCommentImage(post.id, path)">×</button>
                </div>
              </div>

              <div class="publish-tools compact">
                <input
                  :ref="(el) => bindCommentFileRef(post.id, el)"
                  class="hidden-input"
                  type="file"
                  accept="image/*"
                  multiple
                  @change="(e) => onPickCommentImages(post.id, e)"
                />
                <ion-button fill="clear" size="small" class="outline-btn" @click="openCommentImagePicker(post.id)">图片</ion-button>
                <ion-button size="small" class="submit-btn" @click="publishComment(post.id)">发送</ion-button>
              </div>
            </div>
          </article>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup>
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonPage,
  IonText,
  IonTextarea,
  IonTitle,
  IonToolbar,
  onIonViewDidLeave,
  onIonViewWillEnter
} from "@ionic/vue";
import { computed, reactive, ref } from "vue";
import { socialImageUrl, uploadSocialImage } from "../api/social";
import AppImage from "../components/AppImage.vue";
import UserAvatar from "../components/UserAvatar.vue";
import { getUserInfo } from "../composables/useAuth";
import { useRealtime } from "../composables/useRealtime";
import { presentToast } from "../utils/toast";

const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
const user = getUserInfo();
const displayName = computed(() => user.nickname || user.username || "用户");

const { connect, feed, refreshFeed, createPost, createComment, setMomentsActive, markMomentsRead } = useRealtime();

const feedList = computed(() => feed.value || []);
const postDraft = reactive({
  content: "",
  imagePaths: []
});
const commentDrafts = reactive({});
const postFileRef = ref(null);
const commentFileRefs = {};

onIonViewWillEnter(async () => {
  setMomentsActive(true);
  markMomentsRead();
  connect();
  if (!feed.value.length) {
    await reloadFeed();
  }
});

onIonViewDidLeave(() => {
  setMomentsActive(false);
});

function getCommentDraft(postId) {
  if (!commentDrafts[postId]) {
    commentDrafts[postId] = {
      content: "",
      imagePaths: [],
      parentId: null,
      replyNickname: null
    };
  }
  return commentDrafts[postId];
}

function bindCommentFileRef(postId, el) {
  commentFileRefs[postId] = el;
}

function openPostImagePicker() {
  postFileRef.value?.click();
}

function openCommentImagePicker(postId) {
  commentFileRefs[postId]?.click();
}

async function validateImageFile(file) {
  if (!file?.type?.startsWith("image/")) {
    await presentToast("仅支持图片文件", "warning");
    return false;
  }
  if (file.size > MAX_IMAGE_BYTES) {
    await presentToast("图片大小不能超过10MB", "warning");
    return false;
  }
  return true;
}

async function reloadFeed() {
  await refreshFeed(20);
}

async function onPickPostImages(event) {
  const files = Array.from(event.target.files || []);
  for (const file of files) {
    if (!(await validateImageFile(file))) continue;
    const data = await uploadSocialImage(file);
    if (data?.path) {
      postDraft.imagePaths.push(data.path);
    }
  }
  if (postFileRef.value) postFileRef.value.value = "";
}

function removePostImage(path) {
  postDraft.imagePaths = postDraft.imagePaths.filter((item) => item !== path);
}

async function publishPost() {
  if (!postDraft.content.trim() && postDraft.imagePaths.length === 0) {
    await presentToast("请输入动态内容或上传图片", "warning");
    return;
  }
  await createPost({
    content: postDraft.content,
    imagePaths: postDraft.imagePaths
  });
  postDraft.content = "";
  postDraft.imagePaths = [];
  await presentToast("发布成功", "success");
}

function replyTo(postId, comment) {
  const draft = getCommentDraft(postId);
  draft.parentId = comment.id;
  draft.replyNickname = comment.nickname;
}

function cancelReply(postId) {
  const draft = getCommentDraft(postId);
  draft.parentId = null;
  draft.replyNickname = null;
}

async function onPickCommentImages(postId, event) {
  const files = Array.from(event.target.files || []);
  const draft = getCommentDraft(postId);
  for (const file of files) {
    if (!(await validateImageFile(file))) continue;
    const data = await uploadSocialImage(file);
    if (data?.path) {
      draft.imagePaths.push(data.path);
    }
  }
  const input = commentFileRefs[postId];
  if (input) input.value = "";
}

function removeCommentImage(postId, path) {
  const draft = getCommentDraft(postId);
  draft.imagePaths = draft.imagePaths.filter((item) => item !== path);
}

async function publishComment(postId) {
  const draft = getCommentDraft(postId);
  if (!draft.content.trim() && draft.imagePaths.length === 0) {
    await presentToast("评论内容不能为空", "warning");
    return;
  }
  await createComment({
    postId,
    parentId: draft.parentId,
    content: draft.content,
    imagePaths: draft.imagePaths
  });
  draft.content = "";
  draft.imagePaths = [];
  draft.parentId = null;
  draft.replyNickname = null;
}
</script>

<style scoped>
.moments-shell {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.publish-box {
  padding: 12px;
}

.publish-head,
.moment-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.meta strong {
  font-size: 15px;
  color: #202a37;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta small {
  color: #8291a6;
  font-size: 12px;
}

.publish-input,
.comment-input {
  margin-top: 8px;
  --background: #f4f7fc;
  --padding-start: 12px;
  --padding-end: 12px;
  border-radius: 12px;
}

.hidden-input {
  display: none;
}

.publish-tools {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.publish-tools.compact {
  margin-top: 6px;
}

.outline-btn {
  --color: #2d64de;
  --background: #edf3ff;
  --border-radius: 10px;
}

.submit-btn {
  --border-radius: 10px;
  min-width: 72px;
}

.feed-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.moment-card {
  padding: 12px;
}

.moment-content {
  margin: 10px 0;
  white-space: pre-wrap;
  color: #1f2a35;
  font-size: 14px;
}

.moment-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.moment-images img {
  width: 106px;
  height: 106px;
  object-fit: cover;
  border-radius: 10px;
}

.comment-zone {
  margin-top: 12px;
  border-top: 1px solid #eef2f7;
  padding-top: 10px;
}

.comment-title {
  font-size: 13px;
  color: #7c8b9f;
  margin-bottom: 8px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.comment-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.comment-body {
  flex: 1;
  background: #f7f9fe;
  border-radius: 12px;
  padding: 8px;
}

.comment-line {
  display: flex;
  align-items: center;
  gap: 6px;
}

.comment-line strong {
  color: #2f3c4f;
  font-size: 13px;
}

.reply-label {
  font-size: 12px;
  color: #607089;
}

.comment-body p {
  margin: 4px 0 0;
  white-space: pre-wrap;
  color: #253241;
  font-size: 13px;
}

.comment-images {
  margin-top: 6px;
}

.comment-images img {
  width: 86px;
  height: 86px;
}

.comment-bottom {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #8a99ad;
  font-size: 11px;
}

.comment-bottom button,
.reply-tip button {
  border: 0;
  background: transparent;
  color: #3c74f5;
  font-size: 12px;
}

.empty-comment {
  display: inline-block;
  margin-bottom: 8px;
}

.reply-tip {
  margin: 6px 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #6f7f95;
}

.preview-grid {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-grid.small {
  margin-top: 6px;
}

.preview-item {
  width: 66px;
  height: 66px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  right: 2px;
  top: 2px;
  width: 18px;
  height: 18px;
  border: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.62);
  color: #fff;
  font-size: 14px;
  line-height: 1;
}
</style>
