<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import PageCard from '@/components/PageCard.vue'
import InstanceDetailDrawer from '@/components/workflow/InstanceDetailDrawer.vue'
import TaskHandleDialog, { type TaskHandleMode } from '@/components/workflow/TaskHandleDialog.vue'
import {
  claimTask,
  fetchDoneTasks,
  fetchTodoTasks,
  unclaimTask,
} from '@/api/workflow/task'
import { useAuthStore } from '@/stores/auth'
import type { TaskVO } from '@/types/workflow'
import { formatDateTime, pageTotal } from '@/utils/workflow'
import { hasPerm } from '@/utils/permission'

const auth = useAuthStore()

const loading = ref(false)
const tableData = ref<TaskVO[]>([])
const total = ref(0)
const activeTab = ref<'todo' | 'done'>('todo')

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
  return !!row.assignee && row.assignee === auth.user?.userId
}

function needsClaim(row: TaskVO) {
  return !row.assignee
}

async function loadData() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    const res =
      activeTab.value === 'todo' ? await fetchTodoTasks(params) : await fetchDoneTasks(params)
    tableData.value = res.records
    total.value = pageTotal(res.total)
  } finally {
    loading.value = false
  }
}

function openDetail(row: TaskVO) {
  if (!row.processInstanceId) return
  detailInstanceId.value = row.processInstanceId
  detailVisible.value = true
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
    </el-tabs>

    <el-table v-loading="loading" :data="tableData" border stripe class="data-table">
      <el-table-column label="任务" min-width="100" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.title || row.name || '-' }}
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
      <el-table-column label="操作" :width="activeTab === 'todo' ? 220 : 80" align="center">
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
                v-if="isMyTask(row as TaskVO)"
                link
                type="info"
                @click="handleUnclaim(row as TaskVO)"
              >
                取消认领
              </el-button>
              <el-button
                v-if="isMyTask(row as TaskVO) || needsClaim(row as TaskVO)"
                link
                type="success"
                @click="openHandle(row as TaskVO, 'complete')"
              >
                办理
              </el-button>
              <el-button
                v-if="isMyTask(row as TaskVO) || needsClaim(row as TaskVO)"
                link
                type="warning"
                @click="openHandle(row as TaskVO, 'reject')"
              >
                驳回
              </el-button>
              <el-button
                v-if="isMyTask(row as TaskVO)"
                link
                type="primary"
                @click="openHandle(row as TaskVO, 'transfer')"
              >
                转办
              </el-button>
            </template>
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
</style>
