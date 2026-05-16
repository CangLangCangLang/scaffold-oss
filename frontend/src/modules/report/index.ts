/**
 * report 模块（前端）：报表中心 M-8。
 *
 * <p>组成：
 * <ul>
 *   <li>ReportList：模板列表（CRUD + 跳转运行）</li>
 *   <li>ReportEdit：模板编辑（SQL 文本 + 校验 + 数据源 / 配额）</li>
 *   <li>ReportRun：运行模板（参数表单 + 表格预览 + CSV/xlsx 导出）</li>
 *   <li>DashboardList：看板列表</li>
 *   <li>DashboardView：看板查看 / 编辑（卡片网格 + ECharts 懒加载）</li>
 *   <li>DataSourceList：外部数据源 CRUD + 测连接</li>
 *   <li>RunLogList：运行日志（含失败 / 超时筛选）</li>
 * </ul>
 *
 * 路由 path 与后端菜单 path 对齐：/report/template /report/dashboard /report/datasource /report/log
 *（admin 和 mine 皆走同一路由路径，由 menu_id 7002-7005 单独控权限）。
 */
import type { RouteRecordRaw } from 'vue-router'
import type { ScaffoldFrontendModule } from '../loader'

const routes: RouteRecordRaw[] = [
  {
    path: 'report/template',
    name: 'ReportList',
    component: () => import('./views/ReportList.vue'),
    meta: { title: 'report.menu.templates' }
  },
  {
    path: 'report/template/:id',
    name: 'ReportEdit',
    component: () => import('./views/ReportEdit.vue'),
    meta: { title: 'report.menu.editTemplate', activeMenu: '/report/template', hidden: true }
  },
  {
    path: 'report/run/:id',
    name: 'ReportRun',
    component: () => import('./views/ReportRun.vue'),
    meta: { title: 'report.menu.run', activeMenu: '/report/template', hidden: true }
  },
  {
    path: 'report/dashboard',
    name: 'DashboardList',
    component: () => import('./views/DashboardList.vue'),
    meta: { title: 'report.menu.dashboards' }
  },
  {
    path: 'report/dashboard/:id/:mode',
    name: 'DashboardView',
    component: () => import('./views/DashboardView.vue'),
    meta: { title: 'report.menu.dashboard', activeMenu: '/report/dashboard', hidden: true }
  },
  {
    path: 'report/datasource',
    name: 'ReportDataSourceList',
    component: () => import('./views/DataSourceList.vue'),
    meta: { title: 'report.menu.datasources' }
  },
  {
    path: 'report/log',
    name: 'ReportRunLogList',
    component: () => import('./views/RunLogList.vue'),
    meta: { title: 'report.menu.logs' }
  }
]

const reportModule: ScaffoldFrontendModule = {
  name: 'report',
  routes,
  locales: {
    'zh-CN': {
      report: {
        menu: {
          root: '报表中心',
          templates: '报表模板',
          editTemplate: '编辑模板',
          run: '运行模板',
          dashboards: '看板列表',
          dashboard: '看板',
          datasources: '数据源',
          logs: '运行日志'
        },
        common: {
          search: '查询',
          reset: '重置',
          action: '操作',
          edit: '编辑',
          remove: '删除',
          back: '返回',
          save: '保存',
          saveOk: '保存成功',
          cancel: '取消',
          confirm: '确认',
          deleteOk: '已删除'
        },
        template: {
          add: '新建模板',
          colCode: '编码',
          colName: '名称',
          colCategory: '分类',
          colDataSource: '数据源',
          colSql: 'SQL',
          colRowLimit: '行数上限',
          colTimeoutMs: '超时(ms)',
          colPermKey: '运行权限 key',
          colStatus: '状态',
          colCreator: '创建者',
          colCreateTime: '创建时间',
          colRemark: '备注',
          searchNamePh: '名称 LIKE',
          codePh: '建议小写英文 + 下划线',
          permKeyPh: '为空 = 仅登录可用；填了再做二次校验',
          sqlPh: '仅允许 SELECT / WITH 起头；参数用 ${name} 占位',
          paramSchemaLabel: '参数声明（JSON 数组）',
          paramSchemaTip: '示例：{sample}',
          paramSchemaInvalid: '参数声明不是合法 JSON',
          paramSchemaNotArray: '参数声明必须是数组',
          dsMain: '主库',
          statusActive: '启用',
          statusDisabled: '停用',
          actionRun: '运行',
          actionEdit: '编辑',
          actionRemove: '删除',
          actionValidate: '校验 SQL',
          validateOk: 'SQL 通过 SqlGuard 检查',
          confirmDelete: '确定删除模板 "{name}"？相关运行日志保留',
          deleteOk: '模板已删除'
        },
        run: {
          back: '返回',
          params: '参数',
          noParams: '此模板未声明参数',
          run: '运行',
          exportCsv: '导出 CSV',
          exportXlsx: '导出 xlsx',
          runOk: '查询完成：{rows} 行，耗时 {ms} ms',
          truncated: '已截断到 {rows} 行（达到 rowLimit）',
          truncatedTag: '已截断',
          resultMeta: '结果：{rows} 行，耗时 {ms} ms'
        },
        dashboard: {
          add: '新建看板',
          actionView: '查看',
          actionEdit: '编辑',
          actionRemove: '删除',
          colCode: '编码',
          colName: '看板名',
          colCategory: '分类',
          codePh: '建议小写英文 + 下划线',
          confirmDelete: '确定删除看板 "{name}"？',
          addCard: '加卡片',
          untitledCard: '未命名卡片',
          noActiveTemplate: '请先在「报表模板」创建并启用至少一个模板',
          cardTitle: '卡片标题',
          cardTemplate: '关联模板',
          cardChart: '图表类型',
          cardLayout: '宽度 / 高度',
          cardConfig: '图表配置',
          cardConfigPh: '可选 JSON：{"x":"列名","y":"列名"}（图表类卡片）',
          cardParam: '默认参数',
          cardParamPh: '可选 JSON：{"minId":1}',
          notLoaded: '尚未加载，请点右上角「刷新」',
          refresh: '刷新',
          refreshAll: '全部刷新',
          confirmRefreshAll: '即将并发执行所有卡片，是否继续？',
          refreshOk: '已全部刷新'
        },
        datasource: {
          add: '新建数据源',
          edit: '编辑数据源',
          test: '测试连接',
          testOk: '连接成功',
          colCode: '编码',
          colName: '名称',
          colType: '类型',
          colJdbcUrl: 'JDBC URL',
          colDriverClass: '驱动类',
          colUsername: '用户名',
          colPassword: '密码',
          jdbcUrlPh: '示例：jdbc:mysql://host:3306/db?...',
          driverClassPh: '留空 = 用类型默认驱动',
          passwordPh: '新数据源：填明文（后端 AES 加密落库）',
          passwordEditPh: '留空 = 不动；填新值 = 重新加密',
          clearPassword: '清空密码',
          confirmDelete: '确定删除数据源 "{name}"？引用了它的报表会运行失败'
        },
        runlog: {
          colTemplateId: '模板 ID',
          colTemplateCode: '模板编码',
          colDataSource: '数据源',
          colRowCount: '行数',
          colCostMs: '耗时(ms)',
          colStatus: '状态',
          colError: '错误',
          colSqlPreview: 'SQL 预览',
          statusOk: '成功',
          statusFail: '失败',
          statusTimeout: '超时',
          purge: '清 90 天前',
          purgeOk: '清理已触发'
        }
      }
    },
    'en-US': {
      report: {
        menu: {
          root: 'Report Center',
          templates: 'Report Templates',
          editTemplate: 'Edit Template',
          run: 'Run Template',
          dashboards: 'Dashboards',
          dashboard: 'Dashboard',
          datasources: 'Data Sources',
          logs: 'Run Logs'
        },
        common: {
          search: 'Search',
          reset: 'Reset',
          action: 'Actions',
          edit: 'Edit',
          remove: 'Delete',
          back: 'Back',
          save: 'Save',
          saveOk: 'Saved',
          cancel: 'Cancel',
          confirm: 'Confirm',
          deleteOk: 'Deleted'
        },
        template: {
          add: 'New Template',
          colCode: 'Code',
          colName: 'Name',
          colCategory: 'Category',
          colDataSource: 'Data Source',
          colSql: 'SQL',
          colRowLimit: 'Row Limit',
          colTimeoutMs: 'Timeout (ms)',
          colPermKey: 'Run Perm Key',
          colStatus: 'Status',
          colCreator: 'Creator',
          colCreateTime: 'Created',
          colRemark: 'Remark',
          searchNamePh: 'name LIKE',
          codePh: 'lower_snake_case',
          permKeyPh: 'empty = login only; otherwise additionally checked',
          sqlPh: 'SELECT / WITH only; use ${name} for params',
          paramSchemaLabel: 'Param Schema (JSON array)',
          paramSchemaTip: 'Example: {sample}',
          paramSchemaInvalid: 'Param schema is not valid JSON',
          paramSchemaNotArray: 'Param schema must be an array',
          dsMain: 'Main DB',
          statusActive: 'Active',
          statusDisabled: 'Disabled',
          actionRun: 'Run',
          actionEdit: 'Edit',
          actionRemove: 'Delete',
          actionValidate: 'Validate SQL',
          validateOk: 'SQL passed SqlGuard',
          confirmDelete: 'Delete template "{name}"? Run logs are kept.',
          deleteOk: 'Template deleted'
        },
        run: {
          back: 'Back',
          params: 'Parameters',
          noParams: 'No declared parameters',
          run: 'Run',
          exportCsv: 'Export CSV',
          exportXlsx: 'Export xlsx',
          runOk: 'Done: {rows} rows in {ms} ms',
          truncated: 'Truncated to {rows} rows (rowLimit hit)',
          truncatedTag: 'Truncated',
          resultMeta: 'Result: {rows} rows in {ms} ms'
        },
        dashboard: {
          add: 'New Dashboard',
          actionView: 'View',
          actionEdit: 'Edit',
          actionRemove: 'Delete',
          colCode: 'Code',
          colName: 'Name',
          colCategory: 'Category',
          codePh: 'lower_snake_case',
          confirmDelete: 'Delete dashboard "{name}"?',
          addCard: 'Add Card',
          untitledCard: 'Untitled Card',
          noActiveTemplate: 'Create at least one active template first',
          cardTitle: 'Card Title',
          cardTemplate: 'Template',
          cardChart: 'Chart Type',
          cardLayout: 'Width / Height',
          cardConfig: 'Chart Config',
          cardConfigPh: 'optional JSON: {"x":"col","y":"col"}',
          cardParam: 'Default Params',
          cardParamPh: 'optional JSON: {"minId":1}',
          notLoaded: 'Not loaded; click Refresh',
          refresh: 'Refresh',
          refreshAll: 'Refresh All',
          confirmRefreshAll: 'Run all cards in parallel?',
          refreshOk: 'All refreshed'
        },
        datasource: {
          add: 'New Data Source',
          edit: 'Edit Data Source',
          test: 'Test Connection',
          testOk: 'Connected',
          colCode: 'Code',
          colName: 'Name',
          colType: 'Type',
          colJdbcUrl: 'JDBC URL',
          colDriverClass: 'Driver Class',
          colUsername: 'Username',
          colPassword: 'Password',
          jdbcUrlPh: 'e.g. jdbc:mysql://host:3306/db?...',
          driverClassPh: 'empty = use type default',
          passwordPh: 'plaintext (encrypted by AES on save)',
          passwordEditPh: 'empty = keep; non-empty = re-encrypt',
          clearPassword: 'Clear password',
          confirmDelete: 'Delete data source "{name}"? Reports using it will fail.'
        },
        runlog: {
          colTemplateId: 'Template ID',
          colTemplateCode: 'Template Code',
          colDataSource: 'Data Source',
          colRowCount: 'Rows',
          colCostMs: 'Cost (ms)',
          colStatus: 'Status',
          colError: 'Error',
          colSqlPreview: 'SQL Preview',
          statusOk: 'OK',
          statusFail: 'Failed',
          statusTimeout: 'Timeout',
          purge: 'Purge 90d+',
          purgeOk: 'Purge triggered'
        }
      }
    }
  }
}

export default reportModule
export * from './api'
export { default as EchartsCard } from './components/EchartsCard.vue'
