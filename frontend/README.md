# Frontend（Vue 3）

脚手架前端，采用 Vue 3 + Vite + TypeScript。

## 技术栈

- Vue 3.5 + `<script setup>` + TypeScript（strict 模式）
- Vite 5 + 代理（`/dev-api -> http://localhost:8080`）
- Element Plus 2.8 + `unplugin-auto-import` / `unplugin-vue-components`
- Pinia 2（`app` / `user` / `permission` 三个 store）
- vue-router 4，静态路由 + 后端 `/getRouters` 动态路由
- ECharts 5 + vue-echarts，按需注册
- ESLint + Prettier + EditorConfig

## 启动

```powershell
cd frontend
npm install
npm run dev
```

默认监听 `http://localhost:8081`。开发态请求经 Vite 代理转发到后端：

- `/dev-api/*  -> http://localhost:8080/*`
- `/v3/api-docs -> http://localhost:8080/v3/api-docs`

## 质量与构建

```powershell
npm run lint        # ESLint + Prettier
npm run type-check  # vue-tsc 严格类型检查
npm run build       # 生产构建，产物位于 ./dist
```

## 容器化

仓库根 `docker compose up frontend` 会基于本目录的 `Dockerfile` 构建多阶段镜像（Node 构建 → Nginx 托管），并通过 `nginx.conf` 透传 `X-Trace-Id`。

## 通用 CRUD 抽象

业务页面使用统一的 CRUD 工具集，新增一个管理页通常 ≤ 200 行：

| 模块 | 用途 |
| --- | --- |
| `composables/useCrud.ts` | 列表 / 增 / 改 / 删 / 选中 / 表单态机 |
| `components/SearchForm.vue` | 字段化的搜索条 |
| `components/PageToolbar.vue` | 新增 / 删除 / 刷新 工具栏 |
| `components/DataTable.vue` | 字典自动翻译、状态开关、日期格式化、自定义插槽 |
| `components/FormDialog.vue` | 校验 + 提交按钮的对话框包装 |
| `components/Pagination.vue` | 双向 `pageNum / pageSize` 绑定 |
| `composables/useDict.ts` | 字典缓存与并发请求合并 |

## 默认账号

```
admin / admin123
```

## 与后端的协议

| 行为 | 路径 |
|---|---|
| 登录 | `POST {VITE_APP_BASE_API}/login` |
| 验证码 | `GET {VITE_APP_BASE_API}/captchaImage` |
| 用户信息 | `GET {VITE_APP_BASE_API}/getInfo` |
| 动态路由 | `GET {VITE_APP_BASE_API}/getRouters` |
| 退出登录 | `POST {VITE_APP_BASE_API}/logout` |
| 字典数据 | `GET {VITE_APP_BASE_API}/system/dict/data/type/{dictType}` |

axios 请求层会自动透传 `Authorization: Bearer <token>` 和 `X-Trace-Id`，在响应失败时显示 traceId 便于排查。
