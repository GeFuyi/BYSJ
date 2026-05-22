# backend

社区便民服务系统后端，负责统一承载 Web 端与移动端的认证、用户、便民服务、物业报修、电费代缴、邻里圈、聊天和实时推送能力。

## 1. 后端职责

后端当前主要提供 6 类能力：

- **认证与账号体系**：注册、账号密码登录、短信验证码登录、JWT 鉴权。
- **人员管理**：当前登录用户信息、用户列表、用户新增/修改/删除、头像上传。
- **便民服务平台**：服务分类、服务入驻、审核、上下架、预约、评价。
- **物业报修**：工单提交、图片上传、状态流转、流程备注、详情追踪。
- **电费代缴**：订单创建、支付二维码/链接、订单状态刷新、支付宝异步回调。
- **邻里圈与聊天**：动态、评论、好友、群组、私聊、群聊、离线消息补拉。

## 2. 技术栈

- Spring Boot 2.7.18
- Java 17
- MyBatis + XML Mapper
- MySQL 8.x
- Redis 6.x / 7.x
- Apache Shiro + JWT
- Spring WebSocket
- 阿里云短信
- 支付宝开放平台
- ZXing（二维码生成）

关键依赖定义见 [pom.xml](./pom.xml)。

## 3. 架构设计

### 3.1 分层结构

```text
backend
├─ src/main/java/com/community
│  ├─ controller        # REST 接口入口
│  ├─ service           # 业务接口
│  ├─ service/impl      # 业务实现
│  ├─ mapper            # MyBatis Mapper 接口
│  ├─ entity            # 持久化实体
│  ├─ dto               # 请求/响应 DTO
│  ├─ config            # 配置、跨域、Redis、Shiro 等
│  ├─ security          # JWT、Realm、过滤器
│  ├─ ws                # WebSocket 鉴权、连接、命令分发、离线消息
│  ├─ payment           # 支付宝与二维码封装
│  └─ sms               # 短信网关封装
└─ src/main/resources
   ├─ application.yml   # 主配置
   ├─ schema.sql        # 建表脚本
   ├─ data.sql          # 演示数据
   └─ mapper/*.xml      # MyBatis SQL
```

### 3.2 运行链路

- **HTTP 接口**：客户端 → `controller` → `service` → `mapper` → MySQL / Redis / 第三方服务。
- **实时消息**：客户端 → `/ws/community` → `ws` 模块 → `SocialChatDomainService` → MySQL / Redis。
- **文件上传**：客户端上传 → 业务服务写入本地目录 → 通过 `/api/repair/file`、`/api/services/file`、`/api/social/file` 对外访问。

### 3.3 核心基础设施

- **MySQL**：保存用户、工单、便民服务、订单、聊天和社交数据。
- **Redis**：缓存用户与业务数据、控制短信发送间隔、缓存离线消息。
- **本地文件目录**：保存报修图片、服务图片、邻里圈/聊天图片。
- **支付宝**：用于电费代缴创建支付订单、查询状态和接收回调。
- **阿里云短信**：用于发送登录验证码。

## 4. 核心模块说明

### 4.1 认证与用户模块

- 入口控制器：`AuthController`、`UserController`
- 主要服务：`SysUserService` / `SysUserServiceImpl`
- 主要能力：
  - 注册与账号登录
  - 短信验证码发送与短信登录
  - 当前用户资料查询
  - 人员管理
  - 头像上传

### 4.2 物业报修模块

- 入口控制器：`RepairOrderController`
- 主要服务：`RepairOrderService` / `RepairOrderServiceImpl`
- 主要能力：
  - 报修工单提交
  - 工单列表与详情
  - 流程状态流转
  - 图片上传与回显

### 4.3 便民服务平台模块

- 入口控制器：`ServicePlatformController`
- 主要服务：`ServicePlatformService` / `ServicePlatformServiceImpl`
- 主要能力：
  - 服务分类查询
  - 服务入驻与编辑
  - 入驻审核
  - 服务状态管理
  - 用户预约与评价

### 4.4 电费代缴模块

- 入口控制器：`ElectricityPaymentController`
- 主要服务：`ElectricityPaymentService` / `ElectricityPaymentServiceImpl`
- 第三方封装：`AlipayGateway`、`QrCodeService`
- 主要能力：
  - 获取默认缴费参数
  - 创建电费订单
  - 查询订单
  - 刷新支付状态
  - 处理支付宝异步通知

### 4.5 邻里圈与聊天模块

- REST 入口：`SocialController`
- WebSocket 入口：`/ws/community`
- 主要服务：`SocialChatDomainService`
- 主要能力：
  - 动态列表与发布
  - 评论与回复
  - 好友关系
  - 群聊与群公告
  - 私聊与群聊历史消息
  - 在线状态广播与离线消息补拉

> 注意：`/api/social/*` 只负责图片上传与图片访问。邻里圈、好友、群聊、私聊等核心交互主要通过 WebSocket 完成。

## 5. REST 接口总览

接口统一返回 `ApiResponse`，成功时 `code = 0`。

### 5.1 认证接口 `AuthController`

基础路径：`/api/auth`

| 方法 | 路径 | 作用 | 典型调用方 |
| --- | --- | --- | --- |
| POST | `/register` | 注册账号 | Web / 移动端游客 |
| POST | `/login` | 账号密码登录并返回 JWT | Web / 移动端游客 |
| POST | `/sms/send` | 发送短信验证码 | Web / 移动端游客 |
| POST | `/sms/login` | 使用手机号验证码登录 | Web / 移动端游客 |

### 5.2 用户接口 `UserController`

基础路径：`/api/users`

| 方法 | 路径 | 作用 | 典型调用方 |
| --- | --- | --- | --- |
| GET | `/me` | 获取当前登录用户信息 | 所有已登录角色 |
| GET | `/` | 获取用户列表 | `ADMIN` / `EMPLOYEE` |
| GET | `/{id}` | 获取指定用户详情 | `ADMIN` / `EMPLOYEE` |
| POST | `/` | 新增用户 | `ADMIN` / `EMPLOYEE` |
| PUT | `/{id}` | 修改用户 | `ADMIN` / `EMPLOYEE` |
| POST | `/me/avatar` | 上传当前用户头像 | 所有已登录角色 |
| DELETE | `/{id}` | 删除用户，且管理员不能删除自己 | `ADMIN` |

### 5.3 报修接口 `RepairOrderController`

基础路径：`/api/repair`

| 方法 | 路径 | 作用 | 典型调用方 |
| --- | --- | --- | --- |
| POST | `/upload-image` | 上传报修相关图片 | 已登录用户 |
| POST | `/orders` | 提交报修工单 | 已登录用户 |
| GET | `/orders` | 查询工单列表，可带 `status`、`mineOnly` | 用户 / 员工 / 管理员 |
| GET | `/orders/{id}` | 查询工单详情与流程记录 | 用户 / 员工 / 管理员 |
| PUT | `/orders/{id}/status` | 流转工单状态并记录备注/流程图片 | 员工 / 管理员 / 特定住户确认 |
| GET | `/file?path=...` | 读取报修图片 | 前端图片回显 |

### 5.4 便民服务接口 `ServicePlatformController`

基础路径：`/api/services`

#### 公共浏览能力

| 方法 | 路径 | 作用 | 典型调用方 |
| --- | --- | --- | --- |
| GET | `/categories` | 获取启用中的服务分类 | 所有用户 |
| GET | `/list` | 获取已发布服务列表，可按关键字、分类、状态过滤 | 所有用户 |
| GET | `/{id}` | 获取服务详情 | 游客 / 已登录用户 |
| GET | `/{id}/reviews` | 获取服务评价列表 | 所有用户 |
| GET | `/file?path=...` | 读取服务图片 | 前端图片回显 |

#### 服务提供方能力

| 方法 | 路径 | 作用 | 典型调用方 |
| --- | --- | --- | --- |
| POST | `/upload-image` | 上传服务图片 | 服务发布者 |
| POST | `/provider/entries` | 创建服务入驻条目 | `ADMIN` / `EMPLOYEE` |
| PUT | `/provider/entries/{id}` | 修改服务入驻条目 | `ADMIN` / `EMPLOYEE` |
| GET | `/provider/entries` | 查询自己名下服务条目，可按审核状态筛选 | `ADMIN` / `EMPLOYEE` |
| PUT | `/provider/entries/{id}/operate-status` | 调整服务运营状态 | `ADMIN` / `EMPLOYEE` |

#### 审核与用户交易能力

| 方法 | 路径 | 作用 | 典型调用方 |
| --- | --- | --- | --- |
| GET | `/audit/entries` | 审核列表，可按审核状态和关键字过滤 | `ADMIN` |
| PUT | `/audit/entries/{id}` | 审核通过 / 退回 / 拒绝 | `ADMIN` |
| POST | `/{id}/bookings` | 预约服务 | 已登录用户 |
| GET | `/my/bookings` | 查询当前用户预约记录 | 已登录用户 |
| POST | `/{id}/reviews` | 对已预约服务进行评价 | 已登录用户 |

### 5.5 电费代缴接口 `ElectricityPaymentController`

基础路径：`/api/electricity`

| 方法 | 路径 | 作用 | 典型调用方 |
| --- | --- | --- | --- |
| GET | `/defaults` | 获取默认缴费参数与默认账单信息 | 已登录用户 |
| POST | `/orders` | 创建电费代缴订单 | `USER` / `ADMIN` |
| GET | `/orders/my` | 获取当前用户的电费订单列表 | `USER` / `ADMIN` |
| GET | `/orders/{id}` | 获取单个订单详情 | `USER` / `ADMIN` |
| POST | `/orders/{id}/refresh` | 主动刷新支付状态 | `USER` / `ADMIN` |
| POST | `/notify` | 支付宝异步通知入口 | 支付宝回调 |

### 5.6 社交文件接口 `SocialController`

基础路径：`/api/social`

| 方法 | 路径 | 作用 | 典型调用方 |
| --- | --- | --- | --- |
| POST | `/upload-image` | 上传邻里圈或聊天图片 | 已登录用户 |
| GET | `/file?path=...` | 读取邻里圈或聊天图片 | 前端图片回显 |

### 5.7 健康检查 `HealthController`

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| GET | `/api/health` | 返回后端健康状态 |

## 6. WebSocket 设计

### 6.1 连接入口

- 地址：`/ws/community`
- 鉴权方式：
  - `Authorization: Bearer <token>`
  - 或查询参数 `?token=<jwt>`
- 握手逻辑：`WsAuthHandshakeInterceptor`
  - 校验 JWT
  - 读取用户信息
  - 将 `userId`、`username`、`role`、`nickname`、`avatarPath` 注入会话上下文

### 6.2 连接生命周期

1. 客户端建立连接并通过握手鉴权。
2. 服务端读取 Redis 离线消息队列，若存在则推送 `OFFLINE_BATCH`。
3. 服务端广播最新在线用户列表 `ONLINE_LIST`。
4. 客户端通过 `INIT` 拉取初始数据，包含：
   - feed
   - friends
   - friendRequests
   - groups
   - onlineUsers
   - userDirectory

### 6.3 指令分组

#### 在线与初始化

- `PING` / `PONG`
- `ONLINE_LIST`
- `INIT`
- `USER_DIRECTORY`
- `OFFLINE_BATCH`

#### 好友关系

- `FRIEND_LIST`
- `FRIEND_ADD`
- `FRIEND_REQUEST_LIST`
- `FRIEND_REQUEST_HANDLE`
- `FRIEND_REMOVE`

#### 群组能力

- `GROUP_LIST`
- `GROUP_CREATE`
- `GROUP_QUIT`
- `GROUP_MUTE`
- `GROUP_ANNOUNCEMENT_SET`
- `GROUP_ANNOUNCEMENT_ACK`

#### 邻里圈能力

- `POST_LIST`
- `POST_CREATE`
- `COMMENT_LIST`
- `COMMENT_CREATE`
- 推送事件：`NEW_POST`、`NEW_COMMENT`

#### 聊天能力

- `PRIVATE_SEND`
- `GROUP_SEND`
- `PRIVATE_HISTORY`
- `GROUP_HISTORY`
- 推送事件：`NEW_PRIVATE_MESSAGE`、`NEW_GROUP_MESSAGE`

### 6.4 离线消息

- 在线时通过 `WsPushService` 直接推送。
- 不在线时由 `WsOfflineQueueService` 写入 Redis。
- 下次连接建立后统一通过 `OFFLINE_BATCH` 补拉。

## 7. 角色与权限

系统当前使用三种角色：

- `USER`：住户，主要使用便民服务、报修、邻里圈、聊天、电费代缴。
- `EMPLOYEE`：社区员工，可处理报修工单、发布服务、管理人员。
- `ADMIN`：管理员，拥有审核与系统管理能力。

接口和页面上的具体控制规则还体现在：

- 用户列表、新增、修改：`ADMIN` / `EMPLOYEE`
- 删除用户：`ADMIN`
- 服务入驻：`ADMIN` / `EMPLOYEE`
- 服务审核：`ADMIN`
- 电费代缴：当前前端仅 `USER` / `ADMIN` 可见
- 报修查看范围：普通用户通常只看自己的工单；员工/管理员可处理更大范围工单

## 8. 配置说明

主配置文件：[`src/main/resources/application.yml`](./src/main/resources/application.yml)

### 8.1 服务与数据库

- `server.port`：服务端口，默认 `8080`
- `spring.datasource.url`：MySQL 连接串
- `spring.datasource.username` / `password`：数据库账号密码

### 8.2 Redis

- `spring.redis.host`
- `spring.redis.port`
- `spring.redis.database`
- `spring.redis.timeout`

### 8.3 JWT

- `jwt.secret`
- `jwt.expiration`

### 8.4 短信

- `sms.access-key-id`
- `sms.access-key-secret`
- `sms.sign-name`
- `sms.template-code`
- `sms.valid-time`
- `sms.interval-seconds`

### 8.5 文件上传

- `repair.upload-dir`
- `service-platform.upload-dir`
- `social.upload-dir`
- 各模块 `max-file-size-mb`

### 8.6 支付宝

- `alipay.enabled`
- `alipay.app-id`
- `alipay.private-key`
- `alipay.alipay-public-key`
- `alipay.notify-url`
- `alipay.default-*`

### 8.7 日志

- `logging.level.com.community.mapper`

## 9. 本地启动

### 9.1 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.x
- Redis 6.x / 7.x

### 9.2 创建数据库

```sql
CREATE DATABASE community_app DEFAULT CHARACTER SET utf8mb4;
```

### 9.3 初始化与启动

1. 修改 `application.yml` 中的数据库、Redis、JWT、短信、支付宝配置。
2. 启动 Redis。
3. 运行后端：

```bash
mvn spring-boot:run
```

默认端口：`8080`

### 9.4 打包

```bash
mvn -DskipTests package
```

可执行产物由 `spring-boot-maven-plugin` 生成。

## 10. 数据初始化

- 应用启动时会执行 `schema.sql` 建表。
- 如需演示账号，可手动执行 [`src/main/resources/data.sql`](./src/main/resources/data.sql)。

演示账号对应用户名：

- `admin`
- `employee`
- `user`

## 11. 部署说明

### 11.1 部署依赖

至少需要准备：

- 一个 MySQL 实例
- 一个 Redis 实例
- 一个持久化文件目录，用于挂载 `uploads/repair`、`uploads/service`、`uploads/social`
- 如需启用电费代缴与短信登录，还需要有效的支付宝和阿里云短信配置

### 11.2 生产环境重点

- 不要保留默认数据库密码与默认 JWT Secret。
- `alipay.notify-url` 必须是支付宝能访问到的公网地址。
- 上传目录不要部署在临时磁盘，否则图片会丢失。
- 如果前端和后端分域部署，需要额外处理 CORS 与反向代理。
- WebSocket 需要允许 `/ws/community` 正常升级连接。

## 12. 当前鉴权配置说明

代码中完整实现了基于 Shiro + JWT 的鉴权与过滤链，`ShiroConfig` 中也配置了匿名路径和 JWT 过滤器链；但当前 [`application.yml`](./src/main/resources/application.yml) 中：

- `shiro.enabled: false`
- `shiro.annotations.enabled: false`
- `shiro.web.enabled: false`

因此文档只能如实说明“仓库中存在这套鉴权实现”，具体运行时是否启用，以你当前部署配置为准。

## 13. 相关文档

- [项目总 README](../README.md)
- [Web 端 README](../frontend/README.md)
- [移动端 README](../mobileend/README.md)
