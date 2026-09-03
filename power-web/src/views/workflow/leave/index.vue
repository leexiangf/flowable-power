<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import PageCard from '@/components/PageCard.vue'
import InstanceDetailDrawer from '@/components/workflow/InstanceDetailDrawer.vue'
import ProcessStatusTag from '@/components/workflow/ProcessStatusTag.vue'
import { createLeave, fetchLeaveDetail } from '@/api/workflow/leave'
import type { LeaveCreateRequest, LeaveVO } from '@/types/workflow'
import { formatDateTime } from '@/utils/workflow'

const route = useRoute()
const router = useRouter()

const submitting = ref(false)
const formRef = ref<FormInstance>()
const result = ref<LeaveVO | null>(null)
const detailLoading = ref(false)

const detailVisible = ref(false)

const form = reactive<LeaveCreateRequest>({
  days: 1,
  reason: '',
  startDate: '',
  endDate: '',
})

const rules: FormRules = {
  days: [{ required: true, message: '请输入请假天数', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入请假事由', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [
    { required: true, message: '请选择结束日期', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (form.startDate && value && value < form.startDate) {
          callback(new Error('结束日期不能早于开始日期'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
}

function resetForm() {
  form.days = 1
  form.reason = ''
  form.startDate = ''
  form.endDate = ''
  formRef.value?.clearValidate()
}

async function loadDetail(id: string) {
  detailLoading.value = true
  try {
    result.value = await fetchLeaveDetail(id)
  } finally {
    detailLoading.value = false
  }
}

async function submitForm() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const data = await createLeave({
      days: Number(form.days),
      reason: form.reason.trim(),
      startDate: form.startDate,
      endDate: form.endDate,
    })
    result.value = data
    ElMessage.success('请假申请已提交')
    resetForm()
    router.replace({ query: { id: data.id } })
  } finally {
    submitting.value = false
  }
}

function openInstance() {
  if (!result.value?.processInstanceId) return
  detailVisible.value = true
}

function goMyInstances() {
  router.push('/workflow/instance')
}

onMounted(() => {
  const id = route.query.id as string | undefined
  if (id) {
    loadDetail(id)
  }
})
</script>

<template>
  <PageCard title="请假管理">
    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <div class="panel">
          <div class="panel-title">提交申请</div>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="72px">
            <el-form-item label="请假天数" prop="days">
              <el-input-number v-model="form.days" :min="0.5" :step="0.5" :precision="1" />
            </el-form-item>
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker
                v-model="form.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker
                v-model="form.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="事由" prop="reason">
              <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请填写请假原因" />
            </el-form-item>
            <el-form-item>
              <el-button v-perm="'workflow:leave:apply'" type="primary" :loading="submitting" @click="submitForm">
                提交申请
              </el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>

      <el-col :xs="24" :md="12">
        <div v-loading="detailLoading" class="panel">
          <div class="panel-title">申请结果</div>
          <template v-if="result">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="单号">{{ result.id }}</el-descriptions-item>
              <el-descriptions-item label="申请人">{{ result.username || '-' }}</el-descriptions-item>
              <el-descriptions-item label="天数">{{ result.days }} 天</el-descriptions-item>
              <el-descriptions-item label="日期">
                {{ result.startDate }} ~ {{ result.endDate }}
              </el-descriptions-item>
              <el-descriptions-item label="事由">{{ result.reason }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <ProcessStatusTag :leave-status="result.status" />
              </el-descriptions-item>
              <el-descriptions-item label="提交时间">
                {{ formatDateTime(result.createTime) }}
              </el-descriptions-item>
            </el-descriptions>
            <div class="result-actions">
              <el-button
                v-if="result.processInstanceId"
                type="primary"
                size="small"
                @click="openInstance"
              >
                查看流程
              </el-button>
              <el-button size="small" @click="goMyInstances">我发起的流程</el-button>
            </div>
          </template>
          <el-empty v-else description="提交后将在此展示申请详情" />
        </div>
      </el-col>
    </el-row>

    <el-alert
      class="tip"
      type="info"
      :closable="false"
      show-icon
      title="暂无请假列表接口；历史申请可在「流程实例 → 我发起的」中按 leave 流程查看。"
    />

    <InstanceDetailDrawer
      v-model="detailVisible"
      :process-instance-id="result?.processInstanceId ?? null"
    />
  </PageCard>
</template>

<style scoped lang="scss">
.panel {
  padding: 12px;
  border: 1px solid var(--power-border-light);
  border-radius: var(--power-radius);
  background: var(--el-fill-color-blank);
  min-height: 280px;
}

.panel-title {
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--power-text);
}

.result-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.tip {
  margin-top: 12px;
}
</style>
