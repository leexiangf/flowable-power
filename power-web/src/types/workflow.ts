import type { PageResult } from '@/types/system'

/** 分页查询（与后端 PageQuery 对齐） */
export interface WorkflowPageQuery {
  pageNum?: number
  pageSize?: number
}

export type { PageResult }

// ── 流程分类 ──

export interface CategoryVO {
  id: string
  code: string
  name: string
  sort?: number
  status?: number
  remark?: string
  createTime?: string
}

export interface CategorySaveRequest {
  code: string
  name: string
  sort?: number
  status?: number
  remark?: string
}

// ── 流程定义 ──

export interface ProcessDefinitionVO {
  id: string
  key: string
  name?: string
  version: number
  deploymentId?: string
  category?: string
  suspended: boolean
  deploymentTime?: string
}

// ── 流程实例 ──

export interface ProcessInstanceVO {
  id: string
  processDefinitionId?: string
  processDefinitionKey?: string
  processDefinitionName?: string
  businessKey?: string
  startUserId?: string
  startUserName?: string
  startTime?: string
  endTime?: string
  ended: boolean
  suspended: boolean
  title?: string
  variables?: Record<string, unknown>
}

export interface ProcessStartRequest {
  processDefinitionKey: string
  businessKey?: string
  title?: string
  variables?: Record<string, unknown>
}

export interface ProcessCancelRequest {
  reason?: string
}

export interface ActivityTraceVO {
  activityId?: string
  activityName?: string
  activityType?: string
  assignee?: string
  startTime?: string
  endTime?: string
  durationInMillis?: number
  comment?: string
}

export interface ProcessHighlightVO {
  processInstanceId?: string
  processDefinitionId?: string
  activeActivityIds?: string[]
  finishedActivityIds?: string[]
  bpmnXml?: string
}

// ── 任务 ──

export interface TaskVO {
  id: string
  name?: string
  processInstanceId?: string
  processDefinitionId?: string
  processDefinitionKey?: string
  businessKey?: string
  assignee?: string
  assigneeName?: string
  formKey?: string
  createTime?: string
  endTime?: string
  title?: string
}

export interface TaskCompleteRequest {
  comment?: string
  variables?: Record<string, unknown>
}

export interface TaskRejectRequest {
  comment?: string
  targetActivityId?: string
}

export interface TaskTransferRequest {
  targetUserId: string
  comment?: string
}

// ── 流程模型 ──

export interface ModelVO {
  id: string
  modelKey: string
  name: string
  categoryCode?: string
  bpmnXml?: string
  version?: number
  remark?: string
  updateTime?: string
}

export interface ModelSaveRequest {
  modelKey: string
  name: string
  categoryCode?: string
  bpmnXml: string
  remark?: string
}

// ── 请假 ──

export interface LeaveVO {
  id: string
  userId: string
  username?: string
  days: number | string
  reason: string
  startDate: string
  endDate: string
  status?: number
  processInstanceId?: string
  createTime?: string
}

export interface LeaveCreateRequest {
  days: number
  reason: string
  startDate: string
  endDate: string
}

/** 请假状态：1审批中 2通过 3驳回 4撤销 */
export const LEAVE_STATUS = {
  PENDING: 1,
  APPROVED: 2,
  REJECTED: 3,
  CANCELLED: 4,
} as const

export const LEAVE_STATUS_LABEL: Record<number, string> = {
  1: '审批中',
  2: '已通过',
  3: '已驳回',
  4: '已撤销',
}

// ── 工作流身份（auth 模块） ──

export interface WorkflowUserVO {
  userId: string
  username: string
  nickname?: string
  email?: string
  status?: number
}

// ── 引擎探活 ──

export interface WorkflowEngineInfoVO {
  engineName?: string
  version?: string
  asyncExecutorActive?: boolean
}
