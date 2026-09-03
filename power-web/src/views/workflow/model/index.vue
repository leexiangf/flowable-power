<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageCard from '@/components/PageCard.vue'
import { fetchEnabledCategories } from '@/api/workflow/category'
import {
  deleteModel,
  deployModel,
  fetchModelDetail,
  fetchModelPage,
  saveModel,
} from '@/api/workflow/model'
import type { CategoryVO, ModelSaveRequest, ModelVO } from '@/types/workflow'
import { DEFAULT_BPMN_TEMPLATE, formatDateTime, pageTotal } from '@/utils/workflow'
import { hasPerm } from '@/utils/permission'

const loading = ref(false)
const tableData = ref<ModelVO[]>([])
const total = ref(0)
const categoryOptions = ref<CategoryVO[]>([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
})

const editorVisible = ref(false)
const editorTitle = ref('新建模型')
const submitting = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<ModelSaveRequest>({
  modelKey: '',
  name: '',
  categoryCode: '',
  bpmnXml: DEFAULT_BPMN_TEMPLATE,
  remark: '',
})

const rules: FormRules = {
  modelKey: [
    { required: true, message: '请输入模型 Key', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_-]*$/, message: '字母开头，仅含字母数字_- ', trigger: 'blur' },
  ],
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  bpmnXml: [{ required: true, message: '请填写 BPMN XML', trigger: 'blur' }],
}

async function loadCategories() {
  try {
    categoryOptions.value = await fetchEnabledCategories()
  } catch {
    categoryOptions.value = []
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchModelPage({
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
  form.modelKey = ''
  form.name = ''
  form.categoryCode = ''
  form.bpmnXml = DEFAULT_BPMN_TEMPLATE
  form.remark = ''
}

function openCreate() {
  editingId.value = null
  editorTitle.value = '新建模型'
  resetForm()
  editorVisible.value = true
}

async function openEdit(row: ModelVO) {
  editingId.value = row.id
  editorTitle.value = `编辑模型 - ${row.name}`
  const detail = await fetchModelDetail(row.id)
  form.modelKey = detail.modelKey
  form.name = detail.name
  form.categoryCode = detail.categoryCode || ''
  form.bpmnXml = detail.bpmnXml || DEFAULT_BPMN_TEMPLATE
  form.remark = detail.remark || ''
  editorVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload: ModelSaveRequest = {
      modelKey: form.modelKey.trim(),
      name: form.name.trim(),
      categoryCode: form.categoryCode?.trim() || undefined,
      bpmnXml: form.bpmnXml,
      remark: form.remark?.trim() || undefined,
    }
    await saveModel(payload)
    ElMessage.success('模型已保存')
    editorVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: ModelVO) {
  await ElMessageBox.confirm(`确认删除模型「${row.name}」？`, '提示', { type: 'warning' })
  await deleteModel(row.id)
  ElMessage.success('已删除')
  loadData()
}

async function handleDeploy(row: ModelVO) {
  await ElMessageBox.confirm(`确认部署模型「${row.name}」到流程引擎？`, '提示', { type: 'info' })
  const def = await deployModel(row.id)
  ElMessage.success(`部署成功：${def.name || def.key} v${def.version}`)
}

onMounted(async () => {
  await loadCategories()
  await loadData()
})
</script>

<template>
  <PageCard title="流程模型">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mode-tip"
      title="当前为轻量版：直接编辑 BPMN XML 并保存草稿；部署后可在「流程定义」中管理。"
    />

    <el-form :inline="true" class="search-bar" @submit.prevent="handleSearch">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="Key/名称" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button v-perm="'workflow:model:edit'" type="success" @click="openCreate">新建</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe class="data-table">
      <el-table-column prop="modelKey" label="Key" min-width="88" show-overflow-tooltip />
      <el-table-column prop="name" label="名称" min-width="96" show-overflow-tooltip />
      <el-table-column prop="categoryCode" label="分类" width="72" show-overflow-tooltip />
      <el-table-column prop="version" label="版本" width="56" align="center" />
      <el-table-column label="更新时间" width="148" show-overflow-tooltip>
        <template #default="{ row }">
          {{ formatDateTime(row.updateTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="88" show-overflow-tooltip />
      <el-table-column label="操作" width="168" align="center">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button
              v-if="hasPerm('workflow:model:list')"
              link
              type="primary"
              @click="openEdit(row as ModelVO)"
            >
              编辑
            </el-button>
            <el-button
              v-if="hasPerm('workflow:definition:deploy')"
              link
              type="success"
              @click="handleDeploy(row as ModelVO)"
            >
              部署
            </el-button>
            <el-button
              v-if="hasPerm('workflow:model:edit')"
              link
              type="danger"
              @click="handleDelete(row as ModelVO)"
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

    <el-drawer v-model="editorVisible" :title="editorTitle" size="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="72px">
        <el-form-item label="Key" prop="modelKey">
          <el-input
            v-model="form.modelKey"
            :disabled="!!editingId"
            placeholder="如 leave"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="流程名称" maxlength="128" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select
            v-model="form.categoryCode"
            placeholder="选填"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="item in categoryOptions"
              :key="item.id"
              :label="`${item.name} (${item.code})`"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" maxlength="255" />
        </el-form-item>
        <el-form-item label="BPMN" prop="bpmnXml" class="xml-item">
          <el-input
            v-model="form.bpmnXml"
            type="textarea"
            :rows="18"
            class="xml-editor"
            spellcheck="false"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button v-perm="'workflow:model:edit'" type="primary" :loading="submitting" @click="submitForm">
          保存草稿
        </el-button>
      </template>
    </el-drawer>
  </PageCard>
</template>

<style scoped lang="scss">
.mode-tip {
  margin-bottom: 10px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.table-actions {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0 2px;
}

.xml-item {
  :deep(.el-form-item__content) {
    width: 100%;
  }
}

.xml-editor {
  :deep(textarea) {
    font-family: ui-monospace, 'Cascadia Code', Consolas, monospace;
    font-size: 11px;
    line-height: 1.45;
  }
}
</style>
