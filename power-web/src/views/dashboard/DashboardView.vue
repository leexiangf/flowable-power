<script setup lang="ts">
import { computed } from 'vue'
import PageCard from '@/components/PageCard.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const roleText = computed(() => auth.user?.roles?.join(', ') || '-')
const permCount = computed(() => auth.user?.authorities?.length ?? 0)
</script>

<template>
  <PageCard title="首页">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="用户">
        {{ auth.displayName }}（{{ auth.user?.username }}）
      </el-descriptions-item>
      <el-descriptions-item label="角色">
        {{ roleText }}
      </el-descriptions-item>
      <el-descriptions-item label="权限码数量">
        {{ permCount }}
      </el-descriptions-item>
    </el-descriptions>

    <el-alert
      class="tip"
      type="info"
      :closable="false"
      show-icon
      title="开发提示"
      description="接口统一走网关 8080；开发时 Vite 已配置 /auth、/system、/workflow 代理。动态路由来自 GET /auth/menus/tree。"
    />
  </PageCard>
</template>

<style scoped lang="scss">
.tip {
  margin-top: 14px;
}
</style>
