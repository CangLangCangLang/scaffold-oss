import { reactive, ref, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

export interface UseCrudOptions<TQuery, TRecord> {
  /** 默认查询条件 */
  defaultQuery: () => TQuery
  /** 列表查询函数，返回 { rows, total } 或与之兼容的 PageResult */
  fetchList: (query: TQuery) => Promise<{ rows?: TRecord[]; total?: number; data?: TRecord[]; code?: number }>
  /** 单行 ID 字段名，例如 userId / configId / dictCode */
  rowKey?: keyof TRecord & string
  /** 默认表单数据 */
  defaultForm?: () => Partial<TRecord>
  /** 创建函数 */
  create?: (data: TRecord) => Promise<unknown>
  /** 更新函数 */
  update?: (data: TRecord) => Promise<unknown>
  /** 删除函数（接收 id 或 id 数组） */
  remove?: (id: any) => Promise<unknown>
  /** 单条详情查询，编辑前会调用 */
  getOne?: (id: number | string) => Promise<any>
}

export interface UseCrudReturn<TQuery, TRecord> {
  loading: Ref<boolean>
  query: TQuery
  list: Ref<TRecord[]>
  total: Ref<number>
  selected: Ref<TRecord[]>
  dialogVisible: Ref<boolean>
  dialogTitle: Ref<string>
  form: Partial<TRecord>
  resetForm: () => void
  fetchList: () => Promise<void>
  resetQuery: () => void
  handleAdd: () => void
  handleEdit: (row: TRecord) => Promise<void>
  handleDelete: (row?: TRecord) => Promise<void>
  handleSubmit: () => Promise<void>
  handleSelectionChange: (rows: TRecord[]) => void
}

export function useCrud<TQuery extends { pageNum?: number; pageSize?: number }, TRecord extends object>(
  options: UseCrudOptions<TQuery, TRecord>
): UseCrudReturn<TQuery, TRecord> {
  const loading = ref(false)
  const list = ref([]) as Ref<TRecord[]>
  const total = ref(0)
  const selected = ref([]) as Ref<TRecord[]>
  const dialogVisible = ref(false)
  const dialogTitle = ref('')

  const query = reactive(options.defaultQuery()) as TQuery
  const form = reactive(options.defaultForm ? options.defaultForm() : {}) as Partial<TRecord>

  function applyQueryDefaults() {
    const defaults = options.defaultQuery()
    Object.assign(query, defaults)
  }

  function resetForm() {
    const defaults = options.defaultForm ? options.defaultForm() : ({} as Partial<TRecord>)
    Object.keys(form).forEach((key) => {
      delete (form as Record<string, unknown>)[key]
    })
    Object.assign(form, defaults)
  }

  async function doFetch() {
    loading.value = true
    try {
      const res = await options.fetchList(query)
      list.value = (res.rows || res.data || []) as TRecord[]
      total.value = res.total ?? list.value.length
    } finally {
      loading.value = false
    }
  }

  function resetQuery() {
    applyQueryDefaults()
    if ('pageNum' in query) (query as { pageNum?: number }).pageNum = 1
    doFetch()
  }

  function handleAdd() {
    resetForm()
    dialogTitle.value = '新增'
    dialogVisible.value = true
  }

  async function handleEdit(row: TRecord) {
    resetForm()
    dialogTitle.value = '编辑'
    if (options.getOne && options.rowKey) {
      const id = row[options.rowKey] as number | string
      const res = await options.getOne(id)
      Object.assign(form, (res.data ?? res) as Partial<TRecord>)
    } else {
      Object.assign(form, row)
    }
    dialogVisible.value = true
  }

  async function handleDelete(row?: TRecord) {
    if (!options.remove) {
      ElMessage.warning('未配置删除函数')
      return
    }
    let ids: Array<number | string>
    if (row && options.rowKey) {
      ids = [(row as Record<string, unknown>)[options.rowKey] as number | string]
    } else if (selected.value.length && options.rowKey) {
      ids = selected.value.map((r) => (r as Record<string, unknown>)[options.rowKey!] as number | string)
    } else {
      ElMessage.warning('请选择要删除的记录')
      return
    }
    try {
      await ElMessageBox.confirm(`确认删除选中的 ${ids.length} 条记录吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return
    }
    await options.remove(ids.length === 1 ? ids[0] : ids)
    ElMessage.success('删除成功')
    await doFetch()
  }

  async function handleSubmit() {
    const formRec = form as Record<string, unknown>
    if (options.rowKey && formRec[options.rowKey]) {
      if (!options.update) throw new Error('未配置 update')
      await options.update(form as TRecord)
      ElMessage.success('修改成功')
    } else {
      if (!options.create) throw new Error('未配置 create')
      await options.create(form as TRecord)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await doFetch()
  }

  function handleSelectionChange(rows: TRecord[]) {
    selected.value = rows
  }

  return {
    loading,
    query,
    list,
    total,
    selected,
    dialogVisible,
    dialogTitle,
    form,
    resetForm,
    fetchList: doFetch,
    resetQuery,
    handleAdd,
    handleEdit,
    handleDelete,
    handleSubmit,
    handleSelectionChange
  }
}
