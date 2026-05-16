/**
 * 审计 diff 渲染用的字段字典：把 RFC 6902 JSON Pointer（如 `/userName`）翻成显示名（双语）。
 * 按资源类型分组，配合 detail.resourceType 选取使用。
 *
 * 设计取舍：
 * - 字典只覆盖 **受 @AuditLog 保护的写操作** 会生成 diff 的字段；不为只读列表字段加翻译。
 * - 字段名用 「业务术语」 而非 「数据库列名」（如 phonenumber → 手机号 而非 电话号码）。
 * - 枚举值翻译尽量与 element-plus 标签习惯一致（启用 / 停用，0 / 1 等）。
 * - 字典找不到 path 时 viewer 会 fallback 到原始 JSON Pointer，不会爆掉。
 *
 * **i18n 策略（保养向 P5 补齐）**：
 * - label 与 formatter 输出全部走 vue-i18n key（`audit.*` 全局命名空间）。
 * - 此模块**不直接依赖** vue-i18n 实例 —— viewer 调用时把 `t` 函数传进来，避免在 .ts 里 useI18n（`useI18n` 必须在 setup 中调用）。
 * - 这样脚本逻辑可单测、纯前端 i18n 切换可零成本生效。
 */

export type Translator = (key: string, named?: Record<string, unknown>) => string

export interface FieldEntry {
  /** i18n key（指向显示名）。 */
  labelKey: string
  /** 可选：把后端实际值翻译成显示串（如 status=0 → "启用"）；返回 undefined 走 fallback。 */
  format?: (raw: unknown, t: Translator) => string | undefined
}

export interface ResourceFieldDict {
  /** 资源类型显示名 i18n key（如 「用户」 / 「Article」），列表页表头用。 */
  labelKey: string
  /** path 头段（不带前导 /） → FieldEntry */
  fields: Record<string, FieldEntry>
}

/** 通用 status 翻译（0 启用 / 1 停用） */
const statusActiveDisable: FieldEntry['format'] = (v, t) => {
  if (v === '0' || v === 0) return t('audit.enum.statusEnable')
  if (v === '1' || v === 1) return t('audit.enum.statusDisable')
  return undefined
}

/** 通用 delFlag 翻译（0 正常 / 2 软删） */
const delFlagFormat: FieldEntry['format'] = (v, t) => {
  if (v === '0' || v === 0) return t('audit.enum.delFlagNormal')
  if (v === '2' || v === 2) return t('audit.enum.delFlagDeleted')
  return undefined
}

/** 通用布尔翻译 */
const boolFormat: FieldEntry['format'] = (v, t) => {
  if (v === true || v === 'true' || v === 1 || v === '1') return t('audit.enum.boolYes')
  if (v === false || v === 'false' || v === 0 || v === '0') return t('audit.enum.boolNo')
  return undefined
}

/** dataScope 五种数据范围（与后端 SysRole.DATA_SCOPE_* 常量一致）*/
const dataScopeFormat: FieldEntry['format'] = (v, t) => {
  const k = String(v)
  if (k === '1') return t('audit.enum.dataScope1')
  if (k === '2') return t('audit.enum.dataScope2')
  if (k === '3') return t('audit.enum.dataScope3')
  if (k === '4') return t('audit.enum.dataScope4')
  if (k === '5') return t('audit.enum.dataScope5')
  return undefined
}

/** 性别（0 男 / 1 女 / 2 未知）*/
const sexFormat: FieldEntry['format'] = (v, t) => {
  const k = String(v)
  if (k === '0') return t('audit.enum.sex0')
  if (k === '1') return t('audit.enum.sex1')
  if (k === '2') return t('audit.enum.sex2')
  return undefined
}

/** 文章状态（DRAFT / PENDING / PUBLISHED / UNPUBLISHED） */
const articleStatusFormat: FieldEntry['format'] = (v, t) => {
  const k = String(v)
  if (k === 'DRAFT') return t('audit.enum.articleStatusDraft')
  if (k === 'PENDING') return t('audit.enum.articleStatusPending')
  if (k === 'PUBLISHED') return t('audit.enum.articleStatusPublished')
  if (k === 'UNPUBLISHED') return t('audit.enum.articleStatusUnpublished')
  return undefined
}

const userDict: ResourceFieldDict = {
  labelKey: 'audit.user.label',
  fields: {
    userId: { labelKey: 'audit.user.userId' },
    deptId: { labelKey: 'audit.user.deptId' },
    userName: { labelKey: 'audit.user.userName' },
    nickName: { labelKey: 'audit.user.nickName' },
    email: { labelKey: 'audit.user.email' },
    phonenumber: { labelKey: 'audit.user.phonenumber' },
    sex: { labelKey: 'audit.user.sex', format: sexFormat },
    avatar: { labelKey: 'audit.user.avatar' },
    status: { labelKey: 'audit.user.status', format: statusActiveDisable },
    delFlag: { labelKey: 'audit.user.delFlag', format: delFlagFormat },
    loginIp: { labelKey: 'audit.user.loginIp' },
    loginDate: { labelKey: 'audit.user.loginDate' },
    pwdUpdateDate: { labelKey: 'audit.user.pwdUpdateDate' },
    remark: { labelKey: 'audit.user.remark' },
    roleIds: { labelKey: 'audit.user.roleIds' },
    postIds: { labelKey: 'audit.user.postIds' },
    password: { labelKey: 'audit.user.password' }
  }
}

const roleDict: ResourceFieldDict = {
  labelKey: 'audit.role.label',
  fields: {
    roleId: { labelKey: 'audit.role.roleId' },
    roleName: { labelKey: 'audit.role.roleName' },
    roleKey: { labelKey: 'audit.role.roleKey' },
    roleSort: { labelKey: 'audit.role.roleSort' },
    dataScope: { labelKey: 'audit.role.dataScope', format: dataScopeFormat },
    menuCheckStrictly: { labelKey: 'audit.role.menuCheckStrictly', format: boolFormat },
    deptCheckStrictly: { labelKey: 'audit.role.deptCheckStrictly', format: boolFormat },
    status: { labelKey: 'audit.role.status', format: statusActiveDisable },
    delFlag: { labelKey: 'audit.role.delFlag', format: delFlagFormat },
    remark: { labelKey: 'audit.role.remark' },
    menuIds: { labelKey: 'audit.role.menuIds' },
    deptIds: { labelKey: 'audit.role.deptIds' }
  }
}

const articleDict: ResourceFieldDict = {
  labelKey: 'audit.article.label',
  fields: {
    id: { labelKey: 'audit.article.id' },
    channelId: { labelKey: 'audit.article.channelId' },
    title: { labelKey: 'audit.article.title' },
    slug: { labelKey: 'audit.article.slug' },
    summary: { labelKey: 'audit.article.summary' },
    coverUrl: { labelKey: 'audit.article.coverUrl' },
    contentHtml: { labelKey: 'audit.article.contentHtml' },
    source: { labelKey: 'audit.article.source' },
    author: { labelKey: 'audit.article.author' },
    status: { labelKey: 'audit.article.status', format: articleStatusFormat },
    metaTitle: { labelKey: 'audit.article.metaTitle' },
    metaDescription: { labelKey: 'audit.article.metaDescription' },
    metaKeywords: { labelKey: 'audit.article.metaKeywords' },
    canonicalUrl: { labelKey: 'audit.article.canonicalUrl' },
    publishedAt: { labelKey: 'audit.article.publishedAt' },
    viewCount: { labelKey: 'audit.article.viewCount' },
    sortOrder: { labelKey: 'audit.article.sortOrder' },
    delFlag: { labelKey: 'audit.article.delFlag', format: delFlagFormat },
    processInstanceId: { labelKey: 'audit.article.processInstanceId' },
    tagIds: { labelKey: 'audit.article.tagIds' }
  }
}

const channelDict: ResourceFieldDict = {
  labelKey: 'audit.channel.label',
  fields: {
    id: { labelKey: 'audit.channel.id' },
    parentId: { labelKey: 'audit.channel.parentId' },
    code: { labelKey: 'audit.channel.code' },
    name: { labelKey: 'audit.channel.name' },
    orderNum: { labelKey: 'audit.channel.orderNum' },
    status: { labelKey: 'audit.channel.status', format: statusActiveDisable },
    keywords: { labelKey: 'audit.channel.keywords' },
    description: { labelKey: 'audit.channel.description' },
    template: { labelKey: 'audit.channel.template' },
    delFlag: { labelKey: 'audit.channel.delFlag', format: delFlagFormat }
  }
}

const tagDict: ResourceFieldDict = {
  labelKey: 'audit.tag.label',
  fields: {
    id: { labelKey: 'audit.tag.id' },
    name: { labelKey: 'audit.tag.name' },
    color: { labelKey: 'audit.tag.color' }
  }
}

const formSchemaDict: ResourceFieldDict = {
  labelKey: 'audit.formSchema.label',
  fields: {
    id: { labelKey: 'audit.formSchema.id' },
    formKey: { labelKey: 'audit.formSchema.formKey' },
    name: { labelKey: 'audit.formSchema.name' },
    schemaJson: { labelKey: 'audit.formSchema.schemaJson' },
    version: { labelKey: 'audit.formSchema.version' },
    enabled: { labelKey: 'audit.formSchema.enabled', format: boolFormat },
    remark: { labelKey: 'audit.formSchema.remark' }
  }
}

const taskDict: ResourceFieldDict = {
  labelKey: 'audit.task.label',
  fields: {
    taskId: { labelKey: 'audit.task.taskId' },
    processInstanceId: { labelKey: 'audit.task.processInstanceId' },
    assignee: { labelKey: 'audit.task.assignee' },
    candidateUsers: { labelKey: 'audit.task.candidateUsers' },
    variables: { labelKey: 'audit.task.variables' },
    comment: { labelKey: 'audit.task.comment' }
  }
}

/** 默认字典：所有 resourceType 都能 fallback 用（含 BaseEntity 通用字段）*/
const baseDict: Record<string, FieldEntry> = {
  createBy: { labelKey: 'audit.base.createBy' },
  createTime: { labelKey: 'audit.base.createTime' },
  updateBy: { labelKey: 'audit.base.updateBy' },
  updateTime: { labelKey: 'audit.base.updateTime' },
  remark: { labelKey: 'audit.base.remark' }
}

const RESOURCE_DICT: Record<string, ResourceFieldDict> = {
  user: userDict,
  role: roleDict,
  article: articleDict,
  channel: channelDict,
  tag: tagDict,
  formSchema: formSchemaDict,
  task: taskDict
}

/**
 * 把 JSON Pointer（如 `/userName` 或 `/tags/0/name`）翻译成显示名。
 *
 * - resourceType 找不到 → 走 baseDict + path 自身。
 * - 嵌套路径只翻第一段（其他段追加在后面），如 `/tags/0/name` → 「标签名 → tags/0/name」。
 *   这样既能让用户看懂主字段，又保留嵌套位置。
 * - i18n：调用方需要传入 vue-i18n 的 `t` 函数。
 */
export function translatePath(
  resourceType: string | undefined,
  path: string,
  t: Translator
): string {
  if (!path || path === '/') return path || '/'
  const segments = path.replace(/^\//, '').split('/')
  const head = segments[0]
  const tail = segments.slice(1).join('/')
  const dict = resourceType ? RESOURCE_DICT[resourceType] : undefined
  const entry = dict?.fields[head] ?? baseDict[head]
  const label = entry ? t(entry.labelKey) : head
  return tail ? `${label} → ${tail}` : label
}

/**
 * 把字段值翻译成显示串（如 status=0 → "启用" / "Enabled"）。
 * 找不到 formatter 时返回 undefined（让 viewer 显示原值）。
 */
export function translateValue(
  resourceType: string | undefined,
  path: string,
  value: unknown,
  t: Translator
): string | undefined {
  if (value === null || value === undefined) return undefined
  const head = path.replace(/^\//, '').split('/')[0]
  const dict = resourceType ? RESOURCE_DICT[resourceType] : undefined
  const entry = dict?.fields[head]
  return entry?.format?.(value, t)
}

/**
 * 翻译资源类型名（如 user → 「用户」 / "User"）。
 */
export function translateResourceLabel(resourceType: string | undefined, t: Translator): string {
  if (!resourceType) return ''
  const dict = RESOURCE_DICT[resourceType]
  return dict ? t(dict.labelKey) : resourceType
}
