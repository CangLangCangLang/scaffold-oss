import type { RouteRecordRaw } from 'vue-router'
import type { ScaffoldFrontendModule } from '../loader'

const routes: RouteRecordRaw[] = [
  {
    path: 'cms/channel',
    name: 'CmsChannelList',
    component: () => import('./views/ChannelList.vue'),
    meta: { title: '栏目管理' }
  },
  {
    path: 'cms/article',
    name: 'CmsArticleList',
    component: () => import('./views/ArticleList.vue'),
    meta: { title: '文章管理' }
  },
  {
    path: 'cms/article-edit/:id?',
    name: 'CmsArticleEdit',
    component: () => import('./views/ArticleEdit.vue'),
    meta: { title: '文章编辑', activeMenu: '/cms/article' }
  },
  {
    path: 'cms/tag',
    name: 'CmsTagList',
    component: () => import('./views/TagList.vue'),
    meta: { title: '标签管理' }
  }
]

const cmsModule: ScaffoldFrontendModule = {
  name: 'cms',
  routes,
  locales: {
    'zh-CN': {
      cms: {
        menu: {
          title: 'CMS',
          channel: '栏目管理',
          article: '文章管理',
          articleEdit: '文章编辑',
          articleCreate: '新建文章',
          tag: '标签管理'
        },
        articleCreate: '新建文章',
        status: {
          DRAFT: '草稿',
          PENDING: '待审核',
          PUBLISHED: '已发布',
          UNPUBLISHED: '已下线'
        },
        action: {
          submit: '提交审核',
          submitWf: '提交审批',
          approve: '审核通过',
          approveWf: '审批通过',
          reject: '驳回',
          rejectWf: '审批驳回',
          unpublish: '下线',
          publish: '重新上线',
          backToDraft: '退回草稿',
          progress: '查看审批进度',
          flow: '流转'
        },
        confirm: {
          submit: '提交审核后将不能直接编辑，确定吗？',
          submitWf: '提交后将进入审批流程，确定吗？',
          approve: '确认审核通过并发布？',
          approveWf: '确认审批通过并发布？',
          unpublish: '下线后用户将无法访问，确定吗？',
          publish: '重新上线后立即对外可访问，确定吗？',
          backToDraft: '退回草稿后内容将下线，可继续编辑。',
          deleteArticle: '确定删除文章 [{title}] ？',
          deleteChannel: '确定删除栏目 [{name}] 吗？子栏目 / 文章不为空时无法删除。',
          deleteTag: '确定删除标签 [{name}] ？同时会删除所有引用该标签的文章关联。'
        },
        reason: {
          title: '原因',
          prompt: '请填写原因（可选，将记录到审计日志）'
        },
        article: {
          title: '标题',
          slug: 'URL Slug',
          slugHint: '留空时按标题生成',
          summary: '摘要',
          content: '正文',
          channel: '栏目',
          channelAll: '全部栏目',
          channelPick: '选择栏目',
          tagsLabel: '标签',
          tagsAll: '全部标签',
          tagsPick: '选择标签',
          statusAll: '全部状态',
          coverUrl: '封面图 URL',
          coverHint: '贴图片 URL；或在正文里上传后复制 src',
          author: '作者',
          source: '来源',
          searchPlaceholder: '按标题/摘要搜索',
          inWorkflow: '走 workflow',
          inWorkflowTitle: '流程实例: {id}',
          viewCount: '阅读',
          deleteSoftTitle: '软删除',
          submitting: '保存中…',
          articleNoFlow: '该文章没有关联的审批流程实例',
          progressTitle: '文章 #{id} 审批',
          tipTitleRequired: '请输入标题',
          tipChannelRequired: '请选择栏目',
          tipNameRequired: '请输入标签名称',
          tipChannelCodeRequired: '请输入栏目编码',
          tipChannelNameRequired: '请输入栏目名称'
        },
        channel: {
          name: '栏目名称',
          code: '编码',
          codeHint: '唯一，建议英文短串，公开 API URL 用',
          parent: '父栏目',
          parentRoot: '（根栏目）',
          parentPick: '选择父栏目',
          dialogCreate: '新建栏目',
          dialogEdit: '编辑栏目',
          createRoot: '新建根栏目',
          addChild: '添加子栏目',
          seoKeywords: 'SEO 关键词',
          seoKeywordsHint: '逗号分隔',
          seoDescription: 'SEO 描述',
          template: '门户模板',
          templateHint: '保留字段'
        },
        tag: {
          searchPlaceholder: '按名称搜索',
          createBtn: '新建标签',
          dialogCreate: '新建标签',
          dialogEdit: '编辑标签',
          color: '颜色'
        },
        edit: {
          newTitle: '新建文章',
          editTitle: '编辑文章',
          seo: 'SEO'
        },
        editor: {
          placeholder: '在这里输入正文...（拖拽 / 粘贴图片会自动上传到 /cms/upload/image）',
          uploadNoUrl: '后端没有返回 url',
          uploadFailed: '图片上传失败: {msg}'
        }
      }
    },
    'en-US': {
      cms: {
        menu: {
          title: 'CMS',
          channel: 'Channels',
          article: 'Articles',
          articleEdit: 'Edit',
          articleCreate: 'New article',
          tag: 'Tags'
        },
        articleCreate: 'New article',
        status: {
          DRAFT: 'Draft',
          PENDING: 'Pending',
          PUBLISHED: 'Published',
          UNPUBLISHED: 'Unpublished'
        },
        action: {
          submit: 'Submit for review',
          submitWf: 'Submit for approval',
          approve: 'Approve & publish',
          approveWf: 'Approve & publish',
          reject: 'Reject',
          rejectWf: 'Reject',
          unpublish: 'Unpublish',
          publish: 'Republish',
          backToDraft: 'Back to draft',
          progress: 'View approval progress',
          flow: 'Transition'
        },
        confirm: {
          submit: 'After submission you will no longer be able to edit directly. Continue?',
          submitWf: 'Submitting will start the approval workflow. Continue?',
          approve: 'Approve and publish?',
          approveWf: 'Approve and publish?',
          unpublish: 'Once unpublished, users will not be able to access it. Continue?',
          publish: 'Republishing will make it publicly visible immediately. Continue?',
          backToDraft: 'Sending back to draft will unpublish content, and editing can resume.',
          deleteArticle: 'Are you sure you want to delete article [{title}] ?',
          deleteChannel: 'Are you sure you want to delete channel [{name}] ? Channels with sub-channels or articles cannot be removed.',
          deleteTag: 'Are you sure you want to delete tag [{name}] ? All article-tag associations will also be removed.'
        },
        reason: {
          title: 'Reason',
          prompt: 'Optional reason (will be recorded in audit log)'
        },
        article: {
          title: 'Title',
          slug: 'URL Slug',
          slugHint: 'Auto-generated from title if blank',
          summary: 'Summary',
          content: 'Content',
          channel: 'Channel',
          channelAll: 'All channels',
          channelPick: 'Pick channel',
          tagsLabel: 'Tags',
          tagsAll: 'All tags',
          tagsPick: 'Pick tags',
          statusAll: 'All statuses',
          coverUrl: 'Cover image URL',
          coverHint: 'Paste an image URL or copy src after uploading inside content',
          author: 'Author',
          source: 'Source',
          searchPlaceholder: 'Search by title / summary',
          inWorkflow: 'In workflow',
          inWorkflowTitle: 'Process instance: {id}',
          viewCount: 'Views',
          deleteSoftTitle: 'Soft delete',
          submitting: 'Saving…',
          articleNoFlow: 'This article has no associated workflow instance',
          progressTitle: 'Article #{id} approval',
          tipTitleRequired: 'Please enter a title',
          tipChannelRequired: 'Please pick a channel',
          tipNameRequired: 'Please enter a tag name',
          tipChannelCodeRequired: 'Please enter a channel code',
          tipChannelNameRequired: 'Please enter a channel name'
        },
        channel: {
          name: 'Channel name',
          code: 'Code',
          codeHint: 'Unique, short ASCII string used in public API URLs',
          parent: 'Parent channel',
          parentRoot: '(root)',
          parentPick: 'Pick parent channel',
          dialogCreate: 'New channel',
          dialogEdit: 'Edit channel',
          createRoot: 'New root channel',
          addChild: 'Add child',
          seoKeywords: 'SEO keywords',
          seoKeywordsHint: 'Comma-separated',
          seoDescription: 'SEO description',
          template: 'Portal template',
          templateHint: 'Reserved field'
        },
        tag: {
          searchPlaceholder: 'Search by name',
          createBtn: 'New tag',
          dialogCreate: 'New tag',
          dialogEdit: 'Edit tag',
          color: 'Color'
        },
        edit: {
          newTitle: 'New article',
          editTitle: 'Edit article',
          seo: 'SEO'
        },
        editor: {
          placeholder: 'Enter content here... (drag / paste images will auto-upload to /cms/upload/image)',
          uploadNoUrl: 'Backend returned no url',
          uploadFailed: 'Image upload failed: {msg}'
        }
      }
    }
  }
}

export default cmsModule
