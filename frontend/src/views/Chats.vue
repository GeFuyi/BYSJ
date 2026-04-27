<template>
  <div class="chats-page">
    <div class="hero">
      <div>
        <h2>聊天</h2>
        <p>查看在线用户、添加好友、创建群聊，并进入对应会话。</p>
      </div>
      <div class="hero-side">
        <el-tag :type="wsStateTagType" effect="dark">{{ wsStateText }}</el-tag>
        <el-button size="small" @click="reloadLists">刷新列表</el-button>
      </div>
    </div>

    <div class="chat-layout">
      <el-card shadow="never">
        <template #header>
          <span class="card-title">在线用户</span>
        </template>
        <div class="online-list">
          <span v-for="item in onlineUsers" :key="item.userId" class="online-chip">
            {{ item.nickname || item.username }}
          </span>
          <el-empty v-if="!onlineUsers.length" description="当前暂无在线用户" :image-size="56" />
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <span class="card-title">好友与群聊</span>
        </template>

        <div class="tool-section">
          <div class="section-title">添加好友</div>
          <div class="inline-row">
            <el-select v-model="addFriendUserId" filterable placeholder="选择用户" style="flex: 1">
              <el-option
                v-for="user in addableUsers"
                :key="user.userId"
                :label="`${user.nickname || user.username} (${user.role})`"
                :value="user.userId"
              />
            </el-select>
            <el-button type="primary" @click="addFriend">添加</el-button>
          </div>
        </div>

        <div class="tool-section">
          <div class="section-title">创建群聊</div>
          <el-input v-model="groupDraft.name" maxlength="80" placeholder="群名称" />
          <el-select
            v-model="groupDraft.memberIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择好友"
            style="width: 100%; margin-top: 8px"
          >
            <el-option
              v-for="friend in friends"
              :key="friend.userId"
              :label="friend.nickname || friend.username"
              :value="friend.userId"
            />
          </el-select>
          <el-button style="margin-top: 8px; width: 100%" @click="handleCreateGroup">创建群聊</el-button>
        </div>

        <div class="session-block">
          <div class="section-title">好友私聊</div>
          <div class="session-list" v-if="friends.length">
            <button
              v-for="friend in friends"
              :key="`f-${friend.userId}`"
              type="button"
              class="session-item"
              @click="openPrivate(friend)"
            >
              <div>
                <div class="session-name">{{ friend.nickname || friend.username }}</div>
                <small>{{ getConversationPreview('PRIVATE', friend.userId) }}</small>
              </div>
              <div class="session-side">
                <el-tag size="small" :type="friend.online ? 'success' : 'info'">{{ friend.online ? '在线' : '离线' }}</el-tag>
                <el-badge v-if="unreadCount('PRIVATE', friend.userId) > 0" :value="unreadCount('PRIVATE', friend.userId)" />
              </div>
            </button>
          </div>
          <el-empty v-else description="暂无好友" :image-size="56" />
        </div>

        <div class="session-block">
          <div class="section-title">群聊</div>
          <div class="session-list" v-if="groups.length">
            <button
              v-for="group in groups"
              :key="`g-${group.groupId}`"
              type="button"
              class="session-item"
              @click="openGroup(group)"
            >
              <div>
                <div class="session-name">{{ group.name }}</div>
                <small>{{ getConversationPreview('GROUP', group.groupId) }}</small>
              </div>
              <div class="session-side">
                <span class="member-count">{{ group.members?.length || 0 }} 人</span>
                <el-badge v-if="unreadCount('GROUP', group.groupId) > 0" :value="unreadCount('GROUP', group.groupId)" />
              </div>
            </button>
          </div>
          <el-empty v-else description="暂无群聊" :image-size="56" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from "vue";
import { useRouter } from "vue-router";
import { useDesktopSocial } from "../composables/useDesktopSocial";

const router = useRouter();
const {
  friends,
  groups,
  onlineUsers,
  addFriendUserId,
  publishing,
  groupDraft,
  wsStateTagType,
  wsStateText,
  addableUsers,
  connect,
  reloadLists,
  addFriend,
  createGroup,
  unreadCount,
  getConversationPreview
} = useDesktopSocial();

async function openPrivate(friend) {
  router.push(`/home/chat/PRIVATE/${friend.userId}`);
}

async function openGroup(group) {
  router.push(`/home/chat/GROUP/${group.groupId}`);
}

async function handleCreateGroup() {
  const group = await createGroup();
  if (group?.groupId) {
    router.push(`/home/chat/GROUP/${group.groupId}`);
  }
}

onMounted(async () => {
  await connect();
  await reloadLists();
});
</script>

<style scoped>
.chats-page {
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

.chat-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 12px;
}

.card-title {
  font-weight: 600;
}

.online-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: flex-start;
}

.online-chip {
  border-radius: 999px;
  background: #e9f7ef;
  color: #1f8f4f;
  padding: 4px 10px;
  font-size: 12px;
}

.tool-section + .tool-section,
.session-block {
  margin-top: 14px;
}

.section-title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.inline-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.session-item {
  width: 100%;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}

.session-item:hover {
  border-color: #89c4ea;
  background: #f5fbff;
}

.session-name {
  font-weight: 600;
  color: #273548;
}

.session-item small {
  display: block;
  margin-top: 4px;
  color: #909399;
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-side {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.member-count {
  color: #7a8aa0;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }

  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-side {
    justify-content: flex-start;
  }
}
</style>
