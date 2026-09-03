<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageCard from '@/components/PageCard.vue'
import {
  createCategory,
  deleteCategory,
  fetchCategoryPage,
  updateCategory,
} from '@/api/workflow/category'
import type { CategorySaveRequest, CategoryVO } from '@/types/workflow'
import { formatDateTime, pageTotal } from '@/utils/workflow'

const loading = ref(false)
const tableData = ref<CategoryVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const submitting = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<CategorySaveRequest>({
  code: '',
  name: '',
  sort: 0,
  status: 1,
  remark: '',
})

const rules: FormRules = {
  code: [
    { required: true, message: '请输入分类编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_-]*$/, message: '字母开头，仅含字母数字_- ', trigger: 'blur' },
  ],
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchCategoryPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
    })
    tableData.value = res.records
    total.value = pageTotal(res.total)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function resetForm() {
  form.code = ''
  form.name = ''
  form.sort = 0
  form.status = 1
  form.remark = ''
}

function openCreate() {
  editingId.value = null
  dialogTitle.value = '新增分类'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: CategoryVO) {
  editingId.value = row.id
  dialogTitle.value = '编辑分类'
  form.code = row.code
  form.name = row.name
  form.sort = row.sort ?? 0
  form.status = row.status ?? 1
  form.remark = row.remark || ''
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload: CategorySaveRequest = {
      code: form.code.trim(),
      name: form.name.trim(),
      sort: form.sort,
      status: form.status,
      remark: form.remark?.trim() || undefined,
    }
    if (editingId.value) {
      await updateCategory(editingId.value, payload)
      ElMessage.success('分类已更新')
    } else {
      await createCategory(payload)
      ElMessage.success('分类已创建')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: CategoryVO) {
  await ElMessageBox.confirm(`确认删除分类「${row.name}」？`, '提示', { type: 'warning' })
  await deleteCategory(row.id)
  ElMessage.success('已删除')
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageCard title="流程分类">
    <el-form :inline="true" class="search-bar" @submit.prevent="handleSearch">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="编码/名称" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button v-perm="'workflow:category:edit'" type="success" @click="openCreate">新增</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe class="data-table">
      <el-table-column prop="code" label="编码" min-width="88" show-overflow-tooltip />
      <el-table-column prop="name" label="名称" min-width="96" show-overflow-tooltip />
      <el-table-column prop="sort" label="排序" width="64" align="center" />
      <el-table-column label="状态" width="64" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
      <el-table-column label="创建时间" width="148" show-overflow-tooltip>
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button
              v-perm="'workflow:category:edit'"
              link
              type="primary"
              @click="openEdit(row as CategoryVO)"
            >
              编辑
            </el-button>
            <el-button
              v-perm="'workflow:category:edit'"
              link
              type="danger"
              @click="handleDelete(row as CategoryVO)"
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="440px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="72px">
        <el-form-item label="编码" prop="code">
          <el-input
            v-model="form.code"
            :disabled="!!editingId"
            placeholder="如 leave"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如 请假" maxlength="64" />
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
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
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
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.table-actions {
  display: inline-flex;
  justify-content: center;
  gap: 0 2px;
}
</style>
