# 城市社区便民服务整合 APP（基础框架）

当前已实现前后端分离基础版：

- 前端：Vue3 + Vite + Element Plus + Axios
- 后端：Spring Boot + MyBatis + MySQL + Redis + Shiro + JWT
- 已实现：
1. 账号密码注册登录
2. 手机号绑定（`sys_user.phone`）
3. 手机号短信验证码登录（验证码存 Redis 核验，预留阿里云对接点）
4. 三角色（管理员/员工/用户）
5. 人员管理 CRUD

## 1. 项目结构

```text
.
├─ backend   # Spring Boot 后端
└─ frontend  # Vue 管理端
```

## 2. 后端启动

1. 准备 MySQL（8.x）和 Redis（6.x/7.x）。
2. 在 MySQL 中创建数据库：

```sql
CREATE DATABASE community_app DEFAULT CHARACTER SET utf8mb4;
```

3. 修改 `backend/src/main/resources/application.yml` 中数据库账号密码。
4. 首次初始化执行：
   - `backend/src/main/resources/schema.sql`
   - `backend/src/main/resources/data.sql`
5. 启动后端：

```bash
cd backend
mvn spring-boot:run
```

默认端口：`8080`

说明：
- 对于旧数据库（没有 `phone` 字段），系统启动时会自动尝试补字段。
- 也可手动执行：`backend/src/main/resources/migration_add_phone.sql`

## 3. 前端启动

```bash
cd frontend
npm install
npm run dev
```

默认端口：`5173`（已代理 `/api` 到 `http://localhost:8080`）

## 4. 默认账号

- 管理员：`admin / admin123`
- 员工：`employee / employee123`
- 用户：`user / user123`

默认手机号（初始化数据）：`15138114047`

## 5. 接口列表

认证相关：
- `POST /api/auth/register` 注册（支持手机号）
- `POST /api/auth/login` 账号密码登录
- `POST /api/auth/sms/send` 发送短信验证码
- `POST /api/auth/sms/login` 手机号验证码登录

用户相关：
- `GET /api/users/me` 当前登录用户
- `GET /api/users` 用户列表（ADMIN/EMPLOYEE）
- `GET /api/users/{id}` 用户详情（ADMIN/EMPLOYEE）
- `POST /api/users` 创建用户（ADMIN/EMPLOYEE）
- `PUT /api/users/{id}` 更新用户（ADMIN/EMPLOYEE）
- `DELETE /api/users/{id}` 删除用户（ADMIN）

## 6. Redis 缓存策略

- 用户详情缓存：`sys:user:{id}`
- 用户列表缓存：`sys:user:list`
- 短信验证码缓存：`sms:login:code:{countryCode}:{phone}`
- 发送频控缓存：`sms:login:interval:{countryCode}:{phone}`

## 7. 短信参数配置

在 `backend/src/main/resources/application.yml` 中：

- `sms.scheme-name` 方案名称
- `sms.default-country-code` 默认国家编码
- `sms.default-phone-number` 默认手机号
- `sms.code-length` 验证码长度（4~8）
- `sms.valid-minutes` 验证码有效分钟数
- `sms.interval-seconds` 发送间隔秒数
- `sms.return-verify-code` 是否返回验证码（开发阶段建议 `true`）

已实现直接接入阿里云真实短信网关（AK/SK + API 调用），可以在现有接口上无缝替换发送与校验实现（仅支持阿里云绑定的测试手机号码使用）。
