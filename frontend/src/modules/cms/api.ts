import request from '@/utils/request'
import type { ApiResult } from '@/types/api'

/**
 * CMS 文章状态枚举（与后端 {@code Article.STATUS_*} 常量一一对应）。
 */
export type ArticleStatus = 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'UNPUBLISHED'

export interface CmsTag {
  id: number
  name: string
  color?: string
  createTime?: string
  createBy?: string
}

export interface CmsChannel {
  id: number
  parentId: number
  code: string
  name: string
  orderNum: number
  /** '0'=启用 '1'=停用 */
  status: string
  keywords?: string
  description?: string
  template?: string
  delFlag?: string
  createTime?: string
  updateTime?: string
}

export interface CmsChannelTreeNode extends CmsChannel {
  children?: CmsChannelTreeNode[]
}

export interface CmsArticleSummary {
  id: number
  channelId: number
  title: string
  slug: string
  summary?: string
  coverUrl?: string
  source?: string
  author?: string
  status: ArticleStatus
  metaTitle?: string
  metaDescription?: string
  metaKeywords?: string
  canonicalUrl?: string
  publishedAt?: string
  viewCount?: number
  sortOrder?: number
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
  /** 列表回填用，编辑保存时也会带回。 */
  tagIds?: number[]
  /** 列表回填用：标签详情对象，避免再请求一次 tag 字典。 */
  tags?: CmsTag[]
  /**
   * M-4 cms-workflow 桥模块启用且文章走过 workflow 时存在的流程实例 id。
   * 前端用来：1) 显示「查看审批进度」按钮 2) 切「通过/驳回」按钮文案
   * 桥模块未启用时永远是 undefined / null。
   */
  processInstanceId?: string | null
}

export interface CmsArticleDetail extends CmsArticleSummary {
  /** 富文本 HTML（仅详情接口返回；列表为节省网络不返回）。 */
  contentHtml?: string
}

export interface CmsArticleQuery {
  channelId?: number
  status?: ArticleStatus
  keyword?: string
  tagId?: number
  createBy?: string
  startTime?: string
  endTime?: string
  pageNum?: number
  pageSize?: number
}

export interface CmsArticleSaveRequest {
  /** 编辑时填，新建留空。 */
  id?: number
  channelId: number
  title: string
  /** 留空时由后端按 title 生成。 */
  slug?: string
  summary?: string
  coverUrl?: string
  contentHtml?: string
  source?: string
  author?: string
  metaTitle?: string
  metaDescription?: string
  metaKeywords?: string
  canonicalUrl?: string
  sortOrder?: number
  tagIds?: number[]
}

export interface CmsArticlePage {
  rows: CmsArticleSummary[]
  total: number
  code?: number
  msg?: string
}

/* ===== 栏目 ===== */

export const listChannels = () =>
  request.get<ApiResult<CmsChannel[]>, ApiResult<CmsChannel[]>>('/cms/channel/list')

export const getChannelTree = (activeOnly = false) =>
  request.get<ApiResult<CmsChannelTreeNode[]>, ApiResult<CmsChannelTreeNode[]>>(
    '/cms/channel/tree',
    { params: { activeOnly } }
  )

export const getChannel = (id: number) =>
  request.get<ApiResult<CmsChannel>, ApiResult<CmsChannel>>(`/cms/channel/${id}`)

export const createChannel = (data: Partial<CmsChannel>) =>
  request.post<ApiResult<CmsChannel>, ApiResult<CmsChannel>>('/cms/channel', data)

export const updateChannel = (data: Partial<CmsChannel> & { id: number }) =>
  request.put<ApiResult<CmsChannel>, ApiResult<CmsChannel>>('/cms/channel', data)

export const deleteChannel = (id: number) =>
  request.delete<ApiResult, ApiResult>(`/cms/channel/${id}`)

/* ===== 标签 ===== */

export const listTags = (name?: string) =>
  request.get<ApiResult<CmsTag[]>, ApiResult<CmsTag[]>>('/cms/tag/list', {
    params: name ? { name } : {}
  })

export const createTag = (data: Partial<CmsTag>) =>
  request.post<ApiResult<CmsTag>, ApiResult<CmsTag>>('/cms/tag', data)

export const updateTag = (data: Partial<CmsTag> & { id: number }) =>
  request.put<ApiResult<CmsTag>, ApiResult<CmsTag>>('/cms/tag', data)

export const deleteTag = (id: number) =>
  request.delete<ApiResult, ApiResult>(`/cms/tag/${id}`)

/* ===== 文章 ===== */

export const listArticles = (params: CmsArticleQuery = {}) =>
  request.get<CmsArticlePage, CmsArticlePage>('/cms/article/list', { params })

export const getArticle = (id: number) =>
  request.get<ApiResult<CmsArticleDetail>, ApiResult<CmsArticleDetail>>(`/cms/article/${id}`)

export const saveArticle = (data: CmsArticleSaveRequest) =>
  data.id
    ? request.put<ApiResult<CmsArticleDetail>, ApiResult<CmsArticleDetail>>('/cms/article', data)
    : request.post<ApiResult<CmsArticleDetail>, ApiResult<CmsArticleDetail>>('/cms/article', data)

export const deleteArticle = (id: number) =>
  request.delete<ApiResult, ApiResult>(`/cms/article/${id}`)

/* ===== 状态机：6 个流转端点 ===== */

export type ArticleAction =
  | 'submit'
  | 'approve'
  | 'reject'
  | 'publish'
  | 'unpublish'
  | 'back-to-draft'

export const transitArticle = (id: number, action: ArticleAction, reason?: string) =>
  request.post<ApiResult<CmsArticleDetail>, ApiResult<CmsArticleDetail>>(
    `/cms/article/${id}/${action}`,
    reason ? { reason } : {}
  )

/* ===== 公开门户（匿名） ===== */

export const listPublicChannels = () =>
  request.get<ApiResult<CmsChannelTreeNode[]>, ApiResult<CmsChannelTreeNode[]>>(
    '/cms/public/channels',
    { isToken: false } as never
  )

export const listPublicArticles = (params: CmsArticleQuery = {}) =>
  request.get<CmsArticlePage, CmsArticlePage>('/cms/public/articles', {
    params,
    isToken: false
  } as never)

export const getPublicArticleBySlug = (slug: string) =>
  request.get<ApiResult<CmsArticleDetail>, ApiResult<CmsArticleDetail>>(
    `/cms/public/articles/${encodeURIComponent(slug)}`,
    { isToken: false } as never
  )
