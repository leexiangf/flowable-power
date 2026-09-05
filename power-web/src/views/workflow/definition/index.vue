<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile } from 'element-plus'
import PageCard from '@/components/PageCard.vue'
import { fetchEnabledCategories } from '@/api/workflow/category'
import {
  deleteDeployment,
  activateDefinition,
  deployDefinition,
  downloadDefinitionXml,
  fetchDefinitionPage,
  fetchDefinitionXml,
  suspendDefinition,
} from '@/api/workflow/definition'
import type { CategoryVO, ProcessDefinitionVO } from '@/types/workflow'
import { formatDateTime, pageTotal } from '@/utils/workflow'
import { hasPerm } from '@/utils/permission'

const loading = ref(false)
const tableData = ref<ProcessDefinitionVO[]>([])
const total = ref(0)
const categoryOptions = ref<CategoryVO[]>([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  suspended: undefined as boolean | undefined,
  category: undefined as string | undefined,
})

const deployVisible = ref(false)
const deploySubmitting = ref(false)
const deployFile = ref<File | null>(null)

const xmlVisible = ref(false)
const xmlLoading = ref(false)
const xmlContent = ref('')
const xmlTitle = ref('BPMN XML')

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
    const res = await fetchDefinitionPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      suspended: query.suspended,
      category: query.category || undefined,
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

function openDeploy() {
  deployFile.value = null
  deployVisible.value = true
}

function handleDeployFileChange(file: UploadFile) {
  deployFile.value = file.raw ?? null
}

function handleDeployFileRemove() {
  deployFile.value = null
}

async function submitDeploy() {
  if (!deployFile.value) {
    ElMessage.warning('请选择 BPMN 文件')
    return
  }
  deploySubmitting.value = true
  try {
    await deployDefinition(deployFile.value)
    ElMessage.success('部署成功')
    deployVisible.value = false
    loadData()
  } finally {
    deploySubmitting.value = false
  }
}

async function openXml(row: ProcessDefinitionVO) {
  xmlTitle.value = `${row.name || row.key} v${row.version}`
  xmlContent.value = ''
  xmlVisible.value = true
  xmlLoading.value = true
  try {
    xmlContent.value = await fetchDefinitionXml(row.id)
  } finally {
    xmlLoading.value = false
  }
}

async function toggleSuspend(row: ProcessDefinitionVO) {
  const action = row.suspended ? '激活' : '挂起'
  await ElMessageBox.confirm(`确认${action}流程「${row.name || row.key}」？`, '提示', {
    type: 'warning',
  })
  if (row.suspended) {
    await activateDefinition(row.id)
  } else {
    await suspendDefinition(row.id)
  }
  ElMessage.success(`已${action}`)
  loadData()
}

async function handleDownload(row: ProcessDefinitionVO) {
  await downloadDefinitionXml(row.id, `${row.key}-v${row.version}.bpmn20.xml`)
}

async function handleDeleteDeployment(row: ProcessDefinitionVO) {
  if (!row.deploymentId) {
    ElMessage.warning('缺少 deploymentId')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认删除部署「${row.name || row.key} v${row.version}」？\n选择「级联删除」会同时删除关联运行/历史实例。`,
      '危险操作',
      {
        type: 'warning',
        distinguishCancelAndClose: true,
        confirmButtonText: '级联删除',
        cancelButtonText: '仅删部署',
      },
    )
    await deleteDeployment(row.deploymentId, true)
    ElMessage.success('已级联删除部署')
    loadData()
  } catch (action) {
    if (action === 'cancel') {
      await deleteDeployment(row.deploymentId, false)
      ElMessage.success('已删除部署')
      loadData()
    }
    // close / ESC：取消操作
  }
}

onMounted(async () => {
  await loadCategories()
  await loadData()
})
</script>

<template>
  <PageCard title="流程定义">
    <el-form :inline="true" class="search-bar" @submit.prevent="handleSearch">
      <el-form-item label="分类">
        <el-select
          v-model="query.category"
          placeholder="全部"
          clearable
          filterable
          style="width: 120px"
        >
          <el-option
            v-for="item in categoryOptions"
            :key="item.id"
            :label="item.name"
            :value="item.code"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.suspended" placeholder="全部" clearable style="width: 110px">
          <el-option label="正常" :value="false" />
          <el-option label="已挂起" :value="true" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button v-perm="'workflow:definition:deploy'" type="success" @click="openDeploy">
          部署 BPMN
        </el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe class="data-table">
      <el-table-column prop="key" label="Key" min-width="80" show-overflow-tooltip />
      <el-table-column prop="name" label="名称" min-width="96" show-overflow-tooltip />
      <el-table-column prop="version" label="版本" width="56" align="center" />
      <el-table-column prop="category" label="分类" width="72" show-overflow-tooltip />
      <el-table-column label="状态" width="72" align="center">
        <template #default="{ row }">
          <el-tag :type="row.suspended ? 'warning' : 'success'" size="small">
            {{ row.suspended ? '挂起' : '正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="部署时间" width="148" show-overflow-tooltip>
        <template #default="{ row }">
          {{ formatDateTime(row.deploymentTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" align="center">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button
              v-if="hasPerm('workflow:definition:list')"
              link
              type="primary"
              @click="openXml(row as ProcessDefinitionVO)"
            >
              XML
            </el-button>
            <el-button
              v-if="hasPerm('workflow:definition:list')"
              link
              type="primary"
              @click="handleDownload(row as ProcessDefinitionVO)"
            >
              下载
            </el-button>
            <el-button
              v-if="hasPerm('workflow:definition:suspend')"
              link
              :type="row.suspended ? 'success' : 'warning'"
              @click="toggleSuspend(row as ProcessDefinitionVO)"
            >
              {{ row.suspended ? '激活' : '挂起' }}
            </el-button>
            <el-button
              v-if="hasPerm('workflow:definition:remove') && row.deploymentId"
              link
              type="danger"
              @click="handleDeleteDeployment(row as ProcessDefinitionVO)"
            >
              删部署
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

    <el-dialog v-model="deployVisible" title="部署 BPMN" width="440px" destroy-on-close>
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        accept=".bpmn,.xml,.bpmn20.xml"
        :on-change="handleDeployFileChange"
        :on-remove="handleDeployFileRemove"
      >
        <div class="upload-tip">将 .bpmn / .bpmn20.xml 拖到此处，或点击选择</div>
      </el-upload>
      <p class="upload-hint">部署后同 key 会生成新版本；挂起后不可再发起新实例。</p>
      <template #footer>
        <el-button @click="deployVisible = false">取消</el-button>
        <el-button type="primary" :loading="deploySubmitting" @click="submitDeploy">部署</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="xmlVisible" :title="xmlTitle" width="640px" destroy-on-close class="xml-dialog">
      <div v-loading="xmlLoading" class="xml-wrap">
        <pre v-if="xmlContent" class="xml-pre">{{ xmlContent }}</pre>
        <el-empty v-else-if="!xmlLoading" description="暂无 XML 内容" />
      </div>
      <template #footer>
        <el-button @click="xmlVisible = false">关闭</el-button>
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

.upload-tip {
  font-size: 12px;
  color: var(--power-text-secondary);
  padding: 12px 0;
}

.upload-hint {
  margin: 10px 0 0;
  font-size: 11px;
  color: var(--power-text-muted);
}

.xml-wrap {
  min-height: 200px;
  max-height: 420px;
  overflow: auto;
}

.xml-pre {
  margin: 0;
  padding: 10px;
  font-size: 11px;
  line-height: 1.45;
  white-space: pre-wrap;
  word-break: break-all;
  background: var(--el-fill-color-light);
  border: 1px solid var(--power-border-light);
  border-radius: var(--power-radius);
}
</style>
