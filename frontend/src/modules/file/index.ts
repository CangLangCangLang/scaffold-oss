/**
 * file 模块（前端）：文件中心 M-6。
 *
 * <p>组成：
 * <ul>
 *   <li>FileList：列表 + 文件夹树 + 上传 + 编辑 + 软删 + 引用查看</li>
 *   <li>ShareList：当前用户创建的分享链接管理</li>
 *   <li>FilePicker：复用组件 — 给 CMS / form 等模块在表单 / 富文本里选 / 上传文件用</li>
 *   <li>FilePreview / ShareDialog：FileList 用的两个对话框组件，re-export 后任意模块可用</li>
 * </ul>
 */
import type { RouteRecordRaw } from 'vue-router'
import type { ScaffoldFrontendModule } from '../loader'

const routes: RouteRecordRaw[] = [
  {
    path: 'file/mine',
    name: 'FileListMine',
    component: () => import('./views/FileList.vue'),
    meta: { title: 'file.menu.mine' }
  },
  {
    path: 'file/all',
    name: 'FileListAll',
    component: () => import('./views/FileList.vue'),
    meta: { title: 'file.menu.all' }
  },
  {
    path: 'file/share',
    name: 'FileShareList',
    component: () => import('./views/ShareList.vue'),
    meta: { title: 'file.menu.share' }
  }
]

const fileModule: ScaffoldFrontendModule = {
  name: 'file',
  routes,
  locales: {
    'zh-CN': {
      file: {
        menu: {
          root: '文件中心',
          mine: '我的文件',
          all: '全部文件',
          share: '分享管理'
        },
        folder: {
          title: '文件夹',
          create: '新建',
          namePh: '文件夹名',
          allFiles: '所有文件',
          createOk: '已创建',
          deleteOk: '已删除',
          confirmDelete: '确定删除文件夹 "{name}"？子级会一起标记软删'
        },
        list: {
          search: {
            name: '文件名',
            ext: '扩展名',
            bucket: '业务桶',
            category: '分类',
            delFlag: '回收站',
            delFlag0: '正常',
            delFlag2: '已软删'
          },
          colName: '文件名',
          colExt: '类型',
          colSize: '大小',
          colBucket: '桶',
          colRef: '被引用',
          colCreator: '上传人',
          colCreateTime: '上传时间',
          colTags: '标签',
          colCategory: '分类',
          colRemark: '备注',
          colAction: '操作',
          upload: '上传',
          uploadOk: '上传成功',
          editTitle: '编辑文件元信息',
          editFolder: '所属文件夹',
          editFolderPh: '不限',
          editOk: '已保存',
          tagsPh: '逗号分隔',
          batchDelete: '批量删除（{count}）',
          confirmSoft: '确定软删 "{name}"？30 天后定时任务清磁盘',
          confirmBatch: '确定批量软删 {count} 个文件？',
          deleteOk: '已删除',
          refBlock: '此文件被 {count} 处引用，请先在引用方解除',
          actionDownload: '下载',
          actionShare: '分享',
          actionEdit: '编辑',
          actionRefs: '引用',
          actionRemove: '删除',
          cancel: '取消',
          save: '保存'
        },
        share: {
          title: '分享 "{name}"',
          expireDays: '过期天数',
          expireTip: '0 表示永久；最多 365 天',
          oneTime: '一次性',
          oneTimeTip: '开启后链接被首次访问后自动失效',
          password: '访问密码',
          passwordPh: '可选；建议 6-20 位',
          submit: '生成链接',
          close: '关闭',
          copy: '复制',
          copied: '已复制到剪贴板',
          createOk: '分享已创建',
          createdAt: '创建于 {time}',
          passwordReminder: '已设访问密码，请单独告知接收方'
        },
        shareList: {
          colFile: '文件 ID',
          colToken: 'Token',
          colStatus: '状态',
          colExpire: '过期时间',
          colOneTime: '一次性',
          colVisits: '访问次数',
          colCreate: '创建时间',
          colAction: '操作',
          statusActive: '有效',
          statusDisabled: '已停用',
          statusConsumed: '已用尽',
          never: '永久',
          yes: '是',
          no: '否',
          copy: '复制链接',
          disable: '停用',
          disableOk: '已停用',
          remove: '删除',
          deleteOk: '已删除',
          confirmDelete: '确定删除该分享？删除后不可恢复'
        },
        refs: {
          title: '"{name}" 的跨模块引用',
          none: '暂无引用',
          module: '模块',
          type: '类型',
          id: '业务 ID'
        },
        preview: {
          unsupported: '当前 mime "{mime}" 暂不支持内嵌预览',
          downloadInstead: '点这里下载查看'
        },
        picker: {
          pick: '选择文件',
          upload: '直接上传',
          drawerTitle: '选择 / 上传文件',
          choose: '选择',
          picked: '已选 "{name}"',
          uploaded: '上传成功',
          unknown: '未命名文件',
          clear: '清空'
        }
      }
    },
    'en-US': {
      file: {
        menu: {
          root: 'File Center',
          mine: 'My Files',
          all: 'All Files',
          share: 'Shared Links'
        },
        folder: {
          title: 'Folders',
          create: 'Add',
          namePh: 'Folder name',
          allFiles: 'All Files',
          createOk: 'Created',
          deleteOk: 'Deleted',
          confirmDelete: 'Delete folder "{name}"? Children will be soft-deleted too.'
        },
        list: {
          search: {
            name: 'Filename',
            ext: 'Ext',
            bucket: 'Bucket',
            category: 'Category',
            delFlag: 'View',
            delFlag0: 'Active',
            delFlag2: 'Recycle Bin'
          },
          colName: 'Filename',
          colExt: 'Type',
          colSize: 'Size',
          colBucket: 'Bucket',
          colRef: 'Refs',
          colCreator: 'Uploader',
          colCreateTime: 'Uploaded',
          colTags: 'Tags',
          colCategory: 'Category',
          colRemark: 'Remark',
          colAction: 'Actions',
          upload: 'Upload',
          uploadOk: 'Uploaded',
          editTitle: 'Edit File Metadata',
          editFolder: 'Folder',
          editFolderPh: 'Any',
          editOk: 'Saved',
          tagsPh: 'comma separated',
          batchDelete: 'Batch Delete ({count})',
          confirmSoft: 'Soft delete "{name}"? Disk purge runs after 30d.',
          confirmBatch: 'Soft delete {count} files?',
          deleteOk: 'Deleted',
          refBlock: 'Referenced {count} time(s); detach in source first',
          actionDownload: 'Download',
          actionShare: 'Share',
          actionEdit: 'Edit',
          actionRefs: 'Refs',
          actionRemove: 'Delete',
          cancel: 'Cancel',
          save: 'Save'
        },
        share: {
          title: 'Share "{name}"',
          expireDays: 'Expires (days)',
          expireTip: '0 = never; up to 365',
          oneTime: 'One-time',
          oneTimeTip: 'Link is consumed on first access',
          password: 'Password',
          passwordPh: 'Optional; 6-20 chars',
          submit: 'Generate',
          close: 'Close',
          copy: 'Copy',
          copied: 'Copied to clipboard',
          createOk: 'Share created',
          createdAt: 'Created at {time}',
          passwordReminder: 'Password is set; share it separately with the recipient.'
        },
        shareList: {
          colFile: 'File ID',
          colToken: 'Token',
          colStatus: 'Status',
          colExpire: 'Expires',
          colOneTime: 'One-time',
          colVisits: 'Visits',
          colCreate: 'Created',
          colAction: 'Actions',
          statusActive: 'Active',
          statusDisabled: 'Disabled',
          statusConsumed: 'Consumed',
          never: 'Never',
          yes: 'Yes',
          no: 'No',
          copy: 'Copy URL',
          disable: 'Disable',
          disableOk: 'Disabled',
          remove: 'Delete',
          deleteOk: 'Deleted',
          confirmDelete: 'Delete this share? This cannot be undone.'
        },
        refs: {
          title: 'Refs of "{name}"',
          none: 'No references',
          module: 'Module',
          type: 'Type',
          id: 'Business ID'
        },
        preview: {
          unsupported: 'Inline preview not supported for mime "{mime}"',
          downloadInstead: 'Download to view'
        },
        picker: {
          pick: 'Pick',
          upload: 'Upload',
          drawerTitle: 'Pick or Upload',
          choose: 'Pick',
          picked: 'Picked "{name}"',
          uploaded: 'Uploaded',
          unknown: 'unnamed',
          clear: 'Clear'
        }
      }
    }
  }
}

export default fileModule

/* re-export 给其它模块复用 */
export { default as FilePicker } from './components/FilePicker.vue'
export { default as FilePreview } from './components/FilePreview.vue'
export { default as ShareDialog } from './components/ShareDialog.vue'
export * from './api'
