import type { ApiResult } from '@/types/api'
import type {
  PageResult,
  TaskCompleteRequest,
  TaskRejectRequest,
  TaskTransferRequest,
  TaskVO,
  WorkflowPageQuery,
} from '@/types/workflow'
import { request, unwrap } from '../request'

export function fetchTodoTasks(params: WorkflowPageQuery) {
  return unwrap(request.get<ApiResult<PageResult<TaskVO>>>('/workflow/tasks/todo', { params }))
}

export function fetchDoneTasks(params: WorkflowPageQuery) {
  return unwrap(request.get<ApiResult<PageResult<TaskVO>>>('/workflow/tasks/done', { params }))
}

export function claimTask(taskId: string) {
  return unwrap(request.post<ApiResult<void>>(`/workflow/tasks/${taskId}/claim`))
}

export function unclaimTask(taskId: string) {
  return unwrap(request.post<ApiResult<void>>(`/workflow/tasks/${taskId}/unclaim`))
}

export function completeTask(taskId: string, data?: TaskCompleteRequest) {
  return unwrap(
    request.post<ApiResult<void>>(`/workflow/tasks/${taskId}/complete`, data ?? {}),
  )
}

export function rejectTask(taskId: string, data?: TaskRejectRequest) {
  return unwrap(request.post<ApiResult<void>>(`/workflow/tasks/${taskId}/reject`, data ?? {}))
}

export function transferTask(taskId: string, data: TaskTransferRequest) {
  return unwrap(request.post<ApiResult<void>>(`/workflow/tasks/${taskId}/transfer`, data))
}
