<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  activateProcessInstance,
  cancelProcessInstance,
  fetchInstanceActiveTasks,
  fetchInstanceDetail,
  fetchInstanceDiagram,
  fetchInstanceHighlight,
  fetchInstanceTimeline,
  suspendProcessInstance,
  terminateProcessInstance,
  urgeProcessInstance,
} from '@/api/workflow/instance'
import BpmnViewer from '@/components/workflow/BpmnViewer.vue'
import ProcessStatusTag from '@/components/workflow/ProcessStatusTag.vue'
import TaskHandleDialog, { type TaskHandleMode } from '@/components/workflow/TaskHandleDialog.vue'
import type { ActivityTraceVO, ProcessHighlightVO, ProcessInstanceVO, TaskVO } from '@/types/workflow'
import { formatDateTime, processTypeLabel } from '@/utils/workflow'
import { useAuthStore } from '@/stores/auth'
import { hasPerm } from '@/utils/permission'

const auth = useAuthStore()

const props = defineProps<{
  processInstanceId?: string | null
  readonly?: boolean
}>()

const emit = defineEmits<{
  refreshed: []
}>()

const visible = defineModel<boolean>({ default: false })

const loading = ref(false)
const detail = ref<ProcessInstanceVO | null>(null)
const timeline = ref<ActivityTraceVO[]>([])
const activeTasks = ref<TaskVO[]>([])
const highlight = ref<ProcessHighlightVO | null>(null)
const diagramUrl = ref('')
const diagramViewerFailed = ref(false)
const activeTab = ref('info')
const bpmnViewerRef = ref<{ fitView?: () => void } | null>(null)

const handleVisible = ref(false)
const handleMode = ref<TaskHandleMode>('assign')
const handleTask = ref<TaskVO | null>(null)

/** 业务相关变量（隐藏引擎/内部字段） */
const HIDDEN_VAR_KEYS = new Set([
  'startUserId',
  'startUsername',
  'title',
  'approved',
  'assignee',
  'managerUserId',
  'countersignUserIds',
  'nrOfInstances',
  'nrOfActiveInstances',
  'nrOfCompletedInstances',
  'loopCounter',
])

const businessVars = computed(() => {
  const vars = detail.value?.variables
  if (!vars || typeof vars !== 'object') return [] as Array<{ key: string; label: string; value: string }>
  const labelMap: Record<string, string> = {
    amount: '金额',
    reason: '事由',
    managerUserName: '部门经理',
    countersignUserNames: '会签人',
    businessType: '业务类型',
    days: '天数',
    startDate: '开始日期',
    endDate: '结束日期',
  }
  return Object.entries(vars)
    .filter(([k]) => !HIDDEN_VAR_KEYS.has(k) && !k.startsWith('addSign'))
    .map(([key, raw]) => {
      let value: string
      if (raw == null) value = '-'
      else if (Array.isArray(raw)) value = raw.map(String).join('、')
      else if (typeof raw === 'object') value = JSON.stringify(raw)
      else value = String(raw)
      return { key, label: labelMap[key] || key, value }
    })
})

const canCancel = computed(() => {
  if (props.readonly || !detail.value || detail.value.ended) return false
  if (!hasPerm('workflow:instance:list')) return false
  return detail.value.startUserId === auth.user?.userId
})

const canUrge = computed(() => {
  if (!detail.value || detail.value.ended) return false
  return hasPerm('workflow:task:urge')
})

const canTerminate = computed(() => {
  if (!detail.value || detail.value.ended) return false
  return hasPerm('workflow:instance:terminate')
})

const canSuspendToggle = computed(() => {
  if (!detail.value || detail.value.ended) return false
  return hasPerm('workflow:instance:suspend')
})

/** 有办理权限时可尝试重新指派（后端再鉴权：办理人/设计办理人/发起人/监控员） */
const canReassign = computed(() => {
  if (props.readonly || !detail.value || detail.value.ended) return false
  return hasPerm('workflow:task:handle')
})

const showBpmnViewer = computed(
  () =>
    activeTab.value === 'diagram' &&
    !!highlight.value?.bpmnXml &&
    !diagramViewerFailed.value,
)

async function loadDiagramPng(id: string) {
  if (diagramUrl.value) {
    URL.revokeObjectURL(diagramUrl.value)
    diagramUrl.value = ''
  }
  const blob = await fetchInstanceDiagram(id)
  if (blob) {
    diagramUrl.value = URL.createObjectURL(blob)
  }
}

async function loadDetail() {
  const id = props.processInstanceId
  if (!id) return
  loading.value = true
  diagramViewerFailed.value = false
  try {
    detail.value = await fetchInstanceDetail(id)
    timeline.value = await fetchInstanceTimeline(id)
    try {
      activeTasks.value = detail.value?.ended ? [] : await fetchInstanceActiveTasks(id)
    } catch {
      activeTasks.value = []
    }
    try {
      highlight.value = await fetchInstanceHighlight(id)
    } catch {
      highlight.value = null
    }
    // 始终拉 PNG 作兜底；Viewer 失败或 XML 缺失时使用
    await loadDiagramPng(id)
  } finally {
    loading.value = false
  }
}

function openAssign(task: TaskVO) {
  handleTask.value = task
  handleMode.value = 'assign'
  handleVisible.value = true
}

async function onAssignSuccess() {
  await loadDetail()
  emit('refreshed')
}

async function handleCancel() {
  if (!props.processInstanceId || !detail.value) return
  await ElMessageBox.confirm('确认撤销该流程实例？', '提示', { type: 'warning' })
  await cancelProcessInstance(props.processInstanceId, { reason: '申请人撤销' })
  await loadDetail()
  emit('refreshed')
}

async function handleUrge() {
  if (!props.processInstanceId) return
  const { value } = await ElMessageBox.prompt('请输入催办说明', '催办', {
    inputValue: '请尽快处理',
  })
  await urgeProcessInstance(props.processInstanceId, { comment: value || '请尽快处理' })
  ElMessage.success('已发送催办')
}

async function handleTerminate() {
  if (!props.processInstanceId) return
  await ElMessageBox.confirm('确认强制终止该流程？此操作不可恢复。', '危险操作', {
    type: 'warning',
  })
  await terminateProcessInstance(props.processInstanceId, { reason: '管理员强制终止' })
  await loadDetail()
  emit('refreshed')
}

async function handleSuspendToggle() {
  if (!props.processInstanceId || !detail.value) return
  if (detail.value.suspended) {
    await activateProcessInstance(props.processInstanceId)
    ElMessage.success('实例已激活')
  } else {
    await suspendProcessInstance(props.processInstanceId)
    ElMessage.success('实例已挂起')
  }
  await loadDetail()
  emit('refreshed')
}

watch(
  () => [visible.value, props.processInstanceId] as const,
  ([open, id]) => {
    if (open && id) {
      activeTab.value = 'info'
      loadDetail()
    }
  },
)

watch(visible, (open) => {
  if (!open && diagramUrl.value) {
    URL.revokeObjectURL(diagramUrl.value)
    diagramUrl.value = ''
  }
  if (!open) {
    highlight.value = null
    activeTasks.value = []
  }
})

// 切到流程图 Tab 时重新适配视口（隐藏 Tab 下初次渲染常为空白）
watch(activeTab, async (tab) => {
  if (tab !== 'diagram') return
  await nextTick()
  bpmnViewerRef.value?.fitView?.()
})
</script>

<template>
  <el-drawer
    v-model="visible"
    title="流程实例详情"
    size="640px"
    destroy-on-close
    class="instance-drawer"
  >
    <div v-loading="loading">
      <template v-if="detail">
        <div class="drawer-head">
          <div class="drawer-title">{{ detail.title?.trim() || processTypeLabel(detail.processDefinitionKey, detail.processDefinitionName) }}</div>
          <ProcessStatusTag :instance="detail" />
        </div>

        <el-tabs v-model="activeTab" class="drawer-tabs">
          <el-tab-pane label="基本信息" name="info">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="实例 ID">{{ detail.id }}</el-descriptions-item>
              <el-descriptions-item label="标题">{{ detail.title?.trim() || '-' }}</el-descriptions-item>
              <el-descriptions-item label="类型">
                {{ processTypeLabel(detail.processDefinitionKey, detail.processDefinitionName) }}
              </el-descriptions-item>
              <el-descriptions-item label="流程 Key">{{ detail.processDefinitionKey || '-' }}</el-descriptions-item>
              <el-descriptions-item label="业务主键">{{ detail.businessKey || '-' }}</el-descriptions-item>
              <el-descriptions-item label="发起人">{{ detail.startUserName || detail.startUserId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="开始时间">{{ formatDateTime(detail.startTime) }}</el-descriptions-item>
              <el-descriptions-item label="结束时间">{{ formatDateTime(detail.endTime) }}</el-descriptions-item>
              <el-descriptions-item
                v-for="item in businessVars"
                :key="item.key"
                :label="item.label"
              >
                {{ item.value }}
              </el-descriptions-item>
            </el-descriptions>
            <div class="drawer-actions">
              <el-button v-if="canCancel" type="warning" size="small" @click="handleCancel">
                撤销流程
              </el-button>
              <el-button v-if="canUrge" type="primary" size="small" @click="handleUrge">
                催办
              </el-button>
              <el-button
                v-if="canSuspendToggle"
                size="small"
                @click="handleSuspendToggle"
              >
                {{ detail.suspended ? '激活实例' : '挂起实例' }}
              </el-button>
              <el-button v-if="canTerminate" type="danger" size="small" @click="handleTerminate">
                强制终止
              </el-button>
            </div>
          </el-tab-pane>

          <el-tab-pane label="当前任务" name="tasks">
            <el-table v-if="activeTasks.length" :data="activeTasks" size="small" border>
              <el-table-column prop="name" label="任务" min-width="120" show-overflow-tooltip />
              <el-table-column label="办理人" min-width="100" show-overflow-tooltip>
                <template #default="{ row }">
                  <span v-if="row.assigneeName || row.assignee">
                    {{ row.assigneeName || row.assignee }}
                  </span>
                  <el-tag v-else type="danger" size="small">无人认领</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="创建时间" width="150">
                <template #default="{ row }">
                  {{ formatDateTime(row.createTime) }}
                </template>
              </el-table-column>
              <el-table-column v-if="canReassign" label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openAssign(row as TaskVO)">
                    重新指派
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else :description="detail.ended ? '流程已结束' : '暂无活动任务'" />
          </el-tab-pane>

          <el-tab-pane label="流转记录" name="timeline">
            <el-timeline v-if="timeline.length">
              <el-timeline-item
                v-for="(item, idx) in timeline"
                :key="`${item.activityId}-${idx}`"
                :timestamp="formatDateTime(item.endTime || item.startTime)"
                placement="top"
              >
                <div class="timeline-title">{{ item.activityName || item.activityId }}</div>
                <div v-if="item.assigneeName || item.assignee" class="timeline-meta">
                  办理人：{{ item.assigneeName || item.assignee }}
                </div>
                <div v-if="item.comment" class="timeline-meta">意见：{{ item.comment }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无流转记录" />
          </el-tab-pane>

          <el-tab-pane label="流程图" name="diagram" lazy>
            <BpmnViewer
              v-if="showBpmnViewer"
              ref="bpmnViewerRef"
              :xml="highlight!.bpmnXml"
              :active-ids="highlight!.activeActivityIds"
              :finished-ids="highlight!.finishedActivityIds"
              @failed="diagramViewerFailed = true"
            />
            <img
              v-else-if="diagramUrl"
              :src="diagramUrl"
              alt="流程图"
              class="diagram-img"
            />
            <el-empty v-else description="暂无流程图" />
          </el-tab-pane>
        </el-tabs>
      </template>
    </div>

    <TaskHandleDialog
      v-model="handleVisible"
      :task="handleTask"
      :mode="handleMode"
      @success="onAssignSuccess"
    />
  </el-drawer>
</template>

<style scoped lang="scss">
.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}

.drawer-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--power-text);
  word-break: break-all;
}

.drawer-tabs {
  :deep(.el-tabs__item) {
    font-size: 12px;
  }
}

.drawer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.timeline-title {
  font-size: 12px;
  font-weight: 500;
}

.timeline-meta {
  margin-top: 2px;
  font-size: 11px;
  color: var(--power-text-muted);
}

.diagram-img {
  display: block;
  max-width: 100%;
  border: 1px solid var(--power-border-light);
  border-radius: var(--power-radius);
}
</style>
