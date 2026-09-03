import type { ApiResult } from '@/types/api'
import type { LeaveCreateRequest, LeaveVO } from '@/types/workflow'
import { request, unwrap } from '../request'

export function createLeave(data: LeaveCreateRequest) {
  return unwrap(request.post<ApiResult<LeaveVO>>('/workflow/leave', data))
}

export function fetchLeaveDetail(id: string) {
  return unwrap(request.get<ApiResult<LeaveVO>>(`/workflow/leave/${id}`))
}
