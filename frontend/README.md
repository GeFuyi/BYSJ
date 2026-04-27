# frontend

Web 端基于 Vue 3 + Element Plus，既承担管理员/员工的管理能力，也承载普通住户的便民服务、报修、邻里圈、聊天和个人中心能力。

## 1. 项目定位

frontend 是一个与移动端并行的 Web 客户端：

- 与 `mobileend` 共用同一套后端 REST 接口。
- 与 `mobileend` 共用同一条 WebSocket 实时通道 `/ws/community`。
- 既不是纯后台系统，也不是只面向住户的单一前台，而是一个按角色展示能力的综合客户端。

## 2. 技术栈

- Vue 3
- Vue Router 4
- Element Plus
- Axios
- Vite

包定义见 [package.json](./package.json)。

## 3. 目录结构

```text
frontend
├─ src
│  ├─ api            # REST 请求封装与业务 API
│  ├─ composables    # 复用的业务状态逻辑
│  ├─ router         # 路由与权限守卫
│  ├─ views          # 页面组件
│  ├─ App.vue        # 根组件
│  └─ main.js        # 应用入口
├─ vite.config.js    # 本地开发代理配置
└─ package.json
```

### 3.1 关键目录职责

- `src/views`：页面层，按页面组织功能。
- `src/api`：对后端接口进行模块化封装。
- `src/router`：定义路径、页面映射和角色访问限制。
- `src/composables/useDesktopSocial.js`：负责邻里圈、好友、群聊、私聊、未读状态等实时逻辑。

## 4. 路由结构

主路由定义见 [src/router/index.js](./src/router/index.js)。

### 4.1 游客页

| 路径 | 页面 | 作用 |
| --- | --- | --- |
| `/login` | 登录页 | 支持账号密码登录和短信验证码登录 |
| `/register` | 注册页 | 注册用户名、密码、手机号、昵称和角色 |

### 4.2 登录后主容器

- 主路径：`/home`
- 页面容器：`Home.vue`
- 能力：
  - 左侧菜单导航
  - 顶部当前页标题与用户信息
  - 按角色控制菜单项显示
  - 内部通过 `<router-view />` 加载子页面

## 5. 页面功能说明

### 5.1 `/home/dashboard` 首页

首页是登录后的总入口，主要功能：

- 展示当前用户的用户名、昵称、手机号和角色。
- 根据角色展示快捷入口。
- 快速跳转到便民服务、报修、邻里圈、聊天、电费代缴、服务入驻、入驻审核、人员管理、个人中心。

### 5.2 `/home/services` 便民服务

这是住户和管理角色都可访问的服务市场页，主要功能：

- 查看已发布的便民服务列表。
- 按关键字、分类、服务状态筛选。
- 查看服务详情、图片、联系方式和评价。
- 已登录用户可预约服务。
- 已登录用户可查看自己的预约记录。
- 已登录用户可对服务提交评价。

### 5.3 `/home/service-provider` 服务入驻

这是服务提供方入口，面向 `ADMIN` / `EMPLOYEE`：

- 提交服务入驻申请。
- 填写服务名称、分类、描述、联系方式、地址、容量等信息。
- 上传封面和图片。
- 查看自己发布的服务条目。
- 修改已提交或被退回的服务内容。
- 对审核通过的服务调整运营状态。

### 5.4 `/home/service-audit` 入驻审核

这是管理员审核页，仅 `ADMIN` 可访问：

- 查看服务入驻审核列表。
- 按审核状态和关键字筛选。
- 查看服务详情和审核记录。
- 对服务执行通过、退回、拒绝等操作。

### 5.5 `/home/repair` 物业报修

物业报修页面同时面向住户和管理角色：

- 提交报修工单。
- 上传报修图片。
- 查看工单列表。
- 按状态筛选工单。
- 查看工单详情和流程时间线。
- 管理角色可推进工单状态流转。
- 住户可跟踪进度并在适当阶段确认完成。

### 5.6 `/home/electricity` 电费代缴

当前仅 `USER` / `ADMIN` 可见，主要功能：

- 获取默认缴费参数。
- 创建电费代缴订单。
- 展示支付二维码或跳转支付链接。
- 主动刷新支付状态。
- 查看历史订单。

### 5.7 `/home/moments` 邻里圈

邻里圈页面通过 WebSocket 获取和推送动态，主要功能：

- 发布文字动态。
- 上传动态图片。
- 查看动态列表。
- 查看评论。
- 发表评论和回复。
- 显示 WebSocket 在线状态。
- 动态实时更新。

### 5.8 `/home/chats` 聊天总览

聊天总览页主要功能：

- 查看在线用户。
- 查看好友列表。
- 查看群组列表。
- 发起添加好友。
- 创建群聊。
- 查看未读消息数量。
- 查看最近消息预览。
- 进入私聊或群聊详情。

### 5.9 `/home/chat/:chatType/:targetId` 聊天室

聊天室页负责承载单个会话：

- 加载私聊或群聊历史消息。
- 发送文本消息。
- 发送图片消息。
- 会话内自动滚动与刷新消息。
- 使用路由参数区分私聊和群聊目标。

### 5.10 `/home/profile` 个人中心

个人中心主要功能：

- 查看当前账号信息。
- 上传头像。
- 刷新资料。
- 通过快捷入口跳转到其他功能。
- 退出登录。

### 5.11 `/home/users` 人员管理

仅 `ADMIN` / `EMPLOYEE` 可访问，主要功能：

- 查看用户列表。
- 新增用户。
- 修改用户资料。
- 修改角色与状态。
- 删除用户。

## 6. 角色可见范围

路由守卫与菜单控制主要定义在：

- [src/router/index.js](./src/router/index.js)
- [src/views/Home.vue](./src/views/Home.vue)

### 6.1 `USER`

可访问：

- 首页
- 便民服务
- 物业报修
- 电费代缴
- 邻里圈
- 聊天
- 个人中心

不可直接访问：

- 服务入驻
- 入驻审核
- 人员管理

### 6.2 `EMPLOYEE`

可访问：

- 首页
- 便民服务
- 物业报修
- 邻里圈
- 聊天
- 个人中心
- 服务入驻
- 人员管理

不可直接访问：

- 入驻审核
- 电费代缴

### 6.3 `ADMIN`

可访问全部页面，包含：

- 服务入驻
- 入驻审核
- 人员管理
- 电费代缴

## 7. 与后端的交互方式

### 7.1 REST API

统一请求封装在 [src/api/request.js](./src/api/request.js)：

- `baseURL` 固定为 `/api`
- 自动从 `sessionStorage` 读取 `token`
- 自动附带 `Authorization: Bearer <token>`
- 统一处理后端 `ApiResponse`
- 遇到 `401` 时清理登录态并跳回 `/login`

### 7.2 API 模块划分

- [src/api/auth.js](./src/api/auth.js)：登录、注册、短信登录
- [src/api/user.js](./src/api/user.js)：用户资料、用户管理、头像
- [src/api/service.js](./src/api/service.js)：便民服务、服务入驻、审核、预约、评价
- [src/api/repair.js](./src/api/repair.js)：报修工单、状态流转、图片上传
- [src/api/electricity.js](./src/api/electricity.js)：电费代缴订单与状态
- [src/api/social.js](./src/api/social.js)：社交/聊天图片上传与 URL 拼接

### 7.3 实时能力

[useDesktopSocial.js](./src/composables/useDesktopSocial.js) 负责：

- 建立 `/ws/community?token=...` 连接
- 初始化动态、好友、群组、在线用户和用户目录
- 维护未读数和最近消息预览
- 处理离线消息补拉 `OFFLINE_BATCH`
- 处理 `NEW_POST`、`NEW_COMMENT`、`NEW_PRIVATE_MESSAGE`、`NEW_GROUP_MESSAGE`
- 自动重连与聊天历史加载

> Web 端实时能力依赖浏览器当前站点地址自动推导 WebSocket 地址，因此它更适合走同源部署或反向代理部署。

## 8. 登录态与存储

Web 端当前把登录态保存在 `sessionStorage`：

- `token`
- `userInfo`

特点：

- 浏览器标签页内可用。
- 页面刷新后仍可读取。
- 浏览器关闭后通常会失效。
- 通过 `user-info-updated` 事件和 `storage` 事件同步页面显示。

## 9. 本地开发

### 9.1 环境要求

- Node.js 18+
- npm 9+
- 已启动的后端服务（默认 `http://localhost:8080`）

### 9.2 安装与启动

```bash
cd frontend
npm install
npm run dev
```

开发服务器默认端口：`5173`

### 9.3 构建

```bash
npm run build
```

### 9.4 本地代理

Vite 配置见 [vite.config.js](./vite.config.js)：

- `/api` → `http://localhost:8080`
- `/ws` → `ws://localhost:8080`

因此开发环境下前端可以直接请求：

- `/api/...`
- `/ws/community`

## 10. 部署说明

### 10.1 部署方式

Web 端当前最适合以下方式部署：

1. 将前端构建产物部署为静态站点。
2. 在反向代理层把 `/api` 转发到 Spring Boot 后端。
3. 在反向代理层把 `/ws/community` 对应的 `/ws` 升级连接转发到后端。

### 10.2 为什么需要同源或代理

因为当前前端请求层写死了：

- REST 基础前缀：`/api`
- WebSocket 地址：`当前协议 + 当前域名 + /ws/community`

这意味着：

- 前端不依赖独立的 `.env` 接口地址配置。
- 生产环境应由 Nginx 或其他代理把 API 和 WebSocket 正确转发。
- 如果前后端完全分域且不做代理，需要先调整请求层实现。

## 11. 文档补充说明

- 当前注册页允许在 Web 端直接选择 `ADMIN` / `EMPLOYEE` / `USER` 角色，这属于当前实现行为，README 只做如实记录。
- 代码仓库里存在 `SocialHub.vue`，但当前路由实际使用的是 `Moments.vue`、`Chats.vue`、`ChatRoom.vue` 这一套拆分页面。

## 12. 相关文档

- [项目总 README](../README.md)
- [后端 README](../backend/README.md)
- [移动端 README](../mobileend/README.md)
