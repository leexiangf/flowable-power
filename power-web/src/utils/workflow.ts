/** 格式化后端日期字符串 */
export function formatDateTime(value?: string | null): string {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 分页 total 转 number */
export function pageTotal(total: number | string | undefined): number {
  return Number(total ?? 0)
}

/** 流程类型展示名（与「发起业务」选项对齐；勿与发起标题混淆） */
const PROCESS_TYPE_LABELS: Record<string, string> = {
  expense: '费用报销',
  'countersign-or': '并行或签',
  'countersign-seq': '串行会签',
  leave: '请假审批',
}

export function processTypeLabel(
  key?: string | null,
  definitionName?: string | null,
): string {
  if (key && PROCESS_TYPE_LABELS[key]) return PROCESS_TYPE_LABELS[key]
  if (definitionName) return definitionName
  return key || '-'
}

/** 新建模型时的 BPMN 模板（轻量编辑器默认内容） */
export const DEFAULT_BPMN_TEMPLATE = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://www.power.workflow/model">
  <process id="newProcess" name="新建流程" isExecutable="true">
    <startEvent id="startEvent" name="开始"/>
    <userTask id="approveTask" name="审批" flowable:candidateGroups="APPROVER"/>
    <endEvent id="endEvent" name="结束"/>
    <sequenceFlow id="flow1" sourceRef="startEvent" targetRef="approveTask"/>
    <sequenceFlow id="flow2" sourceRef="approveTask" targetRef="endEvent"/>
  </process>
</definitions>`
