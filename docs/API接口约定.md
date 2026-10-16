# 解忧摄影 API 接口约定

> 本文档依据仓库中已有的两份原始文档整理，作为前后端协作的接口约定依据：
> - `docs/解忧摄影_需求分析说明书.docx`（以下简称《需求文档》）
> - `docs/解忧摄影_数据库设计说明书.docx`（以下简称《数据库设计》）
>
> 本文档**不新增原始文档之外的功能、字段与状态**。原始文档未明确规定的内容，本文档给出方案并在
> 第 9 节集中登记；其中**团队已确认的推荐方案**在本版本中已提升为正式约定（标注【已确认】）。
>
> **原始文档未被修改**（遵循任务约束）。第 9.8 节记录了两份原始文档中的内部冲突及本文档采用的
> 处理口径，供文档归口人后续同步修订原始文档。
>
> 数据库字段名、类型以《数据库设计》2.3 节为准，本文档不修改任何表结构。

---

## 1. 标注约定

| 标记 | 含义 |
| --- | --- |
| 【文档明确】 | 该内容在《需求文档》或《数据库设计》中有直接依据 |
| 【已确认】 | 原始文档未明确规定，团队已确认本文档给出的推荐方案，作为正式约定 |
| 【文档冲突】 | 两份文档之间或文档内部存在不一致，处理口径见第 9.8 节 |

---

## 2. 通用约定

### 2.1 基础路径与协议

- 协议：HTTP/HTTPS，REST 风格，数据格式 `application/json`（文件上传为 `multipart/form-data`）。
- 基础路径：`/api`。【文档明确】《需求文档》登录用例中给出 `POST /api/auth/login`。
- 其余各接口路径采用本文档第 5 节约定（`/api` 前缀、RESTful、管理端置于 `/api/admin/**`）。【已确认】

### 2.2 认证与请求头

- 鉴权方式：JWT。【文档明确】
- 登录成功后签发 JWT，有效期 **24 小时**。【文档明确】
- 后续请求统一在请求头携带令牌：【文档明确】

```
Authorization: Bearer <token>
```

- JWT 中**包含角色声明（USER / ADMIN）**。【文档明确】《需求文档》3.2.6-1 数据需求 D-1。
- 未登录访问返回 **401**；越权访问（角色不匹配）返回 **403**。【文档明确】

### 2.3 统一响应结构

所有**普通 JSON 接口**（不含文件流接口）返回统一结构：【文档明确】《需求文档》3.4.2 与 3.3.3。

```json
{
  "code": 0,
  "message": "成功",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | int | 业务状态码（详见 2.5），`0` 表示成功【已确认：成功码取 0】 |
| message | string | 可读提示信息；失败时为错误原因【文档明确：错误信息包含可读提示】 |
| data | object / array / null | 业务数据；无数据时为 `null` |

- 文件流接口（原图/水印图预览与下载）**不适用**上述结构，直接返回文件流或重定向地址。【文档明确：照片输出为图片/下载地址，非 JSON 包裹】

### 2.4 HTTP 状态码

【已确认】采用语义化状态码，响应体 `code` 与 HTTP 状态码保持一致映射，便于前端统一处理：

| 状态码 | 含义 | 依据 |
| --- | --- | --- |
| 200 | 请求成功 | 【已确认】 |
| 400 | 请求参数校验失败 | 【已确认】 |
| 401 | 未登录 / 令牌无效或过期 | 【文档明确】 |
| 403 | 越权访问 / 无权访问该资源（如他人订单、订单未交付下载原图） | 【文档明确】 |
| 404 | 资源不存在 | 【已确认】 |
| 409 | 业务状态冲突（如非法状态流转） | 【已确认】 |
| 500 | 服务器内部错误 | 【已确认】 |

### 2.5 业务错误码

《需求文档》3.4.2 明确“非法状态流转返回业务错误码”，但未给出具体数值。【已确认】采用下表编码，后续按模块段位扩展（如 41xxx 订单）。

| code | 含义 | HTTP 状态 |
| --- | --- | --- |
| 0 | 成功 | 200 |
| 40000 | 参数校验失败 | 400 |
| 40100 | 未登录 / 令牌无效或过期 | 401 |
| 40300 | 越权访问 | 403 |
| 40400 | 资源不存在 | 404 |
| 40900 | 业务状态非法（非法状态流转 / 前置条件不满足） | 409 |
| 40901 | 唯一性冲突（用户名已存在、订单号重复等） | 409 |
| 50000 | 服务器内部错误 | 500 |

### 2.6 分页约定

- 列表查询一律分页，默认每页 **10 条**。【文档明确】《需求文档》3.2.3-3、3.5.2。
- 分页请求参数：【已确认】

| 参数 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| page | int | 1 | 页码，从 1 开始 |
| pageSize | int | 10 | 每页条数，上限 100 |

- 分页响应结构：【已确认】

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "total": 100,
    "page": 1,
    "pageSize": 10,
    "list": []
  }
}
```

### 2.7 格式与精度约定

【文档明确】《需求文档》3.3.1：

- 套餐价格：人民币元，保留两位小数，数据库类型 `DECIMAL(10,2)`。
- 文件大小：以字节（byte）为单位记录，展示时换算为 KB/MB。
- 拍摄日期格式：`yyyy-MM-dd`。
- 拍摄时间段格式：`HH:mm-HH:mm`（上午 09:00-12:00、下午 13:00-18:00、全天 09:00-18:00）。
- 订单创建时间与状态变更时间：精确到秒。
- 订单状态：严格限定为 8 种枚举值，不允许非法状态或非法流转。

【已确认】JSON 中日期时间序列化：日期 `yyyy-MM-dd`，时间 `yyyy-MM-dd HH:mm:ss`（服务器本地时区，精确到秒）。
【已确认】JSON 使用 camelCase 字段命名，数据库保持 snake_case，由映射层（MyBatis/Jackson）转换。

### 2.8 文件上传约定

【文档明确】《需求文档》3.2.4-4、3.2.5、3.4.1 与《数据库设计》2.2：

- 允许类型白名单：`jpg / jpeg / png`。
- 单文件大小：≤ 10MB。
- 存储位置：服务器本地目录 `uploads/{订单ID}/`，数据库仅存相对路径。
- 上传轮次（`upload_round`）与反馈轮次对应，轮次自增。
- 交付前生成水印预览图，文件名后缀 `_wm`，水印文字为平台名称。
- 上传接口需同时做类型白名单与大小校验，并防范路径穿越与不可信文件名。【AGENTS.md 第 9 节】
- 【已确认】批量上传字段名 `files`，单次上限 20 张；部分文件失败仍返回 HTTP 200，`data` 含成功与失败清单。
- 【已确认】`uploads/` 目录不直接对外暴露为静态资源，文件读取统一经鉴权接口；如需直链使用短期签名 URL。

---

## 3. 角色与权限

### 3.1 角色定义

【文档明确】《需求文档》3.2.1、3.2.6；《数据库设计》2.3.1：

| 角色 | 枚举值 | 说明 |
| --- | --- | --- |
| 普通用户 | `USER` | 注册时固定为该角色；服务端限定，客户端不可指定管理员 |
| 管理员 | `ADMIN` | 由初始化脚本预置，不开放注册 |

### 3.2 功能权限矩阵

【文档明确】《需求文档》3.2.6：

| 功能 | USER | ADMIN |
| --- | :---: | :---: |
| 注册 / 登录 | √ | √ |
| 浏览主题、套餐 | √ | √ |
| 提交预约订单 | √ | × |
| 取消待确认订单 | √ | × |
| 查看自己的订单 | √ | ×（查看全部订单） |
| 确认 / 拒绝订单 | × | √ |
| 标记拍摄完成 | × | √ |
| 上传 / 重新上传照片 | × | √ |
| 查看照片（水印预览 / 原图） | √ | √（原图） |
| 提交修改意见 | √ | × |
| 确认交付 | √ | × |
| 主题管理（增删改） | × | √ |
| 套餐管理（增删改） | × | √ |

### 3.3 鉴权与资源归属规则

【文档明确】《需求文档》3.2.6-1、《AGENTS.md》第 9 节：

1. 每个 API 请求经 JWT 过滤器完成身份认证与角色鉴权。
2. 未携带或令牌无效/过期 → **401**；调用管理端接口（角色不匹配）→ **403**。
3. 用户仅能查看和操作**本人**的订单、反馈与照片；访问他人资源 → **403**。
4. 管理员操作必须在后端进行角色校验，不得仅靠前端隐藏按钮。
5. 【已确认】鉴权过滤器同时校验用户状态（`t_user.status`），被禁用用户（`status = 0`）返回 401/403。

---

## 4. 订单状态流转

### 4.1 状态枚举

【文档明确】《需求文档》3.3.1 与 3.2.3 状态机；《数据库设计》2.3.4：

| 状态码 | 状态名 | 是否终态 |
| --- | --- | :---: |
| `PENDING` | 待确认 | 否 |
| `CONFIRMED` | 已确认（待拍摄） | 否 |
| `REJECTED` | 已拒绝 | 是 |
| `CANCELLED` | 已取消 | 是 |
| `SHOOTING_DONE` | 拍摄完成（修图中） | 否 |
| `UPLOADED` | 精修已上传（待反馈） | 否 |
| `MODIFYING` | 修改中 | 否 |
| `DELIVERED` | 已交付 | 是 |

### 4.2 状态流转规则

【文档明确】《需求文档》3.2.3、3.2.4、3.2.7；《数据库设计》2.3.4“状态枚举与流转”：

| 当前状态 | 合法后继状态 | 触发方 | 触发动作 |
| --- | --- | --- | --- |
| （无） | `PENDING` | 用户 | 提交预约（创建订单，初始态） |
| `PENDING` | `CONFIRMED` | 管理员 | 确认订单 |
| `PENDING` | `REJECTED` | 管理员 | 拒绝订单（原因必填） |
| `PENDING` | `CANCELLED` | 用户 | 取消订单（仅待确认态） |
| `CONFIRMED` | `SHOOTING_DONE` | 管理员 | 标记拍摄完成 |
| `SHOOTING_DONE` | `UPLOADED` | 管理员 | 上传精修照片 |
| `UPLOADED` | `DELIVERED` | 用户 | 确认交付 |
| `UPLOADED` | `MODIFYING` | 用户 | 申请修改 |
| `MODIFYING` | `UPLOADED` | 管理员 | 重新上传照片 |
| `REJECTED` / `CANCELLED` / `DELIVERED` | —（终态） | — | — |

> 【文档冲突】《需求文档》3.2.3 的状态机表中，“触发动作”列与后继状态对应关系存在错位
> （例如把 `CONFIRMED` 的触发动作标为“确认订单”、把 `SHOOTING_DONE` 标为“标记拍摄完成”）。
> 本节以上表为准，依据是各用例说明（3.2.4-1 ~ 3.2.4-4、3.2.7-1、3.2.7-2）与《数据库设计》2.3.4
> 的“状态枚举与流转”描述。原始文档未修改，处理口径登记于第 9.8 节。

约束：【文档明确】

1. 仅 `PENDING` 允许用户取消；取消为终态不可恢复。
2. 仅 `UPLOADED` 允许用户确认交付或申请修改。
3. `MODIFYING` 与 `UPLOADED` 之间可无限循环。
4. 非法状态流转一律拒绝并返回业务错误码。
5. 状态变更采用条件更新（乐观锁），并**在同一事务中**同步写入 `t_order_log`。
6. `REJECTED` 为终态，`DELIVERED` 为终态且不可再申请修改。

### 4.3 状态变更日志

【文档明确】《数据库设计》2.3.7：每次订单状态变更须在同一事务中写入 `t_order_log`，用于订单详情页的状态时间线展示，字段见第 8 节 `t_order_log`。

---

## 5. 接口清单（按模块）

> 第 5 节各接口路径与字段为【已确认】约定，按此实现并回写为正式契约。
> 请求/响应字段中，直接映射数据库字段的以《数据库设计》为准。

### 5.1 用户模块

#### 5.1.1 用户注册

- 方法与路径：`POST /api/auth/register`【已确认】
- 调用角色：未注册用户（无需登录）
- 请求参数（`application/json`）：

| 字段 | 类型 | 必填 | 校验规则 | 依据 |
| --- | --- | --- | --- | --- |
| username | string | 是 | 3~20 字符，全局唯一 | 【文档明确】D-1；长度口径见 9.8 |
| password | string | 是 | ≥6 字符，服务端 BCrypt 加密存储 | 【文档明确】D-2/B-3 |
| nickname | string | 否 | 昵称 | 【文档明确】D-3 |
| phone | string | 否 | 手机号，非必填、不唯一，正则 `^1[3-9]\d{9}$` | 【文档明确】D-3；格式【已确认】 |

- 响应 `data`：无业务数据（或仅返回提示）。角色由服务端固定为 `USER`，**不接受客户端传入角色**。【文档明确 B-2】
- 校验规则：服务端校验用户名唯一；前端与后端双重校验非空与格式。【文档明确】
- 异常情况：
  - 用户名已存在 → 提示“用户名已被占用”，`code = 40901`。
  - 参数缺失或格式不合法 → `code = 40000`。
  - 若客户端尝试指定 `ADMIN` 角色 → 忽略/拒绝，强制 `USER`。【AGENTS.md 第 9 节】

#### 5.1.2 用户登录

- 方法与路径：`POST /api/auth/login`【文档明确】
- 调用角色：已注册用户（无需登录）
- 请求参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | string | 是 | 登录名 |
| password | string | 是 | 密码（后端按 BCrypt 校验） |

- 响应 `data`：`token` 与 `role`。【文档明确】同时返回 `expiresIn`（秒，取 86400）【已确认】。

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "token": "<JWT>",
    "role": "USER",
    "expiresIn": 86400
  }
}
```

- 校验规则：按用户名查询用户，校验 BCrypt 密码；校验 `status = 1`；JWT 有效期 24 小时。【文档明确】
- 异常情况：
  - 用户名或密码错误 → 提示“用户名或密码错误”（不区分具体原因）。
  - 账号被禁用（`status = 0`）→ 提示“账号已禁用”【已确认】。
  - token 过期 → 返回 401，前端跳转登录页重新登录。【文档明确 A-4.2】

#### 5.1.3 退出登录

- 方法与路径：本期**不提供后端退出接口**。【已确认】
- 调用角色：已登录用户
- 说明：【文档明确】COM-03 描述为“前端清除本地 token”。本期采用无状态 JWT，仅前端清除 token；待需要强制失效时再引入令牌黑名单（本期不做）。
- 请求参数：无。
- 响应：无。

#### 5.1.4 获取当前用户信息

- 方法与路径：`GET /api/users/me`【已确认】
- 调用角色：已登录用户（USER / ADMIN）
- 请求参数：无（根据 token 解析用户）。
- 响应 `data`（映射 `t_user`，**排除 `password`**）：

| 字段 | 类型 | 依据 |
| --- | --- | --- |
| id | long | `t_user.id` |
| username | string | `t_user.username` |
| nickname | string | `t_user.nickname` |
| phone | string | `t_user.phone` |
| avatar | string | `t_user.avatar` |
| role | string | `t_user.role` |
| status | int | `t_user.status`（1 正常 / 0 禁用） |
| createTime | datetime | `t_user.create_time` |

- 校验规则：根据 token 返回当前用户资料。【文档明确 COM-04】
- 异常情况：未登录/令牌无效 → 401；用户不存在 → 40400；用户被禁用 → 40100/40300【已确认】。

#### 5.1.5 个人信息维护

- 方法与路径：`PUT /api/users/me`【已确认】
- 调用角色：已登录用户
- 请求参数（可选字段，仅提交需要修改的项）：

| 字段 | 类型 | 必填 | 校验规则 | 依据 |
| --- | --- | --- | --- | --- |
| nickname | string | 否 | 昵称 | 【文档明确 COM-05】 |
| phone | string | 否 | 手机号，正则 `^1[3-9]\d{9}$` | 【文档明确 COM-05】 |
| oldPassword | string | 修改密码时必填 | 校验原密码 | 【已确认】 |
| newPassword | string | 修改密码时必填 | ≥6 字符，重新 BCrypt 加密 | 【文档明确：可修改登录密码】 |

- 说明：`avatar`（头像）字段存在于 `t_user`，但《需求文档》未描述头像上传/修改功能，**本期不启用头像功能**。【已确认】
- 响应 `data`：更新后的用户信息（排除 `password`）。
- 异常情况：原密码错误 → 40000；参数不合法 → 40000。

### 5.2 主题模块

`t_theme` 字段见第 8 节。

#### 5.2.1 管理员查看全部主题

- 方法与路径：`GET /api/admin/themes`【已确认】
- 调用角色：ADMIN
- 请求参数：分页参数（可选）、状态筛选（含上下架）【文档明确 THM-01】
- 响应 `data`：主题列表（含 `status` 上下架状态），分页结构。
- 异常情况：越权 → 403。

#### 5.2.2 新增主题

- 方法与路径：`POST /api/admin/themes`【已确认】
- 调用角色：ADMIN
- 请求参数：

| 字段 | 类型 | 必填 | 校验规则 | 依据 |
| --- | --- | --- | --- | --- |
| name | string | 是 | 主题名称，非空 | `t_theme.name` |
| coverUrl | string | 否 | 封面图 URL | `t_theme.cover_url` |
| description | string | 否 | 描述 | `t_theme.description` |
| tags | string[] | 否 | 标签数组（接口层数组，持久层逗号分隔）【已确认】 | `t_theme.tags` |
| sort | int | 否 | 排序值，默认 0 | `t_theme.sort` |
| status | int | 否 | 默认 1 上架 | `t_theme.status` |

- 响应 `data`：新建主题信息（含 `id`）。
- 异常情况：参数校验失败 → 40000；越权 → 403。

#### 5.2.3 编辑主题

- 方法与路径：`PUT /api/admin/themes/{id}`【已确认】
- 调用角色：ADMIN
- 请求参数：同“新增主题”的可选字段，`id` 为路径参数。
- 异常情况：主题不存在 → 40400；参数校验失败；越权 → 403。

#### 5.2.4 主题上下架

- 方法与路径：`PATCH /api/admin/themes/{id}/status`【已确认】
- 调用角色：ADMIN
- 请求参数：`{ "status": 0 }`（下架）或 `{ "status": 1 }`（上架）。
- 说明：
  - 【文档明确】下架后用户端不可见；有历史订单引用的主题不应破坏历史订单数据。
  - 【已确认】只做软下架（`t_theme.status`：1 上架 / 0 下架），**不提供物理删除**；“删除”语义等同下架。
- 异常情况：主题不存在 → 40400；越权 → 403。

#### 5.2.5 用户端主题列表

- 方法与路径：`GET /api/themes`【已确认】
- 调用角色：用户（未登录 / 已登录均可）【文档明确 3.2.2-1】
- 请求参数：分页参数（可选）；固定按 `sort` 倒序返回【已确认】。
- 响应 `data`：**仅上架主题**的列表（名称 / 封面 / 标签等）。【文档明确 THM-05 / B-1】
- 异常情况：无上架主题 → 返回空列表（前端显示空态）。【文档明确 A-1】

#### 5.2.6 主题详情

- 方法与路径：`GET /api/themes/{id}`【已确认】
- 调用角色：用户 / 管理员
- 说明：用户端可查看上架主题详情；管理端复用同一资源返回完整字段。【已确认】
- 约束：用户端访问已下架主题时应不可见（返回 40400）【已确认】。

### 5.3 套餐模块

`t_package` 字段见第 8 节。

#### 5.3.1 管理员查看全部套餐

- 方法与路径：`GET /api/admin/packages`【已确认】
- 调用角色：ADMIN
- 请求参数：分页参数（可选）、按主题筛选（可选）。【文档明确 PKG-01】
- 响应 `data`：套餐列表（含上下架状态）、分页结构。

#### 5.3.2 新增套餐

- 方法与路径：`POST /api/admin/packages`【已确认】
- 调用角色：ADMIN
- 请求参数：

| 字段 | 类型 | 必填 | 校验规则 | 依据 |
| --- | --- | --- | --- | --- |
| themeId | long | 是 | 必须关联主题 | `t_package.theme_id`；B-3 |
| name | string | 是 | 套餐名称，非空 | `t_package.name` |
| price | decimal | 是 | >0 且保留两位小数 | `t_package.price`；B-1 |
| retouchCount | int | 是 | 正整数（≥1） | `t_package.retouch_count`；B-2 |
| duration | string | 否 | 拍摄时长（如“2 小时”） | `t_package.duration` |
| includes | string | 否 | 包含内容说明 | `t_package.includes` |
| coverUrl | string | 否 | 套餐封面图 URL | `t_package.cover_url` |
| sort | int | 否 | 排序值，默认 0 | `t_package.sort` |
| status | int | 否 | 默认 1 上架 | `t_package.status` |

- 响应 `data`：新建套餐信息（含 `id`）。
- 校验规则：价格 >0 且两位小数；精修张数为正整数；必须关联主题。【文档明确 B-1/B-2/B-3】
- 异常情况：
  - 价格 ≤0 或精修张数 <1 → 40000。
  - 关联主题已下架 → 提示但允许保存。【文档明确 A-5】
  - 越权 → 403。

#### 5.3.3 编辑套餐

- 方法与路径：`PUT /api/admin/packages/{id}`【已确认】
- 调用角色：ADMIN
- 请求参数：同“新增套餐”的可选字段。
- 异常情况：套餐不存在 → 40400；参数校验失败；越权 → 403。

#### 5.3.4 套餐上下架

- 方法与路径：`PATCH /api/admin/packages/{id}/status`【已确认】
- 调用角色：ADMIN
- 请求参数：`{ "status": 0 }` 或 `{ "status": 1 }`。
- 说明：`t_package.status` 为软下架（1 上架 / 0 下架）；下架后用户端不可见。【文档明确 PKG-04】
  【已确认】只做软下架，不提供物理删除。
- 异常情况：套餐不存在 → 40400；越权 → 403。

#### 5.3.5 用户端套餐列表

- 方法与路径：`GET /api/themes/{themeId}/packages`【已确认】
- 调用角色：用户（未登录 / 已登录均可）
- 请求参数：`themeId`（按主题筛选）。【文档明确 PKG-05】
- 响应 `data`：**仅上架套餐**（价格 / 精修张数 / 时长 / 包含内容）。【文档明确 B-1】
- 异常情况：无上架套餐 → 空列表；套餐已下架 → 列表中不展示。【文档明确 A-4】

#### 5.3.6 套餐详情

- 方法与路径：`GET /api/packages/{id}`【已确认】
- 调用角色：用户 / 管理员
- 说明：预约下单前可查看套餐详情；用户端仅上架套餐可见，管理端返回完整字段。【已确认】

### 5.4 预约订单模块

`t_order` 字段见第 8 节。

#### 5.4.1 提交预约（创建订单）

- 方法与路径：`POST /api/orders`【已确认】
- 调用角色：已登录用户（USER）
- 前置条件：用户已登录；所选套餐处于上架状态。【文档明确】
- 请求参数：

| 字段 | 类型 | 必填 | 校验规则 | 依据 |
| --- | --- | --- | --- | --- |
| themeId | long | 是 | 必选主题 | D-2 |
| packageId | long | 是 | 必选且上架套餐，且属于 themeId | D-2 / B-1；关联校验【已确认】 |
| shootDate | date | 是 | 不得早于当天（`yyyy-MM-dd`） | D-3 / A-2 |
| shootSlot | string | 是 | `MORNING` / `AFTERNOON` / `FULL_DAY` | `t_order.shoot_slot` |
| remark | string | 否 | 备注 ≤200 字 | D-4；长度口径见 9.8 |

- 响应 `data`：订单号（`orderNo`）与状态（`PENDING`）。【文档明确】
- 服务端行为：写入订单，`status = PENDING`；`order_no` 按 9.4 O-01 规则生成；`price` 取**下单时套餐价格快照**（`t_order.price`）；同一事务写入 `t_order_log`（动作 `CREATE`，`to_status=PENDING`）。【文档明确】
- 校验规则：仅上架套餐可下单；套餐必须属于所选主题；初始状态固定 `PENDING`；不做档期冲突检测。【文档明确 B-1/B-2/B-3】
- 异常情况：
  - 日期早于今天 → 40000。
  - 未登录 → 401（跳转登录页）。
  - 套餐已下架 → 拒绝下单。
  - 套餐不属于所选主题 → 40000。
  - 管理员角色调用 → 403（管理员不可提交预约）。

#### 5.4.2 我的订单列表

- 方法与路径：`GET /api/orders`【已确认】
- 调用角色：已登录用户（USER）
- 请求参数：分页参数（默认每页 10 条）、状态筛选 `status`（单状态，可选）。【文档明确】
- 响应 `data`：**仅本人订单**分页列表。【文档明确 B-1】
- 异常情况：无订单记录 → 空列表（空态）。

#### 5.4.3 订单详情（用户）

- 方法与路径：`GET /api/orders/{id}`【已确认】
- 调用角色：已登录用户（USER，仅本人）
- 响应 `data`：
  - 订单完整信息（`t_order`，不含敏感内部字段）；
  - 状态时间线（来自 `t_order_log`，按 9.7 S-01 裁剪）；
  - 历轮反馈列表（来自 `t_feedback`）；
  - 照片入口（按状态提供水印预览或原图）。【文档明确 3.2.3-3】
- 异常情况：订单非本人 → **403**。【文档明确 A-4】

#### 5.4.4 取消订单

- 方法与路径：`POST /api/orders/{id}/cancel`【已确认】
- 调用角色：已登录用户（USER，仅本人）
- 前置条件：订单属于本人；状态为 `PENDING`。【文档明确】
- 请求参数：无（前端二次确认属前端行为）。
- 服务端行为：条件更新 `status = CANCELLED`，同事务写入 `t_order_log`（动作 `CANCEL`）。
- 异常情况：
  - 订单非本人 → **403**。
  - 状态非 `PENDING` → 返回 `40900`（非法状态流转）。【文档明确 A-4】

#### 5.4.5 管理员查看全部订单

- 方法与路径：`GET /api/admin/orders`【已确认】
- 调用角色：ADMIN
- 请求参数：分页 + 状态筛选 + 关键字搜索（用户名 / 主题，模糊匹配）。【文档明确 ADM-01】
- 响应 `data`：全部订单分页列表。
- 异常情况：越权 → 403。

#### 5.4.6 管理员查看订单详情

- 方法与路径：`GET /api/admin/orders/{id}`【已确认】
- 调用角色：ADMIN
- 响应 `data`：订单完整信息 + 状态时间线 + 用户历轮反馈（便于按意见修图）。【文档明确 ADM-06 / ADM-07】
- 异常情况：订单不存在 → 40400。

#### 5.4.7 确认订单

- 方法与路径：`POST /api/admin/orders/{id}/confirm`【已确认】
- 调用角色：ADMIN
- 前置条件：订单状态为 `PENDING`。【文档明确】
- 服务端行为：条件更新 `status = CONFIRMED`，同事务写入 `t_order_log`（动作 `CONFIRM`）。
- 异常情况：状态非 `PENDING` → `40900`。【文档明确 A-2】

#### 5.4.8 拒绝订单

- 方法与路径：`POST /api/admin/orders/{id}/reject`【已确认】
- 调用角色：ADMIN
- 前置条件：订单状态为 `PENDING`。【文档明确】
- 请求参数：

| 字段 | 类型 | 必填 | 校验规则 | 依据 |
| --- | --- | --- | --- | --- |
| rejectReason | string | 是 | 拒绝原因，**必填**，建议 ≤200 字 | `t_order.reject_reason`；B-1；长度【已确认】 |

- 服务端行为：条件更新 `status = REJECTED`，写入 `reject_reason`，同事务写入 `t_order_log`（动作 `REJECT`，`remark` 存原因）。
- 响应说明：拒绝原因对用户可见。【文档明确】
- 异常情况：原因为空 → 40000；状态非 `PENDING` → `40900`。

#### 5.4.9 标记拍摄完成

- 方法与路径：`POST /api/admin/orders/{id}/shoot-done`【已确认】
- 调用角色：ADMIN
- 前置条件：订单状态为 `CONFIRMED`；线下拍摄已完成。【文档明确】
- 服务端行为：条件更新 `status = SHOOTING_DONE`，同事务写入 `t_order_log`（动作 `SHOOT_DONE`）；前端需二次确认。
- 异常情况：状态非 `CONFIRMED` → `40900`。【文档明确 A-1】

### 5.5 照片模块

`t_photo` 字段见第 8 节。

#### 5.5.1 上传精修照片

- 方法与路径：`POST /api/admin/orders/{id}/photos`【已确认】
- 调用角色：ADMIN
- 前置条件：订单状态为 `SHOOTING_DONE` 或 `MODIFYING`；文件为 JPG/JPEG/PNG 且单文件 ≤10MB。【文档明确】
- 请求参数：`multipart/form-data`，多文件批量上传，字段名 `files`，单次上限 20 张。【已确认】
- 服务端行为：【文档明确】
  1. 后端二次校验类型与大小；
  2. 文件存储至 `uploads/{订单ID}/`；
  3. 同步生成水印预览图（后缀 `_wm`），失败记录日志并允许重试（`watermark_path` 暂空）【已确认】；
  4. 写入 `t_photo` 记录：`order_id`、`original_name`、`file_path`、`watermark_path`、`file_size`、`upload_round`、`create_time`；
  5. 轮次自增规则：首次 `SHOOTING_DONE → UPLOADED` 时 `upload_round = 1`；此后每经历一次 `MODIFYING → UPLOADED` 轮次加 1；
  6. 订单状态更新为 `UPLOADED`，同事务写入 `t_order_log`（动作 `UPLOAD`）。
- 响应 `data`：HTTP 200，`data = { "success": [], "failed": [{ "name": "", "reason": "" }] }`。【已确认：部分失败仍返回 200】
- 异常情况：
  - 类型或大小不符 → 该文件计入 `failed` 并提示原因。
  - 部分文件失败 → 成功文件正常入库，返回失败清单。【文档明确 A-4】
  - 状态非 `SHOOTING_DONE`/`MODIFYING` → `40900`（非法状态流转）。

#### 5.5.2 查看照片列表 / 水印预览

- 方法与路径：`GET /api/orders/{id}/photos`（列表）与 `GET /api/orders/{id}/photos/{photoId}/preview`（单图字节流）【已确认】
- 调用角色：
  - USER：仅本人订单，且订单状态为 `UPLOADED` 或 `MODIFYING`（交付前）；
  - ADMIN：全程可查看。
- 返回内容：
  - 列表接口返回 JSON：`list[{ id, originalName, fileSize, uploadRound, previewUrl }]`；【已确认】
  - 用户交付前 `previewUrl` 指向**水印预览图**（`watermark_path`，后缀 `_wm`），**不提供原图下载入口**；
  - 管理员预览接口返回原图。
- 【文档明确】《需求文档》3.2.5 / 3.2.5-1 / PHT-02 / PHT-04。
- 响应：字节流接口不套用统一 JSON 结构。【已确认】
- 异常情况：
  - 订单非本人 → **403**。
  - 订单未上传照片 → 空态。
- 说明：交付前即使是本人也**不得**返回原图路径或原图下载地址。【文档明确】

#### 5.5.3 原图下载

- 方法与路径：`GET /api/orders/{id}/photos/{photoId}/download`【已确认】
- 调用角色：已登录用户（USER，仅本人）；管理员可查看原图。
- 前置条件：
  - 订单属于本人；
  - 订单状态为 `DELIVERED`。【文档明确 B-1】
- 服务端行为：后端校验用户身份、订单归属与订单交付状态，通过后返回原图文件流。
- 响应：文件流，`Content-Disposition: attachment; filename*=UTF-8''<encoded>`（文件名取 `original_name`）。本期单张下载。【已确认】
- 异常情况：
  - 订单未交付 → **403** 并提示。
  - 订单非本人 → **403**。
- 【文档明确】《需求文档》3.2.5-2；《AGENTS.md》第 9 节第 8 条。

### 5.6 修改反馈模块

`t_feedback` 字段见第 8 节。

#### 5.6.1 提交修改意见（申请修改）

- 方法与路径：`POST /api/orders/{id}/feedback`【已确认】
- 调用角色：已登录用户（USER，仅本人）
- 前置条件：订单属于本人；状态为 `UPLOADED`。【文档明确】
- 请求参数：

| 字段 | 类型 | 必填 | 校验规则 | 依据 |
| --- | --- | --- | --- | --- |
| content | string | 是 | 必填且 ≤500 字 | `t_feedback.content`；B-2 |

- 说明：`photo_round` 由后端根据订单当前照片轮次（`t_photo.upload_round` 最大值）自动推导，前端不传。【已确认】
- 服务端行为：记录反馈，`round` 与该订单已有反馈数+1 生成（首次申请修改 `round = 1`），`photo_round` 等于当前照片轮次；订单状态更新为 `MODIFYING`；同事务写入 `t_order_log`（动作 `MODIFY`）。【文档明确】
- 响应 `data`：反馈记录（`id`、`round`、`photoRound`、`content`、`createTime`）。
- 异常情况：
  - 意见为空或超长 → 40000。
  - 状态非 `UPLOADED` → `40900`。
  - 订单非本人 → 403。

#### 5.6.2 反馈历史

- 方法与路径：`GET /api/orders/{id}/feedbacks`【已确认】（亦随订单详情返回）
- 调用角色：USER（本人）/ ADMIN
- 响应 `data`：按轮次排列的反馈列表（`round`、`photoRound`、`content`、`createTime`）。【文档明确 FDB-02】
- 说明：反馈与照片上传批次相互对应，支持多轮修改追溯；修改轮次不限。【文档明确】
- 异常情况：订单非本人 → 403。

### 5.7 交付模块

#### 5.7.1 确认交付

- 方法与路径：`POST /api/orders/{id}/deliver`【已确认】
- 调用角色：已登录用户（USER，仅本人）
- 前置条件：订单属于本人；状态为 `UPLOADED`。【文档明确】
- 请求参数：无（前端需**二次确认**防误触，属前端行为）。
- 服务端行为：条件更新 `status = DELIVERED`，写入 `deliver_time`；同事务写入 `t_order_log`（动作 `DELIVER`）；交付后开放原图下载。交付范围为整个订单（含当前轮全部照片）。【文档明确】【已确认：交付范围】
- 异常情况：
  - 状态非 `UPLOADED` → `40900`。
  - 用户取消二次确认 → 维持 `UPLOADED`（前端行为）。
  - 订单非本人 → 403。
- 约束：`DELIVERED` 为终态，不可再申请修改。【文档明确 B-2】

---

## 6. 订单相关状态操作与日志动作对照

> `t_order_log.action` 文档仅示例 `CREATE / CONFIRM / REJECT 等`，完整动作枚举【已确认】采用下表。

| 触发动作 | action 值 | from_status | to_status | operator_role |
| --- | --- | --- | --- | --- |
| 创建订单 | `CREATE` | null | `PENDING` | `USER` |
| 确认订单 | `CONFIRM` | `PENDING` | `CONFIRMED` | `ADMIN` |
| 拒绝订单 | `REJECT` | `PENDING` | `REJECTED` | `ADMIN` |
| 取消订单 | `CANCEL` | `PENDING` | `CANCELLED` | `USER` |
| 标记拍摄完成 | `SHOOT_DONE` | `CONFIRMED` | `SHOOTING_DONE` | `ADMIN` |
| 上传精修照片 | `UPLOAD` | `SHOOTING_DONE` / `MODIFYING` | `UPLOADED` | `ADMIN` |
| 申请修改 | `MODIFY` | `UPLOADED` | `MODIFYING` | `USER` |
| 确认交付 | `DELIVER` | `UPLOADED` | `DELIVERED` | `USER` |

---

## 7. 接口权限速查表

| 模块 | 接口路径 | 方法 | 角色 |
| --- | --- | --- | --- |
| 用户 | `/api/auth/register` | POST | 公开 |
| 用户 | `/api/auth/login`【文档明确】 | POST | 公开 |
| 用户 | `/api/users/me` | GET / PUT | USER / ADMIN |
| 主题 | `/api/admin/themes`、`/api/admin/themes/{id}` | GET/POST/PUT | ADMIN |
| 主题 | `/api/admin/themes/{id}/status` | PATCH | ADMIN |
| 主题 | `/api/themes`、`/api/themes/{id}` | GET | 公开 |
| 套餐 | `/api/admin/packages`、`/api/admin/packages/{id}` | GET/POST/PUT | ADMIN |
| 套餐 | `/api/admin/packages/{id}/status` | PATCH | ADMIN |
| 套餐 | `/api/themes/{themeId}/packages`、`/api/packages/{id}` | GET | 公开 |
| 订单 | `/api/orders` | POST / GET | USER |
| 订单 | `/api/orders/{id}`、`/api/orders/{id}/cancel` | GET / POST | USER（本人） |
| 订单 | `/api/admin/orders`、`/api/admin/orders/{id}` | GET | ADMIN |
| 订单 | `/api/admin/orders/{id}/confirm`、`/reject`、`/shoot-done` | POST | ADMIN |
| 照片 | `/api/admin/orders/{id}/photos` | POST | ADMIN |
| 照片 | `/api/orders/{id}/photos`、`/photos/{photoId}/preview` | GET | USER / ADMIN |
| 照片 | `/api/orders/{id}/photos/{photoId}/download` | GET | USER（本人，交付后）/ ADMIN |
| 反馈 | `/api/orders/{id}/feedback`、`/api/orders/{id}/feedbacks` | POST / GET | USER（本人）/ ADMIN |
| 交付 | `/api/orders/{id}/deliver` | POST | USER（本人） |

> 说明：退出登录本期无后端接口（前端清除 token）；主题/套餐无物理删除接口，仅上下架。

---

## 8. 数据模型（严格对齐《数据库设计》2.3 节）

> 以下字段名、类型、可空性与默认值均取自《数据库设计》，**不得在接口实现中擅自改名或改类型**。

### 8.1 t_user（用户表）

| 字段 | 类型 | 可空 | 默认 | 备注 |
| --- | --- | :---: | --- | --- |
| id | BIGINT | 否 | 自增 | 主键 |
| username | VARCHAR(50) | 否 | | 唯一索引 `uk_username` |
| password | VARCHAR(100) | 否 | | BCrypt 密文，**接口返回忽略** |
| nickname | VARCHAR(50) | 是 | NULL | |
| phone | VARCHAR(20) | 是 | NULL | |
| avatar | VARCHAR(255) | 是 | NULL | 头像 URL（本期不启用） |
| role | VARCHAR(10) | 否 | 'USER' | USER / ADMIN，`idx_role` |
| status | TINYINT | 否 | 1 | 1 正常 / 0 禁用 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | |
| update_time | DATETIME | 否 | 自动更新 | |

### 8.2 t_theme（拍摄主题表）

| 字段 | 类型 | 可空 | 默认 | 备注 |
| --- | --- | :---: | --- | --- |
| id | BIGINT | 否 | 自增 | 主键 |
| name | VARCHAR(50) | 否 | | 主题名称 |
| cover_url | VARCHAR(255) | 是 | NULL | 封面图 URL |
| description | VARCHAR(500) | 是 | NULL | 主题描述 |
| tags | VARCHAR(200) | 是 | NULL | 标签，逗号分隔（接口层数组） |
| sort | INT | 否 | 0 | 排序值（越大越靠前） |
| status | TINYINT | 否 | 1 | 1 上架 / 0 下架（软下架） |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | |
| update_time | DATETIME | 否 | 自动更新 | |

### 8.3 t_package（摄影套餐表）

| 字段 | 类型 | 可空 | 默认 | 备注 |
| --- | --- | :---: | --- | --- |
| id | BIGINT | 否 | 自增 | 主键 |
| theme_id | BIGINT | 否 | | 逻辑外键 → t_theme.id，`idx_theme_id` |
| name | VARCHAR(50) | 否 | | 套餐名称 |
| price | DECIMAL(10,2) | 否 | | 价格（元） |
| retouch_count | INT | 否 | | 精修张数 |
| duration | VARCHAR(50) | 是 | NULL | 拍摄时长 |
| includes | VARCHAR(500) | 是 | NULL | 包含内容说明 |
| cover_url | VARCHAR(255) | 是 | NULL | 套餐封面图 URL |
| sort | INT | 否 | 0 | 排序值 |
| status | TINYINT | 否 | 1 | 1 上架 / 0 下架（软下架） |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | |
| update_time | DATETIME | 否 | 自动更新 | |

### 8.4 t_order（预约订单表）

| 字段 | 类型 | 可空 | 默认 | 备注 |
| --- | --- | :---: | --- | --- |
| id | BIGINT | 否 | 自增 | 主键 |
| order_no | VARCHAR(32) | 否 | | 订单编号，唯一索引 `uk_order_no` |
| user_id | BIGINT | 否 | | 逻辑外键 → t_user.id，`idx_user_id`、`idx_user_status` |
| theme_id | BIGINT | 否 | | 逻辑外键 → t_theme.id |
| package_id | BIGINT | 否 | | 逻辑外键 → t_package.id |
| shoot_date | DATE | 否 | | 拍摄日期 |
| shoot_slot | VARCHAR(20) | 否 | | MORNING / AFTERNOON / FULL_DAY |
| remark | VARCHAR(500) | 是 | NULL | 用户备注（接口层限制 ≤200 字） |
| status | VARCHAR(20) | 否 | 'PENDING' | 订单状态，`idx_status` |
| reject_reason | VARCHAR(500) | 是 | NULL | 拒绝原因，仅 REJECTED 填写 |
| price | DECIMAL(10,2) | 否 | | 下单时套餐价格快照 |
| deliver_time | DATETIME | 是 | NULL | 确认交付时间 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | `idx_create_time` |
| update_time | DATETIME | 否 | 自动更新 | |

### 8.5 t_photo（照片表）

| 字段 | 类型 | 可空 | 默认 | 备注 |
| --- | --- | :---: | --- | --- |
| id | BIGINT | 否 | 自增 | 主键 |
| order_id | BIGINT | 否 | | 逻辑外键 → t_order.id，`idx_order_id` |
| original_name | VARCHAR(255) | 否 | | 上传时原始文件名 |
| file_path | VARCHAR(500) | 否 | | 原图相对路径 |
| watermark_path | VARCHAR(500) | 是 | NULL | 水印图相对路径 |
| file_size | BIGINT | 否 | 0 | 文件大小（字节） |
| upload_round | INT | 否 | 1 | 上传轮次，`idx_order_round(order_id, upload_round)` |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 上传时间 |

### 8.6 t_feedback（用户反馈表）

| 字段 | 类型 | 可空 | 默认 | 备注 |
| --- | --- | :---: | --- | --- |
| id | BIGINT | 否 | 自增 | 主键 |
| order_id | BIGINT | 否 | | 逻辑外键 → t_order.id，`idx_order_id` |
| round | INT | 否 | 1 | 反馈轮次（= 已有反馈数 + 1） |
| photo_round | INT | 否 | 1 | 针对第几轮照片的反馈（对应 t_photo.upload_round） |
| content | VARCHAR(500) | 否 | | 修改意见内容（必填，≤500 字） |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 反馈时间 |

### 8.7 t_order_log（订单操作日志表）

| 字段 | 类型 | 可空 | 默认 | 备注 |
| --- | --- | :---: | --- | --- |
| id | BIGINT | 否 | 自增 | 主键 |
| order_id | BIGINT | 否 | | 逻辑外键 → t_order.id，`idx_order_id` |
| operator_id | BIGINT | 是 | NULL | 操作人 ID |
| operator_role | VARCHAR(10) | 是 | NULL | USER / ADMIN |
| action | VARCHAR(50) | 否 | | 动作：见第 6 节 |
| from_status | VARCHAR(20) | 是 | NULL | 变更前状态 |
| to_status | VARCHAR(20) | 是 | NULL | 变更后状态 |
| remark | VARCHAR(500) | 是 | NULL | 备注 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 操作时间 |

---

## 9. 确认决议登记

> 本节登记原始文档未明确规定或存在冲突的事项及**已确认的最终口径**。
> 原始文档按任务约束未被修改；需同步修订原始文档的项见第 9.8 节。

### 9.1 通用约定类（已确认）

| 编号 | 事项 | 确认结论 |
| --- | --- | --- |
| C-01 | 统一响应 `code` 成功取值 | `code = 0` 表示成功，失败为 5 位错误码 |
| C-02 | 业务错误码 | 采用 2.5 节编码表（0/40000/40100/40300/40400/40900/40901/50000），按模块段位扩展 |
| C-03 | HTTP 状态码 | 语义化 200/400/401/403/404/409/500，与 `code` 一致映射 |
| C-04 | 分页 | 请求 `page`/`pageSize`（默认 10，上限 100），响应 `{ total, page, pageSize, list }` |
| C-05 | 非登录接口路径 | 采用第 5 节路径（`/api` 前缀、RESTful、管理端 `/api/admin/**`） |
| C-06 | DTO 命名风格 | JSON camelCase，DB snake_case，映射层转换 |
| C-07 | 日期时间序列化 | 日期 `yyyy-MM-dd`，时间 `yyyy-MM-dd HH:mm:ss`（本地时区，精确到秒） |

### 9.2 用户模块（已确认）

| 编号 | 事项 | 确认结论 |
| --- | --- | --- |
| U-01 | 退出登录接口 | 本期不提供后端退出接口，仅前端清除 token |
| U-02 | 用户名长度 | 接口层校验 3~20 字符；DB `VARCHAR(50)` 保持不变 |
| U-03 | 手机号规则 | 非必填、不唯一，正则 `^1[3-9]\d{9}$` |
| U-04 | 修改密码 | 需原密码，字段 `oldPassword` / `newPassword` |
| U-05 | 头像功能 | 本期不做，`avatar` 字段保留不启用 |
| U-06 | 禁用用户处理 | 登录时校验 `status = 1`；鉴权过滤器同样校验用户状态 |

### 9.3 主题/套餐模块（已确认）

| 编号 | 事项 | 确认结论 |
| --- | --- | --- |
| T-01 | “删除”语义 | 只做软下架（`status = 0`），不提供物理删除 |
| T-02 | 详情接口 | 提供 `GET /api/themes/{id}`、`GET /api/packages/{id}` |
| T-03 | 用户端排序分页 | 按 `sort` 倒序 + 分页，参数同后台 |
| T-04 | `tags` 形式 | 接口层 `string[]`，DB 逗号分隔 `VARCHAR(200)` 互转 |
| T-05 | 历史订单引用 | 禁止物理删除被订单引用的主题/套餐，仅下架 |

### 9.4 订单模块（已确认）

| 编号 | 事项 | 确认结论 |
| --- | --- | --- |
| O-01 | `order_no` 规则 | `SO` + `yyyyMMddHHmmss` + 4~6 位序列/随机，总长 ≤32 且唯一 |
| O-02 | `remark` 长度 | 接口层 ≤200 字；DB `VARCHAR(500)` 不改 |
| O-03 | `shoot_slot` 映射 | DB 存 `MORNING/AFTERNOON/FULL_DAY`；中文时段由后端配置映射返回 |
| O-04 | 主题/套餐关联校验 | 校验 `packageId` 属于 `themeId`，不一致返回 40000 |
| O-05 | 拒绝原因长度 | 必填，建议 ≤200 字；DB `VARCHAR(500)` 不改 |
| O-06 | `action` 枚举 | `CREATE/CONFIRM/REJECT/CANCEL/SHOOT_DONE/UPLOAD/MODIFY/DELIVER` |
| O-07 | 关键字搜索 | 模糊匹配 `LIKE '%kw%'`（用户名 / 主题名），参数化防注入 |
| O-08 | 状态筛选 | 单状态 `status`（可选），不传返回全部 |

### 9.5 照片模块（已确认）

| 编号 | 事项 | 确认结论 |
| --- | --- | --- |
| P-01 | 列表/预览形式 | 列表返回 JSON 含 `previewUrl`；字节流走 `preview`/`download` 接口 |
| P-02 | 批量上传 | 字段名 `files`，单次上限 20 张；部分失败仍返回 200，含 success/failed 清单 |
| P-03 | 管理员/用户接口 | 复用同一资源接口，按角色与订单状态决定返回水印图或原图 |
| P-04 | 下载方式 | 本期单张下载；`Content-Disposition: attachment; filename*=UTF-8''<encoded>` |
| P-05 | 水印生成 | 同步生成；失败记录日志并允许重试（`watermark_path` 暂空） |
| P-06 | 交付后水印 | 水印接口保留，默认展示原图 |

### 9.6 反馈/交付模块（已确认）

| 编号 | 事项 | 确认结论 |
| --- | --- | --- |
| F-01 | `photo_round` 来源 | 后端按当前照片轮次自动推导，前端不传 |
| F-02 | `round` 与 `photo_round` | 相等（每轮照片最多一次反馈），`round = 已有反馈数 + 1` |
| F-03 | 二次确认与幂等 | 二次确认纯前端；后端条件更新（乐观锁）保证幂等，重复命中 0 行返回幂等成功或 40900 |
| F-04 | 交付范围 | 针对整个订单（含当前轮全部照片） |

### 9.7 安全与其他（已确认）

| 编号 | 事项 | 确认结论 |
| --- | --- | --- |
| S-01 | 详情字段裁剪 | 时间线仅返回 `{ action, fromStatus, toStatus, remark, createTime }`；不返回 `operator_id`、`password` |
| S-02 | 文件防绕过鉴权 | 不暴露 `uploads/` 静态目录，统一经鉴权接口；如需直链用短期签名 URL（5~15 分钟） |
| S-03 | 重复提交幂等 | 状态流转统一条件更新（乐观锁）+ 事务写 `t_order_log`，防重复确认/交付 |

### 9.8 原始文档冲突与处理口径（需文档归口人同步）

> 以下为两份原始文档之间的不一致或文档内部不一致。**本次未修改原始 Word 文档**，仅在本文档登记处理口径。

| 编号 | 冲突说明 | 本文档采用口径 | 需同步修订 |
| --- | --- | --- | --- |
| X-01 | 用户名长度：《需求文档》校验 3~20 字符，数据库 `username VARCHAR(50)` | 接口层按 3~20 校验，DB 不改 | 可在需求文档注明“接口层限制 3~20，DB 字段容量冗余” |
| X-02 | 备注长度：《需求文档》3.4.1 为 ≤200 字，DB `remark VARCHAR(500)` | 接口层按 ≤200 校验，DB 不改 | 同上 |
| X-03 | 状态机“触发动作”列错位：《需求文档》3.2.3 表中 `CONFIRMED` / `SHOOTING_DONE` 行的触发动作标注与后继状态对应错误 | 以本文 4.2 节为准（依据各用例 3.2.4-1~4 与《数据库设计》2.3.4） | 建议修正《需求文档》3.2.3 状态机表 |

---

## 10. 文档维护说明

1. 本文档整理原始文档已有内容并将已确认的推荐方案升格为正式约定，未新增业务功能。
2. 第 9.8 节所列文档冲突须由文档归口人同步修订原始文档；涉及业务规则或数据库变更的，须走变更确认。
3. 后续新增接口或修改约定，应保持与第 2 章通用约定、第 4 章状态机及第 8 章数据模型一致，并在此登记。
