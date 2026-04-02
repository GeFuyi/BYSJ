# backend（社区服务后端）

## 技术栈

- Spring Boot 2.7.18
- MyBatis
- MySQL 8.x
- Redis 6.x/7.x
- JWT + Shiro（权限控制）
- WebSocket（实时聊天/邻里圈消息）
- 阿里云短信网关
- 支付宝生活缴费（电费代缴）

## 功能边界

- 认证与用户：注册、登录、短信登录、用户管理、头像
- 便民服务：分类、服务发布、审核、预约、评价
- 报修工单：提交、状态流转、备注、流程图片、详情追踪
- 电费代缴：订单、预下单、支付回调、状态刷新
- 社交与聊天：朋友圈动态、评论回复、好友关系、私聊群聊、群公告
- 实时能力：在线用户管理、离线消息（Redis）

## 目录说明

```text
backend
├─ src/main/java/com/community
│  ├─ controller   # HTTP 接口
│  ├─ service      # 业务服务
│  ├─ mapper       # MyBatis Mapper
│  ├─ ws           # WebSocket 与实时消息
│  ├─ security     # JWT / Shiro
│  └─ payment/sms  # 支付宝/短信网关
└─ src/main/resources
   ├─ application.yml
   ├─ schema.sql
   ├─ data.sql
   └─ mapper/*.xml
```

## 启动要求

- JDK 8+
- Maven 3.8+
- MySQL 8.x
- Redis 6.x/7.x

## 本地启动

### 1. 创建数据库

```sql
CREATE DATABASE community_app DEFAULT CHARACTER SET utf8mb4;
```

### 2. 配置 `application.yml`

路径：`src/main/resources/application.yml`

至少确认：
- `spring.datasource.url/username/password`
- `spring.redis.host/port`
- `jwt.secret`

可选第三方：
- `sms.*`（阿里云短信）
- `alipay.*`（支付宝电费代缴）

### 3. 启动

```bash
mvn spring-boot:run
```

默认端口：`8080`

### 4. 打包

```bash
mvn -DskipTests package
```

## 数据初始化说明

- 应用启动时会自动执行 `schema.sql`（建表）和内置迁移逻辑
- 演示账号可手动执行 `src/main/resources/data.sql`

默认演示账号（执行 `data.sql` 后）：
- `admin / admin123`
- `employee / employee123`
- `user / user123`

## 关键接口概览

- 认证：`/api/auth/*`
- 用户：`/api/users/*`
- 便民服务：`/api/services/*`
- 物业报修：`/api/repair/*`
- 电费代缴：`/api/electricity/*`
- 社交文件：`/api/social/*`
- 健康检查：`/api/health`

## WebSocket

- 连接地址：`/ws/community`
- 鉴权方式：`Authorization: Bearer <token>` 请求头，或查询参数 `?token=<jwt>`

主要消息类型：
- 初始化与在线：`INIT`、`ONLINE_LIST`
- 好友：`FRIEND_ADD`、`FRIEND_REQUEST_HANDLE`、`FRIEND_REMOVE`
- 群聊：`GROUP_CREATE`、`GROUP_SEND`、`GROUP_MUTE`、`GROUP_ANNOUNCEMENT_SET`
- 朋友圈：`POST_CREATE`、`COMMENT_CREATE`
- 历史消息：`PRIVATE_HISTORY`、`GROUP_HISTORY`

## 上传目录

默认本地目录：
- 报修：`uploads/repair`
- 便民服务：`uploads/service`
- 社交：`uploads/social`

目录可通过 `application.yml` 调整：
- `repair.upload-dir`
- `service-platform.upload-dir`
- `social.upload-dir`
