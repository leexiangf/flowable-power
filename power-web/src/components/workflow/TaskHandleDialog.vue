<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import UserMultiSelect from '@/components/workflow/UserMultiSelect.vue'
import {
  addSignTask,
  assignTask,
  completeTask,
  delegateTask,
  fetchRejectableNodes,
  rejectTask,
  transferTask,
} from '@/api/workflow/task'
import { fetchUsersByRole } from '@/api/workflow/identity'
import type { TaskVO, UserTaskNodeVO, WorkflowUserVO } from '@/types/workflow'

export type TaskHandleMode = 'complete' | 'reject' | 'transfer' | 'delegate' | 'addSign' | 'assign'

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
const rejectNodes = ref<UserTaskNodeVO[]>([])

const form = reactive({
  comment: '',
  targetUserId: '',
  ccUserIds: [] as string[],
  strategy: 'PREVIOUS',
  targetActivityId: '',
  addSignType: 'BEFORE' as 'BEFORE' | 'AFTER',
})

const rules = computed<FormRules>(() => {
  if (props.mode === 'transfer' || props.mode === 'delegate' || props.mode === 'addSign' || props.mode === 'assign') {
    return {
      targetUserId: [{ required: true, message: '请选择用户', trigger: 'change' }],
    }
  }
  if (props.mode === 'reject' && form.strategy === 'TO_NODE') {
    return {
      targetActivityId: [{ required: true, message: '请选择驳回节点', trigger: 'change' }],
    }
  }
  return {}
})

const title = computed(() => {
  const map: Record<TaskHandleMode, string> = {
    complete: props.task?.addSignMode === 'BEFORE' ? '归还前加签' : '办理任务',
    reject: '驳回任务',
    transfer: '转办任务',
    delegate: '委派任务',
    addSign: '加签',
    assign: '重新指派',
  }
  return map[props.mode || 'complete']
})

const showCc = computed(() => props.mode === 'complete' || props.mode === 'delegate')
const needUserPick = computed(
  () =>
    props.mode === 'transfer' ||
    props.mode === 'delegate' ||
    props.mode === 'addSign' ||
    props.mode === 'assign',
)

function resetForm() {
  form.comment = ''
  form.targetUserId = ''
  form.ccUserIds = []
  form.strategy = 'PREVIOUS'
  form.targetActivityId = ''
  form.addSignType = 'BEFORE'
  formRef.value?.clearValidate()
}

async function loadUsers() {
  if (!needUserPick.value) return
  try {
    // 转办 / 委派 / 加签 / 重新指派：仅审批人
    userOptions.value = await fetchUsersByRole('APPROVER')
  } catch {
    userOptions.value = []
  }
}

async function loadRejectNodes() {
  if (props.mode !== 'reject' || !props.task?.id) return
  try {
    rejectNodes.value = await fetchRejectableNodes(props.task.id)
  } catch {
    rejectNodes.value = []
  }
}

async function submit() {
  if (!props.task?.id) return
  await formRef.value?.validate()
  submitting.value = true
  try {
    const comment = form.comment.trim() || undefined
    const ccUserIds = form.ccUserIds.length ? form.ccUserIds : undefined
    if (props.mode === 'complete') {
      await completeTask(props.task.id, { comment, ccUserIds })
      ElMessage.success(props.task.addSignMode === 'BEFORE' ? '已归还任务' : '任务已办理')
    } else if (props.mode === 'reject') {
      await rejectTask(props.task.id, {
        comment,
        strategy: form.strategy,
        targetActivityId:
          form.strategy === 'TO_NODE' || form.strategy === 'PREVIOUS'
            ? form.targetActivityId || undefined
            : undefined,
      })
      ElMessage.success('任务已驳回')
    } else if (props.mode === 'transfer') {
      await transferTask(props.task.id, {
        targetUserId: form.targetUserId,
        comment,
      })
      ElMessage.success('任务已转办')
    } else if (props.mode === 'assign') {
      await assignTask(props.task.id, {
        targetUserId: form.targetUserId,
        comment,
      })
      ElMessage.success('已重新指派')
    } else if (props.mode === 'delegate') {
      await delegateTask(props.task.id, {
        targetUserId: form.targetUserId,
        comment,
        ccUserIds,
      })
      ElMessage.success('任务已委派')
    } else if (props.mode === 'addSign') {
      await addSignTask(props.task.id, {
        type: form.addSignType,
        targetUserId: form.targetUserId,
        comment,
      })
      ElMessage.success('已加签')
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
      if (mode === 'transfer' || mode === 'delegate' || mode === 'addSign' || mode === 'assign') {
        loadUsers()
      }
      if (mode === 'reject') {
        loadRejectNodes()
      }
    }
  },
)

watch(
  () => form.strategy,
  () => {
    form.targetActivityId = ''
    formRef.value?.clearValidate(['targetActivityId'])
  },
)
</script>

<template>
  <el-dialog v-model="visible" :title="title" width="520px" destroy-on-close>
    <div v-if="task" class="task-brief">
      <span>{{ task.title || task.name }}</span>
      <span class="task-id">#{{ task.id }}</span>
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
      <el-form-item v-if="mode === 'addSign'" label="加签类型">
        <el-radio-group v-model="form.addSignType">
          <el-radio value="BEFORE">前加签（办完归还）</el-radio>
          <el-radio value="AFTER">后加签（由加签人推进）</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item
        v-if="needUserPick"
        :label="
          mode === 'delegate'
            ? '委派给'
            : mode === 'addSign'
              ? '加签给'
              : mode === 'assign'
                ? '指派给'
                : '转办给'
        "
        prop="targetUserId"
      >
        <el-select v-model="form.targetUserId" placeholder="选择用户" filterable style="width: 100%">
          <el-option
            v-for="u in userOptions"
            :key="u.userId"
            :label="u.nickname ? `${u.nickname} (${u.username})` : u.username"
            :value="u.userId"
          />
        </el-select>
      </el-form-item>
      <template v-if="mode === 'reject'">
        <el-form-item label="驳回策略">
          <el-select v-model="form.strategy" style="width: 100%">
            <el-option label="退回上一节点" value="PREVIOUS" />
            <el-option label="退回指定节点" value="TO_NODE" />
            <el-option label="退回发起节点" value="TO_STARTER" />
            <el-option label="驳回并结束" value="TERMINATE" />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="form.strategy === 'TO_NODE' || form.strategy === 'PREVIOUS'"
          :label="form.strategy === 'TO_NODE' ? '目标节点' : '覆盖节点'"
          :prop="form.strategy === 'TO_NODE' ? 'targetActivityId' : undefined"
        >
          <el-select
            v-model="form.targetActivityId"
            :placeholder="form.strategy === 'TO_NODE' ? '选择节点' : '可选，覆盖自动探测'"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="n in rejectNodes"
              :key="n.activityId"
              :label="n.activityName ? `${n.activityName} (${n.activityId})` : n.activityId"
              :value="n.activityId"
            />
          </el-select>
        </el-form-item>
      </template>
      <el-form-item :label="mode === 'complete' ? '审批意见' : '说明'">
        <el-input v-model="form.comment" type="textarea" :rows="3" placeholder="选填" />
      </el-form-item>
      <el-form-item v-if="showCc" label="抄送人">
        <UserMultiSelect
          v-model="form.ccUserIds"
          :role-codes="['APPROVER', 'STAFF']"
          placeholder="可抄送审批人或同事"
        />
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
