<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { completeTask, rejectTask, transferTask } from '@/api/workflow/task'
import { fetchUsersByRole } from '@/api/workflow/identity'
import type { TaskVO, WorkflowUserVO } from '@/types/workflow'

export type TaskHandleMode = 'complete' | 'reject' | 'transfer'

const props = defineProps<{
  task?: TaskVO | null
  mode?: TaskHandleMode
}>()

const emit = defineEmits<{
  success: []
}>()

const visible = defineModel<boolean>({ default: false })

const submitting = ref(false)
const formRef = ref<FormInstance>()
const userOptions = ref<WorkflowUserVO[]>([])

const form = reactive({
  comment: '',
  targetUserId: '',
})

const rules = computed<FormRules>(() => {
  if (props.mode === 'transfer') {
    return {
      targetUserId: [{ required: true, message: '请选择转办人', trigger: 'change' }],
    }
  }
  return {}
})

const title = computed(() => {
  const map: Record<TaskHandleMode, string> = {
    complete: '办理任务',
    reject: '驳回任务',
    transfer: '转办任务',
  }
  return map[props.mode || 'complete']
})

function resetForm() {
  form.comment = ''
  form.targetUserId = ''
  formRef.value?.clearValidate()
}

async function loadUsers() {
  if (props.mode !== 'transfer') return
  try {
    userOptions.value = await fetchUsersByRole('APPROVER')
  } catch {
    userOptions.value = []
  }
}

async function submit() {
  if (!props.task?.id) return
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (props.mode === 'complete') {
      await completeTask(props.task.id, { comment: form.comment.trim() || undefined })
      ElMessage.success('任务已办理')
    } else if (props.mode === 'reject') {
      await rejectTask(props.task.id, { comment: form.comment.trim() || undefined })
      ElMessage.success('任务已驳回')
    } else if (props.mode === 'transfer') {
      await transferTask(props.task.id, {
        targetUserId: form.targetUserId,
        comment: form.comment.trim() || undefined,
      })
      ElMessage.success('任务已转办')
    }
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}

watch(
  () => [visible.value, props.mode] as const,
  ([open, mode]) => {
    if (open) {
      resetForm()
      if (mode === 'transfer') {
        loadUsers()
      }
    }
  },
)
</script>

<template>
  <el-dialog v-model="visible" :title="title" width="440px" destroy-on-close>
    <div v-if="task" class="task-brief">
      <span>{{ task.title || task.name }}</span>
      <span class="task-id">#{{ task.id }}</span>
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="72px">
      <el-form-item v-if="mode === 'transfer'" label="转办给" prop="targetUserId">
        <el-select v-model="form.targetUserId" placeholder="选择用户" filterable style="width: 100%">
          <el-option
            v-for="u in userOptions"
            :key="u.userId"
            :label="u.nickname ? `${u.nickname} (${u.username})` : u.username"
            :value="u.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="mode === 'complete' ? '审批意见' : '说明'">
        <el-input v-model="form.comment" type="textarea" :rows="3" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.task-brief {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 10px;
  font-size: 12px;
  background: var(--el-fill-color-light);
  border-radius: var(--power-radius);

  .task-id {
    flex-shrink: 0;
    color: var(--power-text-muted);
    font-size: 11px;
  }
}
</style>
