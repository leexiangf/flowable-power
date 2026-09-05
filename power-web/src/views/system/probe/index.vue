<script setup lang="ts">
import PageCard from '@/components/PageCard.vue'
</script>

<template>
  <PageCard title="系统探针" description="对接 GET /system/ping · Feign · Outbox">
    <el-empty description="可在 Swagger 调用 /system/ping、/system/feign-me、/system/outbox" />
  </PageCard>
</template>
