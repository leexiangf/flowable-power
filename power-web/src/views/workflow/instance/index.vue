<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import PageCard from '@/components/PageCard.vue'
import BusinessStartDialog, {
  type BizProcessKind,
} from '@/components/workflow/BusinessStartDialog.vue'
import InstanceDetailDrawer from '@/components/workflow/InstanceDetailDrawer.vue'
import ProcessStatusTag from '@/components/workflow/ProcessStatusTag.vue'
import { fetchStartableDefinitions } from '@/api/workflow/definition'
import {
  fetchInstanceMonitor,
  fetchMyInstances,
  startProcessInstance,
} from '@/api/workflow/instance'
import type { ProcessDefinitionVO, ProcessInstanceVO } from '@/types/workflow'
import { formatDateTime, pageTotal, processTypeLabel } from '@/utils/workflow'
import { hasPerm } from '@/utils/permission'

/** 需专用发起入口的流程（依赖多实例变量等） */
const BIZ_START_KEYS = new Set(['expense', 'countersign-or', 'countersign-seq', 'leave'])

const canMonitor = computed(() => hasPerm('workflow:instance:monitor'))

const loading = ref(false)
const tableData = ref<ProcessInstanceVO[]>([])
const total = ref(0)

const activeTab = ref<'mine' | 'monitor'>('mine')

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  processDefinitionKey: '',
  finished: undefined as boolean | undefined,
})

const detailVisible = ref(false)
const detailInstanceId = ref<string | null>(null)

const startVisible = ref(false)
const startSubmitting = ref(false)
const startableOptions = ref<ProcessDefinitionVO[]>([])
const genericStartableOptions = computed(() =>
  startableOptions.value.filter((d) => !BIZ_START_KEYS.has(d.key)),
)
const bizStartVisible = ref(false)
const bizInitialKind = ref<BizProcessKind | null>('countersign-or')

const startForm = reactive({
  processDefinitionKey: '',
  title: '',
  businessKey: '',
})

function openBizStart(kind?: BizProcessKind) {
  bizInitialKind.value = kind || 'countersign-or'
  bizStartVisible.value = true
}

function onBizStartSuccess() {
  activeTab.value = 'mine'
  query.pageNum = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const pageQuery = { pageNum: query.pageNum, pageSize: query.pageSize }
    const res =
      activeTab.value === 'mine'
        ? await fetchMyInstances(pageQuery)
        : await fetchInstanceMonitor({
            ...pageQuery,
            processDefinitionKey: query.processDefinitionKey.trim() || undefined,
            finished: query.finished,
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

function openDetail(row: ProcessInstanceVO) {
  detailInstanceId.value = row.id
  detailVisible.value = true
}

async function openStart() {
  startForm.processDefinitionKey = ''
  startForm.title = ''
  startForm.businessKey = ''
  const res = await fetchStartableDefinitions({ pageNum: 1, pageSize: 100 })
  startableOptions.value = res.records
  if (!genericStartableOptions.value.length) {
    ElMessage.info('或签 / 会签 / 报销请用「发起业务」；请假请用「请假管理」')
    openBizStart('countersign-or')
    return
  }
  startVisible.value = true
}

async function submitStart() {
  if (!startForm.processDefinitionKey) {
    ElMessage.warning('请选择流程')
    return
  }
  if (BIZ_START_KEYS.has(startForm.processDefinitionKey)) {
    ElMessage.warning('该流程请使用「发起业务」或对应业务菜单')
    return
  }
  startSubmitting.value = true
  try {
    await startProcessInstance({
      processDefinitionKey: startForm.processDefinitionKey,
      title: startForm.title.trim() || undefined,
      businessKey: startForm.businessKey.trim() || undefined,
    })
    ElMessage.success('流程已发起')
    startVisible.value = false
    activeTab.value = 'mine'
    query.pageNum = 1
    loadData()
  } finally {
    startSubmitting.value = false
  }
}

watch(activeTab, () => {
  query.pageNum = 1
  loadData()
})

onMounted(() => {
  if (!hasPerm('workflow:instance:list') && canMonitor.value) {
    activeTab.value = 'monitor'
  }
  loadData()
})
</script>

<template>
  <PageCard title="流程实例">
    <div class="toolbar">
      <el-tabs v-model="activeTab" class="instance-tabs">
        <el-tab-pane v-if="hasPerm('workflow:instance:list')" label="我发起的" name="mine" />
        <el-tab-pane v-if="canMonitor" label="实例监控" name="monitor" />
      </el-tabs>
      <div class="toolbar-actions">
        <el-button v-perm="'workflow:instance:start'" type="primary" size="small" @click="openBizStart()">
          发起业务
        </el-button>
        <el-button v-perm="'workflow:instance:start'" type="success" size="small" @click="openStart">
          发起流程
        </el-button>
      </div>
    </div>

    <el-form
      v-if="activeTab === 'monitor'"
      :inline="true"
      class="search-bar"
      @submit.prevent="handleSearch"
    >
      <el-form-item label="流程 Key">
        <el-input v-model="query.processDefinitionKey" placeholder="如 leave" clearable />
      </el-form-item>
      <el-form-item label="是否结束">
        <el-select v-model="query.finished" placeholder="全部" clearable style="width: 100px">
          <el-option label="运行中" :value="false" />
          <el-option label="已结束" :value="true" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe class="data-table">
      <el-table-column label="标题" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.title?.trim() || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="类型" min-width="100" show-overflow-tooltip>
        <template #default="{ row }">
          {{ processTypeLabel(row.processDefinitionKey, row.processDefinitionName) }}
        </template>
      </el-table-column>
      <el-table-column prop="processDefinitionKey" label="Key" width="72" show-overflow-tooltip />
      <el-table-column
        v-if="activeTab === 'monitor'"
        label="发起人"
        width="88"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          {{ row.startUserName || row.startUserId || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="72" align="center">
        <template #default="{ row }">
          <ProcessStatusTag :instance="row as ProcessInstanceVO" />
        </template>
      </el-table-column>
      <el-table-column label="开始时间" width="148" show-overflow-tooltip>
        <template #default="{ row }">
          {{ formatDateTime(row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column label="结束时间" width="148" show-overflow-tooltip>
        <template #default="{ row }">
          {{ formatDateTime(row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row as ProcessInstanceVO)">
            详情
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

    <el-dialog v-model="startVisible" title="发起流程" width="440px" destroy-on-close>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="或签 / 串行会签 / 费用报销请点「发起业务」；请假请用「请假管理」。"
        class="start-alert"
      />
      <el-form :model="startForm" label-width="72px">
        <el-form-item label="流程" required>
          <el-select
            v-model="startForm.processDefinitionKey"
            placeholder="选择流程"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="item in genericStartableOptions"
              :key="item.id"
              :label="`${item.name || item.key} (v${item.version})`"
              :value="item.key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="startForm.title" placeholder="流程标题（选填）" />
        </el-form-item>
        <el-form-item label="业务主键">
          <el-input v-model="startForm.businessKey" placeholder="业务单号（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="startVisible = false">取消</el-button>
        <el-button type="primary" :loading="startSubmitting" @click="submitStart">发起</el-button>
      </template>
    </el-dialog>

    <InstanceDetailDrawer
      v-model="detailVisible"
      :process-instance-id="detailInstanceId"
      @refreshed="loadData"
    />

    <BusinessStartDialog
      v-model="bizStartVisible"
      :initial-kind="bizInitialKind"
      @success="onBizStartSuccess"
    />
  </PageCard>
</template>

<style scoped lang="scss">
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.toolbar-actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
}

.instance-tabs {
  flex: 1;
  min-width: 0;

  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__item) {
    font-size: 12px;
    height: 32px;
  }
}

.start-alert {
  margin-bottom: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
