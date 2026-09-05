<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css'

const props = defineProps<{
  xml?: string
  readonly?: boolean
}>()

const xmlModel = defineModel<string>({ default: '' })

const containerRef = ref<HTMLDivElement | null>(null)
let modeler: BpmnModeler | null = null

async function initModeler() {
  if (!containerRef.value) return
  destroyModeler()
  modeler = new BpmnModeler({
    container: containerRef.value,
    keyboard: { bindTo: document },
  })
  const initial = props.xml || xmlModel.value
  if (initial?.trim()) {
    try {
      await modeler.importXML(initial)
    } catch {
      await modeler.createDiagram()
    }
  } else {
    await modeler.createDiagram()
  }
}

function destroyModeler() {
  if (modeler) {
    modeler.destroy()
    modeler = null
  }
}

async function exportXml() {
  if (!modeler) return xmlModel.value
  const { xml } = await modeler.saveXML({ format: true })
  return xml || ''
}

defineExpose({ exportXml })

watch(
  () => props.xml,
  async (val) => {
    if (!modeler || !val?.trim()) return
    try {
      await modeler.importXML(val)
    } catch {
      // ignore invalid external xml while editing
    }
  },
)

onMounted(() => {
  initModeler()
})

onBeforeUnmount(() => {
  destroyModeler()
})

watch(
  () => props.readonly,
  () => {
    initModeler()
  },
)

async function syncToModel() {
  xmlModel.value = await exportXml()
}
</script>

<template>
  <div class="bpmn-editor">
    <div ref="containerRef" class="bpmn-canvas" />
    <div v-if="!readonly" class="bpmn-toolbar">
      <el-button size="small" @click="syncToModel">同步 XML 到表单</el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.bpmn-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  min-height: 420px;
}

.bpmn-canvas {
  flex: 1;
  min-height: 380px;
  border: 1px solid var(--power-border-light);
  border-radius: var(--power-radius);
  background: #fff;
}

.bpmn-toolbar {
  display: flex;
  justify-content: flex-end;
}
</style>
