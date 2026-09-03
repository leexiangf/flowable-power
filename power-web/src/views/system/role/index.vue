<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageCard from '@/components/PageCard.vue'
import { fetchAdminMenuTree } from '@/api/menu'
import {
  assignRoleMenus,
  createRole,
  deleteRole,
  fetchRoleDetail,
  fetchRolePage,
  updateRole,
} from '@/api/role'
import type { MenuDetailVO, RoleSaveRequest, RoleVO } from '@/types/system'

const loading = ref(false)
const tableData = ref<RoleVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: undefined as number | undefined,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const submitting = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<RoleSaveRequest>({
  roleCode: '',
  roleName: '',
  sort: 0,
  status: 1,
  remark: '',
})

const rules: FormRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
}

const menuDialogVisible = ref(false)
const menuSubmitting = ref(false)
const menuTree = ref<MenuDetailVO[]>([])
const menuTreeRef = ref()
const assigningRoleId = ref<string | null>(null)
const assigningRoleName = ref('')

async function loadData() {
  loading.value = true
  try {
    const res = await fetchRolePage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      status: query.status,
    })
    tableData.value = res.records
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function resetForm() {
  form.roleCode = ''
  form.roleName = ''
  form.sort = 0
  form.status = 1
  form.remark = ''
}

function openCreate() {
  editingId.value = null
  dialogTitle.value = '新增角色'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: RoleVO) {
  editingId.value = row.id
  dialogTitle.value = '编辑角色'
  form.roleCode = row.roleCode
  form.roleName = row.roleName
  form.sort = row.sort ?? 0
  form.status = row.status ?? 1
  form.remark = row.remark || ''
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload: RoleSaveRequest = {
      roleCode: form.roleCode.trim(),
      roleName: form.roleName.trim(),
      sort: form.sort,
      status: form.status,
      remark: form.remark?.trim() || undefined,
    }
    if (editingId.value) {
      await updateRole(editingId.value, payload)
      ElMessage.success('角色已更新')
    } else {
      await createRole(payload)
      ElMessage.success('角色已创建')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: RoleVO) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('已删除')
  loadData()
}

async function openMenuAssign(row: RoleVO) {
  assigningRoleId.value = row.id
  assigningRoleName.value = row.roleName
  if (!menuTree.value.length) {
    menuTree.value = await fetchAdminMenuTree()
  }
  const detail = await fetchRoleDetail(row.id)
  menuDialogVisible.value = true
  await nextTickSetChecked(detail.menuIds || [])
}

async function nextTickSetChecked(menuIds: string[]) {
  await new Promise((r) => setTimeout(r, 0))
  menuTreeRef.value?.setCheckedKeys(menuIds)
}

async function submitMenuAssign() {
  if (!assigningRoleId.value) return
  menuSubmitting.value = true
  try {
    const checked: string[] = menuTreeRef.value?.getCheckedKeys(false) || []
    const halfChecked: string[] = menuTreeRef.value?.getHalfCheckedKeys() || []
    const menuIds = [...new Set([...checked, ...halfChecked])]
    await assignRoleMenus(assigningRoleId.value, menuIds)
    ElMessage.success('菜单权限已保存')
    menuDialogVisible.value = false
  } finally {
    menuSubmitting.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageCard title="角色管理">
    <el-form :inline="true" class="search-bar" @submit.prevent="handleSearch">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="编码/名称" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="正常" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button v-perm="'system:role:add'" type="success" @click="openCreate">新增</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe>
      <el-table-column prop="roleCode" label="角色编码" min-width="120" />
      <el-table-column prop="roleName" label="角色名称" min-width="120" />
      <el-table-column prop="sort" label="排序" width="80" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button
            v-perm="'system:role:edit'"
            link
            type="primary"
            @click="openEdit(row as RoleVO)"
          >
            编辑
          </el-button>
          <el-button
            v-perm="'system:role:edit'"
            link
            type="primary"
            @click="openMenuAssign(row as RoleVO)"
          >
            分配菜单
          </el-button>
          <el-button
            v-perm="'system:role:remove'"
            link
            type="danger"
            @click="handleDelete(row as RoleVO)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadData"
        @size-change="handleSearch"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="72px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="!!editingId" placeholder="如 STAFF" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
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

    <el-dialog
      v-model="menuDialogVisible"
      :title="`分配菜单 - ${assigningRoleName}`"
      width="480px"
      destroy-on-close
    >
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        node-key="id"
        show-checkbox
        default-expand-all
        :props="{ label: 'menuName', children: 'children' }"
        class="menu-tree"
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="menuSubmitting" @click="submitMenuAssign">
          保存
        </el-button>
      </template>
    </el-dialog>
  </PageCard>
</template>

<style scoped lang="scss">
.search-bar {
  margin-bottom: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.menu-tree {
  max-height: 420px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px;
}
</style>
