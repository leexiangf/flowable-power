<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { startCountersignOr, startCountersignSeq } from '@/api/workflow/countersign'
import { startExpense } from '@/api/workflow/expense'
import { fetchUsersByRole } from '@/api/workflow/identity'
import type { WorkflowUserVO } from '@/types/workflow'

export type BizProcessKind = 'expense' | 'countersign-or' | 'countersign-seq'

const props = defineProps<{
  /** 打开时预选业务类型 */
  initialKind?: BizProcessKind | null
}>()

const visible = defineModel<boolean>({ default: false })

const emit = defineEmits<{
  success: []
}>()

const submitting = ref(false)
const formRef = ref<FormInstance>()
const userOptions = ref<WorkflowUserVO[]>([])

const form = reactive({
  kind: 'countersign-or' as BizProcessKind,
  title: '',
  managerUserId: '',
  countersignUserIds: [] as string[],
  amount: '',
  reason: '',
})

const kindHint = computed(() => {
  const map: Record<BizProcessKind, string> = {
    expense: '先选部门经理，再选至少 1 名会签人（并行会签）。',
    'countersign-or': '选至少 2 名办理人：并行或签，一人通过即可结束会签。',
    'countersign-seq': '选至少 2 名办理人：按选择顺序串行审批，一票否决。',
  }
  return map[form.kind]
})

const rules = computed<FormRules>(() => {
  const base: FormRules = {
    title: [{ required: true, message: '请填写标题', trigger: 'blur' }],
    countersignUserIds: [
      {
        type: 'array',
        required: true,
        min: form.kind === 'expense' ? 1 : 2,
        message: form.kind === 'expense' ? '请选择会签人' : '请至少选择 2 名办理人',
        trigger: 'change',
      },
    ],
  }
  if (form.kind === 'expense') {
    base.managerUserId = [{ required: true, message: '请选择部门经理', trigger: 'change' }]
  }
  return base
})

function resetForm() {
  const kind = props.initialKind || 'countersign-or'
  form.kind = kind
  form.title = ''
  form.managerUserId = ''
  form.countersignUserIds = []
  form.amount = ''
  form.reason = ''
  formRef.value?.clearValidate()
}

async function loadUsers() {
  try {
    // 会签 / 部门经理仅可选审批人，不含普通员工
    userOptions.value = await fetchUsersByRole('APPROVER')
  } catch {
    userOptions.value = []
  }
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const title = form.title.trim()
    if (form.kind === 'expense') {
      await startExpense({
        managerUserId: form.managerUserId,
        countersignUserIds: form.countersignUserIds,
        title,
        amount: form.amount.trim() || undefined,
        reason: form.reason.trim() || undefined,
      })
    } else if (form.kind === 'countersign-or') {
      await startCountersignOr({
        countersignUserIds: form.countersignUserIds,
        title,
      })
    } else {
      await startCountersignSeq({
        countersignUserIds: form.countersignUserIds,
        title,
      })
    }
    ElMessage.success('流程已发起，请到「任务中心」办理')
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}

watch(visible, (open) => {
  if (open) {
    resetForm()
    loadUsers()
  }
})

watch(
  () => form.kind,
  () => {
    form.managerUserId = ''
    form.countersignUserIds = []
    formRef.value?.clearValidate()
  },
)
</script>

<template>
  <el-dialog v-model="visible" title="发起业务" width="520px" destroy-on-close>
    <p class="biz-hint">{{ kindHint }}</p>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
      <el-form-item label="业务类型" required>
        <el-radio-group v-model="form.kind">
          <el-radio-button value="countersign-or">并行或签</el-radio-button>
          <el-radio-button value="countersign-seq">串行会签</el-radio-button>
          <el-radio-button value="expense">费用报销</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" placeholder="请填写本次申请标题" maxlength="100" show-word-limit />
      </el-form-item>
      <el-form-item v-if="form.kind === 'expense'" label="部门经理" prop="managerUserId">
        <el-select
          v-model="form.managerUserId"
          placeholder="选择经理"
          filterable
          style="width: 100%"
        >
          <el-option
            v-for="u in userOptions"
            :key="u.userId"
            :label="u.nickname ? `${u.nickname} (${u.username})` : u.username"
            :value="u.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        :label="form.kind === 'expense' ? '会签人' : '办理人'"
        prop="countersignUserIds"
      >
        <el-select
          v-model="form.countersignUserIds"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          :placeholder="form.kind === 'expense' ? '选择会签人' : '至少选 2 人'"
          style="width: 100%"
        >
          <el-option
            v-for="u in userOptions"
            :key="u.userId"
            :label="u.nickname ? `${u.nickname} (${u.username})` : u.username"
            :value="u.userId"
          />
        </el-select>
      </el-form-item>
      <template v-if="form.kind === 'expense'">
        <el-form-item label="金额">
          <el-input v-model="form.amount" placeholder="选填，如 1200" />
        </el-form-item>
        <el-form-item label="事由">
          <el-input v-model="form.reason" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </template>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">发起</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.biz-hint {
  margin: 0 0 12px;
  padding: 8px 10px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--power-text-muted);
  background: var(--el-fill-color-light);
  border-radius: var(--power-radius);
}
</style>
