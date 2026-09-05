<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { fetchUsersByRole } from '@/api/workflow/identity'
import type { WorkflowUserVO } from '@/types/workflow'

const model = defineModel<string[]>({ default: () => [] })

const props = withDefaults(
  defineProps<{
    /** 单角色；与 roleCodes 二选一 */
    roleCode?: string
    /** 多角色合并（如抄送：审批人+员工） */
    roleCodes?: string[]
    placeholder?: string
  }>(),
  {
    roleCode: 'APPROVER',
    placeholder: '选择抄送人（可多选）',
  },
)

const options = ref<WorkflowUserVO[]>([])

async function loadUsers() {
  try {
    const codes =
      props.roleCodes && props.roleCodes.length > 0
        ? props.roleCodes
        : [props.roleCode || 'APPROVER']
    const lists = await Promise.all(codes.map((c) => fetchUsersByRole(c)))
    const map = new Map<string, WorkflowUserVO>()
    for (const list of lists) {
      for (const u of list) {
        if (u?.userId) map.set(String(u.userId), u)
      }
    }
    options.value = Array.from(map.values())
  } catch {
    options.value = []
  }
}

watch(
  () => [props.roleCode, props.roleCodes] as const,
  () => {
    loadUsers()
  },
  { deep: true },
)

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <el-select
    v-model="model"
    multiple
    filterable
    collapse-tags
    collapse-tags-tooltip
    :placeholder="placeholder"
    style="width: 100%"
  >
    <el-option
      v-for="u in options"
      :key="u.userId"
      :label="u.nickname ? `${u.nickname} (${u.username})` : u.username"
      :value="u.userId"
    />
  </el-select>
</template>
