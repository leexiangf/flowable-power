import type { ApiResult } from '@/types/api'
import type { LeaveCreateRequest, LeaveVO, PageResult, WorkflowPageQuery } from '@/types/workflow'
import { request, unwrap } from '../request'

export function createLeave(data: LeaveCreateRequest) {
  return unwrap(request.post<ApiResult<LeaveVO>>('/workflow/leave', data))
}

export function fetchLeaveDetail(id: string) {
  return unwrap(request.get<ApiResult<LeaveVO>>(`/workflow/leave/${id}`))
}

export function fetchMyLeaves(params: WorkflowPageQuery) {
  return unwrap(request.get<ApiResult<PageResult<LeaveVO>>>('/workflow/leave/mine', { params }))
}
