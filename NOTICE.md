# Third-Party Notices

This project is built on top of, and contains substantial portions of code derived from, the following open-source projects. Their original license texts are preserved in the [`licenses/`](./licenses/) directory.

## RuoYi-Vue（核心脚手架来源）

- 项目：<https://gitee.com/y_project/RuoYi-Vue> · <https://github.com/yangzongzhuan/RuoYi-Vue>
- 协议：MIT License
- 原版权：Copyright (c) 2018 RuoYi
- 原 LICENSE 全文：[`licenses/RuoYi-Vue-LICENSE.txt`](./licenses/RuoYi-Vue-LICENSE.txt)
- 用途：本项目以 **RuoYi-Vue** 作为脚手架基础，保留并使用其权限管理、字典、数据范围切面、代码生成、定时任务、系统监控等系统模块；并在其之上**新增**了：
  - 开源平台模块：`scaffold-module-workflow / cms / cms-workflow / form / file / report / inbox`
  - 商业业务套件（非开源）：`scaffold-enterprise-crm / ticket / pm / inventory / oa`
  - 模块化 SPI（`ScaffoldModule` / `ModuleRegistry` / 前端 `loader.ts`）
  - 数据范围审计、可观测性、CMS×Workflow 桥、表单引擎、Flowable 增强（add-sign / cancel-presign / timeline）等

如果你只需要权限框架本身，请直接使用 RuoYi-Vue（MIT 自由商用），无须购买本项目的商业版。本项目商业版仅就**作者新增的代码**收费。

## 主要运行时依赖

| 项目 | 协议 | 用途 |
| --- | --- | --- |
| Spring Boot | Apache-2.0 | 后端基础框架 |
| Spring Security | Apache-2.0 | 认证与授权 |
| Flowable Engine | Apache-2.0 | BPMN 工作流引擎 |
| MyBatis / MyBatis-Plus | Apache-2.0 | 数据访问 |
| Liquibase | Apache-2.0 | 数据库迁移 |
| Druid | Apache-2.0 | 连接池 |
| Hutool | MulanPSL-2.0 | 工具库 |
| Vue / Vue Router / Pinia | MIT | 前端框架 |
| Element Plus | MIT | UI 组件库 |
| Vite / vue-tsc | MIT | 前端构建链 |
| ECharts | Apache-2.0 | 可视化 |
| js-cookie / axios | MIT | 浏览器侧基础库 |

完整依赖清单与版本号见 [`backend/pom.xml`](./backend/pom.xml) 与 [`frontend/package.json`](./frontend/package.json)。

## 脚本与图标

- `docs/images/`：UI 截图为本项目实际运行截图，作者持有版权；引用本项目时一并保留。
- 文档章节中以"参考 RuoYi"或"RuoYi 体系"形式提到的工程惯例，仅作技术致敬，不构成对其商标或商业品牌的使用。
