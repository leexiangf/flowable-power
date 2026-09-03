<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Lock } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageCard from '@/components/PageCard.vue'
import {
  createMenu,
  deleteMenu,
  fetchAdminMenuTree,
  fetchMenuList,
  updateMenu,
} from '@/api/menu'
import type { MenuDetailVO, MenuSaveRequest } from '@/types/system'

const MENU_TYPE_DIR = 1
const MENU_TYPE_PAGE = 2
const MENU_TYPE_BTN = 3

const menuTypeMap: Record<number, string> = {
  [MENU_TYPE_DIR]: '目录',
  [MENU_TYPE_PAGE]: '菜单',
  [MENU_TYPE_BTN]: '按钮',
}

const loading = ref(false)
const treeMode = ref(true)
const tableData = ref<MenuDetailVO[]>([])
const fullTree = ref<MenuDetailVO[]>([])

const query = reactive({
  keyword: '',
  status: undefined as number | undefined,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const submitting = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<MenuSaveRequest>({
  parentId: '0',
  menuName: '',
  menuType: MENU_TYPE_DIR,
  path: '',
  component: '',
  perms: '',
  icon: '',
  sort: 0,
  visible: 1,
  status: 1,
  remark: '',
})

const rules = computed<FormRules>(() => ({
  menuName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  parentId: [{ required: true, message: '请选择父节点', trigger: 'change' }],
  perms:
    form.menuType === MENU_TYPE_BTN
      ? [{ required: true, message: '按钮必须填写权限码', trigger: 'blur' }]
      : [],
}))

const parentTreeOptions = computed(() => [
  {
    id: '0',
    menuName: '根目录',
    children: filterBuiltInParents(filterParentOptions(fullTree.value, editingId.value)),
  },
])

/** 内置菜单不可作为父节点，其子树中的自定义节点提升到可选列表 */
function filterBuiltInParents(nodes: MenuDetailVO[]): MenuDetailVO[] {
  const result: MenuDetailVO[] = []
  for (const node of nodes) {
    if (isBuiltInMenu(node)) {
      if (node.children?.length) {
        result.push(...filterBuiltInParents(node.children))
      }
      continue
    }
    result.push({
      ...node,
      children: node.children ? filterBuiltInParents(node.children) : [],
    })
  }
  return result
}

function filterParentOptions(nodes: MenuDetailVO[], excludeId: string | null): MenuDetailVO[] {
  if (!excludeId) {
    return nodes
  }
  const blocked = collectDescendantIds(nodes, excludeId)
  blocked.add(excludeId)
  return filterNodes(nodes, blocked)
}

function collectDescendantIds(nodes: MenuDetailVO[], rootId: string): Set<string> {
  const result = new Set<string>()
  const root = findNode(nodes, rootId)
  if (!root) return result
  const walk = (list?: MenuDetailVO[]) => {
    list?.forEach((n) => {
      result.add(n.id)
      walk(n.children)
    })
  }
  walk(root.children)
  return result
}

function findNode(nodes: MenuDetailVO[], id: string): MenuDetailVO | undefined {
  for (const node of nodes) {
    if (node.id === id) return node
    const found = node.children ? findNode(node.children, id) : undefined
    if (found) return found
  }
  return undefined
}

function filterNodes(nodes: MenuDetailVO[], blocked: Set<string>): MenuDetailVO[] {
  return nodes
    .filter((n) => !blocked.has(n.id))
    .map((n) => ({
      ...n,
      children: n.children ? filterNodes(n.children, blocked) : [],
    }))
}

/** 内置菜单（ID ≤ 999）只读展示，权限码与后端接口绑定 */
function isBuiltInMenu(row: MenuDetailVO) {
  return row.builtIn ?? Number(row.id) <= 999
}

async function loadData() {
  loading.value = true
  try {
    if (query.keyword || query.status !== undefined) {
      tableData.value = await fetchMenuList({
        keyword: query.keyword || undefined,
        status: query.status,
      })
      treeMode.value = false
    } else {
      fullTree.value = await fetchAdminMenuTree()
      tableData.value = fullTree.value
      treeMode.value = true
    }
  } finally {
    loading.value = false
  }
}

async function refreshTreeCache() {
  fullTree.value = await fetchAdminMenuTree()
}

function handleSearch() {
  loadData()
}

function resetForm() {
  form.parentId = '0'
  form.menuName = ''
  form.menuType = MENU_TYPE_DIR
  form.path = ''
  form.component = ''
  form.perms = ''
  form.icon = ''
  form.sort = 0
  form.visible = 1
  form.status = 1
  form.remark = ''
}

function openCreate(parent?: MenuDetailVO) {
  editingId.value = null
  dialogTitle.value = parent ? `新增子节点 - ${parent.menuName}` : '新增菜单'
  resetForm()
  if (parent) {
    form.parentId = parent.id
    if (parent.menuType === MENU_TYPE_DIR) {
      form.menuType = MENU_TYPE_PAGE
    } else if (parent.menuType === MENU_TYPE_PAGE) {
      form.menuType = MENU_TYPE_BTN
    }
  }
  dialogVisible.value = true
}

function openEdit(row: MenuDetailVO) {
  editingId.value = row.id
  dialogTitle.value = '编辑菜单'
  form.parentId = row.parentId || '0'
  form.menuName = row.menuName
  form.menuType = row.menuType
  form.path = row.path || ''
  form.component = row.component || ''
  form.perms = row.perms || ''
  form.icon = row.icon || ''
  form.sort = row.sort ?? 0
  form.visible = row.visible ?? 1
  form.status = row.status ?? 1
  form.remark = row.remark || ''
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload: MenuSaveRequest = {
      parentId: form.parentId || '0',
      menuName: form.menuName.trim(),
      menuType: form.menuType,
      path: form.path?.trim() || undefined,
      component: form.component?.trim() || undefined,
      perms: form.perms?.trim() || undefined,
      icon: form.icon?.trim() || undefined,
      sort: form.sort,
      visible: form.visible,
      status: form.status,
      remark: form.remark?.trim() || undefined,
    }
    if (editingId.value) {
      await updateMenu(editingId.value, payload)
      ElMessage.success('菜单已更新')
    } else {
      await createMenu(payload)
      ElMessage.success('菜单已创建')
    }
    dialogVisible.value = false
    await refreshTreeCache()
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: MenuDetailVO) {
  await ElMessageBox.confirm(`确认删除「${row.menuName}」？`, '提示', { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success('已删除')
  await refreshTreeCache()
  loadData()
}

onMounted(async () => {
  await refreshTreeCache()
  await loadData()
})
</script>

<template>
  <PageCard title="菜单管理" description="内置菜单只读；自定义菜单通过「新增」创建。">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="builtin-tip"
      title="内置菜单与后端权限码绑定，仅展示不可编辑；不可在其下新增子菜单。"
    />

    <el-form :inline="true" class="search-bar" @submit.prevent="handleSearch">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="名称/权限码" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="正常" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button v-perm="'system:menu:add'" type="success" @click="openCreate()">新增</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="!treeMode"
      type="info"
      :closable="false"
      show-icon
      class="mode-tip"
      title="当前为筛选平铺视图；清空条件后恢复树形展示。"
    />

    <el-table
      v-loading="loading"
      :data="tableData"
      row-key="id"
      border
      stripe
      :default-expand-all="treeMode"
      :tree-props="treeMode ? { children: 'children' } : undefined"
    >
      <el-table-column prop="menuName" label="名称" min-width="180">
        <template #default="{ row }">
          <span>{{ row.menuName }}</span>
          <el-tag v-if="isBuiltInMenu(row as MenuDetailVO)" size="small" type="info" class="builtin-tag">
            内置
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag
            size="small"
            :type="row.menuType === MENU_TYPE_BTN ? 'warning' : row.menuType === MENU_TYPE_DIR ? 'primary' : 'success'"
          >
            {{ menuTypeMap[row.menuType] || row.menuType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路由" min-width="120" show-overflow-tooltip />
      <el-table-column prop="component" label="组件" min-width="140" show-overflow-tooltip />
      <el-table-column prop="perms" label="权限码" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.perms">{{ row.perms }}</span>
          <span v-else class="text-muted">—</span>
          <el-tooltip
            v-if="isBuiltInMenu(row as MenuDetailVO) && row.perms"
            content="内置权限码只读，与后端接口绑定"
            placement="top"
          >
            <el-icon class="perms-lock"><Lock /></el-icon>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="70" align="center" />
      <el-table-column label="显示" width="70" align="center">
        <template #default="{ row }">
          {{ row.visible === 1 ? '是' : '否' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <template v-if="!isBuiltInMenu(row as MenuDetailVO)">
            <el-button
              v-if="treeMode && row.menuType !== MENU_TYPE_BTN"
              v-perm="'system:menu:add'"
              link
              type="primary"
              @click="openCreate(row as MenuDetailVO)"
            >
              新增子项
            </el-button>
            <el-button v-perm="'system:menu:edit'" link type="primary" @click="openEdit(row as MenuDetailVO)">
              编辑
            </el-button>
            <el-button
              v-perm="'system:menu:remove'"
              link
              type="danger"
              @click="handleDelete(row as MenuDetailVO)"
            >
              删除
            </el-button>
          </template>
          <span v-else class="text-muted">只读</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="72px">
        <el-form-item label="父节点" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentTreeOptions"
            value-key="id"
            :props="{ label: 'menuName', children: 'children' }"
            check-strictly
            default-expand-all
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio :value="MENU_TYPE_DIR">目录</el-radio>
            <el-radio :value="MENU_TYPE_PAGE">菜单</el-radio>
            <el-radio :value="MENU_TYPE_BTN">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称" prop="menuName">
          <el-input v-model="form.menuName" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== MENU_TYPE_BTN" label="路由">
          <el-input v-model="form.path" placeholder="如 user 或 /system" />
        </el-form-item>
        <el-form-item v-if="form.menuType === MENU_TYPE_PAGE" label="组件">
          <el-input v-model="form.component" placeholder="如 system/user/index" />
        </el-form-item>
        <el-form-item v-if="form.menuType === MENU_TYPE_BTN" label="权限码" prop="perms">
          <el-input v-model="form.perms" placeholder="如 system:user:add" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== MENU_TYPE_BTN" label="图标">
          <el-input v-model="form.icon" placeholder="如 setting" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== MENU_TYPE_BTN" label="显示">
          <el-radio-group v-model="form.visible">
            <el-radio :value="1">显示</el-radio>
            <el-radio :value="0">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </PageCard>
</template>

<style scoped lang="scss">
.builtin-tip {
  margin-bottom: 10px;
}

.search-bar {
  margin-bottom: 10px;
}

.mode-tip {
  margin-bottom: 10px;
}

.builtin-tag {
  margin-left: 4px;
  vertical-align: middle;
}

.perms-lock {
  margin-left: 3px;
  vertical-align: middle;
  color: var(--power-text-muted);
  font-size: 12px;
}

.text-muted {
  color: var(--power-text-muted);
  font-size: 11px;
}
</style>
