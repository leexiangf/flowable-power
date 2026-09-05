import type { ApiResult } from '@/types/api'
import type { ProcessInstanceVO } from '@/types/workflow'
import { request, unwrap } from '../request'

export interface CountersignStartRequest {
  countersignUserIds: string[]
  title?: string
}

export function startCountersignOr(data: CountersignStartRequest) {
  return unwrap(request.post<ApiResult<ProcessInstanceVO>>('/workflow/countersign/or', data))
}

export function startCountersignSeq(data: CountersignStartRequest) {
  return unwrap(request.post<ApiResult<ProcessInstanceVO>>('/workflow/countersign/seq', data))
}
