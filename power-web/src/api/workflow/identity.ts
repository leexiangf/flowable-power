import type { ApiResult } from '@/types/api'
import type { WorkflowUserVO } from '@/types/workflow'
import { request, unwrap } from '../request'

export function fetchWorkflowUser(userId: string) {
  return unwrap(request.get<ApiResult<WorkflowUserVO>>(`/auth/workflow/users/${userId}`))
}

export function fetchUserRoleCodes(userId: string) {
  return unwrap(request.get<ApiResult<string[]>>(`/auth/workflow/users/${userId}/roles`))
}

export function fetchUserIdsByRole(roleCode: string) {
  return unwrap(
    request.get<ApiResult<string[]>>(`/auth/workflow/roles/${roleCode}/user-ids`),
  )
}

export function fetchUsersByRole(roleCode: string) {
  return unwrap(
    request.get<ApiResult<WorkflowUserVO[]>>(`/auth/workflow/roles/${roleCode}/users`),
  )
}
