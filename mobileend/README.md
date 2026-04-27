# mobileend

Ionic Vue 移动端，和 Web 端共用同一套后端接口与实时消息能力，重点覆盖住户在移动场景下的便民服务、报修、邻里互动、聊天与个人中心，同时也支持管理员/员工在手机上使用部分管理能力。

## 1. 项目定位

移动端基于 Ionic Vue + Capacitor，当前承担三类场景：

- **住户场景**：便民服务、报修、电费代缴、邻里圈、聊天、个人资料。
- **员工场景**：服务入驻、人员管理、报修流程处理。
- **管理员场景**：服务审核、人员管理、服务入驻、报修处理、电费代缴。

它和 Web 端的区别主要在于：

- API 地址通过环境变量配置。
- WebSocket 地址也通过环境变量配置。
- 支持 Capacitor 原生能力，如本地通知。
- 可同步到 Android 工程进行打包。

## 2. 技术栈

- Vue 3
- Ionic Vue 8
- Vue Router 4
- Capacitor 6
- Axios
- Capacitor Local Notifications

依赖定义见 [package.json](./package.json)。

## 3. 目录结构

```text
mobileend
├─ src
│  ├─ api            # REST 请求封装与业务 API
│  ├─ composables    # 登录态、实时通信等复用逻辑
│  ├─ pages          # 页面组件
│  ├─ router         # 路由与角色限制
│  ├─ components     # 通用组件
│  ├─ utils          # Toast 等工具
│  └─ main.js        # 应用入口
├─ .env.example      # 环境变量示例
├─ capacitor.config.json
└─ package.json
```

## 4. 路由结构

移动端路由定义见 [src/router/index.js](./src/router/index.js)。

### 4.1 游客页面

| 路径 | 页面 | 作用 |
| --- | --- | --- |
| `/login` | 登录页 | 支持账号密码登录和短信登录 |
| `/register` | 注册页 | 注册普通用户或员工账号 |

### 4.2 登录后主容器

主容器为 `/tabs`，底部标签页定义见 [src/pages/TabsPage.vue](./src/pages/TabsPage.vue)。

默认底部导航包含：

- 首页
- 便民
- 报修
- 邻里圈
- 聊天
- 我的

其中邻里圈和聊天标签会显示未读角标。

## 5. 页面功能说明

### 5.1 `/tabs/dashboard` 首页

首页是移动端总入口，主要功能：

- 展示当前用户头像、昵称和角色。
- 展示常用功能快捷入口：便民服务、物业报修、邻里圈、聊天、电费代缴。
- 对管理员和员工额外展示管理入口：服务入驻、入驻审核、人员管理。

### 5.2 `/tabs/services` 便民服务

该页面面向所有角色，主要功能：

- 浏览便民服务列表。
- 按关键字、分类、服务状态筛选。
- 查看服务详情、联系人、图片和评价。
- 提交服务预约。
- 查看个人预约记录。
- 对服务提交评价。

### 5.3 `/tabs/repair` 物业报修

该页面同时面向住户和管理角色，主要功能：

- 提交新的报修工单。
- 上传报修图片。
- 查看工单列表与工单详情。
- 查看流程记录和流程图片。
- 按状态筛选工单。
- 管理角色可推进工单状态流转。
- 住户可查看自己工单并在适当阶段确认完成。

### 5.4 `/tabs/moments` 邻里圈

邻里圈页面依赖 WebSocket 实时更新，主要功能：

- 发布文字动态。
- 上传动态图片。
- 查看动态列表。
- 查看评论与回复。
- 发表评论。
- 新动态、新评论到达时自动更新页面状态。

### 5.5 `/tabs/chats` 聊天总览

聊天总览页主要功能：

- 查看好友列表。
- 查看好友申请列表。
- 查看群组列表。
- 查看在线用户和用户目录。
- 添加好友。
- 创建群聊。
- 搜索好友/群组。
- 查看未读消息数量和最近消息预览。
- 进入会话详情页。

### 5.6 `/tabs/chat/:chatType/:targetId` 聊天室

该页面承载具体会话：

- 加载私聊历史消息。
- 加载群聊历史消息。
- 发送文字消息。
- 发送图片消息。
- 进入指定私聊或群聊。

### 5.7 `/tabs/electricity` 电费代缴

当前仅 `USER` / `ADMIN` 可访问，主要功能：

- 获取默认缴费信息。
- 创建电费代缴订单。
- 生成支付二维码。
- 打开支付宝支付链接。
- 刷新订单支付状态。
- 查看历史订单。

### 5.8 `/tabs/service-provider` 服务入驻

当前仅 `ADMIN` / `EMPLOYEE` 可访问，主要功能：

- 提交服务入驻信息。
- 上传服务封面和图片。
- 查看自己发布的服务条目。
- 编辑服务条目。
- 调整服务运营状态。

### 5.9 `/tabs/service-audit` 入驻审核

当前仅 `ADMIN` 可访问，主要功能：

- 查看待审核或历史审核服务。
- 按关键字和审核状态筛选。
- 查看服务详情与审核日志。
- 执行通过、退回、拒绝操作。

### 5.10 `/tabs/users` 人员管理

当前仅 `ADMIN` / `EMPLOYEE` 可访问，主要功能：

- 查看用户列表。
- 新增用户。
- 编辑用户。
- 删除用户。
- 管理角色、状态、昵称、手机号等信息。

### 5.11 `/tabs/profile` 我的

个人中心页主要功能：

- 查看当前账号资料。
- 上传头像。
- 刷新个人信息。
- 跳转到角色相关功能页。
- 退出登录。

## 6. 角色可见范围

角色限制主要定义在 [src/router/index.js](./src/router/index.js)。

### 6.1 `USER`

可访问：

- 首页
- 便民服务
- 报修
- 邻里圈
- 聊天
- 我的
- 电费代缴

不可直接访问：

- 服务入驻
- 入驻审核
- 人员管理

### 6.2 `EMPLOYEE`

可访问：

- 首页
- 便民服务
- 报修
- 邻里圈
- 聊天
- 我的
- 服务入驻
- 人员管理

不可直接访问：

- 电费代缴
- 入驻审核

### 6.3 `ADMIN`

可访问全部页面，包括：

- 电费代缴
- 服务入驻
- 入驻审核
- 人员管理

## 7. 登录态与鉴权

登录态封装见 [src/composables/useAuth.js](./src/composables/useAuth.js)。

当前移动端把登录信息保存在 `localStorage`：

- `token`
- `userInfo`

主要行为：

- 登录成功后调用 `setToken()`、`setUserInfo()` 写入本地。
- 接口请求自动读取 token 并附带 `Authorization` 头。
- 遇到 `401` 时会清空登录态并回到登录页。

## 8. REST 接入方式

统一请求封装见 [src/api/request.js](./src/api/request.js)。

### 8.1 请求层特点

- 通过 `VITE_API_BASE_URL` 指定后端 REST 地址。
- 自动添加 `Authorization: Bearer <token>`。
- 自动适配 ngrok 免费域名的请求头。
- 遇到网关返回 HTML 页面时，会给出 ngrok 相关错误提示。
- 提供 `toAbsoluteApiPath()` 用于把相对路径转换为可直接访问的图片地址。
- 对图片请求提供 blob 拉取和缓存能力，减少 ngrok/跨域环境下图片失败问题。

### 8.2 API 模块划分

- [src/api/auth.js](./src/api/auth.js)：注册、登录、短信登录
- [src/api/user.js](./src/api/user.js)：用户资料、人员管理、头像
- [src/api/service.js](./src/api/service.js)：便民服务、入驻、审核、预约、评价
- [src/api/repair.js](./src/api/repair.js)：报修工单、状态流转、图片上传
- [src/api/electricity.js](./src/api/electricity.js)：电费代缴
- [src/api/social.js](./src/api/social.js)：动态/聊天图片上传与图片地址拼接

## 9. 实时通信与通知

实时逻辑封装见 [src/composables/useRealtime.js](./src/composables/useRealtime.js)。

### 9.1 WebSocket 连接

- 默认连接后端 `/ws/community`
- 连接地址优先使用 `VITE_WS_BASE_URL`
- 自动把 JWT 作为 `?token=` 拼接到连接地址
- 断线后自动重连
- 会定时发送 `PING`

### 9.2 初始化数据

连接成功后会发送 `INIT`，获取：

- feed
- friends
- friendRequests
- groups
- onlineUsers
- userDirectory

### 9.3 支持的实时事件

主要处理：

- `ONLINE_LIST`
- `FRIEND_LIST`
- `FRIEND_REQUEST_LIST`
- `GROUP_LIST`
- `NEW_PRIVATE_MESSAGE`
- `NEW_GROUP_MESSAGE`
- `NEW_POST`
- `NEW_COMMENT`
- `OFFLINE_BATCH`

### 9.4 未读与本地通知

移动端会维护：

- 聊天未读数
- 邻里圈未读数
- 最近消息预览
- 当前会话已读状态

在原生 Android / iOS 环境下，还会：

- 申请本地通知权限
- 创建通知渠道 `community-message`
- 在应用退到后台时，为新的私聊、群聊、邻里圈动态和评论推送本地通知

## 10. 环境变量与联调

示例文件见 [`.env.example`](./.env.example)。

```env
VITE_API_BASE_URL=http://10.0.2.2:8080/api
VITE_WS_BASE_URL=ws://10.0.2.2:8080
```

### 10.1 本地模拟器联调

- `10.0.2.2` 是 Android 模拟器访问宿主机 `localhost` 的固定地址。
- 如果后端跑在本机 `8080`，Android 模拟器应使用：
  - `VITE_API_BASE_URL=http://10.0.2.2:8080/api`
  - `VITE_WS_BASE_URL=ws://10.0.2.2:8080`

### 10.2 真机或公网联调

如果用 ngrok 或其他内网穿透：

- `VITE_API_BASE_URL` 应填公网 `https://.../api`
- `VITE_WS_BASE_URL` 应填公网 `wss://...`
- 若使用免费 ngrok，当前请求层已做浏览器警告页绕过处理

### 10.3 HTTPS / WSS 要求

- 在公网真机环境中，优先使用 `https` / `wss`。
- 若 WebSocket 地址不可达、证书异常或被代理拦截，聊天和邻里圈实时能力会受到影响。

## 11. 本地开发

### 11.1 环境要求

- Node.js 18+
- npm 9+
- 后端服务已启动

### 11.2 启动步骤

```bash
cd mobileend
npm install
npm run dev
```

### 11.3 复制环境变量

```bash
# macOS / Linux
cp .env.example .env

# Windows PowerShell
Copy-Item .env.example .env
```

然后根据当前联调环境修改 `.env`。

## 12. Android 打包

### 12.1 首次接入 Android

```bash
npm run build
npm run cap:add:android
npm run cap:sync
npm run cap:android
```

### 12.2 后续资源更新

如果只是前端页面和资源变更，通常执行：

```bash
npm run build
npm run cap:sync
npm run cap:android
```

说明：

- `cap:add:android` 只需要在首次创建 Android 工程时执行。
- `cap:sync` 用于把 Web 构建产物和插件配置同步到原生工程。
- `cap:android` 会打开 Android Studio，由你继续运行或打包 APK / AAB。

## 13. 图片与文件接口

移动端图片展示依赖后端文件接口：

- 报修图片：`/api/repair/file`
- 服务图片：`/api/services/file`
- 邻里圈 / 聊天图片：`/api/social/file`

上传入口也对应后端文件能力：

- 报修上传：`/api/repair/upload-image`
- 服务上传：`/api/services/upload-image`
- 邻里圈 / 聊天上传：`/api/social/upload-image`

## 14. 常见问题

### 14.1 登录成功但接口请求失败

优先检查：

- `VITE_API_BASE_URL` 是否包含 `/api`
- token 是否正确写入本地
- 后端是否已启动

### 14.2 图片显示失败

优先检查：

- 设备是否能访问当前 API 域名
- 图片地址是否被 ngrok 提示页拦截
- 后端上传目录是否存在对应文件

### 14.3 WebSocket 不通

优先检查：

- `VITE_WS_BASE_URL` 是否正确
- 是否使用了 `ws://` / `wss://`
- token 是否有效
- 公网环境下证书与代理是否允许 WebSocket 升级

## 15. 相关文档

- [项目总 README](../README.md)
- [后端 README](../backend/README.md)
- [Web 端 README](../frontend/README.md)
