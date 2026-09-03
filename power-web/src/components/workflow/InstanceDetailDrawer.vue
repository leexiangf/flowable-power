<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import {
  cancelProcessInstance,
  fetchInstanceDetail,
  fetchInstanceDiagram,
  fetchInstanceTimeline,
} from '@/api/workflow/instance'
import ProcessStatusTag from '@/components/workflow/ProcessStatusTag.vue'
import type { ActivityTraceVO, ProcessInstanceVO } from '@/types/workflow'
import { formatDateTime } from '@/utils/workflow'
import { useAuthStore } from '@/stores/auth'
import { hasPerm } from '@/utils/permission'

const auth = useAuthStore()

const props = defineProps<{
  processInstanceId?: string | null
}>()

const emit = defineEmits<{
  refreshed: []
}>()

const visible = defineModel<boolean>({ default: false })

const loading = ref(false)
const detail = ref<ProcessInstanceVO | null>(null)
const timeline = ref<ActivityTraceVO[]>([])
const diagramUrl = ref('')
const activeTab = ref('info')

const canCancel = computed(() => {
  if (!detail.value || detail.value.ended) return false
  if (!hasPerm('workflow:instance:list')) return false
  return detail.value.startUserId === auth.user?.userId
})

async function loadDetail() {
  const id = props.processInstanceId
  if (!id) return
  loading.value = true
  try {
    detail.value = await fetchInstanceDetail(id)
    timeline.value = await fetchInstanceTimeline(id)
    if (diagramUrl.value) {
      URL.revokeObjectURL(diagramUrl.value)
      diagramUrl.value = ''
    }
    const blob = await fetchInstanceDiagram(id)
    if (blob) {
      diagramUrl.value = URL.createObjectURL(blob)
    }
  } finally {
    loading.value = false
  }
}

async function handleCancel() {
  if (!props.processInstanceId || !detail.value) return
  await ElMessageBox.confirm('确认撤销该流程实例？', '提示', { type: 'warning' })
  await cancelProcessInstance(props.processInstanceId, { reason: '申请人撤销' })
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
})
</script>

<template>
  <el-drawer
    v-model="visible"
    title="流程实例详情"
    size="520px"
    destroy-on-close
    class="instance-drawer"
  >
    <div v-loading="loading">
      <template v-if="detail">
        <div class="drawer-head">
          <div class="drawer-title">{{ detail.title || detail.processDefinitionName || detail.id }}</div>
          <ProcessStatusTag :instance="detail" />
        </div>

        <el-tabs v-model="activeTab" class="drawer-tabs">
          <el-tab-pane label="基本信息" name="info">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="实例 ID">{{ detail.id }}</el-descriptions-item>
              <el-descriptions-item label="流程 Key">{{ detail.processDefinitionKey || '-' }}</el-descriptions-item>
              <el-descriptions-item label="流程名称">{{ detail.processDefinitionName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="业务主键">{{ detail.businessKey || '-' }}</el-descriptions-item>
              <el-descriptions-item label="发起人">{{ detail.startUserName || detail.startUserId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="开始时间">{{ formatDateTime(detail.startTime) }}</el-descriptions-item>
              <el-descriptions-item label="结束时间">{{ formatDateTime(detail.endTime) }}</el-descriptions-item>
            </el-descriptions>
            <div v-if="canCancel" class="drawer-actions">
              <el-button type="warning" size="small" @click="handleCancel">撤销流程</el-button>
            </div>
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
                <div v-if="item.assignee" class="timeline-meta">办理人：{{ item.assignee }}</div>
                <div v-if="item.comment" class="timeline-meta">意见：{{ item.comment }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无流转记录" />
          </el-tab-pane>

          <el-tab-pane label="流程图" name="diagram">
            <img v-if="diagramUrl" :src="diagramUrl" alt="流程图" class="diagram-img" />
            <el-empty v-else description="暂无流程图" />
          </el-tab-pane>
        </el-tabs>
      </template>
    </div>
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
  max-width: 100%;
  border: 1px solid var(--power-border-light);
  border-radius: var(--power-radius);
}
</style>
