<template>
  <div class="page-card">
    <div v-if="title || description || $slots.extra" class="page-card__header">
      <div class="page-card__title-wrap">
        <h2 v-if="title" class="page-card__title">{{ title }}</h2>
        <p v-if="description" class="page-card__desc">{{ description }}</p>
      </div>
      <div v-if="$slots.extra" class="page-card__extra">
        <slot name="extra" />
      </div>
    </div>
    <div class="page-card__body">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
  description?: string
}>()
</script>

<style scoped lang="scss">
.page-card {
  background: var(--power-surface);
  border: 1px solid var(--power-border);
  border-radius: var(--power-radius-lg);
  padding: 14px;

  &__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 10px;
    margin-bottom: 12px;
    padding-bottom: 10px;
    border-bottom: 1px solid var(--power-border-light);
  }

  &__title {
    margin: 0;
    font-size: 13px;
    font-weight: 600;
    color: var(--power-text);
    line-height: 1.4;
  }

  &__desc {
    margin: 3px 0 0;
    font-size: 11px;
    color: var(--power-text-muted);
    line-height: 1.5;
  }

  &__extra {
    flex-shrink: 0;
  }

  &__body {
    min-width: 0;

    :deep(.el-form--inline .el-form-item) {
      margin-right: 12px;
    }
  }
}
</style>
