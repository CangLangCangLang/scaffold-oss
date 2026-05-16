/**
 * form 模块（前端）：通用表单引擎 M-10。
 *
 * <p>组成：
 * <ul>
 *   <li>FormRenderer：运行时渲染器（在 P3 五页里 import 用）</li>
 *   <li>6 个高阶 widget：通过 install 钩子注册成全局组件，schema 里 type 直接用</li>
 *   <li>路由：表单模板列表 / 设计器 / 填报 / 提交记录列表 / 详情</li>
 *   <li>i18n：zh-CN / en-US 同步维护</li>
 * </ul>
 */
import type { RouteRecordRaw } from 'vue-router'
import type { ScaffoldFrontendModule } from '../loader'
import FormUserPicker from './widgets/FormUserPicker.vue'
import FormDeptPicker from './widgets/FormDeptPicker.vue'
import FormDictSelect from './widgets/FormDictSelect.vue'
import FormCascaderSelect from './widgets/FormCascaderSelect.vue'
import FormDynamicTable from './widgets/FormDynamicTable.vue'
import FormDetailSubForm from './widgets/FormDetailSubForm.vue'

const routes: RouteRecordRaw[] = [
  {
    path: 'form/template',
    name: 'FormTemplateList',
    component: () => import('./views/TemplateList.vue'),
    meta: { title: 'form.menu.template' }
  },
  {
    path: 'form/template-design/:id?',
    name: 'FormTemplateDesign',
    component: () => import('./views/TemplateDesign.vue'),
    meta: { title: 'form.menu.templateDesign', hidden: true }
  },
  {
    path: 'form/fill/:id',
    name: 'FormFill',
    component: () => import('./views/FormFill.vue'),
    meta: { title: 'form.menu.fill', hidden: true }
  },
  {
    path: 'form/submission',
    name: 'FormSubmissionList',
    component: () => import('./views/SubmissionList.vue'),
    meta: { title: 'form.menu.submission' }
  },
  {
    path: 'form/submission/:id',
    name: 'FormSubmissionDetail',
    component: () => import('./views/SubmissionDetail.vue'),
    meta: { title: 'form.menu.submissionDetail', hidden: true }
  }
]

const formModule: ScaffoldFrontendModule = {
  name: 'form',
  routes,
  locales: {
    'zh-CN': {
      form: {
        menu: {
          root: '表单引擎',
          template: '表单模板',
          templateDesign: '模板设计',
          fill: '填报',
          submission: '提交记录',
          submissionDetail: '提交详情'
        },
        common: {
          required: '必填',
          confirmDelete: '确定删除？删除后不可恢复',
          deleteOk: '已删除',
          publishOk: '已发布',
          archiveOk: '已归档',
          submitOk: '提交成功'
        },
        renderer: {
          errSchemaTitle: '表单 schema 解析失败',
          errSchemaParse: 'schema 解析失败：{msg}',
          errSchemaNotArray: 'schema 必须是 form-create rule 数组'
        },
        template: {
          listTitle: '表单模板',
          colKey: '模板 key',
          colName: '名称',
          colCategory: '分类',
          colVersion: '版本',
          colStatus: '状态',
          colCreate: '创建时间',
          colAction: '操作',
          search: { keyword: '名称', status: '状态', category: '分类' },
          status: { DRAFT: '草稿', PUBLISHED: '已发布', ARCHIVED: '已归档' },
          actionEdit: '编辑',
          actionPublish: '发布',
          actionArchive: '归档',
          actionFill: '填报',
          actionRemove: '删除',
          actionAdd: '新增模板',
          designTitle: '模板设计：{name}',
          designTitleNew: '新增模板',
          designSaveDraft: '保存草稿',
          designPublish: '发布',
          formKey: '模板 key',
          formKeyTip: '模板 key 一旦确定不可修改（与历史提交记录绑定）',
          name: '名称',
          category: '分类',
          description: '描述'
        },
        fill: {
          title: '填报：{name}',
          submit: '提交',
          archived: '当前模板已归档，不接受新提交',
          notFound: '模板不存在或已下线',
          notPublished: '模板尚未发布'
        },
        submission: {
          listTitle: '我的提交记录',
          listTitleAll: '全部提交记录',
          colTpl: '模板',
          colSubmitter: '提交人',
          colVersion: '提交时模板版本',
          colTime: '提交时间',
          colAction: '操作',
          search: { tpl: '模板 key', submitter: '提交人', range: '提交时间' },
          actionView: '查看',
          detailTitle: '提交详情 #{id}',
          backToList: '返回列表'
        },
        widget: {
          userPicker: { placeholder: '搜索人员…' },
          deptPicker: { placeholder: '选择部门' },
          dictSelect: { placeholder: '请选择' },
          cascaderSelect: { placeholder: '请选择' },
          dynamicTable: {
            colAction: '操作',
            empty: '暂无数据，点 添加行 开始',
            addRow: '添加行',
            countTip: '已 {count}/{max} 行',
            maxReached: '已达上限 {max} 行'
          },
          detailSubForm: {
            defaultRowTitle: '第 {index} 项',
            empty: '暂无明细，点 添加 开始',
            addRow: '添加',
            countTip: '已 {count}/{max} 项',
            maxReached: '已达上限 {max} 项'
          }
        }
      }
    },
    'en-US': {
      form: {
        menu: {
          root: 'Forms',
          template: 'Templates',
          templateDesign: 'Template Designer',
          fill: 'Fill',
          submission: 'Submissions',
          submissionDetail: 'Submission Detail'
        },
        common: {
          required: 'required',
          confirmDelete: 'Delete? This cannot be undone',
          deleteOk: 'Deleted',
          publishOk: 'Published',
          archiveOk: 'Archived',
          submitOk: 'Submitted'
        },
        renderer: {
          errSchemaTitle: 'Form schema parse error',
          errSchemaParse: 'Schema parse failed: {msg}',
          errSchemaNotArray: 'Schema must be a form-create rule[] array'
        },
        template: {
          listTitle: 'Form Templates',
          colKey: 'Key',
          colName: 'Name',
          colCategory: 'Category',
          colVersion: 'Version',
          colStatus: 'Status',
          colCreate: 'Created',
          colAction: 'Actions',
          search: { keyword: 'Name', status: 'Status', category: 'Category' },
          status: { DRAFT: 'Draft', PUBLISHED: 'Published', ARCHIVED: 'Archived' },
          actionEdit: 'Edit',
          actionPublish: 'Publish',
          actionArchive: 'Archive',
          actionFill: 'Fill',
          actionRemove: 'Delete',
          actionAdd: 'Add Template',
          designTitle: 'Design: {name}',
          designTitleNew: 'New Template',
          designSaveDraft: 'Save Draft',
          designPublish: 'Publish',
          formKey: 'Template Key',
          formKeyTip: 'Key is immutable once created (bound to historical submissions)',
          name: 'Name',
          category: 'Category',
          description: 'Description'
        },
        fill: {
          title: 'Fill: {name}',
          submit: 'Submit',
          archived: 'This template is archived; no new submissions accepted',
          notFound: 'Template not found or removed',
          notPublished: 'Template not yet published'
        },
        submission: {
          listTitle: 'My Submissions',
          listTitleAll: 'All Submissions',
          colTpl: 'Template',
          colSubmitter: 'Submitter',
          colVersion: 'Template Version',
          colTime: 'Time',
          colAction: 'Actions',
          search: { tpl: 'Template key', submitter: 'Submitter', range: 'Submitted between' },
          actionView: 'View',
          detailTitle: 'Submission #{id}',
          backToList: 'Back to list'
        },
        widget: {
          userPicker: { placeholder: 'Search users…' },
          deptPicker: { placeholder: 'Pick department' },
          dictSelect: { placeholder: 'Select' },
          cascaderSelect: { placeholder: 'Select' },
          dynamicTable: {
            colAction: 'Actions',
            empty: 'No rows; click Add Row to start',
            addRow: 'Add Row',
            countTip: '{count}/{max} rows',
            maxReached: 'Max {max} rows reached'
          },
          detailSubForm: {
            defaultRowTitle: 'Item {index}',
            empty: 'No items; click Add to start',
            addRow: 'Add',
            countTip: '{count}/{max} items',
            maxReached: 'Max {max} items reached'
          }
        }
      }
    }
  },
  install(ctx) {
    // 6 个高阶 widget 注册成全局组件；form-create runtime（@form-create/element-ui，
    // 已由 workflow 模块 install 时注册）解析 schema 时遇到 type 等于这些组件名的字段，
    // 会自动渲染对应 vue 组件。
    ctx.app.component('FormUserPicker', FormUserPicker)
    ctx.app.component('FormDeptPicker', FormDeptPicker)
    ctx.app.component('FormDictSelect', FormDictSelect)
    ctx.app.component('FormCascaderSelect', FormCascaderSelect)
    ctx.app.component('FormDynamicTable', FormDynamicTable)
    ctx.app.component('FormDetailSubForm', FormDetailSubForm)
  }
}

export default formModule

/* re-export 便于其它模块复用（如 cms 富文本里嵌套表单等场景） */
export { default as FormRenderer } from './components/FormRenderer.vue'
export { default as FormUserPicker } from './widgets/FormUserPicker.vue'
export { default as FormDeptPicker } from './widgets/FormDeptPicker.vue'
export { default as FormDictSelect } from './widgets/FormDictSelect.vue'
export { default as FormCascaderSelect } from './widgets/FormCascaderSelect.vue'
export { default as FormDynamicTable } from './widgets/FormDynamicTable.vue'
export { default as FormDetailSubForm } from './widgets/FormDetailSubForm.vue'
