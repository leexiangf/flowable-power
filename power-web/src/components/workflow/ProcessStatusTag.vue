<script setup lang="ts">
import { computed } from 'vue'
import type { ProcessInstanceVO } from '@/types/workflow'
import { LEAVE_STATUS_LABEL } from '@/types/workflow'

const props = defineProps<{
  /** 流程实例 */
  instance?: Pick<ProcessInstanceVO, 'ended' | 'suspended'>
  /** 请假等业务状态码 */
  leaveStatus?: number
}>()

const tag = computed(() => {
  if (props.leaveStatus != null) {
    const label = LEAVE_STATUS_LABEL[props.leaveStatus] || String(props.leaveStatus)
    const type =
      props.leaveStatus === 2
        ? 'success'
        : props.leaveStatus === 1
          ? 'warning'
          : props.leaveStatus === 3
            ? 'danger'
            : 'info'
    return { label, type: type as 'success' | 'warning' | 'danger' | 'info' }
  }
  if (!props.instance) {
    return { label: '-', type: 'info' as const }
  }
  if (props.instance.ended) {
    return { label: '已结束', type: 'info' as const }
  }
  if (props.instance.suspended) {
    return { label: '已挂起', type: 'warning' as const }
  }
  return { label: '运行中', type: 'success' as const }
})
</script>

<template>
  <el-tag :type="tag.type" size="small">{{ tag.label }}</el-tag>
</template>
