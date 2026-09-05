<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageCard from '@/components/PageCard.vue'
import InstanceDetailDrawer from '@/components/workflow/InstanceDetailDrawer.vue'
import TaskHandleDialog, { type TaskHandleMode } from '@/components/workflow/TaskHandleDialog.vue'
import {
  claimTask,
  fetchCcTasks,
  fetchDoneTasks,
  fetchTodoTasks,
  markCcRead,
  reduceSignTask,
  resolveDelegateTask,
  unclaimTask,
} from '@/api/workflow/task'
import { useAuthStore } from '@/stores/auth'
import type { CcVO, TaskVO } from '@/types/workflow'
import { formatDateTime, pageTotal } from '@/utils/workflow'
import { hasPerm } from '@/utils/permission'

const auth = useAuthStore()

const loading = ref(false)
const tableData = ref<TaskVO[]>([])
const ccData = ref<CcVO[]>([])
const total = ref(0)
const activeTab = ref<'todo' | 'done' | 'cc'>('todo')

const query = reactive({
  pageNum: 1,
  pageSize: 10,
})

const detailVisible = ref(false)
const detailInstanceId = ref<string | null>(null)

const handleVisible = ref(false)
const handleTask = ref<TaskVO | null>(null)
const handleMode = ref<TaskHandleMode>('complete')

function isMyTask(row: TaskVO) {
  const me = auth.user?.userId
  if (!me || !row.assignee) return false
  return String(row.assignee) === String(me)
}

/** 委派 PENDING：被委派人 */
function isPendingDelegatee(row: TaskVO) {
  return row.delegationState === 'PENDING' && isMyTask(row)
}

/** 委派 PENDING：原办理人（owner） */
function isDelegatedOwner(row: TaskVO) {
  return row.owner === auth.user?.userId && row.delegationState === 'PENDING'
}

/** 可正常办理（非委派中） */
function canHandle(row: TaskVO) {
  return (isMyTask(row) || needsClaim(row)) && row.delegationState !== 'PENDING'
}

function needsClaim(row: TaskVO) {
  return !row.assignee
}

async function loadData() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (activeTab.value === 'cc') {
      const res = await fetchCcTasks(params)
      ccData.value = res.records
      total.value = pageTotal(res.total)
      return
    }
    const res =
      activeTab.value === 'todo' ? await fetchTodoTasks(params) : await fetchDoneTasks(params)
    tableData.value = res.records
    total.value = pageTotal(res.total)
  } finally {
    loading.value = false
  }
}

function openDetail(row: TaskVO | CcVO) {
  const id = 'processInstanceId' in row ? row.processInstanceId : null
  if (!id) return
  detailInstanceId.value = id
  detailVisible.value = true
  if ('readFlag' in row && row.readFlag === 0 && row.id) {
    markCcRead(row.id).then(() => loadData())
  }
}

function openHandle(row: TaskVO, mode: TaskHandleMode) {
  handleTask.value = row
  handleMode.value = mode
  handleVisible.value = true
}

async function handleClaim(row: TaskVO) {
  await claimTask(row.id)
  ElMessage.success('已认领')
  loadData()
}

async function handleUnclaim(row: TaskVO) {
  await unclaimTask(row.id)
  ElMessage.success('已取消认领')
  loadData()
}

async function handleResolve(row: TaskVO) {
  await resolveDelegateTask(row.id)
  ElMessage.success(isDelegatedOwner(row) ? '已收回委派任务' : '已归还任务')
  loadData()
}

async function handleReduceSign(row: TaskVO) {
  await ElMessageBox.confirm('确认减签并删除当前会签子任务？', '减签', { type: 'warning' })
  await reduceSignTask(row.id)
  ElMessage.success('已减签')
  loadData()
}

function canAddSign(row: TaskVO) {
  return (
    canHandle(row) &&
    isMyTask(row) &&
    !row.addSignMode &&
    hasPerm('workflow:task:addsign')
  )
}

function canReduceSign(row: TaskVO) {
  return canHandle(row) && !!row.multiInstance && hasPerm('workflow:task:addsign')
}

watch(activeTab, () => {
  query.pageNum = 1
  loadData()
})

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageCard title="任务中心">
    <el-tabs v-model="activeTab" class="task-tabs">
      <el-tab-pane label="待办" name="todo" />
      <el-tab-pane label="已办" name="done" />
      <el-tab-pane v-if="hasPerm('workflow:task:cc')" label="抄送" name="cc" />
    </el-tabs>

    <el-table
      v-if="activeTab !== 'cc'"
      v-loading="loading"
      :data="tableData"
      border
      stripe
      class="data-table"
    >
      <el-table-column label="任务" min-width="100" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.title?.trim() || (row as TaskVO).name || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="processDefinitionKey" label="流程" width="72" show-overflow-tooltip />
      <el-table-column label="办理人" width="88" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.assigneeName || row.assignee || (needsClaim(row as TaskVO) ? '待认领' : '-') }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="148" show-overflow-tooltip>
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column v-if="activeTab === 'done'" label="完成时间" width="148" show-overflow-tooltip>
        <template #default="{ row }">
          {{ formatDateTime(row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" :width="activeTab === 'todo' ? 360 : 80" align="center">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button
              v-if="row.processInstanceId"
              link
              type="primary"
              @click="openDetail(row as TaskVO)"
            >
              实例
            </el-button>
            <template v-if="activeTab === 'todo' && hasPerm('workflow:task:handle')">
              <el-button
                v-if="needsClaim(row as TaskVO)"
                link
                type="primary"
                @click="handleClaim(row as TaskVO)"
              >
                认领
              </el-button>
              <el-button
                v-if="(row as TaskVO).canUnclaim && isMyTask(row as TaskVO) && !isPendingDelegatee(row as TaskVO) && !(row as TaskVO).addSignMode"
                link
                type="info"
                @click="handleUnclaim(row as TaskVO)"
              >
                取消认领
              </el-button>
              <el-button
                v-if="canHandle(row as TaskVO)"
                link
                type="success"
                @click="openHandle(row as TaskVO, 'complete')"
              >
                {{ (row as TaskVO).addSignMode === 'BEFORE' ? '归还' : '办理' }}
              </el-button>
              <el-button
                v-if="canHandle(row as TaskVO) && !(row as TaskVO).addSignMode"
                link
                type="warning"
                @click="openHandle(row as TaskVO, 'reject')"
              >
                驳回
              </el-button>
              <el-button
                v-if="canHandle(row as TaskVO) && isMyTask(row as TaskVO) && !(row as TaskVO).addSignMode"
                link
                type="primary"
                @click="openHandle(row as TaskVO, 'transfer')"
              >
                转办
              </el-button>
              <el-button
                v-if="canAddSign(row as TaskVO)"
                link
                type="primary"
                @click="openHandle(row as TaskVO, 'addSign')"
              >
                加签
              </el-button>
              <el-button
                v-if="canReduceSign(row as TaskVO)"
                link
                type="danger"
                @click="handleReduceSign(row as TaskVO)"
              >
                减签
              </el-button>
              <el-button
                v-if="canHandle(row as TaskVO) && isMyTask(row as TaskVO) && !(row as TaskVO).addSignMode && hasPerm('workflow:task:delegate')"
                link
                type="primary"
                @click="openHandle(row as TaskVO, 'delegate')"
              >
                委派
              </el-button>
              <el-button
                v-if="(isPendingDelegatee(row as TaskVO) || isDelegatedOwner(row as TaskVO)) && hasPerm('workflow:task:delegate')"
                link
                type="warning"
                @click="handleResolve(row as TaskVO)"
              >
                {{ isDelegatedOwner(row as TaskVO) ? '收回' : '归还' }}
              </el-button>
            </template>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-table v-else v-loading="loading" :data="ccData" border stripe class="data-table">
      <el-table-column label="标题" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          <el-badge v-if="row.readFlag === 0" is-dot class="cc-badge">
            {{ row.title?.trim() || '-' }}
          </el-badge>
          <span v-else>{{ row.title?.trim() || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="processDefinitionKey" label="流程" width="72" />
      <el-table-column label="抄送人" width="88" show-overflow-tooltip>
        <template #default="{ row }">{{ row.fromUserName || row.fromUserId || '-' }}</template>
      </el-table-column>
      <el-table-column label="时间" width="148">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="72" align="center">
        <template #default="{ row }">
          <el-tag :type="row.ended ? 'info' : 'success'" size="small">
            {{ row.ended ? '已结束' : '进行中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row as CcVO)">查看</el-button>
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
        @size-change="() => { query.pageNum = 1; loadData() }"
      />
    </div>

    <TaskHandleDialog
      v-model="handleVisible"
      :task="handleTask"
      :mode="handleMode"
      @success="loadData"
    />

    <InstanceDetailDrawer
      v-model="detailVisible"
      :process-instance-id="detailInstanceId"
      readonly
    />
  </PageCard>
</template>

<style scoped lang="scss">
.task-tabs {
  margin-bottom: 8px;

  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__item) {
    font-size: 12px;
    height: 32px;
  }
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
  line-height: 1.2;
}

.cc-badge {
  :deep(.el-badge__content.is-dot) {
    top: 4px;
    right: -4px;
  }
}
</style>
