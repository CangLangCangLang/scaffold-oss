# Fullstack Scaffold（开源版）

> 一个通用前后端管理系统脚手架，开源版保留核心框架、系统管理、工作流、CMS、表单、文件、报表、可观测性等平台能力，可作为各类业务系统的基座。
> 商业版额外提供 CRM、Ticket、PM、Inventory、OA 五个完整业务套件，见下方"商业版"章节。

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> 💡 **关于源头**：本项目的系统管理 / 权限 / 字典 / 数据范围 / 代码生成 等核心框架来自
> [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue)（MIT）。在此之上**新增**了
> 工作流、CMS、CMS×Workflow 桥、表单引擎、文件中心、报表中心、可观测性、数据范围审计 等开源平台模块。
>
> RuoYi 原始部分仍为 MIT 自由商用。如果你只需要权限框架，请直接用 RuoYi-Vue。
> 完整三方署名见 [`NOTICE.md`](./NOTICE.md)。

## ✨ 整体能力一览

| 类别 | 模块 |
|------|------|
| 内核 | scaffold-framework / system / quartz / generator / common |
| 开源业务模块 | inbox（站内信）/ workflow（Flowable BPMN 引擎）/ cms（内容）/ cms-workflow（CMS 审批）/ form（动态表单）/ file（文件中心）/ report（报表看板） |
| 开源桥模块 | cms × inbox、CMS × workflow |
| 商业套件（需购买） | CRM / Ticket / PM / Inventory / OA，包含后端模块、inbox 桥、workflow 桥与前端页面 |

完整功能清单见 [`docs/FEATURES.md`](./docs/FEATURES.md)；运维 / 部署 / 卸载手册见 [`docs/RUNBOOK.md`](./docs/RUNBOOK.md)；阶段路线图见 [`docs/ROADMAP.md`](./docs/ROADMAP.md)。

## 🚀 5 分钟启动

```powershell
# 配置（从 .env.example 复制）
Copy-Item .env.example .env

# 后端
cd backend
mvn clean install '-Dmaven.test.skip=true'
java -jar scaffold-admin\target\scaffold-admin.jar

# 前端
cd ..\frontend
npm install
npm run dev
```

默认账号 `admin / admin123`；MySQL / Redis 端口请改 `.env`。

## 📦 商业版（Enterprise Edition）

开源版聚焦平台底座和通用能力；商业版提供 5 个完整业务套件：

| 套件 | 价值 |
|---|---|
| CRM | 线索、客户、公海、商机、合同、销售看板 |
| Ticket | 工单、SLA、知识库、工单看板 |
| PM | 项目、任务、看板、甘特图、里程碑、工时 |
| Inventory | 商品、库存、出入库、盘点、库存看板 |
| OA | 请假、报销、用印、公文、公告、通讯录、会议室 |

### 价格

- **¥1,999 一次性买断**——付款后加入主仓 GitHub Collaborator，`git clone` 即得完整源码
- 无 license 校验、无水印、无运行期通信
- 公司内部不限项目数；可作为给客户做项目交付的一部分
- 禁二次销售 / 禁公开 fork / 禁包装 SaaS 卖订阅

### Bug 与维护（请在购买前认可）

- **AS IS**：作者不为 bug 负责、不承诺持续更新、不承诺响应时间
- 买家拿到代码后**自行修复**遇到的问题；邮件可问，能修就修
- 未来若发布**重大版本**（架构重构 / 新增整套业务模块），老用户 **¥499 一次性升级**到新主仓

### 退款

- 收到 GitHub 邀请前可全额退款；已 clone 后不退（代码已交付，无法收回）

### 联系方式

- 邮件 `568185583@qq.com`：附 GitHub 用户名 + "购买商业版"
- 收到邮件后回告收款方式（微信/支付宝），付款截图回邮即发邀请
- 全程邮件，不签合同，不发 PDF——这是个人开发者作品，按朋友间交易处理

## 💖 支持开源开发

如果开源版对你有用，欢迎请作者喝杯咖啡：

<p align="center">
  <img src="docs/images/alipay-qr.png" alt="支付宝赞赏码" width="240" />
</p>

赞赏属于自愿赠与，不构成任何对价或售后承诺。

## 🤝 贡献

**当前阶段不接受 Issue / PR。** 这个仓库仅用于发布开源版本，开发主线在私有主仓。
如果你发现 bug 或想贡献功能，请直接通过上方"联系方式"邮件沟通；后续如果开源社区活跃度起来，会重新打开 Issue 区。

## 📄 许可证

[MIT](./LICENSE) — 你可以自由用于商业项目，但请保留版权声明 + 同时保留 RuoYi-Vue 的署名（见 [`NOTICE.md`](./NOTICE.md) 与 [`licenses/RuoYi-Vue-LICENSE.txt`](./licenses/RuoYi-Vue-LICENSE.txt)）。
