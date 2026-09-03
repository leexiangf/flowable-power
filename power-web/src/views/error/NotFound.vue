<template>
  <PageCard title="404" description="页面不存在或尚未配置路由。">
    <el-result icon="warning" title="404" sub-title="您访问的页面不存在">
      <template #extra>
        <el-button type="primary" @click="$router.push('/dashboard')">返回首页</el-button>
      </template>
    </el-result>
  </PageCard>
</template>

<script setup lang="ts">
import PageCard from '@/components/PageCard.vue'
</script>
