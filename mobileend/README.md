# mobileend（Ionic Vue 移动端）

移动端基于 `Vue3 + Ionic + Capacitor`，与 `frontend`（Web 端）并行存在，复用同一套后端接口与 WebSocket 能力。

## 技术栈

- Vue 3
- Ionic Vue 8
- Capacitor 6
- Axios

## 功能模块

- 登录/注册（账号密码、短信）
- 首页与角色化入口
- 便民服务（市场、预约、评价）
- 物业报修（提交、详情、状态流转、流程图）
- 电费代缴（订单、支付状态、跳转支付宝）
- 邻里圈（动态、评论、图片）
- 聊天（好友、私聊、群聊、公告、未读提示）
- 人员管理、服务审核（管理员/员工）

## 运行环境

- Node.js 18+
- npm 9+
- Android Studio（打包 Android 时）

## 本地开发

```bash
cd mobileend
npm install
npm run dev
```

## 环境变量配置

1. 复制示例文件：

```bash
# macOS / Linux
cp .env.example .env

# Windows PowerShell
Copy-Item .env.example .env
```

2. 按你的环境修改 `.env`：

```env
VITE_API_BASE_URL=http://10.0.2.2:8080/api
VITE_WS_BASE_URL=ws://10.0.2.2:8080
```

说明：
- `10.0.2.2` 是 Android 模拟器访问宿主机 `localhost` 的固定地址
- 使用 ngrok 时请改为公网地址（`https/wss`）

## Android 打包

```bash
npm run build
npm run cap:add:android
npm run cap:sync
npm run cap:android
```

说明：
- 首次执行需要 `cap:add:android`
- 之后更新前端资源时通常执行 `npm run build && npm run cap:sync`
- 然后在 Android Studio 中运行或打包 APK/AAB

## 与后端联调要点

- 后端默认地址：`http://localhost:8080`
- 移动端登录后会建立 WebSocket 连接，默认路径为后端 `/ws/community`
- 图片展示依赖后端文件接口：`/api/repair/file`、`/api/services/file`、`/api/social/file`

## 常见问题

1. 登录正常但其他接口失败：检查 `.env` 中 `VITE_API_BASE_URL` 是否包含 `/api`，并确认后端 JWT 鉴权与跨域配置正常。
2. 模拟器能登录但图片不显示：检查 API 域名是否可被设备访问，确认接口未被 ngrok 中间提示页拦截。
3. WebSocket 不通：检查 `VITE_WS_BASE_URL` 使用 `ws://` 或 `wss://`，并确认连接时 token 已正确传递。
