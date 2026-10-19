# 解忧摄影 前端

Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router + Axios，
对接仓库内 `backend/` 的 Spring Boot 接口（契约见 `docs/API接口约定.md`）。

## 环境要求

- Node.js 24（开发时使用 v24.19.0）
- npm 11+
- 后端已启动：`http://localhost:8080`

## 1. 安装依赖

```bash
cd frontend
npm install
```

## 2. 启动开发服务器

```bash
npm run dev
```

默认访问 `http://localhost:5173`。开发服务器在 `vite.config.ts` 中将 `/api`
代理到 `http://localhost:8080`，因此本地无需处理跨域。

## 3. 环境变量

| 变量 | 说明 | 开发默认值 |
| --- | --- | --- |
| `VITE_API_BASE` | axios 基础路径 | `/api` |
| `VITE_APP_TITLE` | 站点名称 | `解忧摄影` |

开发与生产配置分别见 `.env.development`、`.env.production`。
若生产环境前后端不同域部署，请修改 `VITE_API_BASE` 为完整后端地址，并确保后端允许该来源的跨域请求。

## 4. 构建

```bash
npm run build
```

`build` 先执行 `vue-tsc --noEmit` 类型检查，再执行 Vite 生产构建，产物输出到 `dist/`。
本地预览生产包：

```bash
npm run preview
```

## 5. 页面与角色

| 路径 | 说明 | 角色 |
| --- | --- | --- |
| `/` | 主题列表（仅上架） | 公开 |
| `/themes/:id` | 主题详情与套餐列表 | 公开 |
| `/packages/:id` | 套餐详情与预约入口 | 公开 |
| `/booking?packageId=` | 预约下单 | USER |
| `/orders`、`/orders/:id` | 我的订单、订单详情与状态操作 | USER |
| `/orders/:id/photos` | 水印预览、交付后原图下载 | USER |
| `/profile` | 个人资料与修改密码 | 登录用户 |
| `/admin/orders` | 订单列表、状态筛选、关键字搜索 | ADMIN |
| `/admin/orders/:id` | 确认 / 拒绝 / 标记拍摄完成、查看反馈 | ADMIN |
| `/admin/orders/:id/upload` | 批量上传精修照片 | ADMIN |
| `/admin/themes`、`/admin/packages` | 主题 / 套餐管理（仅上下架） | ADMIN |

管理员账号由后端初始化脚本创建：`admin / admin123456`。

## 6. 与后端的约定实现

- 统一解包 `{ code, message, data }`；非 0 业务码统一提示，40100/HTTP 401 清除令牌并跳转登录，40300 提示无权限。
- 请求自动携带 `Authorization: Bearer <token>`；令牌与角色持久化在 `localStorage`。
- 路由守卫校验登录态与管理端权限；前端隐藏按钮仅用于体验，权限最终以服务端为准。
- 照片预览与下载接口需要鉴权，不能直接作为 `<img src>`：
  预览通过 axios 拉取 Blob 后生成本地 Object URL（`src/components/AuthImage.vue`，带懒加载与灯箱预览）；
  原图下载通过 Blob + `Content-Disposition` 解析文件名后保存。
- 当前所有页面均连接真实后端接口，未使用 Mock 数据。

## 7. 目录结构

```
src/
├── api/          # axios 实例与按模块接口、类型定义
├── components/   # AuthImage、订单状态标签
├── layouts/      # 用户端 / 管理端布局
├── router/       # 路由与守卫
├── stores/       # Pinia（登录态）
├── styles/       # 全局样式
├── utils/        # 格式化、订单状态、下载、存储
└── views/        # 认证、用户端、管理端页面
```

## 8. 部署提示

前端为单页应用，Nginx 等静态服务器需配置 history 回退到 `index.html`，
并将 `/api` 反向代理到后端服务：

```nginx
location / {
  try_files $uri $uri/ /index.html;
}

location /api/ {
  proxy_pass http://127.0.0.1:8080;
  proxy_set_header Host $host;
  proxy_set_header X-Real-IP $remote_addr;
}
```

## 9. 演示图片

主题与套餐封面存放在 `public/images/`，构建时原样输出到站点根路径，
数据库中的 `cover_url` 使用 `/images/themes/...`、`/images/packages/...` 相对路径：

| 数据 | 文件 |
| --- | --- |
| 森系写真 | `themes/forest.jpg` |
| 婚纱照 | `themes/wedding.jpg` |
| 全家福 | `themes/family.jpg` |
| 森系轻写真 | `packages/forest-light.jpg` |
| 森系全写真 | `packages/forest-full.jpg` |
| 婚纱半日拍 | `packages/wedding-half-day.jpg` |
| 婚纱全日拍 | `packages/wedding-full-day.jpg` |
| 全家福标准 | `packages/family-standard.jpg` |

图片来自 [Unsplash](https://unsplash.com/license)，仅作为项目演示素材。
源文件与演示数据初始化见 `backend/src/main/resources/db/data.sql`。
