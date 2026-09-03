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

/** 新建模型时的 BPMN 模板（轻量编辑器默认内容） */
export const DEFAULT_BPMN_TEMPLATE = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://www.power.workflow/demo">
  <process id="demoProcess" name="示例流程" isExecutable="true">
    <startEvent id="startEvent" name="开始"/>
    <userTask id="approveTask" name="审批" flowable:candidateGroups="APPROVER"/>
    <endEvent id="endEvent" name="结束"/>
    <sequenceFlow id="flow1" sourceRef="startEvent" targetRef="approveTask"/>
    <sequenceFlow id="flow2" sourceRef="approveTask" targetRef="endEvent"/>
  </process>
</definitions>`
