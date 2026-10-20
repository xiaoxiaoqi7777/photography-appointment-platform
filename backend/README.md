# 解忧摄影 后端

Spring Boot 3 + MyBatis-Plus + MySQL 8，提供登录、主题/套餐、订单、照片、反馈与交付接口。

## 环境要求

- JDK 17
- Maven 3.9+
- MySQL 8.0+

## 1. 初始化数据库

```sql
CREATE DATABASE IF NOT EXISTS photography
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;
```

按顺序执行建表与初始化数据脚本（在仓库根目录执行）：

```bash
mysql -u root -p photography < backend/src/main/resources/db/schema.sql
mysql -u root -p photography < backend/src/main/resources/db/data.sql
```

`data.sql` 会写入默认管理员账号：`admin / admin123456`（首次登录后请修改密码）。

## 2. 本地配置

复制配置模板并填写真实值：

```bash
cp backend/src/main/resources/application-example.yml \
   backend/src/main/resources/application-local.yml
```

需要填写：

- `spring.datasource.username` / `spring.datasource.password`：本机 MySQL 账号。
- `app.jwt.secret`：至少 32 字节的随机密钥（生成方式见模板注释）。

`application-local.yml` 已被 `.gitignore` 忽略，不会进入 Git。也可以通过环境变量覆盖：
`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`、`SPRING_PROFILES_ACTIVE`。

## 3. 启动

```bash
cd backend
mvn spring-boot:run
```

默认监听 `http://localhost:8080`。统一响应结构为 `{ code, message, data }`，
登录接口：`POST /api/auth/login`。

## 4. 文件上传目录

- 订单照片：`uploads/{订单ID}/`，不会被静态资源暴露，预览/下载均经过鉴权接口。
- 主题/套餐封面：管理员通过 `POST /api/admin/covers` 上传，落盘 `uploads/covers/`，
  以服务端生成的 UUID 文件名保存；`GET /api/covers/{filename}` 公开只读，仅接受该文件名格式。
  订单照片等其他上传内容不会被这个公开接口读取。

目录可通过 `app.upload-dir` 修改。部署时需保证目录可写并持久化。

## 5. 测试

测试使用本机 `photography` 库（会创建并清理测试数据），并需要真实可连接的 MySQL：

```bash
cd backend
mvn test
```

构建：

```bash
mvn -q package
```
