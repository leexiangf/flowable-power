<script setup lang="ts">
import { useRoute } from 'vue-router'
import PageCard from '@/components/PageCard.vue'

const route = useRoute()
const title = (route.meta.title as string) || '页面建设中'
</script>

<template>
  <PageCard :title="title" description="框架已接入，业务页面待开发。">
    <el-empty description="该页面为占位页，后续可在此对接后端 API。" />
  </PageCard>
</template>
