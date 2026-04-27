<template>
  <div class="moments-page">
    <div class="hero">
      <div>
        <h2>邻里圈</h2>
        <p>分享社区动态、图片和评论互动，实时同步最新内容。</p>
      </div>
      <div class="hero-side">
        <el-tag :type="wsStateTagType" effect="dark">{{ wsStateText }}</el-tag>
        <el-button size="small" @click="reloadFeed">刷新动态</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="title-row">
          <span>发布动态</span>
          <span class="tip">支持文字与图片</span>
        </div>
      </template>
      <el-input
        v-model="postDraft.content"
        type="textarea"
        :rows="4"
        maxlength="1000"
        show-word-limit
        placeholder="分享今天的社区见闻..."
      />
      <div class="draft-image-list" v-if="postDraft.imagePaths.length">
        <div v-for="path in postDraft.imagePaths" :key="path" class="draft-image-item">
          <el-image :src="imageUrl(path)" fit="cover" />
          <el-button link type="danger" @click="removePostImage(path)">移除</el-button>
        </div>
      </div>
      <div class="publish-row">
        <el-upload :show-file-list="false" :before-upload="beforeUploadImage" :http-request="uploadPostImage">
          <el-button>上传图片</el-button>
        </el-upload>
        <el-button type="primary" :loading="publishing" @click="publishPost">发布动态</el-button>
      </div>
    </el-card>

    <div class="feed-list">
      <el-card v-for="post in feed" :key="post.id" shadow="never">
        <div class="post-head">
          <div class="avatar">{{ shortName(post.nickname || post.username) }}</div>
          <div>
            <div class="post-name">{{ post.nickname || post.username }}</div>
            <div class="post-time">{{ formatTime(post.createdAt) }}</div>
          </div>
        </div>
        <div v-if="post.content" class="post-content">{{ post.content }}</div>
        <div v-if="post.imagePaths && post.imagePaths.length" class="post-images">
          <el-image
            v-for="path in post.imagePaths"
            :key="path"
            :src="imageUrl(path)"
            :preview-src-list="post.imagePaths.map(imageUrl)"
            fit="cover"
            preview-teleported
          />
        </div>

        <div class="comment-area">
          <div class="comment-title">评论区</div>
          <div class="comment-list" v-if="post.comments && post.comments.length">
            <div v-for="comment in post.comments" :key="comment.id" class="comment-item">
              <div class="comment-main">
                <span class="comment-user">{{ comment.nickname }}</span>
                <span v-if="comment.replyToNickname" class="comment-reply"> 回复 {{ comment.replyToNickname }}</span>
                ：{{ comment.content }}
              </div>
              <div v-if="comment.imagePaths && comment.imagePaths.length" class="comment-images">
                <el-image
                  v-for="path in comment.imagePaths"
                  :key="path"
                  :src="imageUrl(path)"
                  :preview-src-list="comment.imagePaths.map(imageUrl)"
                  fit="cover"
                  preview-teleported
                />
              </div>
              <div class="comment-meta">
                <span>{{ formatTime(comment.createdAt) }}</span>
                <el-button link type="primary" @click="replyTo(post.id, comment)">回复</el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无评论" :image-size="56" />

          <div class="comment-editor">
            <el-tag
              v-if="getCommentDraft(post.id).replyNickname"
              type="warning"
              closable
              @close="cancelReply(post.id)"
            >
              回复 {{ getCommentDraft(post.id).replyNickname }}
            </el-tag>
            <el-input
              v-model="getCommentDraft(post.id).content"
              type="textarea"
              :rows="2"
              maxlength="1000"
              placeholder="写下你的评论..."
            />
            <div class="draft-image-list" v-if="getCommentDraft(post.id).imagePaths.length">
              <div v-for="path in getCommentDraft(post.id).imagePaths" :key="path" class="draft-image-item">
                <el-image :src="imageUrl(path)" fit="cover" />
                <el-button link type="danger" @click="removeCommentImage(post.id, path)">移除</el-button>
              </div>
            </div>
            <div class="publish-row">
              <el-upload
                :show-file-list="false"
                :before-upload="beforeUploadImage"
                :http-request="(option) => uploadCommentImage(post.id, option)"
              >
                <el-button size="small">上传图片</el-button>
              </el-upload>
              <el-button size="small" type="primary" @click="publishComment(post.id)">发送评论</el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from "vue";
import { useDesktopSocial } from "../composables/useDesktopSocial";

const {
  feed,
  publishing,
  postDraft,
  wsStateTagType,
  wsStateText,
  connect,
  reloadFeed,
  publishPost,
  getCommentDraft,
  replyTo,
  cancelReply,
  publishComment,
  beforeUploadImage,
  uploadPostImage,
  uploadCommentImage,
  removePostImage,
  removeCommentImage,
  imageUrl,
  shortName,
  formatTime
} = useDesktopSocial();

onMounted(async () => {
  await connect();
  if (!feed.value.length) {
    await reloadFeed();
  }
});
</script>

<style scoped>
.moments-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero {
  border-radius: 14px;
  padding: 16px 18px;
  background: linear-gradient(125deg, #0c5a89 0%, #1f7ab3 45%, #49a8d5 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.hero h2 {
  margin: 0;
  font-size: 22px;
}

.hero p {
  margin: 8px 0 0;
  opacity: 0.95;
}

.hero-side {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.title-row {
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tip {
  color: #909399;
  font-size: 12px;
}

.publish-row {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
}

.feed-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #0f7eb5;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.post-name {
  font-weight: 600;
}

.post-time {
  font-size: 12px;
  color: #909399;
}

.post-content {
  margin-top: 8px;
  white-space: pre-wrap;
  color: #303133;
}

.post-images {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 8px;
}

.post-images .el-image {
  width: 100%;
  height: 110px;
  border-radius: 8px;
}

.comment-area {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
}

.comment-title {
  font-weight: 600;
  color: #606266;
}

.comment-list {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.comment-item {
  border-radius: 8px;
  background: #f8fafc;
  padding: 8px 10px;
}

.comment-main {
  color: #303133;
  white-space: pre-wrap;
}

.comment-user {
  color: #0b72ad;
  font-weight: 600;
}

.comment-reply {
  color: #909399;
}

.comment-meta {
  margin-top: 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #909399;
  font-size: 12px;
}

.comment-images {
  margin-top: 6px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.comment-images .el-image {
  width: 72px;
  height: 72px;
  border-radius: 6px;
}

.comment-editor {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.draft-image-list {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.draft-image-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.draft-image-item .el-image {
  width: 70px;
  height: 70px;
  border-radius: 6px;
}

@media (max-width: 900px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-side {
    justify-content: flex-start;
  }
}
</style>
