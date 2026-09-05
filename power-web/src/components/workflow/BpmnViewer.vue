<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import NavigatedViewer from 'bpmn-js/lib/NavigatedViewer'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css'

const props = defineProps<{
  xml?: string
  activeIds?: string[]
  finishedIds?: string[]
}>()

const emit = defineEmits<{
  failed: []
}>()

const containerRef = ref<HTMLDivElement | null>(null)
const error = ref(false)
let viewer: NavigatedViewer | null = null

function destroyViewer() {
  if (viewer) {
    viewer.destroy()
    viewer = null
  }
}

function applyHighlight() {
  if (!viewer) return
  const canvas = viewer.get('canvas') as {
    addMarker: (id: string, cls: string) => void
  }
  const elementRegistry = viewer.get('elementRegistry') as {
    get: (id: string) => unknown
  }

  const mark = (ids: string[] | undefined, cls: string) => {
    for (const id of ids || []) {
      if (!id || !elementRegistry.get(id)) continue
      try {
        canvas.addMarker(id, cls)
      } catch {
        // ignore
      }
    }
  }

  mark(props.finishedIds, 'highlight-finished')
  mark(props.activeIds, 'highlight-active')
}

async function fitView() {
  if (!viewer) return
  await nextTick()
  // 抽屉/Tab 从隐藏到显示时需 resized，否则画布空白
  const canvas = viewer.get('canvas') as {
    resized: () => void
    zoom: (type: string) => void
  }
  try {
    canvas.resized()
    canvas.zoom('fit-viewport')
  } catch {
    // ignore
  }
}

async function render() {
  error.value = false
  await nextTick()
  if (!containerRef.value || !props.xml?.trim()) {
    destroyViewer()
    return
  }
  destroyViewer()
  viewer = new NavigatedViewer({ container: containerRef.value })
  try {
    await viewer.importXML(props.xml)
    applyHighlight()
    // 等布局稳定后再适配视口（双 rAF：Tab 切过来时更稳）
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        fitView()
      })
    })
  } catch {
    destroyViewer()
    error.value = true
    emit('failed')
  }
}

watch(
  () => [props.xml, props.activeIds, props.finishedIds] as const,
  () => {
    render()
  },
  { deep: true, immediate: true },
)

onBeforeUnmount(() => {
  destroyViewer()
})

defineExpose({ fitView })
</script>

<template>
  <div ref="containerRef" class="bpmn-viewer" />
</template>

<style scoped lang="scss">
.bpmn-viewer {
  width: 100%;
  min-height: 360px;
  height: 360px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--power-radius);
  background: #fafafa;
}

:deep(.highlight-active:not(.djs-connection) .djs-visual > :nth-child(1)) {
  stroke: #409eff !important;
  stroke-width: 3px !important;
  fill: rgba(64, 158, 255, 0.15) !important;
}

:deep(.highlight-finished:not(.djs-connection) .djs-visual > :nth-child(1)) {
  stroke: #67c23a !important;
  stroke-width: 2px !important;
  fill: rgba(103, 194, 58, 0.12) !important;
}

:deep(.highlight-active.djs-connection .djs-visual > :nth-child(1)) {
  stroke: #409eff !important;
  stroke-width: 2px !important;
}

:deep(.highlight-finished.djs-connection .djs-visual > :nth-child(1)) {
  stroke: #67c23a !important;
}
</style>
