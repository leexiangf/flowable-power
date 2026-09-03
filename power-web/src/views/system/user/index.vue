<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageCard from '@/components/PageCard.vue'
import {
  createUser,
  deleteUser,
  disableUser,
  enableUser,
  fetchUserPage,
  updateUser,
} from '@/api/user'
import { fetchEnabledRoles } from '@/api/role'
import type { RoleVO, UserSaveRequest, UserVO } from '@/types/system'
import { hasPerm } from '@/utils/permission'

const PROTECTED_ADMIN_ID = '1'

const loading = ref(false)
const tableData = ref<UserVO[]>([])
const total = ref(0)
const roleOptions = ref<RoleVO[]>([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: undefined as number | undefined,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const submitting = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<UserSaveRequest>({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  status: 1,
  remark: '',
  roleIds: [],
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入登录名', trigger: 'blur' }],
  password: [
    {
      validator: (_rule, value, callback) => {
        if (!editingId.value && !value) {
          callback(new Error('新增用户时密码不能为空'))
          return
        }
        if (value && value.length < 6) {
          callback(new Error('密码至少 6 位'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

async function loadRoles() {
  roleOptions.value = await fetchEnabledRoles()
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchUserPage({
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
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.phone = ''
  form.email = ''
  form.status = 1
  form.remark = ''
  form.roleIds = []
}

function openCreate() {
  editingId.value = null
  dialogTitle.value = '新增用户'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: UserVO) {
  editingId.value = row.id
  dialogTitle.value = '编辑用户'
  form.username = row.username
  form.password = ''
  form.nickname = row.nickname || ''
  form.phone = row.phone || ''
  form.email = row.email || ''
  form.status = row.status ?? 1
  form.remark = row.remark || ''
  form.roleIds = row.roleIds ? [...row.roleIds] : []
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload: UserSaveRequest = {
      username: form.username.trim(),
      nickname: form.nickname?.trim() || undefined,
      phone: form.phone?.trim() || undefined,
      email: form.email?.trim() || undefined,
      status: form.status,
      remark: form.remark?.trim() || undefined,
      roleIds: form.roleIds,
    }
    if (form.password) {
      payload.password = form.password
    }
    if (editingId.value) {
      await updateUser(editingId.value, payload)
      ElMessage.success('用户已更新')
    } else {
      await createUser(payload)
      ElMessage.success('用户已创建')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: UserVO) {
  if (String(row.id) === PROTECTED_ADMIN_ID) {
    ElMessage.warning('内置管理员不可删除')
    return
  }
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('已删除')
  loadData()
}

async function toggleStatus(row: UserVO) {
  if (String(row.id) === PROTECTED_ADMIN_ID && row.status === 1) {
    ElMessage.warning('内置管理员不可禁用')
    return
  }
  const action = row.status === 1 ? '禁用' : '启用'
  await ElMessageBox.confirm(`确认${action}用户「${row.username}」？`, '提示', { type: 'warning' })
  if (row.status === 1) {
    await disableUser(row.id)
  } else {
    await enableUser(row.id)
  }
  ElMessage.success(`已${action}`)
  loadData()
}

onMounted(async () => {
  await loadRoles()
  await loadData()
})
</script>

<template>
  <PageCard title="用户管理">
    <el-form :inline="true" class="search-bar" @submit.prevent="handleSearch">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="登录名/昵称/手机" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="正常" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button v-perm="'system:user:add'" type="success" @click="openCreate">新增</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe class="data-table">
      <el-table-column prop="username" label="登录名" min-width="88" show-overflow-tooltip />
      <el-table-column prop="nickname" label="昵称" min-width="72" show-overflow-tooltip />
      <el-table-column prop="phone" label="手机" width="108" show-overflow-tooltip />
      <el-table-column prop="email" label="邮箱" min-width="96" show-overflow-tooltip />
      <el-table-column label="角色" min-width="88" show-overflow-tooltip>
        <template #default="{ row }">
          <template v-if="row.roleCodes?.length">
            <el-tooltip :content="row.roleCodes.join(', ')" placement="top">
              <span class="role-cell">
                <el-tag
                  v-for="code in row.roleCodes.slice(0, 2)"
                  :key="code"
                  size="small"
                  class="role-tag"
                >
                  {{ code }}
                </el-tag>
                <span v-if="row.roleCodes.length > 2" class="role-more">
                  +{{ row.roleCodes.length - 2 }}
                </span>
              </span>
            </el-tooltip>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="64" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="148" show-overflow-tooltip />
      <el-table-column label="操作" width="132" align="center" class-name="col-actions">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button
              v-perm="'system:user:edit'"
              link
              type="primary"
              @click="openEdit(row as UserVO)"
            >
              编辑
            </el-button>
            <el-button
              v-if="hasPerm('system:user:edit')"
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row as UserVO)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button
              v-perm="'system:user:remove'"
              link
              type="danger"
              :disabled="String(row.id) === PROTECTED_ADMIN_ID"
              @click="handleDelete(row as UserVO)"
            >
              删除
            </el-button>
          </div>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="72px">
        <el-form-item label="登录名" prop="username">
          <el-input v-model="form.username" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="editingId ? '留空表示不修改' : '至少 6 位'"
          />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="选择角色" style="width: 100%">
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :label="`${role.roleName} (${role.roleCode})`"
              :value="role.id"
            />
          </el-select>
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
.search-bar {
  margin-bottom: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.role-cell {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  overflow: hidden;
  vertical-align: middle;
}

.role-tag {
  flex-shrink: 0;
  margin-right: 4px;
}

.role-more {
  flex-shrink: 0;
  font-size: 10px;
  color: var(--power-text-muted);
}

.table-actions {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0 2px;
  line-height: 1.2;
}
</style>
