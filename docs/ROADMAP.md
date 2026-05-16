# 路线图：可插拔模块 + 工作流

> 上一版 ROADMAP 是"通用能力候补清单"。用户已挑出几件做掉的事：
> 1. ✅ OAuth2 / OIDC SSO（标准 Authorization Code 模式，已合入主分支）
> 2. ❌ 双因子 / WebAuthn（暂不做）
> 3. ✅ 操作审计（已合入主分支，详见 [FEATURES.md §14](FEATURES.md#14-操作审计-auditlog--sys_audit_log)）
> 4. ✅ 数据级权限（已合入主分支，详见 [FEATURES.md §16](FEATURES.md#16-数据级权限data-scope-已就绪--默认接入审计列表)）
> 5. ✅ 模块加载约定 B-1/B-2/B-3（已合入主分支）
> 6. ✅ 工作流 MVP（Flowable 8 + bpmn-js，已合入主分支，详见 [FEATURES.md §15](FEATURES.md#15-工作流模块m-1可插拔-flowable-8)）
> 7. ✅ 工作流增强 第一批（只读模式 + 运行时态高亮 + 抄送/后加签/退回 + 启动表单引擎，已合入主分支，详见 [FEATURES.md §15.1–15.3](FEATURES.md#工作流增强已合入)）
> 8. ✅ 工作流增强 第二批（任务级动态表单 + 前加签 + 流程实例时间轴，已合入主分支，详见 [FEATURES.md §15.5–15.7](FEATURES.md#155-任务级动态表单task-form)）
> 9. ✅ 工作流增强 第三批（前加签可视化阻塞标识 + 撤销前加签 + 流程图 SVG 导出 / 时间轴打印 + form-create 体积优化 + 实例总列表，已合入主分支，详见 [FEATURES.md §15.5–15.10](FEATURES.md#155-任务级动态表单task-form)）
> 10. ✅ CMS 内容管理 M-3（栏目 + 富文本文章 + 状态机 + 公开门户 + 富文本图片上传，已合入主分支，详见 [FEATURES.md §17](FEATURES.md#17-cms-内容管理m-3可插拔模块)）
> 11. ✅ CMS × Workflow 联动桥 M-4 + CMS × Inbox 通知桥 M-5（CMS 提交走 Flowable 真审批 + 状态变更发站内信，两桥模块独立可插拔，已合入主分支，详见 [FEATURES.md §17.9](FEATURES.md#179-m-4-cms--workflow-联动桥) / [§17.10](FEATURES.md#1710-m-5-cms--inbox-通知桥)）
> 12. ✅ M-10 通用表单引擎 + M-6 文件中心 + M-8 报表中心（form_template / sys_file / sys_report_template 三模块，每个独立可插拔，已合入主分支，详见 ROADMAP 模块进度表 12-14 行）
> 14. ✅ Q-3 可观测性集成进 framework（HttpRequestRecorder 慢请求落表 + 业务表 Gauge + 健康聚合 + Quartz 9001 + inbox 告警，详见 [FEATURES.md §20](FEATURES.md#20-可观测性q-3集成进-framework)）
> 17. 其余条目暂不做
>
> 本版改为聚焦**可插拔业务模块**：脚手架要做到"新项目按需勾选模块"。下面给出**模块化的拆分方法** + **第一个待补的业务模块（工作流）的设计草案**。

---

## 1. 设计原则：什么叫"可插拔模块"

把功能按下表四类切：

| 层级 | 例子 | 在脚手架里的定位 |
|------|------|------------------|
| **核心**（必有） | 用户/角色/菜单/JWT/异常/Liquibase | 必含，删了脚手架就崩 |
| **平台能力**（默认开） | WebSocket 总线、Inbox、限流、SSO 客户端、文件存储 | Maven module / Pinia store 在主仓库内，用 `@ConditionalOnProperty` / `app.module.<x>.enabled=true` 关停；不需要时配置关掉即可，不必改代码 |
| **业务模块**（默认开 / 可拆） | **工作流**、CMS、报表、IM、低代码 | 独立 Maven module + 前端 `src/modules/<name>` + Liquibase 单独 changelog；可以**整目录复制 / 删除**，主项目对它**只通过 Spring Boot 自动装配 / 路由懒加载**引用 |
| **业务私有**（仅本项目） | 订单、风控、客户管理 | 在 `scaffold-business` 或独立 git 子仓里，每个项目自行维护 |

要做到"很容易拆出来或合进去"，硬约束：

1. **后端**：每个业务模块是独立 Maven 子模块，包名 `com.scaffold.module.<name>`；模块内放 `*ModuleAutoConfiguration`，靠 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 自启。`scaffold-admin` 只在 `pom.xml` 里写一行依赖，删除依赖整个模块即下线。
2. **前端**：每个模块是 `frontend/src/modules/<name>/` 一个目录，对外暴露 `index.ts` 导出 `routes / stores / locales / install(app)`。`main.ts` 用 `import.meta.glob('./modules/*/index.ts')` 自动挂载；删目录就消失。
3. **数据库**：每个模块自己的 Liquibase changelog 文件 (`db/changelog/module-<name>.yml`)，主 changelog 用 `include` 引用；删除模块时同步删该 include 即可。
4. **前后端 API 命名空间**：`/<module>/...` 和 `module:<resource>:<action>` 权限 key，避免冲突。

---

## 2. 第一批待补：业务模块

### M-1 工作流（Workflow，✅ MVP + 增强 已完成）

> MVP 与第一批增强均已合入主分支。完整使用文档见 [FEATURES.md §15](FEATURES.md#15-工作流模块m-1可插拔-flowable-8)，增强部分见 [§15.1–15.3](FEATURES.md#工作流增强已合入)。

#### 已交付内容

**后端 `scaffold-module-workflow`**（独立 Maven 子模块）：
- AutoConfiguration + ScaffoldModule 元数据，遵循 B-1 模块加载约定，`app.module.workflow.enabled=false` 一键关停
- `WorkflowFacade` 业务封装：部署 / 启动 / 取消 / 完成 / 认领 / 转办 / 列表
- `WorkflowProcessController` + `WorkflowTaskController`：完整 REST API（13 个端点），全部带 `@PreAuthorize` 与 `@AuditLog`
- `TaskNotifyEventListener`：Flowable `TASK_CREATED` / `TASK_COMPLETED` → `MessagePublisher.toUser` 推送站内信
- 单测 11 用例（参数校验、字段映射、调用顺序）

**前端 `frontend/src/modules/workflow`**（遵循 B-2 自动加载）：
- 4 个页面：流程定义列表 / 待办 / 已办 / 设计器
- bpmn-js 18 集成：导入、编辑、导出、直接部署到引擎
- 启动流程对话框（含 businessKey + variables JSON）
- 完成任务对话框（审批意见 + 表单变量）

**Liquibase**（B-3）：
- 自有 changelog `module-workflow.yml` 仅初始化菜单 + 按钮权限（12 个 menu_id：3001-3022）
- Flowable 自身 30+ 张 `ACT_*` / `FLW_*` 表由引擎自建，**不进入主 Liquibase 流程**
- `workflow-uninstall.sql` 一键清理菜单 + 引擎表

#### 实测可拆分性

| 操作 | 验证结果 |
|------|----------|
| 注释 admin pom 中模块依赖 | ✅ admin 仍可编译 |
| 删除 `frontend/src/modules/workflow` 整目录 | ✅ `npm run build` 成功 |
| `app.module.workflow.enabled=false` | ✅ 模块组件全部不加载，引擎不启动 |

#### 增强部分（✅ 已合入）

| 增强 | 状态 | 备注 |
|------|------|------|
| 真正只读模式 | ✅ | `readonly=true` 切到 `bpmn-js/lib/NavigatedViewer`，无编辑工具栏 |
| 流程图运行时态 | ✅ | active / completed / rejected 三组节点 id；前端 canvas.addMarker + scoped CSS（含蓝色脉冲动画） |
| 抄送 (cc) | ✅ | 不创建 task 节点，流程变量 `scaffoldCcHistory` 留痕 + push bus 站内信 |
| 后加签 | ✅ | `taskService.createTaskBuilder()` 挂同 instance，新任务不进 BPMN sequenceFlow |
| 退回 | ✅ | `runtimeService.createChangeActivityStateBuilder().moveActivityIdTo(...)`；缺省回退到上一 finished userTask |
| 动态表单引擎 | ✅ | `wf_form_schema` 多版本存档；form-create + designer 接入；启动表单运行时按 schema 渲染 |

#### 增强第二批（✅ 已合入）

| 增强 | 状态 | 备注 |
|------|------|------|
| 任务级动态表单 | ✅ | TodoList 完成对话框按 `processDefinitionKey + taskDefinitionKey` 拉 active schema，无 schema 退回 JSON 输入；FormDesigner 节点 id 下拉自动从最新 BPMN xml 解析；详见 [FEATURES.md §15.5](FEATURES.md#155-任务级动态表单task-form) |
| 前加签 | ✅ | 不改 BPMN，靠 task local var (`scaffoldPreSignOriginTaskId` / `scaffoldBlockedByTaskIds`) 实现"原任务被阻塞 → 子任务完成自动唤醒"；菜单 `3026 前加签`，权限 `workflow:task:addsign-before`；详见 [§15.6](FEATURES.md#156-前加签add-sign-before) |
| 流程实例时间轴 | ✅ | `WorkflowFacade.getInstanceTimeline` 聚合 process / activity / task / cc / addsign-after / addsign-before / comment 七路事件；`ProcessProgressDialog` 加"时间轴" tab 与"流程图" tab 并列；详见 [§15.7](FEATURES.md#157-流程实例时间轴process-timeline) |

E2E 回归脚本：`backend/scripts/verify-workflow-enhancements.ps1`，一键覆盖三项 + 第三批的撤销前加签 / 实例总列表。

#### 增强第三批（✅ 已合入）

| 增强 | 状态 | 备注 |
|------|------|------|
| 前加签可视化阻塞标识 | ✅ | `TaskView.blockedByTaskIds` 由后端 `toView(Task)` 回填；前端 TodoList 显示"被加签阻塞 ×N" tag、disable 处理与前加签按钮 + tooltip；详见 [FEATURES.md §15.6](FEATURES.md#156-前加签add-sign-before) |
| 撤销前加签 | ✅ | `DELETE /workflow/task/{childTaskId}/add-sign-before`，仅本子任务发起人或 admin 可撤；删 child task + 摘父 `scaffoldBlockedByTaskIds` + 历史变量打 `cancelled=true`；ProcessProgressDialog 时间轴上对未撤销的 `task.addsign.before` 提供"撤销"按钮；详见 [§15.10](FEATURES.md#1510-撤销前加签) |
| 流程图 SVG 导出 + 时间轴打印 / PDF | ✅ | `BpmnDesigner.getSvg()` (NavigatedViewer.saveSVG) 一键下载；时间轴用浏览器原生 `print()` + 全局 `@media print` 样式，用户可直接选"另存为 PDF" |
| form-create 体积优化 | ✅ | `vendor-form-create-runtime` (143KB) 与 `vendor-form-create-designer` (1.07MB) 拆分；FormDesigner.vue 用 `defineAsyncComponent` 懒加载 designer，未访问该路由时主 bundle 不下载 1MB+ chunk；详见 [§15.8](FEATURES.md#158-form-create-按需加载--体积优化) |
| 流程实例总列表 | ✅ | `GET /workflow/process/instances?defKey=&businessKey=&startUserId=&status=&pageNum=&pageSize=`；admin 看全量，非 admin 强制 `startedBy=current` 过滤（不引入 dataScope，因 ACT_HI_PROCINST 不接 mybatis mapper）；菜单 `3027 实例管理`；前端 `ProcessAdmin.vue` 列表 / 进度 / 取消 复用 `ProcessProgressDialog`；详见 [§15.9](FEATURES.md#159-流程实例管理process-instance-admin) |

#### 真正的 backlog（暂不做）

- 多实例会签 / 或签节点（BPMN 节点定义层，非动态加签）—— 业务复杂度高，等真有用例再做。
- BPMN 调用子流程 + 流程图穿透浏览 —— 同上。

---

### M-3 CMS（Content Management System，✅ 已完成）

> 第三个完整可插拔业务模块。完整使用文档见 [FEATURES.md §17](FEATURES.md#17-cms-内容管理m-3可插拔模块)。
>
> **后端 `scaffold-module-cms`**：4 张业务表（cms_channel / cms_article / cms_tag / cms_article_tag）+ 状态机 6 个流转端点 + 公开门户 API（匿名）+ 富文本图片上传（StorageService 抽象 + 本地磁盘默认实现）+ `CmsWorkflowAdapter` 接口预留（默认空实现，第一批不接 workflow，CMS 模块**不依赖** scaffold-module-workflow）。
>
> **前端 `frontend/src/modules/cms/`**：栏目树 / 文章列表 / 文章编辑 / 标签字典 / 状态机审核条 / wangEditor 富文本（vite manualChunks 拆 `vendor-wangeditor` 独立 chunk，懒加载）。
>
> **测试**：`ArticleServiceTest`（16 用例覆盖状态机全分支 + slug 冲突 + 软删过滤 + workflow adapter 路径）+ `ChannelServiceTest`（7 用例覆盖父子约束 + 循环引用 + 树构建）+ E2E `backend/scripts/verify-cms.ps1` 端到端 8 步覆盖建栏目 + 4 篇文章打满状态机六分支 + 公开 API 过滤 + 软删过滤。
>
> **菜单 ID 段**：4001-4030；卸载脚本：`cms_uninstall.sql`。

下面是设计阶段保留的方案细节，已全部落地：

### M-3 CMS（设计存档）

> 第三个可插拔业务模块。定位"对外资讯门户"——栏目 + 文章（富文本）+ 封面 + SEO 字段 + 状态机审核流。
> 走与 M-1 工作流一样的可插拔约定（B-1/B-2/B-3）：独立 Maven 子模块、前端 `frontend/src/modules/cms/`、自有 Liquibase changelog、删 jar 即下线。

#### 1. 边界与不做的事

**做：**
- 栏目（Channel/Category）树形管理：CRUD + 排序 + 启用/停用 + 关键字 / 描述 / 模板字段
- 文章（Article）CRUD：标题 / 摘要 / 封面图 / 正文（**富文本 HTML**，wangEditor 输出）/ 标签 / 来源 / 作者 / 排序 / 阅读量
- 状态机：草稿 → 待审核 → 已发布 → 已下线（再编辑回到草稿）；权限按角色拆 `cms:article:submit / approve / publish / unpublish`
- SEO 字段：slug / metaTitle / metaDescription / metaKeywords / canonicalUrl
- 富文本图片上传：调通用 `StorageService`（本地磁盘实现），路径 `/upload/cms/yyyyMM/<uuid>.<ext>`；nginx 静态目录或 Spring `ResourceHandler` 暴露
- 公开 API：`GET /cms/public/channels`、`GET /cms/public/articles`、`GET /cms/public/articles/{slug}` —— 无需 token，仅返回 status=PUBLISHED 的内容；可选 simple ETag/Last-Modified
- 后台 API：标签、列表搜索（按 channel / status / 关键词 LIKE 标题摘要 / 时间区间）、详情、保存、状态流转、删除（软删）
- 工作流接入预留：`@ConditionalOnClass` 探测 workflow facade；启用时"提交审核"动作走 workflow 启动，否则走自闭环状态机。**第一批不实现工作流接入**，只把 hook 接口预留好

**不做（明确）：**
- 多语言 i18n 文章（一篇文章一个语言版本就够，不上 polyglot 表）
- 评论 / 点赞 / 收藏（C 选项才需要，B 不需要）
- 内容个性化推荐 / AB 测试
- ES 全文检索（用 MySQL FULLTEXT 索引就够）
- 对象存储（先抽接口，落本地磁盘；将来切 MinIO 只换 bean）
- 静态化 / SSG / CDN 缓存（前端门户暂不在脚手架范围）
- 在线协作编辑 / 版本 diff（第一批用"再编辑回草稿"足够）

#### 2. 数据模型

四张表，全部 `cms_` 前缀，对外通过模块 changelog 管理：

```
cms_channel              栏目（树）
  id, parent_id, name, code (uniq), order_num, status, keywords, description, template,
  create_by, create_time, update_by, update_time, del_flag

cms_article              文章
  id, channel_id, title, slug (uniq), summary, cover_url, content_html,
  source, author, status (DRAFT|PENDING|PUBLISHED|UNPUBLISHED),
  meta_title, meta_description, meta_keywords, canonical_url,
  published_at, view_count, sort_order,
  create_by, create_time, update_by, update_time, del_flag

cms_tag                  标签字典
  id, name (uniq), color, create_time

cms_article_tag          多对多
  article_id, tag_id  (PK 双列)
```

索引建议：
- `cms_article` (channel_id, status, published_at desc)：列表查询主索引
- `cms_article` FULLTEXT (title, summary, content_html) WITH PARSER ngram：搜索索引（MySQL 5.7+ 自带 ngram 中文分词）
- `cms_article` (slug) UNIQUE：公开 API 查询索引

#### 3. 状态机（含审计 + 权限）

| 当前 | 动作 | 目标 | 权限 | 审计 action |
|------|------|------|------|-------------|
| (无) | 创建 | DRAFT | `cms:article:add` | `CREATE` |
| DRAFT | 提交审核 | PENDING | `cms:article:submit` | `SUBMIT` |
| DRAFT / PENDING / PUBLISHED / UNPUBLISHED | 编辑保存 | DRAFT（PUBLISHED → DRAFT 提示用户即将下线）| `cms:article:edit` | `EDIT` |
| PENDING | 通过 | PUBLISHED（同时写 published_at） | `cms:article:approve` | `APPROVE` |
| PENDING | 驳回 | DRAFT（带评论） | `cms:article:approve` | `REJECT` |
| PUBLISHED | 下线 | UNPUBLISHED | `cms:article:unpublish` | `UNPUBLISH` |
| UNPUBLISHED | 重新上线 | PUBLISHED | `cms:article:publish` | `REPUBLISH` |
| 任意 | 软删 | del_flag=2 | `cms:article:remove` | `DELETE` |

每个流转动作 `@AuditLog` 落库，复用 M-2 审计列表。
工作流接入 hook：`CmsWorkflowAdapter` 接口（默认空实现），如果上下文里有 `WorkflowFacade` bean，则 `submit()` 调 `startProcess(processDefinitionKey="cms_article_review", businessKey=articleId)`，监听 process complete 事件回写状态。**第一批先不实现，只预留接口。**

#### 4. 后端结构

```
backend/scaffold-module-cms/
  pom.xml                                 (依赖 scaffold-common + scaffold-framework)
  src/main/java/com/scaffold/module/cms/
    CmsModuleAutoConfiguration.java       (@AutoConfiguration, @ConditionalOnProperty app.module.cms.enabled)
    domain/    Channel / Article / Tag / ArticleTag
    mapper/    *.java + *.xml
    dto/       ArticleView / ArticleSaveRequest / ArticleStatusChangeRequest / ChannelTreeNode / PublicArticleSummary
    service/   ChannelService / ArticleService / TagService / StorageService(接口) / LocalDiskStorageService
    controller/
      ChannelController        /cms/channel/*       admin
      ArticleController        /cms/article/*       admin
      TagController            /cms/tag/*           admin
      PublicCmsController      /cms/public/*        匿名
      UploadController         /cms/upload/image    需要 cms:article:edit
    workflow/  CmsWorkflowAdapter (接口) + DefaultCmsWorkflowAdapter (空实现)
  src/main/resources/
    db/changelog/
      module-cms.yml                       由主 changelog include
      sql/
        cms_init.sql                       建表 + 索引
        cms_menu.sql                       菜单 + 权限（4001-4030 ID 段）
        cms_uninstall.sql                  反向卸载脚本
    META-INF/spring/
      org.springframework.boot.autoconfigure.AutoConfiguration.imports
  src/test/java/...
    ArticleServiceTest                     状态机全分支 + 字段映射 + slug 冲突检测
    PublicCmsControllerTest                只放 PUBLISHED + 软删过滤
```

权限 key 段（与工作流的 `workflow:` 同一风格）：

```
cms:channel:list / add / edit / remove
cms:article:list / add / edit / remove / submit / approve / publish / unpublish
cms:tag:list / add / edit / remove
cms:upload:image
```

菜单 ID 段：4001（CMS 父）/ 4002（栏目管理）/ 4003（文章管理）/ 4004（标签管理）/ 4010-4030（按钮权限）。

#### 5. 前端结构

```
frontend/src/modules/cms/
  index.ts                       (ScaffoldFrontendModule, 注册路由 + locales)
  api.ts                         (Channel/Article/Tag/Upload 全套 typed wrapper)
  views/
    ChannelList.vue              (el-tree + CRUD 抽屉)
    ArticleList.vue              (筛选 + 分页表格 + 状态 tag + 操作下拉)
    ArticleEdit.vue              (左侧元数据表单 / 右侧 wangEditor 富文本；保存即可发布预览)
    TagList.vue                  (标签字典 CRUD)
  components/
    StatusTag.vue                (DRAFT/PENDING/PUBLISHED/UNPUBLISHED 颜色统一)
    ArticleEditor.vue            (wangEditor 富文本 + 上传图片对接 /cms/upload/image)
    ArticleReviewBar.vue         (顶部状态机操作条：提交/通过/驳回/上线/下线)
```

富文本编辑器选型：`@wangeditor/editor` + `@wangeditor/editor-for-vue`，对中文运营人员友好；正文存 HTML（`content_html`），不再保留 markdown 二份。打包大小约 700KB+ 单 chunk，走 vite manualChunks 单独拆 `vendor-wangeditor` chunk，仅 `ArticleEdit` 路由打开时才下载——和 form-create designer 一个套路。

#### 6. 落地节奏（推荐 5 天分批）

| 批次 | 工作量 | 内容 |
|------|--------|------|
| **第 1 批：脚手架 + 栏目 + 文章 CRUD** | 2 天 | scaffold-module-cms Maven 模块、AutoConfiguration、4 张表 changelog、Channel/Article/Tag service + mapper + controller、菜单 SQL、单测 + 前端 ChannelList/ArticleList/TagList |
| **第 2 批：状态机 + 审计 + 公开 API** | 1 天 | 6 个状态流转端点 + `@AuditLog`、PublicCmsController、StatusTag.vue、ArticleReviewBar.vue |
| **第 3 批：富文本编辑器 + 图片上传** | 1 天 | StorageService 抽象 + LocalDiskStorageService 实现、UploadController、wangEditor 集成 + manualChunks 拆分（vendor-wangeditor）、ArticleEdit.vue 完整页面 |
| **第 4 批：搜索 + E2E + 文档** | 1 天 | MySQL FULLTEXT 索引 + LIKE 关键词、E2E `verify-cms.ps1`（4 篇文章覆盖状态机全分支 + 公开 API 过滤）、FEATURES §17 + RUNBOOK 段落 |

每批分别 commit，跟 M-1 工作流的 PR 风格一致。

---

### M-4 CMS × Workflow 联动桥（✅ 已完成）

> 让 CMS 的「提交审核」从自闭环状态机切到 Flowable 真审批流，验证两个可插拔模块的实际联动。完整使用文档见 [FEATURES.md §17.9](FEATURES.md#179-m-4-cms--workflow-联动桥)。
>
> **后端 `scaffold-module-cms-workflow`**：独立桥模块，POM 同时依赖 cms 与 workflow 两个本体；`@ConditionalOnProperty(app.module.cms.workflow.enabled=true)` 默认关；自带 `cms_article_review.bpmn20.xml`，启动按 key+md5 智能部署；`WorkflowAwareCmsAdapter` 实现 `CmsWorkflowAdapter`（覆盖默认空实现）→ submit 时 `workflowFacade.startProcess` 并把 piid 写回 cms_article 表；`ArticleWorkflowEventListener` 订阅 Flowable `PROCESS_COMPLETED`，按 `approved` 变量回调 `articleService.onWorkflowApprove/onWorkflowReject` 反向同步状态。
>
> **CMS 本体改动（最小侵入）**：`cms_article` 表新增 `process_instance_id` 列；`Article` 实体加 `processInstanceId` 字段；`ArticleService.submit` 新增 adapter 适配分支（adapter 接管时 ArticleService 不再自改状态，避免双写）；新增 `onWorkflowApprove(id)` / `onWorkflowReject(id, reason, actor)` 两个 workflow 回写口子；新增 `ArticleStatusChangedEvent` 事件（M-5 也消费）。
>
> **前端**：`ArticleReviewBar.vue` 当 `processInstanceId` 存在时按钮文案变「提交审批 / 审批通过 / 审批驳回」并显示「查看审批进度」按钮（懒加载 workflow 模块的 `ProcessProgressDialog`）；`ArticleList.vue` 列表加 `走 workflow` 标签。
>
> **测试**：`WorkflowAwareCmsAdapterTest`（8 用例：onSubmit 启流程 / 已有 piid skip / startProcess 抛异常 / 找不到 article / onApprove cancelInstance / onReject cancelInstance / cancel 异常仅 warn）+ `ArticleServiceWorkflowCallbackTest`（5 用例：onWorkflowApprove → PUBLISHED / onWorkflowReject → DRAFT 带 reason / onWorkflowApprove 跳过非 PENDING / reject 通过 ReviewBar 调 adapter / submit adapter 接管时不自改 status）+ E2E `backend/scripts/verify-cms-workflow.ps1` 覆盖审批通过 / 驳回 / 直接 approve 触发 cancelInstance 三条路径。

下面是设计阶段保留的方案细节，已全部落地：

#### 决策（已与用户对齐）

| 维度 | 决策 |
|------|------|
| 默认开关 | **默认关**（保持「CMS 不依赖 workflow」的硬承诺）；需 `app.module.cms.workflow.enabled=true` 显式开启 |
| 流程定义来源 | **桥模块自带 `cms_article_review.bpmn20.xml`**，启动时按 key+md5 检测引擎现状，缺失或变更则部署新版；用户也可在 workflow 设计器里覆盖最新版 |
| 驳回入口 | 前端**两个入口**都能驳回：workflow TodoList 与 文章 ReviewBar，后端走同一回调 `articleService.onWorkflowReject(articleId, reason)` |
| 包结构 | **独立桥模块** `scaffold-module-cms-workflow`，CMS 与 workflow 模块本体均不变 |

#### 数据流

```
[草稿] DRAFT
   │ submit
   ↓
[ArticleService.submit]
   │   ─ 桥模块未启用 → 自闭环：直接 PENDING（M-3 行为，不变）
   │   ─ 桥模块启用：CmsWorkflowAdapter (= WorkflowAwareCmsAdapter)
   │                    ├─ workflowFacade.startProcess("cms_article_review", businessKey=articleId)
   │                    ├─ article.process_instance_id = procInstId
   │                    └─ status = PENDING
   ↓
[审核人在 TodoList 或 文章 ReviewBar]
   │ complete(taskId, {approved: true/false, reason})
   ↓
[Flowable 引擎 PROCESS_COMPLETED 事件]
   ↓
[ArticleWorkflowEventListener]
   │   ├─ approved=true  → articleService.onWorkflowApprove(articleId)  → PUBLISHED + 首发 published_at
   │   └─ approved=false → articleService.onWorkflowReject(articleId, reason) → DRAFT + reason 进审计
   ↓
[ArticleStatusChangedEvent 发布] ── 给 M-5 inbox 桥消费
```

#### 后端结构

```
backend/scaffold-module-cms-workflow/
  pom.xml                                  (依赖 scaffold-module-cms + scaffold-module-workflow，二者都 optional=false)
  src/main/java/com/scaffold/module/cms/workflow/
    CmsWorkflowBridgeAutoConfiguration.java  (@AutoConfiguration, @ConditionalOnProperty enabled=false 默认关)
    WorkflowAwareCmsAdapter.java             (实现 CmsWorkflowAdapter, @ConditionalOnMissingBean 覆盖默认空实现)
    ArticleWorkflowEventListener.java        (订阅 Flowable PROCESS_COMPLETED, 反写文章状态)
    ArticleProcessDeployer.java              (启动后按 key+md5 检测部署 cms_article_review.bpmn20.xml)
  src/main/resources/
    processes/cms_article_review.bpmn20.xml  (起草人 → 审核节点 → 排他网关按 approved 分叉 → 结束)
    META-INF/spring/...AutoConfiguration.imports
  src/test/java/...
    WorkflowAwareCmsAdapterTest              (启动流程 → process_instance_id 写入)
    ArticleProcessDeployerTest               (key 缺失 → deploy；key 存在且 md5 同 → skip)
```

CMS 本体改动（最小侵入，与 M-3 兼容）：
- `cms_article` 表新增列 `process_instance_id VARCHAR(64) NULL`（changelog `module-cms.yml` 增 changeSet）
- `Article` 实体加 `processInstanceId` 字段
- `ArticleService.submit` 流程中：调 `workflowAdapter.onSubmit(article)` 并把返回的 `processInstanceId` 写入实体
- 新增 `ArticleService.onWorkflowApprove(id)` / `onWorkflowReject(id, reason)` 两个回调（仅桥模块调用，但放在 cms 本体里以便单测）
- 在所有状态转换的事务提交后发 `ArticleStatusChangedEvent`（M-5 消费）

#### 前端改动

- `Article` DTO 增加 `processInstanceId`（可空）
- `ArticleReviewBar.vue` 当 `processInstanceId` 存在时：
  - 加一个「查看审批进度」按钮 → 跳到 `ProcessProgressDialog`（复用 workflow 模块组件）
  - 「驳回」按钮文案改为「审批驳回」，调 workflow 的 `complete(taskId, approved=false)` 接口（前端先查 `processInstanceId` 对应当前 task）
  - 「通过」同理，转走 workflow `complete(approved=true)`
- `ArticleList.vue` 操作列：审批中的文章只显示「查看审批进度」+「撤回」（撤回功能 = workflowFacade.cancel）

#### E2E

`backend/scripts/verify-cms-workflow.ps1`：
1. 开启 `app.module.cms.workflow.enabled=true`
2. 创建栏目 + 4 篇文章
3. 文章 A submit → 检查 `process_instance_id != null` + 状态 `PENDING`
4. 模拟审核人 complete approved=true → 状态 `PUBLISHED` + `published_at` 不为空
5. 文章 B submit → 审核人 complete approved=false reason=「标题不合规」→ 状态 `DRAFT` + 审计日志含 reason
6. 关闭桥模块重跑 → 文章 C submit → 状态直接 PENDING（自闭环回退，不报错）

---

### M-5 CMS × Inbox 通知桥（✅ 已完成）

> CMS 文章状态发生关键变更时，自动推送站内信给相关人员，无需作者主动盯系统。完整使用文档见 [FEATURES.md §17.10](FEATURES.md#1710-m-5-cms--inbox-通知桥)。
>
> **后端 `scaffold-module-cms-inbox`**：独立桥模块，POM 仅依赖 cms 本体（`MessagePublisher` 在 framework 中，**不直接依赖 scaffold-module-inbox**——inbox 启用则自动落库 message_inbox 走持久化离线，inbox 未启用则只走 WebSocket 推送）；`@ConditionalOnProperty(app.module.cms.inbox.enabled=true)` 默认开；`ArticleStatusInboxListener` 用 `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)` 订阅 `ArticleStatusChangedEvent`，按 b/c/d 三路调 `MessagePublisher.toUser`，自动跳过 `actor==author` / `author 为空` / `publisher 不可用` 等异常情形。
>
> **触发表（仅给作者发，不打扰自己）**：
> | 状态变更 | 站内信 type | 文案模板 |
> |----------|-------------|---------|
> | * → PUBLISHED（通过 / 重新上线） | `cms.article.published` | 【已发布】您的文章《{title}》已发布上线 |
> | PENDING → DRAFT（必须 reason 非空）| `cms.article.rejected` | 【已驳回】《{title}》原因：{reason} |
> | PUBLISHED → UNPUBLISHED（下线） | `cms.article.unpublished` | 【已下线】《{title}》已被下线 |
>
> 所有 payload 含 `articleId / title / channelId / oldStatus / newStatus / actor / content / link=/cms/article-edit/{id}`，前端 NotificationBell 收到后 `router.push(link)` 一键跳转。**a) DRAFT → PENDING 提交审核** 不由 inbox 桥发——M-4 启用时由 workflow 自带的 `TaskNotifyEventListener` 通知审核人；M-4 关闭时 CMS 自闭环也无明确审核人概念，避免群发污染。
>
> **测试**：`ArticleStatusInboxListenerTest`（10 用例：通过/重新上线/驳回带 reason/驳回 reason 为空跳过/下线/提交跳过/actor==author 跳过/author 为空跳过/publisher 不可用容错/payload 含 piid）+ E2E `verify-cms-workflow.ps1` 用 cms_author 作者账号验证三种通知能被作者 inbox 拾到，链接指向编辑页。

下面是设计阶段保留的方案细节，已全部落地：

#### 决策（已与用户对齐）

| 维度 | 决策 |
|------|------|
| 触发点 | a/b/c/d 全做：a) 提交审核→审核人；b) 审核通过→作者；c) 驳回→作者带 reason；d) 下线→作者。e) 群发订阅角色——不做 |
| 实现方式 | **Spring `ApplicationEvent` 事件驱动**：CMS 发 `ArticleStatusChangedEvent`，inbox 桥订阅后翻译成站内信 |
| 与 M-4 协同 | M-4 桥启用时，触发点 a（PENDING 通知审核人）由 workflow 的 `TaskNotifyEventListener` 处理，**inbox 桥跳过 a**避免重复；M-4 关闭时，inbox 桥按角色 `cms_reviewer` 群发 |
| 深链 | 站内信 `link=/cms/article-edit/{id}`；不发审计日志（避免淹审计） |

#### 后端结构

```
backend/scaffold-module-cms-inbox/
  pom.xml                                   (依赖 scaffold-module-cms + scaffold-module-inbox)
  src/main/java/com/scaffold/module/cms/inbox/
    CmsInboxBridgeAutoConfiguration.java     (@AutoConfiguration, 默认开 enabled=true)
    ArticleStatusInboxListener.java          (@EventListener ArticleStatusChangedEvent)
    ArticleInboxMessageBuilder.java          (按状态翻译成站内信文案 / link)
  src/test/java/...
    ArticleStatusInboxListenerTest           (a/b/c/d 触发表 + M-4 启用时 a 跳过)
```

CMS 本体改动（仅事件层，无业务耦合）：
- 在 `ArticleService` 所有状态转换方法（submit / approve / reject / publish / unpublish / republish）的事务提交后，通过 `ApplicationEventPublisher.publishEvent(new ArticleStatusChangedEvent(...))` 发事件
- `ArticleStatusChangedEvent` 字段：`articleId, articleTitle, channelId, oldStatus, newStatus, actorId, authorId, reason, processInstanceId`

#### 文案（按状态分支）

| 触发 | 收件人 | 文案 | 触发判定 |
|------|--------|------|----------|
| a) DRAFT → PENDING | 审核人（M-4 关时按角色 cms_reviewer 群发） | 【待审】《xxx》请审核 | M-4 桥未启用 |
| b) PENDING → PUBLISHED 或 UNPUBLISHED → PUBLISHED | 作者 | 【已发布】您的文章《xxx》已发布上线 | 永远 |
| c) PENDING → DRAFT（reason 非空）| 作者 | 【已驳回】《xxx》原因：{reason} | 永远 |
| d) PUBLISHED → UNPUBLISHED | 作者 | 【已下线】《xxx》已被下线 | 永远（actor != author 时才发，避免自己下线给自己发）|

全部站内信 `type=cms`，方便前端按类型过滤。

#### E2E（与 M-4 合并到同一脚本）

`verify-cms-workflow.ps1` 在 M-4 各步骤后续上检查：
- 步骤 4 后：作者 inbox 拾到「【已发布】」未读消息
- 步骤 5 后：作者 inbox 拾到「【已驳回】原因：标题不合规」
- 关闭 M-4 桥（仅 inbox 桥启用）：作者一路通知正常，不会因找不到 workflow Bean 报错

#### 落地节奏（4 批一气干完）

| 批次 | 内容 |
|------|------|
| **第 1 批 M-4 后端 + BPMN + 反向同步** | CMS 本体加 `ArticleStatusChangedEvent` + `process_instance_id` 列；新建 cms-workflow 桥模块、AutoConfig、`WorkflowAwareCmsAdapter`、`ArticleWorkflowEventListener`、自带 BPMN + 启动部署器 |
| **第 2 批 M-4 前端 + E2E** | 文章 ReviewBar 接 workflow 完成接口；查看审批进度跳 ProcessProgressDialog；E2E `verify-cms-workflow.ps1` 提交→通过/驳回完整链路 |
| **第 3 批 M-5 桥模块** | 新建 cms-inbox 桥模块、`ArticleStatusInboxListener`、按 a/b/c/d 翻译站内信；与 M-4 协同跳过 a；深链 `/cms/article-edit/{id}` |
| **第 4 批 单测 + 联动 E2E + 文档** | `WorkflowAwareCmsAdapterTest` / `ArticleServiceWorkflowCallbackTest` / `ArticleStatusInboxListenerTest`；联动 E2E 续上检查 inbox；FEATURES §17.x + §17.y；ROADMAP M-4/M-5 翻 ✅；RUNBOOK 命令速查 |

---

> 原本是一个 ticket 里的两件事，目前两件都已合入主分支：
> - **操作审计 ✅** —— 详见 [FEATURES.md §14](FEATURES.md#14-操作审计-auditlog--sys_audit_log)
> - **数据级权限 ✅** —— 详见 [FEATURES.md §16](FEATURES.md#16-数据级权限data-scope-已就绪--默认接入审计列表)

#### 操作审计（已交付内容）

`@AuditLog` + `sys_audit_log` 横切能力，自带**结构化资源标识 + before/after 快照 + RFC 6902 JSON Patch diff** + 异步落库。

- 注解：`@AuditLog(module=, action=, resourceType=, resourceId=, beforeProvider=, comment=, recordReturn=, excludeFields=)`，全部 SpEL 友好
- 切面：`scaffold-framework/.../AuditLogAspect`（Around，业务异常不阻断）
- diff：`zjsonpatch 0.6.2` 输出标准 JSON Patch；前端按操作类型上色
- 写入：`AsyncManager` + `recordAudit` task，与 `recordOper` 同机制
- 列表页：`/system/audit/log`，按 module / action / resourceType / resourceId / actor / status / 时间区间检索；详情页含 diff 表 + before/after 折叠
- 已挂示例：`SysUserController` 的 `add / edit / remove / resetPwd / changeStatus` 五个写操作；`SysRoleController` 的 `add / edit / dataScope` 三个写操作

与原有 `@Log` + `sys_oper_log` **互补共存**：流水进 oper_log，事件进 audit_log，关键写操作两边都挂。

#### 数据级权限（已交付内容）

`@DataScope` + `DataScopeAspect` 横切能力，按角色 dataScope 自动按部门 / 本人维度过滤业务列表 SQL。

| 模块 | 落地内容 |
|------|----------|
| 切面 | 已存在的 `DataScopeAspect` 完整覆盖五种数据范围（全部 / 自定义 / 本部门 / 本部门及以下 / 仅本人）|
| 角色 UI | 新建/编辑表单加 `dataScope` 下拉；列表新增"数据范围"列；操作列加"数据权限"按钮 → 独立 dialog（部门树 + check-strictly 切换）|
| 审计联动 | `SysRoleController.add / edit / dataScope` 三处加 `@AuditLog`，分配数据权限走 `system.role / AUTH_DATA_SCOPE` action |
| Demo 接入 | `/system/audit/log/list` 自身挂 `@DataScope(deptAlias="d", userAlias="u")`：admin 看全量，部门角色仅看本部门 actor 触发的审计；表 `sys_audit_log` 加 `actor_dept_id` + 历史回填 changeset |
| 测试 | E2E 脚本 `backend/scripts/verify-data-scope.ps1`：自动创建测试角色 / 用户，验证 dataScope=1/3/5 三种隔离行为 |
| 文档 | [FEATURES.md §16](FEATURES.md#16-数据级权限data-scope-已就绪--默认接入审计列表)：含三步快速接入指南、五种数据范围 SQL 拼接对照、踩坑提示 |

---

## 3. 模块化基础设施（已完成 ✅）

为了让 M-1 工作流真正"装上即用、删掉即走"，先把脚手架抽出"模块加载约定"。
**B-1/B-2/B-3 已合入主分支**，使用手册见 [FEATURES.md §13](FEATURES.md#13-可插拔模块加载约定-b-1b-2b-3)。

### B-1 后端模块自动装配模板 ✅

- ✅ `scaffold-common` 提供 `ScaffoldModule` 元数据 + `ModuleRegistry`。
- ✅ 业务模块独立 Maven 子模块（`backend/scaffold-module-<name>`），通过 `@AutoConfiguration` + `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 自启。
- ✅ `/actuator/scaffold-modules` 端点列出当前启用模块。
- ✅ 启动类 `ScaffoldApplication` 把 `com.scaffold.module.*` 从默认 ComponentScan 中排除，AutoConfig 在 `app.module.<name>.enabled=false` 时整体跳过——真正实现"删 jar 即下线 / 配置即关停"。
- ✅ inbox 抽出为第一个样板模块 `scaffold-module-inbox`，并验证：
  - 注释掉 `scaffold-admin/pom.xml` 里的依赖 → 整体仍可编译 + 运行（少一张表 + 少一个 controller）。
  - `app.module.inbox.enabled=false` → 模块组件全部不加载。
- ✅ `RedisMessageBus` 通过 `MessageBusRecorder` 接口与 inbox 解耦，framework **不依赖**任何业务模块。

### B-2 前端模块加载约定 ✅

- ✅ `frontend/src/modules/<name>/index.ts` 默认导出 `ScaffoldFrontendModule { name, routes?, locales?, install? }`。
- ✅ `main.ts` 用 `import.meta.glob('./modules/*/index.ts', { eager: true })` 在启动时自动注入 i18n + addRoute + 执行 install 钩子。
- ✅ `frontend/src/layout/widgets.ts` 提供 TopBar 组件槽，模块通过 `registerTopBarWidget` 向顶栏注入图标——TopBar 自己**不引用**任何模块。
- ✅ inbox 模块抽出为 `frontend/src/modules/inbox/`：API、store、`NotificationBell` 全部内聚；删整目录即下线，TopBar 上的铃铛随之消失。已验证：删除整个目录后 `npm run build` 成功。

### B-3 Liquibase changelog 拆分 ✅

- ✅ 主 changelog 改为 `include` 模块的 `db/changelog/module-<name>.yml`，并带 `errorIfMissingOrEmpty: false` —— 模块缺失也能正常启动。
- ✅ 模块的 changelog 与 SQL 都打包到自己的 jar；inbox 的 SQL 已搬到 `scaffold-module-inbox/src/main/resources/db/changelog/`。
- ✅ 模块自带 `*-uninstall.sql` 模板（如 `message_inbox-uninstall.sql`），方便拆出项目后清表。

---

## 4. 不做的（明确）

- 双因子 / WebAuthn / 设备指纹（M-2 旧编号 2，用户已拒绝）。
- OAuth2 Resource Server / Token Introspection。
- Saga / 补偿事务、分布式追踪、Webhook 出站平台、多租户 schema、Feature Flags、PWA、代码生成 ↔ OpenAPI 双向、消息可靠投递（用 inbox 已经够用）等旧 ROADMAP 中"剩下的都不做"项。

如果将来需要其中之一，再单独提需求。

---

## 5. 推荐落地节奏

| 阶段 | 内容 | 估时 | 产物 |
|------|------|------|------|
| 0 | ✅ 模块加载约定（B-1/B-2/B-3） | 已完成 | 抽象到位，inbox 是第一个样板模块 |
| 1 | ✅ 操作审计（@AuditLog + sys_audit_log + diff 渲染） | 已完成 | 平台合规底线 |
| 2 | ✅ M-1 工作流 MVP（Flowable 8 + bpmn-js 设计器 + 待办/已办） | 已完成 | 第二个"可拆/可装"的业务模块样板，与审计 / 推送总线联动 |
| 3 | ✅ 工作流增强（只读模式 + 运行时态高亮 + 抄送/后加签/退回 + 启动表单引擎） | 已完成 | 业务可用，详见 FEATURES §15.1–15.3 |
| 4 | ✅ 数据级权限（@DataScope + 角色管理 UI + 审计列表 demo + 三步接入指南） | 已完成 | 详见 FEATURES §16 |
| 5 | ✅ 工作流增强第二批（任务级动态表单 + 前加签 + 流程实例时间轴） | 已完成 | 详见 FEATURES §15.5–15.7；E2E 脚本 `backend/scripts/verify-workflow-enhancements.ps1` |
| 6 | ✅ 工作流增强第三批（阻塞 tag / 撤销前加签 / 导出 / 体积优化 / 实例总列表） | 已完成 | 详见 FEATURES §15.6, §15.8–15.10；E2E 脚本同上（已扩展撤销前加签 + 实例分页断言） |
| 7 | ✅ M-3 CMS（栏目 / 文章 / 状态机 / 公开 API / wangEditor / 存储抽象） | 已完成 | 第三个可插拔业务模块。详见 FEATURES §17；E2E 脚本 `backend/scripts/verify-cms.ps1`；前端 `vendor-wangeditor` 独立 chunk 懒加载。 |
| 8 | ✅ M-4 CMS × Workflow 联动桥 + M-5 CMS × Inbox 通知桥 | 已完成 | 两个独立桥模块（`scaffold-module-cms-workflow` / `scaffold-module-cms-inbox`），CMS 本体只加事件通道与 process_instance_id 列；详见 FEATURES §17.9 / §17.10；联动 E2E 脚本 `backend/scripts/verify-cms-workflow.ps1`。 |
| 9 | ✅ 保养向（P1 + P4 + P2 + P5） | 已完成 | P1 CMS DTO/domain 全量 `@Schema`（commit `89676ff`）；P4 审计 diff 字段级表格 + 复用 `AuditDiffViewer.vue`（`fc98e85`）；P2 Inbox 全页面 `/system/message/inbox` + 批量操作（`4f7f8a6`）；P5 三模块（CMS / Inbox / Workflow）静态文案全量迁移到 zh-CN + en-US locales（`62e0a89`）。详见 FEATURES §14 / §17.11 / Inbox 章节。 |
| 10 | ✅ 保养向 P3 — Workflow BPMN 版本对比（双面板 + 节点 add/remove/rename 摘要） | 已完成 | 后端 `WorkflowFacade.listVersionsByKey` + `GET /workflow/process/definitions/by-key/{key}/versions`；前端 `BpmnVersionDiffDialog.vue`（自研 DOMParser diff，零新增依赖）；commit `e42167b`。 |
| 11 | ✅ refactor(storage) — UploadStorageService 上提 framework + 通用 `/system/upload/file` | 已完成 | 把 CMS 自造的 StorageService 上提到 framework 层并改委托 framework 已有 `FileStorageService`；新增通用上传端点 `/system/upload/file`（admin 通配 + `system:upload:file`）；CMS bucket 改为 `cms/image`，对外 URL 形态保持不变。commit `0d3a995`（M-10 Pre-Phase）。 |
| 12 | ✅ M-10 通用表单引擎（form_template / form_submission + 6 widget + 5 前端页面） | 已完成 | 第四个可插拔业务模块。后端 `scaffold-module-form` 双表 + 状态机（DRAFT/PUBLISHED/ARCHIVED）+ 版本派生（PUBLISHED 编辑自动 v+1）；前端 modules/form：FormRenderer 运行时 + 6 高阶 widget（DynamicTable / DetailSubForm / UserPicker / DeptPicker / DictSelect / CascaderSelect）+ 5 个页面（TemplateList / TemplateDesign / FormFill / SubmissionList / SubmissionDetail）；vendor-form-create-designer chunk 仅 TemplateDesign 入口懒加载（~1MB / 340KB gzip 不污染填报场景）；E2E 脚本 `backend/scripts/verify-form.ps1` 覆盖 12 步（CRUD 状态机 / 版本派生 / 通用上传白名单）；commits `7b79e99` / `2dc168e` / `8a72c85`。详见 FEATURES §18。 |
| 13 | ✅ M-6 文件中心（sys_file 主表 + 文件夹 + 分享 + 引用计数 + 鉴权下载） | 已完成 | 第五个可插拔业务模块 `scaffold-module-file`。四张表：`sys_file` 主表（refCount + 软删 + deleteTime）/ `sys_file_folder` 用户级文件夹树 / `sys_file_share` 分享链接（过期 / 一次性 / BCrypt 密码）/ `sys_file_ref` 跨模块引用计数明细。两阶段删除：默认软删 → quartz 30 天清磁盘（`FileCleanupJob.purge` + `sys_job` id 6025 已预装；备用入口 POST `/file/file/purge-now`）。鉴权下载 `/file/download/{id}` 流式输出 + path traversal 防护；S3 模式自动 302。分享访问 `/file/share/access/{token}` 走 `@Anonymous`，五重校验（status / 过期 / 一次性 / 密码 / 文件未删）。前端 modules/file：FileList 左侧文件夹树 + 列表 / 上传 / 编辑 / 引用查看 / 软删 / 预览（图片 + pdf 内嵌）；ShareList 全部分享管理；FilePicker / FilePreview / ShareDialog 三个复用组件 re-export 给 CMS / form 等业务模块。33 例后端单测 + 12 步 E2E 脚本 `backend/scripts/verify-file.ps1`；commits `ef290d9` / `03b4adf`。 |
| 14 | ✅ M-8 报表中心（SQL 模板 + 看板 + 外部数据源 + ECharts 懒加载） | 已完成 | 第六个可插拔业务模块 `scaffold-module-report`。五张表：`sys_report_template`（SQL 模板，rowLimit/timeoutMs/permKey 三闸）/ `sys_report_run_log`（运行历史，含失败 / 超时 / 慢查询）/ `sys_report_dashboard` + `sys_report_dashboard_card`（看板 + 卡片）/ `sys_report_datasource`（外部 JDBC 数据源，密码 AES 加密落库）。安全：`ReportSqlGuard` 静态护栏（select-only / 注释剥离 / 字符串字面量保护 / 多语句拒 / OUTFILE / @@ 拒）+ `ReportParamBinder`（`${name}` → PreparedStatement `?`）+ `ReportRunner` 行数 + 超时双闸 + BigDecimal→Double 类型规范化。数据源：`ReportDataSourceManager` 主库 id=0 直接复用；外部 id>0 懒建 Druid + 缓存 + 修改 / 删除自动 invalidate；`Aes256Util`（scaffold-common 新增）AES-256-CBC 可逆加密，每次 IV 随机；`/report/datasource/test` 接口测连接不入池。`ReportRunLogCleanupJob`：`sys_job` 7025 每天凌晨 4 点清 90 天前日志。前端 modules/report：ReportList / ReportEdit / ReportRun（参数表单 + 表格预览 + CSV/xlsx 导出）/ DashboardList / DashboardView（卡片网格 + table/number 直渲、line/bar/pie 走 EchartsCard）/ DataSourceList（增删改 + 测连接 + 密码 null=不动 / 空=清空 / 非空=改）/ RunLogList。`vendor-charts` chunk（1.05 MB）保持单独，仅在第一次渲染图表卡片时下载，不污染主包。51 例后端单测（SqlGuard 16 + ParamBinder 5 + Runner 8 + Template 6 + DataSource 9 + Dashboard 7）+ 14 步 E2E 脚本 `backend/scripts/verify-report.ps1`；commits `4f76b59` / `43c50a6`。 |

| 16 | ✅ Q-3 可观测性（Slow API / Prometheus 指标 / 健康检查 / 告警 inbox） | 已完成 | 集成进 `scaffold-framework`（不需要新模块）：①`HttpRequestRecorder` Filter — 慢请求 / 5xx / 4xx（可选）落 `sys_slow_request`，复用已有 TraceId 写 trace_id 字段；②`SlowRequestPersistService` `@Async` 持久化（避免 Filter 阻塞）；③`BusinessMetricsBinder` — 启动后扫 information_schema 自动给 15 张业务表注册 `scaffold.business.rows{table=...}` Gauge，60s 刷一次，缺表自动跳过；④`ScaffoldModulesHealthIndicator` — `/actuator/health/scaffoldModules` 聚合模块清单；⑤`SlowApiAlertJob` — `sys_job` 9001 每 5 分钟扫窗口（默认 10 min）pending 慢/错请求，按 reason 聚合发 inbox（`observability.slow_request` / `observability.server_error`，每条最多 5 个样本，多接收人逗号分隔）；⑥前端 3 页（菜单 9001-9004）— SlowRequestList（filter / purge / scan-now / 单删）/ BusinessMetrics（Prometheus 文本解析展示业务表 + JVM + HTTP TOP10）/ HealthDashboard（components 状态卡 + 模块清单）。配置：`app.observability.{enabled, slowMs, recordClientError, alertWindowMinutes, alertRecipients, excludeUriPattern, purgeDays}`。156 例后端单测（新增 `SlowApiAlertJobTest` 7 + `HttpRequestRecorderTest` 8 + `BusinessMetricsBinderTest` 5 + `ScaffoldModulesHealthIndicatorTest` 2 + 复用 132 + 2）+ 10 步 E2E `backend/scripts/verify-observability.ps1`；详见 FEATURES §20。 |

| 16 | ✅ Q-3 可观测性集成进 framework（慢请求落表 + 业务指标 + 健康聚合 + inbox 告警） | 已完成 | 框架级集成，不新增模块：①`HttpRequestRecorder` Filter（order=HIGHEST_PRECEDENCE+10）紧跟 TraceIdFilter 后，命中阈值 / 5xx 才异步落 `sys_slow_request`（含 traceId）。②`BusinessMetricsBinder` 启动时扫 information_schema 给约定 15 张业务表注册 `scaffold.business.rows` Gauge。③`ScaffoldModulesHealthIndicator` `/actuator/health/scaffoldModules`。④Quartz 9001 每 5 分钟跑 `SlowApiAlertJob`，按 SLOW / SERVER_ERROR 分组发 inbox（payload 取耗时 TopN 5 条防膨胀）。前端 3 页（慢请求列表 / 业务指标 / 健康检查）。22 例后端单测 + 10 步 E2E。详见 [FEATURES.md §20](FEATURES.md#20-可观测性q-3集成进-framework)
按这个节奏 2–3 周可见 MVP。后续 IM / 财务等模块可以照同一个套路 fork。

> **下一步候选**：① Q-1 缓存抽象（自动缓存 + 失效）；② M-7 IM 群组聊天（如有需求）；③ M-16 简易财务（凭证 / 应收应付 / 总账 / 利润表）；④ M-17 BI 自助分析（拖拽报表）。